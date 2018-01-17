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
	// static ArrayList<Direction> NSEW = new ArrayList<Direction>();
	static ResearchInfo researchInfo;
	static AsteroidPattern asteroidPattern;
	static long[][] earthKarb, marsKarb, marsTime;
	static long totalRocketCost;
	static ArrayList<MapLocation> blueprintLocations;
	static ArrayList<Direction>[][] go;

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
		// NSEW.add(Direction.North);
		// NSEW.add(Direction.South);
		// NSEW.add(Direction.East);
		// NSEW.add(Direction.West);

		// Connect to the manager, starting the game
		gc = new GameController();

		// Direction is a normal java enum.
		// Direction[] directions = Direction.values();

		earthMap = gc.startingMap(Planet.Earth);
		marsMap = gc.startingMap(Planet.Mars);
		asteroidPattern = gc.asteroidPattern();

		earthWidth = (int) earthMap.getWidth();
		marsWidth = (int) marsMap.getWidth();
		earthHeight = (int) earthMap.getHeight();
		marsHeight = (int) marsMap.getHeight();

		earthKarb = new long[earthWidth][earthHeight];
		marsKarb = new long[marsWidth][marsHeight];
		marsTime = new long[marsWidth][marsHeight];

		totalRocketCost = bc.bcUnitTypeBlueprintCost(UnitType.Rocket);

		for(int i = 0; i < earthWidth; i++) {
			for(int j = 0; j < earthHeight; j++) {
				MapLocation curLoc = new MapLocation(Planet.Earth, i, j);
				earthKarb[i][j] = earthMap.initialKarboniteAt(curLoc);
			}
		}

		// Start of BFS Code

		int[][] dist = new int[earthWidth][earthHeight];
		Direction[][] trace = new Direction[earthWidth][earthHeight];
		go = new ArrayList[earthWidth][earthHeight];
		for(int i = 0; i < earthWidth; i++) {
			for(int j = 0; j < earthHeight; j++) {
				dist[i][j] = -1;
				go[i][j] = new ArrayList<Direction>();
			}
		}
		Queue<MapLocation> bfs = new LinkedList<MapLocation>();
		int bfsSize = 0;

		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			// assert unit.unitType() == UnitType.Worker;
			MapLocation cur = unit.location().mapLocation();
			if(bfs.offer(cur)) bfsSize++;
			dist[cur.getX()][cur.getY()] = 0;
		}

		while(bfsSize > 0) {
			MapLocation cur = bfs.poll();
			if(cur == null) break; // just in case
			bfsSize--;
			int cx = cur.getX(), cy = cur.getY();
			for(Direction dir : directions) {
				MapLocation nxt = cur.add(dir);
				if(earthMap.onMap(nxt) == false) continue;
				if(earthMap.isPassableTerrainAt(nxt) == 0) continue; // should be boolean?
				int x = nxt.getX(), y = nxt.getY();
				if(dist[x][y] != -1) continue;
				dist[x][y] = dist[cx][cy] + 1;
				trace[x][y] = bc.bcDirectionOpposite(dir);
				if(bfs.offer(nxt)) bfsSize++;
			}
		}

		boolean vis[][] = new boolean[earthWidth][earthHeight];
		for(int i = 0; i < earthWidth; i++) {
			for(int j = 0; j < earthHeight; j++) {
				// condition for if we want to collect from this cell
				// for now we will get any cell that has karbonite
				if(earthKarb[i][j] > 0) {
					MapLocation cur = new MapLocation(Planet.Earth, i, j);
					while(true) {
						int x = cur.getX(), y = cur.getY();
						if(vis[x][y]) break;
						vis[x][y] = true;
						if(trace[x][y] == null) break;
						MapLocation par = cur.add(trace[x][y]);
						int px = par.getX(), py = par.getY();
						go[px][py].add(bc.bcDirectionOpposite(trace[x][y]));
						System.out.println("(" + px + ", " + py + ") --> (" + x + ", " + y + ")");
						cur = par;
					}
				}
			}
		}

		// End of BFS Code

		// Distances
		for(int j = earthHeight - 1; j >= 0; j--) {
			for(int i = 0; i < earthWidth; i++) {
				System.out.print(dist[i][j] + " ");
			}
			System.out.println();
		}

		// Visualizing the Directions
		for(int j = earthHeight - 1; j >= 0; j--) {
			for(int i = 0; i < earthWidth; i++) {
				Direction d = trace[i][j];
				if(d == Direction.North) System.out.print("| ");
				else if(d == Direction.Northwest) System.out.print("\\ ");
				else if(d == Direction.Northeast) System.out.print("/ ");
				else if(d == Direction.East) System.out.print("- ");
				else System.out.print("  ");
			}
			System.out.println();
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

		processResearch();

		while (true) {

			researchInfo = gc.researchInfo();
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
				// System.out.println(researchInfo.toJson());
			}

			// TODO exception handling (try/catch loop)
			if (gc.planet() == Planet.Earth) {
				doEarthTurn();
			} else {
				// assert gc.planet() == Planet.Mars;
				doMarsTurn();
			}

			// Submit the actions we've done, and wait for our next turn.
			gc.nextTurn();
		}
	}

	static void followKarbTest(Unit unit) {
		int id = unit.id();
		MapLocation loc = unit.location().mapLocation();
		int x = loc.getX(), y = loc.getY();
		if(go[x][y].size() == 0) return;
		for(Direction dir : go[x][y]) {
			if(gc.canReplicate(id, dir)) {
				System.out.println("unit " + id + " at x = " + x + " y = " + y);
				System.out.println("there are " + go[x][y].size() + " branches here!");
				System.out.println("replicated in dir " + dir);
				gc.replicate(id, dir);
			}
		}
		// if ((unit.movementHeat() < 10) && gc.canMove(id, dir)) {
		// 	gc.moveRobot(id, dir);
		// }
	}

	static void doEarthTurn() {

		if (gc.round() == 1) {

			VecUnit units = gc.myUnits();
			for (int i = 0; i < units.size(); i++) {
				Unit unit = units.get(i);
				// assert unit.unitType() == UnitType.Worker;
				tryReplicateRandom(unit); // assumes that there are only workers to start
			}

		} else {

			VecUnit units = gc.myUnits();

			blueprintLocations = new ArrayList<MapLocation>();
			for (int i = 0; i < units.size(); i++) {
				Unit unit = units.get(i);
				if ((unit.unitType() == UnitType.Factory) || (unit.unitType() == UnitType.Rocket)) {
					if (unit.structureIsBuilt() == 0) {
						blueprintLocations.add(unit.location().mapLocation());
					}
				}
			}

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
			followKarbTest(unit);
			harvestMax(unit);
			// tryMoveRandom(unit);
			// tryReplicateRandom(unit);
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
		gc.queueResearch(UnitType.Rocket); // 100 - allows us to build rockets
		gc.queueResearch(UnitType.Worker); // 75 - faster building contruction and healing
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