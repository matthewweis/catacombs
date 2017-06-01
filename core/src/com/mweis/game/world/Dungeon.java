package com.mweis.game.world;

import java.awt.geom.Line2D;

import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.IntMap.Keys;
import com.mweis.game.box2d.Box2dBodyFactory;
import com.mweis.game.util.Constants;
import com.mweis.game.util.Messages;
import com.mweis.game.world.Room.RoomType;
import com.mweis.game.world.graph.DGraph;
import com.mweis.game.world.graph.Edge;


public class Dungeon implements Telegraph {
	
	public World world;
	
	public final int WIDTH, HEIGHT, MIN_SIDE_LENGTH, MAX_SIDE_LENGTH, HALL_WIDTH, CORRIDOR_COUNT, ROOM_COUNT, HALL_COUNT;
	public final float MIN_RATIO, MAX_RATIO;
	
	private Room startRoom, endRoom;
	private Array<Room> noncriticalRooms, criticalRooms, allRooms, halls, dungeon;
	private DGraph<Room> criticalRoomGraph;
	private DGraph<Room> dungeonGraph;
	
	public final int UNITS_PER_PARTITION = 200; // units per square in the spatial partition for vectors -> rooms
	public final int PARTITION_WIDTH; // with in CHUNKS
	private final IntMap<Array<Room>> spatialPartition; // where Integer is x+y*unitsPerPartition coord
	
	public Dungeon(Room start, Room end, Array<Room> rooms, Array<Room> corridors, Array<Room> halls, DGraph<Room> criticalRoomGraph,
			int minSideLength, int maxSideLength, int hallWidth, float minRatio, float maxRatio) {
		world = new World(Vector2.Zero, true);
		
		this.startRoom = start;
		this.endRoom = end;
		this.noncriticalRooms = rooms;
		this.criticalRooms = corridors;
		this.allRooms = new Array<Room>(rooms);
		this.allRooms.addAll(corridors);
		this.halls =  halls;
		this.criticalRoomGraph = criticalRoomGraph;
		this.MIN_SIDE_LENGTH = minSideLength;
		this.MAX_SIDE_LENGTH = maxSideLength;
		this.HALL_WIDTH = hallWidth;
		this.CORRIDOR_COUNT = corridors.size;
		this.ROOM_COUNT = rooms.size;
		this.HALL_COUNT = halls.size;
		this.MIN_RATIO = minRatio;
		this.MAX_RATIO = maxRatio;
		
		this.dungeon = new Array<Room>();
		
		/*
		 * Add all rooms to dungeon and mark their type
		 */
		for (Room room : rooms) {
			this.dungeon.add(room);
			room.setType(RoomType.NONCRITICAL);
		}
		for (Room corridor : corridors) {
			this.dungeon.add(corridor);
			corridor.setType(RoomType.CRITICAL);
		}
		for (Room hall : halls) {
			this.dungeon.add(hall);
			hall.setType(RoomType.HALLWAY);
		}
		
		/*
		 * Normalize dungeon and create it's graph
		 */
		this.putDungeonInWorldSpace();
		
		this.WIDTH = this.calculateWidth();
		this.HEIGHT = this.calculateHeight();
		
		this.PARTITION_WIDTH = (int) Math.ceil((double)this.WIDTH / this.UNITS_PER_PARTITION);
		this.spatialPartition = this.createSpatialParition();
		
		this.dungeonGraph = this.createDungeonGraph();
//		this.populateBox2dWorld();
		this.populateBox2dWorldV2();
		
		
		
		
		
		MessageManager.getInstance().addListener(this, Messages.Dungeon.SPAWN_ENTITY);
	}
	
	public Array<Room> getDungeon() {
		return dungeon;
	}
	
	public Array<Room> getRooms() {
		return noncriticalRooms;
	}
	
	public Array<Room> getHalls() {
		return halls;
	}
	
	public Array<Room> getCorridors() {
		return criticalRooms;
	}
	
	public DGraph<Room> getCriticalRoomGraph() {
		return criticalRoomGraph;
	}
	
