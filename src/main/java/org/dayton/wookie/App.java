package org.dayton.wookie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

public class App {
    /**
     * almost zero, for precision problems
     */
    private static final double ALMOST_ZERO = Math.pow(10, -10);
    private static final int[] PRIORITY = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
    private static Properties properties;
    private static final class Properties {
		double[] teamMember1 = { 30, 10, 20 };
		double[] teamMember2 = { 15, 20, 10 };
		double[] teamMember3 = { 50, 40, 25 };
		double[] teamMember4 = { 45, 40, 15 };
		double[][] teamMembers = {teamMember1, teamMember2, teamMember3, teamMember4};
		double[][][] possibleDrillsArray = {{{5, 10, 10}, {20, 10, 5}, {10, 10, 10}, {25, 5, 5}},
			{{5, 10, 10}, {20, 10, 5}, {10, 10, 10}, {25, 5, 5}},
			{{25, 5, 40}, {40, 40, 40}, {20, 40, 40}, {40, 40, 50}}
			};
		List<double[][]> possibleDrills = Arrays.asList(possibleDrillsArray);
		final int[] timeForDrill = {3, 2, 3};
		final int skillsPerTeam = 3;
		final int numberOfTeams = 4;
		int minimum = 70;
		Random random = new Random();
		double decay = .95;
    }

    public static void main(String[] args) throws IOException {
    	String fileName = args.length == 0 ? "defaults.json" : args[0];
    	Gson gson = new Gson();
    	properties = new Properties();
    	properties = gson.fromJson(new String(Files.readAllBytes(Paths.get(fileName)), "UTF-8"), Properties.class);
    	System.out.println(gson.toJson(properties));
        Steps bestPath = bestPath();
        System.out.println("Best path: " + StringUtils.join(bestPath) + ", time taken: " + bestPath.getTimeTaken());
    }

    private static void reinitialize() {
    }

    private static void randomize() {
        System.out.println("Team members:");
        for (double[] teamMember : properties.teamMembers) {
            System.out.print("Team member 1: ");
            for (int i = 0; i < teamMember.length; i++) {
                teamMember[i] = properties.random.nextInt(46) + 5; // 50 is max skill per team member
                System.out.print(teamMember[i] + ",");
            }
            System.out.println();
        }

        System.out.println("Drills:");
        for (double[][] possibleDrill : properties.possibleDrillsArray) {
            for (double[] impactPerSkill : possibleDrill) {
                for (int i = 0; i < impactPerSkill.length; i++) {
                    impactPerSkill[i] = properties.random.nextInt(36) + 5; // maximum of 40 impact per drill skill
                }
            }
        }

    }

    public static double distanceInAMinute(double value, int times, double original) {
        return properties.minimum - original - decay(value, times);
    }

    public static double decay(double value, int times) {
        return Math.pow(value, Math.pow(properties.decay, times));
    }

    public static Steps bestPath() {
        Steps bestSteps = new Steps();
        int[] drillsUsedCounts = new int[properties.possibleDrills.size()];
        for (int i = 0; i < properties.possibleDrills.size(); i++) {
            drillsUsedCounts[i] = 0;
        }

        while (!bestSteps.status.isDone()) {
            int bestDrill = bestDrill(drillsUsedCounts, bestSteps.status);
            drillsUsedCounts[bestDrill]++;
            bestSteps.add(bestDrill);
        }
        return bestSteps;
    }

    public static double calculateAverageDistancePerMinute(int drillsUsedCount, int timeForDrill, TeamStatus status, double[][] drill) {
        double averageDistancePerMinute = 0d;

        for (int j = 0; j < properties.numberOfTeams; j++) {
            System.out.print("{");
            for (int k = 0; k < properties.skillsPerTeam; k++) {
                double[] drillForPosition = drill[j];
                double skill = drillForPosition[k];
                double currentDistance = distanceInAMinute(skill, drillsUsedCount, status.teamBySkill[j][k]);
                averageDistancePerMinute += Math.max(0, currentDistance * timeForDrill);
                System.out.printf("%.2f,", distanceInAMinute(skill, drillsUsedCount, status.teamBySkill[j][k]));
            }
            System.out.print("}");
        }

        return averageDistancePerMinute /= properties.numberOfTeams * properties.skillsPerTeam;
    }

    public static int bestDrill(int[] drillsUsedCounts, TeamStatus status) {
        double lowestDistance = Double.MAX_VALUE;
        int bestDrill = 0;
        for (int i = 0; i < properties.possibleDrills.size(); i++) {
            System.out.print("Drill " + i + ": {");
            double averageDistancePerMinute = calculateAverageDistancePerMinute(drillsUsedCounts[i], properties.timeForDrill[i], status, properties.possibleDrills.get(i));
            System.out.print("}" + averageDistancePerMinute + "\n");
			// if so, we've found one that will finish the practice, go to special case
            // sloppy check for 0, since we could have precision problems
            if (averageDistancePerMinute < ALMOST_ZERO) {
                System.out.println("\nFound end. Will now find best ending drill.");
                return bestEndDrill(drillsUsedCounts, status);
            }
            if (averageDistancePerMinute < lowestDistance) {
                lowestDistance = averageDistancePerMinute;
                bestDrill = i;
            }
        }
        System.out.print('\n');
        return bestDrill;
    }

