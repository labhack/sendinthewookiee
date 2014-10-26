package org.dayton.wookie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

public class App {
	/** almost zero, for precision problems */
	private static final double ALMOST_ZERO = Math.pow(10, -10);
	private static final int[] PRIORITY = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
	static double[] teamMember1 = {30, 10, 20};
	static double[] teamMember2 = {15, 20, 10};
	static double[] teamMember3 = {50, 40, 25};
	static double[] teamMember4 = {45, 40, 15};
	static double[][] teamMembers = {teamMember1, teamMember2, teamMember3, teamMember4};
	static double[][][] possibleDrillsArray = {{{5, 10, 10}, {20, 10, 5}, {10, 10, 10}, {25, 5, 5}},
									{{5, 10, 10}, {20, 10, 5}, {10, 10, 10}, {25, 5, 5}},	
									{{25, 5, 40}, {40, 40, 40}, {20, 40, 40}, {40, 40, 50}}
									};
	static List<double[][]> possibleDrills = Arrays.asList(possibleDrillsArray);
	static final int[] timeForDrill = {3, 2, 3};
	static final int skillsPerTeam = 3;
	static final int numberOfTeams = 4;
	static int minimum = 70;
	static Random random = new Random();

	public static void main(String[] args) {
		Steps bestPath = bestPath();
		System.out.println("Best path: " + StringUtils.join(bestPath) + ", time taken: " + bestPath.getTimeTaken());
	}

	
	private static void reinitialize() {
	}
	private static void randomize() {
		System.out.println("Team members:");
		for (double[] teamMember: teamMembers) {
			System.out.print("Team member 1: ");
			for (int i = 0; i < teamMember.length; i++) {
				teamMember[i] = random.nextInt(46) + 5; // 50 is max skill per team member
				System.out.print(teamMember[i] + ",");
			}
			System.out.println();
		}

		System.out.println("Drills:");
		for (double[][] possibleDrill: possibleDrillsArray) {
			for (double[] impactPerSkill: possibleDrill) {
				for (int i = 0; i < impactPerSkill.length; i++) {
					impactPerSkill[i] = random.nextInt(36) + 5; // maximum of 40 impact per drill skill
				}
			}
		}
		
		
		
	}

	public static double distanceInAMinute (double value, int times, double original) {
		return minimum - original - decay(value, times);
	}
	
	public static double decay(double value, int times) {
		return Math.pow(value, Math.pow(.95, times));
	}
	