	public DGraph<Room> getDungeonGraph() {
		return dungeonGraph;
	}
	
	public Room getStartRoom() {
		return startRoom;
	}
	
	public Room getEndRoom() {
		return endRoom;
	}
	
	/*
	 * Because dungeons are created in the realm of -RADIUS to RADIUS, and are never guarenteed to hit the borders,
	 * this function will normalize the rightmost and bottommost rooms to (0,y) and (x, 0) respectively
	 * this should only be called once in the constructor
	 */
	private void putDungeonInWorldSpace() {
		int leftmostWall = Integer.MAX_VALUE;
		int bottomWall = Integer.MAX_VALUE;
		for (Room room : this.getDungeon()) {
			if (room.getLeft() < leftmostWall) {
				leftmostWall = room.getLeft();
			}
			if (room.getBottom() < bottomWall) {
				bottomWall = room.getBottom();
			}
		}
		
		// usually this will shift up and right, but if the leftmost happens to be positive (which is extremely unlikely) this still orients it to 0,0
		int dx = -leftmostWall;
		int dy = -bottomWall;
		
		for (Room room : this.getDungeon()) {
			room.shift(dx, dy);
		}
	}
	
	private DGraph<Room> createDungeonGraph() {
		/*
		 * Rooms connect iff a hall passes through them
		 * a hall is a room
		 */
		DGraph<Room> graph = new DGraph<Room>();
		// because a hallway will always connect two rooms, we use this as a reference for graph building
		for (Room hall : this.getHalls()) {
			for (Room room : this.getDungeon()) {
				if (room.getType() != RoomType.HALLWAY) { // this also implicitly checks that hall != room
					if (hall.touches(room)) {
						float dist = new Vector2(hall.getCenterX(), hall.getCenterY()).dst(new Vector2(room.getCenterX(), room.getCenterY()));						
						if (!graph.hasKey(hall)) {
							graph.addKey(hall);
						}
						graph.addConnection(hall, new Edge<Room>(hall, room, dist));
						
						if (!graph.hasKey(room)) {
							graph.addKey(room);
						}
						graph.addConnection(room, new Edge<Room>(room, hall, dist));
					}
				}
			}
		}
		
		for (int i=0; i < getHalls().size; i++) {
			for (int j=i+1; j < getHalls().size; j++) {
				Room h1 = getHalls().get(i);
				Room h2 = getHalls().get(j);
				if (h1.touches(h2)) {
					float dist = new Vector2(h1.getCenterX(), h1.getCenterY()).dst(new Vector2(h2.getCenterX(), h2.getCenterY()));
					if (!graph.hasKey(h1)) {
						graph.addKey(h1);
					}
					graph.addConnection(h1, new Edge<Room>(h1, h2, dist));
					
					if (!graph.hasKey(h2)) {
						graph.addKey(h2);
					}
					graph.addConnection(h2, new Edge<Room>(h2, h1, dist));
				}
			}
		}
		
		return graph;
	}
	