    /**
     * For the last step, give them the best we can taking into account their
     * higher priority skills.
     */
    public static int bestEndDrill(int[] drillsUsedCounts, TeamStatus status) {
        // stub
        double[][] bestDrillResults = newDrillResult();
        int[][] bestDrillNumbers = newDrillNumber();
        for (int i = 0; i < properties.possibleDrills.size(); i++) {
            System.out.print("drill " + i + ": {");
            double averageDistancePerMinute = calculateAverageDistancePerMinute(drillsUsedCounts[i], properties.timeForDrill[i], status, properties.possibleDrills.get(i));

            // this is a candidate for the best ending
            if (averageDistancePerMinute < ALMOST_ZERO) {
                for (int j = 0; j < properties.numberOfTeams; j++) {
                    for (int k = 0; k < properties.skillsPerTeam; k++) {
                        double[] drillForPosition = properties.possibleDrills.get(i)[j];
                        double skill = drillForPosition[k];
                        double increase = decay(skill, drillsUsedCounts[i]);
                        if (increase > bestDrillResults[j][k] && increase > ALMOST_ZERO) {
                            bestDrillResults[j][k] = increase;
                            bestDrillNumbers[j][k] = i;
                        }
                    }
                }
            }
            System.out.println("}");
        }

        int bestDrill = 0;

        for (int i = 0; i < PRIORITY.length; i++) {
            double bestDrillResult = 0;
            for (int j = 0; j < properties.numberOfTeams; j++) {
                for (int k = 0; k < properties.skillsPerTeam; k++) {
                    if (bestDrillResults[j][k] > ALMOST_ZERO && bestDrillResults[j][k] > bestDrillResult) {
                        bestDrill = bestDrillNumbers[j][k];
                        bestDrillResult = bestDrillResults[j][k];
                    }
                }
            }
        }

        return bestDrill;
    }

    private static class Steps extends ArrayList<Integer> {

        int timeTaken = 0;
        TeamStatus status = new TeamStatus();

        Steps() {
            super();
        }

        public boolean add(Integer toAdd) {
            timeTaken += properties.timeForDrill[toAdd];
            int drillCount = 0;
            for (Integer step : this) {
                if (step.equals(toAdd)) {
                    drillCount++;
                }
            }
            status.add(stepAvailable(toAdd, drillCount));
            return super.add(toAdd);
        }

        public double[][] stepAvailable(int drillNumber, int numberOfTimesUsed) {
            double[][] drill = properties.possibleDrills.get(drillNumber);
            double[][] drillResult = newDrillResult();

            for (int i = 0; i < drill.length; i++) {
                for (int j = 0; j < drill[i].length; j++) {
                    drillResult[i][j] = decay(drill[i][j], numberOfTimesUsed);
                }
            }

            return drillResult;
        }

        public int getTimeTaken() {
            return timeTaken;
        }
    }

    private static double[][] newDrillResult() {
        double[][] drillResult = new double[properties.numberOfTeams][];

        for (int i = 0; i < properties.numberOfTeams; i++) {
            drillResult[i] = new double[properties.skillsPerTeam];
            for (int j = 0; j < properties.skillsPerTeam; j++) {
                drillResult[i][j] = 0;
            }
        }

        return drillResult;
    }

    private static int[][] newDrillNumber() {
        int[][] drillNumber = new int[properties.numberOfTeams][];

        for (int i = 0; i < properties.numberOfTeams; i++) {
            drillNumber[i] = new int[properties.skillsPerTeam];
            for (int j = 0; j < properties.skillsPerTeam; j++) {
                drillNumber[i][j] = 0;
            }
        }

        return drillNumber;
    }

    private static class TeamStatus {

        Double[][] teamBySkill;

        TeamStatus() {
            teamBySkill = new Double[properties.numberOfTeams][];
            for (int i = 0; i < properties.numberOfTeams; i++) {
                teamBySkill[i] = new Double[properties.skillsPerTeam];
                for (int j = 0; j < properties.skillsPerTeam; j++) {
                    teamBySkill[i][j] = properties.teamMembers[i][j];
                }
            }
        }

        void add(double[][] drillResult) {
            for (int i = 0; i < teamBySkill.length; i++) {
                for (int j = 0; j < teamBySkill[i].length; j++) {
                    teamBySkill[i][j] += drillResult[i][j];
                }
            }
        }

        public boolean isDone() {
            for (Double[] skillset : teamBySkill) {
                for (double skill : skillset) {
                    if (skill < properties.minimum) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
