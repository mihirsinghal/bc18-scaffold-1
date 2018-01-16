// import the API.
// See xxx for the javadocs.

import bc.*;
import java.util.*;

public class Player {

	static GameController gc;
	static PlanetMap earthMap, marsMap;
	static int earthWidth, marsWidth, earthHeight, marsHeight;
	static Random rand = new Random(0xbeef);
	static ArrayList<Direction> directions = new ArrayList<Direction>();
	static ResearchInfo researchInfo;
	static AsteroidPattern asteroidPattern;
	static long[][] earthKarb, marsKarb, marsTime;

	public static void main(String[] args) {
		// You can use other files in this directory, and in subdirectories.
		// Extra extra = new Extra(27);
		// System.out.println(extra.toString());

		// MapLocation is a data structure you'll use a lot.
		// MapLocation loc = new MapLocation(Planet.Earth, 10, 20);
		// System.out.println("loc: " + loc + ", one step to the Northwest: " + loc.add(Direction.Northwest));
		// System.out.println("loc sd: " + loc.getX());

		// One slightly weird thing: some methods are currently static methods on a static class called bc.
		// This will eventually be fixed :/

		// System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));
		// for (Direction dir : Direction.values()) {
		// 	System.out.println(dir);
		// }

		for (Direction dir : Direction.values()) {
			// if (dir != Direction.Center) {
			directions.add(dir);
			// }
		}

		// Connect to the manager, starting the game
		gc = new GameController();

		// Direction is a normal java enum.
		// Direction[] directions = Direction.values();

		earthMap = gc.startingMap(Planet.Earth);
		marsMap = gc.startingMap(Planet.Mars);
		researchInfo = gc.researchInfo();
		asteroidPattern = gc.asteroidPattern();

		earthWidth = (int) earthMap.getWidth();
		marsWidth = (int) marsMap.getWidth();
		earthHeight = (int) earthMap.getHeight();
		marsHeight = (int) marsMap.getHeight();

		earthKarb = new long[earthHeight][earthWidth];
		marsKarb = new long[marsHeight][marsWidth];
		marsTime = new long[marsHeight][marsWidth];

		for(int i = 0; i < earthHeight; i++) {
			for(int j = 0; j < earthWidth; j++) {
				MapLocation curLoc = new MapLocation(Planet.Earth, i, j);
				earthKarb[i][j] = earthMap.initialKarboniteAt(curLoc);
			}
		}

		for(long round = 1; round <= 1000; round++) {
			if(asteroidPattern.hasAsteroid(round)) {
				AsteroidStrike strike = asteroidPattern.asteroid(round);
				MapLocation curLoc = strike.getLocation();
				int x = curLoc.getX(), y = curLoc.getY();
				marsKarb[x][y] = strike.getKarbonite();
				marsTime[x][y] = round;
			}
		}

		if(gc.team() == Team.Red) processResearch();

		while (true) {
			if(gc.round() % 100 == 0) {
				System.out.println("Round: " + gc.round());
				System.out.println("Karbonite: " + gc.karbonite());
				System.out.println("Research");
				System.out.println("--------");
				for(UnitType type : UnitType.values()) {
					System.out.println(type + " at level " + researchInfo.getLevel(type));
				}
				if(researchInfo.hasNextInQueue())
					System.out.println("Next up: " + researchInfo.nextInQueue());
				System.out.println(researchInfo.toJson());
			}

			// TODO exception handling (try/catch loop)
			if (gc.planet() == Planet.Earth) {
				doEarthTurn();
			} else {
				assert gc.planet() == Planet.Mars;
				doMarsTurn();
			}

			// Submit the actions we've done, and wait for our next turn.
			gc.nextTurn();
		}
	}

	static void doEarthTurn() {

		if (gc.round() == 1) {

			VecUnit units = gc.myUnits();
			for (int i = 0; i < units.size(); i++) {
				Unit unit = units.get(i);
				assert unit.unitType() == UnitType.Worker;
				tryReplicateRandom(unit); // assumes that there are only workers to start
			}

		} else {

			VecUnit units = gc.myUnits();
			for (int i = 0; i < units.size(); i++) {
				Unit unit = units.get(i);

				switch (unit.unitType()) {

					case Worker:
						defaultEarthWorkerAction(unit);
						break;
					case Knight:
						break;
					case Ranger:
						break;
					case Mage:
						break;
					case Healer:
						break;
					case Factory:
						break;
					case Rocket:
						break;
					default:
						break;

				}

			}

		}

	}

