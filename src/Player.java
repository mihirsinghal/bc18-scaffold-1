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

		earthWidth = (int) earthMap.getWidth();
		marsWidth = (int) marsMap.getWidth();
		earthHeight = (int) earthMap.getHeight();
		marsHeight = (int) marsMap.getHeight();

		long[][] earthKarb = new long[earthHeight][earthWidth];
		long[][] marsKarb = new long[marsHeight][marsWidth];

		while (true) {
			if(gc.round() % 100 == 0) {
				System.out.println("Round: " + gc.round());
				System.out.println("K15: " + gc.karbonite());
			}
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


		}

	}

	static void defaultEarthWorkerAction(Unit unit) {

		for(Direction dir : directions) {
			if(tryHarvest(unit, dir)) break;
		}
		tryMoveRandom(unit);
		tryReplicateRandom(unit);

	}

	static boolean tryHarvest(Unit unit, Direction direction) { // id must be id of worker
		int id = unit.id();
		if (gc.canHarvest(id, direction)) {
			long k15 = gc.karbonite();
			gc.harvest(id, direction);
			k15 = gc.karbonite() - k15;
			System.out.println("Harvested: " + k15 + " K15!");
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
		Collections.shuffle(directions, rand);
	}


}