	private void populateBox2dWorld() {
		// room pass
		for (Room room : allRooms) {
			/*
			 * Create walls as lines
			 */
			Line2D left = new Line2D.Float(room.getLeft(), room.getBottom(), room.getLeft(), room.getTop());
			Line2D right = new Line2D.Float(room.getRight(), room.getBottom(), room.getRight(), room.getTop());
			Line2D top = new Line2D.Float(room.getLeft(), room.getTop(), room.getRight(), room.getTop());
			Line2D bottom = new Line2D.Float(room.getLeft(), room.getBottom(), room.getRight(), room.getBottom());
			Line2D left2 = null, right2 = null, top2 = null, bottom2 = null; // null unless needed
			boolean lflag = false, rflag = false, tflag = false, bflag = false;
			
			// left
			for (Connection<Room> connection : dungeonGraph.getConnections(room)) {
				Room iroom = connection.getToNode();
				if (left.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
					left = new Line2D.Float(room.getLeft(), room.getBottom(), room.getLeft(), iroom.getBottom());
					left2 = new Line2D.Float(room.getLeft(), iroom.getTop(), room.getLeft(), room.getTop());
					lflag = true;
					break;
				}
			}
			
			// right
			for (Connection<Room> connection : dungeonGraph.getConnections(room)) {
				Room iroom = connection.getToNode();
				if (right.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
					right = new Line2D.Float(room.getRight(), room.getBottom(), room.getRight(), iroom.getBottom());
					right2 = new Line2D.Float(room.getRight(), iroom.getTop(), room.getRight(), room.getTop());
					rflag = true;
					break;
				}
			}
			
			
			// top
			for (Connection<Room> connection : dungeonGraph.getConnections(room)) {
				Room iroom = connection.getToNode();
				if (top.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
					top = new Line2D.Float(room.getLeft(), room.getTop(), iroom.getLeft(), room.getTop());
					top2 = new Line2D.Float(iroom.getRight(), room.getTop(), room.getRight(), room.getTop());
					tflag = true;
					break;
				}
			}
			
			// bottom
			for (Connection<Room> connection : dungeonGraph.getConnections(room)) {
				Room iroom = connection.getToNode();
				if (bottom.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
					bottom = new Line2D.Float(room.getLeft(), room.getBottom(), iroom.getLeft(), room.getBottom());
					bottom2 = new Line2D.Float(iroom.getRight(), room.getBottom(), room.getRight(), room.getBottom());
					bflag = true;
					break;
				}
			}
			
//			for (Room iroom : this.getPotentialRoomsInArea(room.getBounds())) {
//				if (!lflag || !rflag || !tflag || !bflag) {
//					if (!lflag) {
//						if (left.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
//							left = new Line2D.Float(room.getLeft(), room.getBottom(), room.getLeft(), iroom.getBottom());
//							left2 = new Line2D.Float(room.getLeft(), iroom.getTop(), room.getLeft(), room.getTop());
//							lflag = true;
//						}
//					}
//					if (!rflag) {
//						if (right.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
//							right = new Line2D.Float(room.getRight(), room.getBottom(), room.getRight(), iroom.getBottom());
//							right2 = new Line2D.Float(room.getRight(), iroom.getTop(), room.getRight(), room.getTop());
//							rflag = true;
//						}
//					}
//					if (!tflag) {
//						if (top.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
//							top = new Line2D.Float(room.getLeft(), room.getTop(), iroom.getLeft(), room.getTop());
//							top2 = new Line2D.Float(iroom.getRight(), room.getTop(), room.getRight(), room.getTop());
//							tflag = true;
//						}
//					}
//					if (!bflag) {
//						if (bottom.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
//							bottom = new Line2D.Float(room.getLeft(), room.getBottom(), iroom.getLeft(), room.getBottom());
//							bottom2 = new Line2D.Float(iroom.getRight(), room.getBottom(), room.getRight(), room.getBottom());
//							bflag = true;
//						}
//					}
//				} else {
//					break;
//				}
//			}
			
			/*
			 * Make Box2d copies of these lines
			 */
			
			// LEFT
			Box2dBodyFactory.createEdge(new Vector2((float)left.getX1(), (float)left.getY1()),
					new Vector2((float)left.getX2(), (float)left.getY2()), world);
			
			if (left2 != null) {
				Box2dBodyFactory.createEdge(new Vector2((float)left2.getX1(), (float)left2.getY1()),
						new Vector2((float)left2.getX2(), (float)left2.getY2()), world);
			}
			
			// RIGHT
			Box2dBodyFactory.createEdge(new Vector2((float)right.getX1(), (float)right.getY1()),
					new Vector2((float)right.getX2(), (float)right.getY2()), world);
			
			if (right2 != null) {
				Box2dBodyFactory.createEdge(new Vector2((float)right2.getX1(), (float)right2.getY1()),
						new Vector2((float)right2.getX2(), (float)right2.getY2()), world);
			}
			
			// TOP
			Box2dBodyFactory.createEdge(new Vector2((float)top.getX1(), (float)top.getY1()),
					new Vector2((float)top.getX2(), (float)top.getY2()), world);
			
			if (top2 != null) {
				Box2dBodyFactory.createEdge(new Vector2((float)top2.getX1(), (float)top2.getY1()),
						new Vector2((float)top2.getX2(), (float)top2.getY2()), world);
			}
			
			// BOTTOM
			Box2dBodyFactory.createEdge(new Vector2((float)bottom.getX1(), (float)bottom.getY1()),
					new Vector2((float)bottom.getX2(), (float)bottom.getY2()), world);
			
			if (bottom2 != null) {
				Box2dBodyFactory.createEdge(new Vector2((float)bottom2.getX1(), (float)bottom2.getY1()),
						new Vector2((float)bottom2.getX2(), (float)bottom2.getY2()), world);
			}
		}
		
		
		
		// hallway pass
		for (Room hall : halls) {
//			for (Connection<Room> connection : dungeonGraph.getConnections(hall).items) {
//				Room iroom = connection.getToNode();
//				
//			}
//			if (dungeonGraph.getConnections(hall).items.length == 2) {
//				Room r1 = 
//			}
//			System.out.println(dungeonGraph.getConnections(hall).size);
		}
	}
	