	public static Steps bestPath() {
		Steps bestSteps = new Steps();
		int[] drillsUsedCounts = new int[possibleDrills.size()];
		for (int i = 0; i < possibleDrills.size(); i++) {
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

		for (int j = 0; j < numberOfTeams; j++) {
			System.out.print("{");
			for (int k = 0; k < skillsPerTeam; k++) {
				double[] drillForPosition = drill[j];
				double skill = drillForPosition[k];
				double currentDistance = distanceInAMinute(skill, drillsUsedCount, status.teamBySkill[j][k]);
				averageDistancePerMinute += Math.max(0,  currentDistance *timeForDrill);
				System.out.printf("%.2f,", distanceInAMinute(skill, drillsUsedCount, status.teamBySkill[j][k]));
			}
			System.out.print("}");
		}
		
		return averageDistancePerMinute /= numberOfTeams * skillsPerTeam;
	}
	
	public static int bestDrill(int[] drillsUsedCounts, TeamStatus status) {
		double lowestDistance = Double.MAX_VALUE;
		int bestDrill = 0;
		for (int i = 0; i < possibleDrills.size(); i++) {
			System.out.print("Drill #: " + i + "{");
			double averageDistancePerMinute = calculateAverageDistancePerMinute(drillsUsedCounts[i], timeForDrill[i], status, possibleDrills.get(i));
			System.out.print("}" + averageDistancePerMinute + "\n");
			// if so, we've found one that will finish the practice, go to special case
			// sloppy check for 0, since we could have precision problems
			if (averageDistancePerMinute < ALMOST_ZERO) {
				System.out.println("\nFound end will now find best ending drill.");
				return bestEndDrill(drillsUsedCounts, status);
			}
			if (averageDistancePerMinute < lowestDistance) {
//				System.out.println("drill #: " + i + ", averageDistance: " + averageDistancePerMinute);
				lowestDistance = averageDistancePerMinute;
				bestDrill = i;
			}
		}
		System.out.println();
		return bestDrill;
	}
	
	/**
	 * For the last step, give them the best we can taking into account their higher priority skills.
	 */
	public static int bestEndDrill(int[] drillsUsedCounts, TeamStatus status) {
		// stub
		double[][] bestDrillResults = newDrillResult();
		int[][] bestDrillNumbers = newDrillNumber();
		for (int i = 0; i < possibleDrills.size(); i++) {
			double averageDistancePerMinute = calculateAverageDistancePerMinute(drillsUsedCounts[i], timeForDrill[i], status, possibleDrills.get(i));
			
			// this is a candidate for the best ending
			if (averageDistancePerMinute < ALMOST_ZERO) {
				for (int j = 0; j < numberOfTeams; j++) {
					System.out.print("{");
					for (int k = 0; k < skillsPerTeam; k++) {
						double[] drillForPosition = possibleDrills.get(i)[j];
						double skill = drillForPosition[k];
						double increase = decay(skill, drillsUsedCounts[i]);
						if (increase > bestDrillResults[j][k] && increase > ALMOST_ZERO) {
							bestDrillResults[j][k] = increase;
							bestDrillNumbers[j][k] = i;
						}
					}
					System.out.print("}");
				}
			}
		}

		int bestDrill = 0;

		for (int i = 0; i < PRIORITY.length; i++) {
			double bestDrillResult = 0;
			for (int j = 0; j < numberOfTeams; j++) {
				for (int k = 0; k < skillsPerTeam; k++) {
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
			timeTaken += timeForDrill[toAdd];
			int drillCount = 0;
			for (Integer step: this) {
				if (step.equals(toAdd)) {
					drillCount++;
				}
			}
			status.add(stepAvailable(toAdd, drillCount));
			return super.add(toAdd);
		}
		
		
		public double[][] stepAvailable (int drillNumber, int numberOfTimesUsed) {
			double[][] drill = possibleDrills.get(drillNumber);
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
		double[][] drillResult = new double[numberOfTeams][];

		for (int i = 0; i < numberOfTeams; i++) {
			drillResult[i] = new double[skillsPerTeam];
			for (int j = 0; j < skillsPerTeam; j++) {
				drillResult[i][j] = 0;
			}
		}

		return drillResult;
	}

	private static int[][] newDrillNumber() {
		int[][] drillNumber = new int[numberOfTeams][];

		for (int i = 0; i < numberOfTeams; i++) {
			drillNumber[i] = new int[skillsPerTeam];
			for (int j = 0; j < skillsPerTeam; j++) {
				drillNumber[i][j] = 0;
			}
		}

		return drillNumber;
	}
	private static class TeamStatus {
		Double [][] teamBySkill;
		TeamStatus() {
			teamBySkill = new Double[numberOfTeams][];
			for (int i = 0; i < numberOfTeams; i++) {
				teamBySkill[i] = new Double [skillsPerTeam];
				for (int j = 0; j < skillsPerTeam; j++) {
					teamBySkill[i][j] = teamMembers[i][j];
				}
			}
		}

		void add (double[][] drillResult) {
			for (int i = 0; i < teamBySkill.length; i++) {
				for (int j = 0; j < teamBySkill[i].length; j++) {
					teamBySkill[i][j] += drillResult[i][j];
				}
			}
		}
		public boolean isDone() {
			for (Double[] skillset: teamBySkill) {
				for (double skill: skillset) {
					if (skill < minimum) {
						return false;
					}
				}
			}
			return true;
		}
	}
}
