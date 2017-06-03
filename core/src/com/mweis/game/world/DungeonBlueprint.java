package com.mweis.game.world;

import com.badlogic.gdx.utils.Array;
import com.mweis.game.world.graph.DGraph;

class DungeonBlueprint {
	
	private Room start;
	private Room end;
	private Array<Room> rooms;
	private Array<Room> corridors;
	private Array<Room> halls;
	private DGraph<Room> criticalRoomGraph;
	private int minSideLength;
	private int maxSideLength;
	private int hallWidth;
	private float minRatio;
	private float maxRatio;
	
	DungeonBlueprint(Room start, Room end, Array<Room> rooms, Array<Room> corridors, Array<Room> halls, DGraph<Room> criticalRoomGraph,
			int minSideLength, int maxSideLength, int hallWidth, float minRatio, float maxRatio) {
		
		this.setStart(start);
		this.setEnd(end);
		this.setRooms(rooms);
		this.setCorridors(corridors);
		this.setHalls(halls);
		this.setCriticalRoomGraph(criticalRoomGraph);
		this.setMinSideLength(minSideLength);
		this.setMaxSideLength(maxSideLength);
		this.setHallWidth(hallWidth);
		this.setMinRatio(minRatio);
		this.setMaxRatio(maxRatio);
	}
	
	public Room getStart() {
		return start;
	}
	
	public DungeonBlueprint setStart(Room start) {
		this.start = start;
		return this;
	}

	public Room getEnd() {
		return end;
	}

	public DungeonBlueprint setEnd(Room end) {
		this.end = end;
		return this;
	}

	public Array<Room> getRooms() {
		return rooms;
	}

	public DungeonBlueprint setRooms(Array<Room> rooms) {
		this.rooms = rooms;
		return this;
	}

	public Array<Room> getCorridors() {
		return corridors;
	}

	public DungeonBlueprint setCorridors(Array<Room> corridors) {
		this.corridors = corridors;
		return this;
	}

	public Array<Room> getHalls() {
		return halls;
	}

	public DungeonBlueprint setHalls(Array<Room> halls) {
		this.halls = halls;
		return this;
	}

	public DGraph<Room> getCriticalRoomGraph() {
		return criticalRoomGraph;
	}

	public DungeonBlueprint setCriticalRoomGraph(DGraph<Room> criticalRoomGraph) {
		this.criticalRoomGraph = criticalRoomGraph;
		return this;
	}

	public int getMinSideLength() {
		return minSideLength;
	}

	public DungeonBlueprint setMinSideLength(int minSideLength) {
		this.minSideLength = minSideLength;
		return this;
	}

	public int getMaxSideLength() {
		return maxSideLength;
	}

	public DungeonBlueprint setMaxSideLength(int maxSideLength) {
		this.maxSideLength = maxSideLength;
		return this;
	}

	public int getHallWidth() {
		return hallWidth;
	}

	public DungeonBlueprint setHallWidth(int hallWidth) {
		this.hallWidth = hallWidth;
		return this;
	}

	public float getMinRatio() {
		return minRatio;
	}

	public DungeonBlueprint setMinRatio(float minRatio) {
		this.minRatio = minRatio;
		return this;
	}

	public float getMaxRatio() {
		return maxRatio;
	}

	public DungeonBlueprint setMaxRatio(float maxRatio) {
		this.maxRatio = maxRatio;
		return this;
	}
	
}