	/*
	 * N^3 slow algorithm, but is very precise
	 */
	private void populateBox2dWorldV2() {
		int size = spatialPartition.size;
		System.out.println(size);
//		
//		spatialPartition.keys().reset();
//		Keys itr = spatialPartition.keys();
////		int i = 0;
//		while (itr.hasNext) {
////		while (i < size) {
//			int key = itr.next();
////			int key = i;
//			
//			int blockSize = 20;
////			Array<Room> potentialColls = spatialPartition.get(key);
//			
//			int chunkX = key % PARTITION_WIDTH;
//			int chunkY = key / PARTITION_WIDTH;
//			for (int y=chunkY*UNITS_PER_PARTITION; y < UNITS_PER_PARTITION*chunkY+chunkY; y += blockSize) {
//				for (int x=chunkX*UNITS_PER_PARTITION; x < UNITS_PER_PARTITION*chunkX+chunkX; x += blockSize) {
//					if (this.getRoomsContainingArea(new Rectangle(x, y, blockSize, blockSize)).size == 0) {
//						Box2dBodyFactory.createStaticSquare(new Vector2(x, y), blockSize, world);
//					}
//				}
//			}
//			System.out.format("iter %d / %d%n", key, size);
////			i++;
//		}
//		spatialPartition.keys().reset(); // reset again to be safe
		
		int blockSize = 3 * Constants.PPM;
		for (int y=0; y < HEIGHT; y += blockSize) {
			for (int x=0; x < WIDTH; x += blockSize) {
				if (this.getRoomsContainingArea(new Rectangle(x, y, blockSize, blockSize)).size == 0) {
					Box2dBodyFactory.createStaticSquare(new Vector2(x, y), blockSize, world);
				}
			}
		}
	}
	
