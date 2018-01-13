// import the API.
// See xxx for the javadocs.

import bc.*;

import java.util.*;

public class Player {

	static GameController gc;
	static PlanetMap earthMap, marsMap;
	static int earthWidth, marsWidth, earthHeight, marsHeight;
	static Random r = new Random(0xbeef);
	static ArrayList<Direction> directions = new ArrayList<Direction>();

	public static void main(String[] args) {
		// You can use other files in this directory, and in subdirectories.
		// Extra extra = new Extra(27);
		// System.out.println(extra.toString());

		// MapLocation is a data structure you'll use a lot.
//        MapLocation loc = new MapLocation(Planet.Earth, 10, 20);
//        System.out.println("loc: " + loc + ", one step to the Northwest: " + loc.add(Direction.Northwest));
//        System.out.println("loc sd: " + loc.getX());

		// One slightly weird thing: some methods are currently static methods on a static class called bc.
		// This will eventually be fixed :/
		System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));
		for (Direction dir : Direction.values()) {
			System.out.println(dir);
		}

		for (Direction dir : Direction.values()) {
//			if (dir != Direction.Center) {
			directions.add(dir);
//			}
		}

		// Connect to the manager, starting the game
		gc = new GameController();

		// Direction is a normal java enum.
		Direction[] directions = Direction.values();

		earthMap = gc.startingMap(Planet.Earth);
		marsMap = gc.startingMap(Planet.Mars);

		earthWidth = (int) earthMap.getWidth();
		marsWidth = (int) marsMap.getWidth();
		earthHeight = (int) earthMap.getHeight();
		earthHeight = (int) marsMap.getHeight();

		long[][] earthKarb = new long[(int) earthMap.getHeight()][(int) earthMap.getWidth()];
		long[][] marsKarb = new long[(int) marsMap.getHeight()][(int) marsMap.getWidth()];


		while (true) {
			System.out.println("Current round: " + gc.round());
			// VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.

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

	static void doEarthTurn() { // TODO at end of turn all workers should harvest if possible

		if (gc.round() == 1) {

			VecUnit units = gc.myUnits();
			for (int i = 0; i < units.size(); i++) {
				Unit unit = units.get(i);
				tryReplicateRandom(unit);
			}

		} else {

			VecUnit units = gc.myUnits();
			for (int i = 0; i < units.size(); i++) {
				Unit unit = units.get(i);

				switch (unit.unitType()) {

					case Worker:
						doWorkerAction(unit);

				}

			}

		}

	}

	static void doMarsTurn() {

		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);


		}

	}

	static void doWorkerAction(Unit unit) {

		tryMoveRandom(unit);
		tryReplicateRandom(unit);

	}

	static boolean tryHarvest(Unit unit, Direction direction) { // id must be id of worker
		int id = unit.id();
		if (gc.canHarvest(id, direction)) {
			gc.harvest(id, direction);
			return true;
		} else {
			return false;
		}
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
		Collections.shuffle(directions, r);
	}


}