	static void doMarsTurn() {

		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);

			switch (unit.unitType()) {

				case Worker:
					defaultEarthWorkerAction(unit); // TODO use the asteroid pattern
					break;
				case Knight:
					break;
				case Ranger:
					break;
				case Mage:
					break;
				case Healer:
					break;
				case Factory:
					break;
				case Rocket:
					break;
				default:
					break;

			}

			if (gc.getTimeLeftMs() < 25) {
				break;
			}

		}

	}

	static void defaultEarthWorkerAction(Unit unit) {

		if (unit.location().isOnMap()) {
//			for (Direction dir : directions) {
//				if (tryHarvest(unit, dir)) break;
//			}
			harvestMax(unit);
			tryMoveRandom(unit);
			tryReplicateRandom(unit);
		}

	}

	static boolean tryHarvest(Unit unit, Direction direction) { // id must be id of worker
		int id = unit.id();
		if (gc.canHarvest(id, direction)) {
			long amt = gc.karbonite();
			gc.harvest(id, direction);
			amt = gc.karbonite() - amt;
			// System.out.println("Harvested " + amt + " Karbonite!");
			return true;
		} else {
			return false;
		}
	}

	static boolean harvestMax(Unit unit) {
		int id = unit.id();
		MapLocation loc = unit.location().mapLocation();
		long maxk = 0;
		Direction maxdir = null;
		for (Direction dir: directions) {
			if (gc.canSenseLocation(loc.add(dir)) && (gc.karboniteAt(loc.add(dir)) > maxk)) {
				maxk = gc.karboniteAt(loc.add(dir));
				maxdir = dir;
			}
		}
		if (maxdir != null) {
			return tryHarvest(unit, maxdir);
		}
		return false;
	}

	static boolean tryMove(Unit unit, Direction direction) {
		int id = unit.id();
		if ((unit.movementHeat() < 10) && gc.canMove(id, direction)) {
			gc.moveRobot(id, direction);
			return true;
		} else {
			return false;
		}
	}

	static boolean tryMoveRandom(Unit unit) {
		shuffleDirections();
		for (Direction dir : directions) {
			if (tryMove(unit, dir)) {
				return true;
			}
		}
		return false;
	}

	static boolean tryReplicate(Unit unit, Direction direction) {
		int id = unit.id();
		if (gc.canReplicate(id, direction)) {
			gc.replicate(id, direction);
			return true;
		} else {
			return false;
		}
	}

	static boolean tryReplicateRandom(Unit unit) {
		shuffleDirections();
		for (Direction dir : directions) {
			if (tryReplicate(unit, dir)) {
				return true;
			}
		}
		return false;
	}

	static void shuffleDirections() {
		Collections.shuffle(directions, rand);
	}

	static void processResearch() {
		// Using these build tree each time is decent
		// We can try adding upgrades based on current units
		// But that's not too important right now
		gc.queueResearch(UnitType.Worker); // 25 - additional karbonite harvesting
		gc.queueResearch(UnitType.Worker); // 75 - faster building contruction and healing
		gc.queueResearch(UnitType.Rocket); // 100 - allows us to build rockets
		gc.queueResearch(UnitType.Worker); // 75 - faster building contruction and healing
		gc.queueResearch(UnitType.Worker); // 75 - faster building contruction and healing
		// with Basics, Total: 350
		gc.queueResearch(UnitType.Knight); // 25 - better armor
		gc.queueResearch(UnitType.Knight); // 75 - better armor
		// use javelin if we want to continue creating knights
		// but I think that we'd be better off rushing Ranger
		/*
		gc.queueResearch(UnitType.Knight); // 150 - unlocks javelin
		*/
		// with Exploration, Total: 450
		gc.queueResearch(UnitType.Ranger); // 25 - less movement cooldown
		gc.queueResearch(UnitType.Ranger); // 100 - increased vision
		gc.queueResearch(UnitType.Ranger); // 200 - unlimited range
		// with long-range sniping, Total: 775
		// current strategy is to build just enough tanky knights to explore the map
		// then use use knights, but mainly rangers, for damage on any target
		// alternatives:
		// mage tree for high DPS melee combat
		/*
		gc.queueResearch(UnitType.Mage); // 25 - more damage for mages
		gc.queueResearch(UnitType.Mage); // 75 - more damage for mages
		gc.queueResearch(UnitType.Mage); // 100 - more damage for mages
		*/
	}

}