	ShapeRenderer sr = new ShapeRenderer();
	public void render(Matrix4 combined) {
		sr.setProjectionMatrix(combined);
	    sr.begin(ShapeRenderer.ShapeType.Filled);
	    
	    
	    sr.setColor(Color.BROWN);
	    for (Room corridor : getCorridors()) {
	    	sr.rect(corridor.getLeft(), corridor.getBottom(), corridor.getWidth(), corridor.getHeight());
	    }
	    sr.setColor(Color.GREEN);
		for (Room rooms : getRooms()) {
			sr.rect(rooms.getLeft(), rooms.getBottom(), rooms.getWidth(), rooms.getHeight());
		}
		
		// will draw start and end rooms twice, but it's ok to overlap
		sr.setColor(Color.LIGHT_GRAY);
		sr.rect(startRoom.getLeft(), startRoom.getBottom(), startRoom.getWidth(), startRoom.getHeight());
		sr.setColor(Color.GOLD);
		sr.rect(endRoom.getLeft(), endRoom.getBottom(), endRoom.getWidth(), endRoom.getHeight());
		
		sr.setColor(Color.RED);
		for (Room halls : getHalls()) {
			sr.rect(halls.getLeft(), halls.getBottom(), halls.getWidth(), halls.getHeight());
		}
		
		sr.end();
		sr.begin(ShapeRenderer.ShapeType.Line);
		
		// DRAW SPATIAL PARTITION
		sr.setColor(Color.BLACK);
		Keys keys = spatialPartition.keys();
		while (keys.hasNext) {
			int i = keys.next();
			int x = (i % PARTITION_WIDTH) * UNITS_PER_PARTITION, y = (i / PARTITION_WIDTH) * UNITS_PER_PARTITION;
			sr.rect(x, y, UNITS_PER_PARTITION, UNITS_PER_PARTITION);
		}
		
		// DRAW CRITICAL GRAPH
//		sr.setColor(Color.CYAN);
//		for (Room start : criticalRoomGraph.keySet()) {
//			for (Room end : criticalRoomGraph.get(start)) {
//				sr.line(start.getCenterX(), start.getCenterY(), end.getCenterX(), end.getCenterY());
//			}
//		}
		
		// DRAW DUNGEON GRAPH
		sr.setColor(Color.BLACK);
		for (Room room : dungeonGraph.getKeys()) {
			for (Connection<Room> edge : dungeonGraph.getConnections(room)) {
				Room a = edge.getToNode();
				Room b = edge.getFromNode();
				sr.line(a.getCenterX(), a.getCenterY(), b.getCenterX(), b.getCenterY());
			}
		}
		
		// DRAW TEST GRAPH
//		sr.setColor(Color.WHITE);
//		for (Room a : testMap.keySet()) {
//			for (Room b : testMap.get(a)) {
//				sr.line(a.getCenterX(), a.getCenterY(), b.getCenterX(), b.getCenterY());
//			}
//		}
	    
		
		sr.end();
	}
	
	/*
	 * Creates a spatial partition, where world cords / unitsPerPartition map to the rooms.
	 * Make sure dungeon is in world space before calling this method.
	 */
	private IntMap<Array<Room>> createSpatialParition() {
		IntMap<ObjectSet<Room>> map = new IntMap<ObjectSet<Room>>(); // no repeats
		
		/*
		 * HORRIBLE RUNTIME. But only needs to be run once per dungeon generation.
		 * It feels like x and y could increment by unitsPerPartition each time, but this produces weird results
		 */
		for (Room room : this.getDungeon()) {
			for (int y=room.getBottom(); y <= room.getTop(); y++) {
				for (int x=room.getLeft(); x <= room.getRight(); x++) {
					int key = calculatePartitionKey(x, y);
					if (map.containsKey(key)) {
						map.get(key).add(room);
					} else {
						map.put(key, new ObjectSet<Room>());
					}
				}
			}
		}
		
		IntMap<Array<Room>> ret = new IntMap<Array<Room>>();
		Keys keys = map.keys();
		while (keys.hasNext) {
			int key = keys.next();
			ret.put(key, new Array<Room>());
			for (Room room : map.get(key)) {
				ret.get(key).add(room);
			}
//			map.keys().remove();
		}
		return ret;
	}
	
	private ObjectSet<Room> getPotentialRoomsInArea(Rectangle area) {
		// create a list of rooms that area could potentially have from spatial partition
		ObjectSet<Room> potentialRooms = new ObjectSet<Room>();
		
		Integer aa = calculatePartitionKey(area.x, area.y);
		Integer bb = calculatePartitionKey(area.x + area.width, area.y);
		Integer cc = calculatePartitionKey(area.x, area.y + area.height);
		Integer dd = calculatePartitionKey(area.x + area.width, area.y + area.height);
		
		// no repeat rooms thanks to hashset
		if (spatialPartition.containsKey(aa)) {
			potentialRooms.addAll(spatialPartition.get(aa));
		} else if (aa != bb) {
			if (spatialPartition.containsKey(bb)) {
				potentialRooms.addAll(spatialPartition.get(bb));
			} else if (bb != cc) {
				if (spatialPartition.containsKey(cc)) {
					potentialRooms.addAll(spatialPartition.get(cc));
				} else if (cc != dd) {
					if (spatialPartition.containsKey(dd)) {
						potentialRooms.addAll(spatialPartition.get(dd));
					}
				}
			}
		} else {
			if (bb != cc) {
				if (spatialPartition.containsKey(cc)) {
					potentialRooms.addAll(spatialPartition.get(cc));
				} else if (cc != dd) {
					if (spatialPartition.containsKey(dd)) {
						potentialRooms.addAll(spatialPartition.get(dd));
					}
				}
			} else {
				if (cc != dd) {
					if (spatialPartition.containsKey(dd)) {
						potentialRooms.addAll(spatialPartition.get(dd));
					}
				}
			}
		}
		return potentialRooms;
	}
	
	public Array<Room> getRoomsInArea(Rectangle area) {
		int biggest = HALL_WIDTH > MIN_SIDE_LENGTH ? HALL_WIDTH : MIN_SIDE_LENGTH;
		if (area.width > biggest || area.height > biggest) {
			try {
				throw new Exception();
			} catch (Exception e) {
				System.out.println("WARNING: Area -> Room algorithm has no case for entities larger than rooms");
				e.printStackTrace();
			}
		}
		
		// create a list of rooms that area could potentially have from spatial partition
		ObjectSet<Room> potentialRooms = this.getPotentialRoomsInArea(area);
		
		// perform a bounds check on the potential rooms
		Array<Room> rooms = new Array<Room>(potentialRooms.size);
		for (Room room : potentialRooms) {			
			if (room.getBounds().overlaps(area)) {
				rooms.add(room);
			}
		}
		
		return rooms;
	}
	
	public Array<Room> getRoomsContainingArea(Rectangle area) {
		// create a list of rooms that area could potentially have from spatial partition
		ObjectSet<Room> potentialRooms = this.getPotentialRoomsInArea(area);
			
		// perform a bounds check on the potential rooms
		Array<Room> rooms = new Array<Room>(potentialRooms.size);
		for (Room room : potentialRooms) {			
			if (room.getBounds().contains(area)) {
				rooms.add(room);
			}
		}
		
		return rooms;
	}
	
	public Room getRoomAtPoint(Vector2 point) {
		Array<Room> rooms = spatialPartition.get(calculatePartitionKey(point.x, point.y));
		if (rooms != null) {
//			Room nonHallway = null; // allows us to prefer hallways over other rooms, uncomment if this behavior is desired
			for (Room room : rooms) {
				if (room.getBounds().contains(point)) {
//					if (roomTypeMap.get(room) == RoomType.HALLWAY) {
						return room;
//					} else {
//						nonHallway = room;
//					}
				}
//				return nonHallway;
			}
		}
		return null;
	}
	
	private int calculateWidth() {
		int leftmostWall = Integer.MAX_VALUE;
		int rightmostWall = Integer.MIN_VALUE;
		for (Room room : this.getDungeon()) {
			if (room.getLeft() < leftmostWall) {
				leftmostWall = room.getLeft();
			}
			if (room.getRight() > rightmostWall) {
				rightmostWall = room.getRight();
			}
		}
		return rightmostWall - leftmostWall;
	}
	
	private int calculateHeight() {
		int bottomWall = Integer.MAX_VALUE;
		int topWall = Integer.MIN_VALUE;
		for (Room room : this.getDungeon()) {
			if (room.getBottom() < bottomWall) {
				bottomWall = room.getBottom();
			}
			if (room.getTop() > topWall) {
				topWall = room.getTop();
			}
		}
		return topWall - bottomWall;
	}
	
	public Integer calculatePartitionKey(float x, float y) {
		int px = (int)x / UNITS_PER_PARTITION, py = (int)y / UNITS_PER_PARTITION;
		return px + py * PARTITION_WIDTH;
	}
	
	public Integer calculatePartitionKey(int x, int y) {
		int px = x / UNITS_PER_PARTITION, py = y / UNITS_PER_PARTITION;
		return px + py * PARTITION_WIDTH;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Messages.Dungeon.SPAWN_ENTITY) {
			msg.extraInfo = Box2dBodyFactory.createDynamicSquare(startRoom.getCenter(), world);
			return true;
		}
		return false;
	}
}
