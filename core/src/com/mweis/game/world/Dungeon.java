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
import com.mweis.game.util.Messages;
import com.mweis.game.world.Room.RoomType;
import com.mweis.game.world.graph.DGraph;
import com.mweis.game.world.graph.Edge;


public class Dungeon implements Telegraph {
	
	private static Dungeon INSTANCE = new Dungeon(new DungeonFactory().generateDungeon());
	
	private World world;
	
	public final int WIDTH, HEIGHT, MIN_SIDE_LENGTH, MAX_SIDE_LENGTH, HALL_WIDTH,
		CORRIDOR_COUNT, ROOM_COUNT, HALL_COUNT,
		// adjust this if units walking through walls is an issue:
		MAX_BOX2D_STATIC_BODY_SIZE = 50; // no effect if body size less than 2*blockSize
	public final float MIN_RATIO, MAX_RATIO;
	
	private Room startRoom, endRoom;
	private Array<Room> noncriticalRooms, criticalRooms, allRooms, halls, dungeon;
	private DGraph<Room> criticalRoomGraph;
	private DGraph<Room> dungeonGraph;
	
	public final int UNITS_PER_PARTITION = 5, // width and height of each partition square
			ESTIMATED_MAX_ROOMS_PER_PARTITION = 16, // used for init cap of ObjectSet, THIS IS ALWAYS ROUNDED UP TO NEXT POWER OF TWO
			PARTITION_WIDTH; // with in CHUNKS
	private final IntMap<Array<Room>> spatialPartition; // where Integer is x+y*unitsPerPartition coord
	
	// make dungeon constructor take Blueprint!
//	Dungeon(Room start, Room end, Array<Room> rooms, Array<Room> corridors, Array<Room> halls, DGraph<Room> criticalRoomGraph,
//			int minSideLength, int maxSideLength, int hallWidth, float minRatio, float maxRatio) {
	Dungeon(DungeonBlueprint blueprint) {
		
		if (world != null) {
			world.dispose(); // dispose of last world if new one is made
		}
		
		world = new World(Vector2.Zero, true);
		
		this.startRoom = blueprint.getStart();
		this.endRoom = blueprint.getEnd();
		this.noncriticalRooms = blueprint.getRooms();
		this.criticalRooms = blueprint.getCorridors();
		this.allRooms = new Array<Room>(blueprint.getRooms());
		this.allRooms.addAll(blueprint.getCorridors());
		this.halls =  blueprint.getHalls();
		this.criticalRoomGraph = blueprint.getCriticalRoomGraph();
		this.MIN_SIDE_LENGTH = blueprint.getMinSideLength();
		this.MAX_SIDE_LENGTH = blueprint.getMaxSideLength();
		this.HALL_WIDTH = blueprint.getHallWidth();
		this.CORRIDOR_COUNT = blueprint.getCorridors().size;
		this.ROOM_COUNT = blueprint.getRooms().size;
		this.HALL_COUNT = blueprint.getHalls().size;
		this.MIN_RATIO = blueprint.getMinRatio();
		this.MAX_RATIO = blueprint.getMaxRatio();
		
		this.dungeon = new Array<Room>();
		
		/*
		 * Add all rooms to dungeon and mark their type
		 */
		for (Room room : blueprint.getRooms()) {
			this.dungeon.add(room);
			room.setType(RoomType.NONCRITICAL);
		}
		for (Room corridor : blueprint.getCorridors()) {
			this.dungeon.add(corridor);
			corridor.setType(RoomType.CRITICAL);
		}
		for (Room hall : blueprint.getHalls()) {
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
		
		MessageManager.getInstance().addListener(this, Messages.SOLICIT.ENTITY_SPAWNED);
	}
	
	
	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Messages.ANNOUNCE.DUNGEON_NEW) {
			DungeonFactory factory = new DungeonFactory();
			DungeonBlueprint builder = factory.generateDungeon();
//			this.INSTANCE = new Dungeon();
			// make dungeon constructor take Blueprint!
			
		} else if (msg.message == Messages.SOLICIT.ENTITY_SPAWNED) {
			msg.extraInfo = Box2dBodyFactory.createDynamicSquare(startRoom.getCenter(), world);
			return true;
		}
		return false;
	}
	
	public World getWorld() {
		return this.world;
	}
	
	public Array<Room> getDungeon() {
		return dungeon;
	}
	
	public Array<Room> getOptionalRooms() {
		return noncriticalRooms;
	}
	
	public Array<Room> getHalls() {
		return halls;
	}
	
	public Array<Room> getRooms() { // rooms only, not halls
		return allRooms;
	}
	
	public Array<Room> getCriticalRooms() {
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
	
	/*
	 * Depreciated and not fully implemented, would be more efficient if possible though
	 */
	@SuppressWarnings("unused")
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
			
			for (Room iroom : this.getPotentialRoomsInArea(room.getBounds())) {
				if (!lflag || !rflag || !tflag || !bflag) {
					if (!lflag) {
						if (left.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
							left = new Line2D.Float(room.getLeft(), room.getBottom(), room.getLeft(), iroom.getBottom());
							left2 = new Line2D.Float(room.getLeft(), iroom.getTop(), room.getLeft(), room.getTop());
							lflag = true;
						}
					}
					if (!rflag) {
						if (right.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
							right = new Line2D.Float(room.getRight(), room.getBottom(), room.getRight(), iroom.getBottom());
							right2 = new Line2D.Float(room.getRight(), iroom.getTop(), room.getRight(), room.getTop());
							rflag = true;
						}
					}
					if (!tflag) {
						if (top.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
							top = new Line2D.Float(room.getLeft(), room.getTop(), iroom.getLeft(), room.getTop());
							top2 = new Line2D.Float(iroom.getRight(), room.getTop(), room.getRight(), room.getTop());
							tflag = true;
						}
					}
					if (!bflag) {
						if (bottom.intersects(iroom.getBounds().x, iroom.getBounds().y, iroom.getBounds().width, iroom.getBounds().height)) {
							bottom = new Line2D.Float(room.getLeft(), room.getBottom(), iroom.getLeft(), room.getBottom());
							bottom2 = new Line2D.Float(iroom.getRight(), room.getBottom(), room.getRight(), room.getBottom());
							bflag = true;
						}
					}
				} else {
					break;
				}
			}
			
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
	 * N^3 slow algorithm, but is very precise and only runs one time at level creation
	 */
	private void populateBox2dWorldV2() {		
		
		/*
		 * TileMerging algorithm from https://love2d.org/wiki/TileMerging, this merges into VERTICAL WALLS ONLY
		 */
		int blockSize = 1;
		
		// a different way of representing rectangles, consistent with the TileMerging algorithm
		class PointRectangle implements Comparable<PointRectangle> {
			int start_x, start_y, end_x, end_y;
			PointRectangle(int sx, int sy, int ex, int ey) {
				start_x = sx;
				start_y = sy;
				end_x = ex;
				end_y = ey;
			}
			@Override
			public int compareTo(PointRectangle other) {
				return this.start_y < other.start_y ? 1 : -1;
			}
		}
		
//		Array<PointRectangle> rectangles = new Array<PointRectangle>(false, 100); // should use this one! much faster w/o ordering
		Array<PointRectangle> rectangles = new Array<PointRectangle>();
		for (int x = 0; x < WIDTH; x += blockSize) {
			// Integers mean we need to watch the == sign!
			Integer start_y = null;
			Integer end_y = null;
			
			for (int y = 0; y < HEIGHT; y += blockSize) {
				// WARNING: if getRoomsContainingArea is changed, so must be the other call to it below!
				if (this.getRoomsContainingArea(new Rectangle(x, y, blockSize, blockSize)).size == 0) { // check if chunk goes here
					if (start_y == null) {
						start_y = y;
					}
					end_y = y;
				} else if (start_y != null) {
					Array<PointRectangle> overlaps = new Array<PointRectangle>();
					for (PointRectangle r : rectangles) {
						if (r.end_x == x - blockSize // ? blockSize or 1 ?
							&& start_y <= r.start_y
							&& end_y >= r.end_y) {
							overlaps.add(r);
						}
					}
					overlaps.sort();
					for (PointRectangle r : overlaps) {
						if (start_y < r.start_y) {
							PointRectangle new_rect = new PointRectangle(x, start_y, x, r.start_y - blockSize);
							rectangles.add(new_rect);
							start_y = r.start_y;
						}
						
						if (start_y == r.start_y) {
							r.end_x = r.end_x + blockSize;
							if (end_y == r.end_y) {
								start_y = null;
								end_y = null;
								break; // safeguard. if NullPointerException shows up eventually they put safeguard at start of this for-loop
							} else if (end_y > r.end_y) {
								start_y = r.end_y + blockSize;
							}
						}
					}
					if (start_y != null) {
						PointRectangle new_rect = new PointRectangle(x, start_y, x, end_y);
						rectangles.add(new_rect);
						start_y = null;
						end_y = null;
					}
				}
			}
			if (start_y != null) {
				PointRectangle new_rect = new PointRectangle(x, start_y, x, end_y);
				rectangles.add(new_rect);
				start_y = null;
				end_y = null;
			}
		}
		
		/*
		 * Merge walls into larger rectangles iff the walls have the same start and end y
		 */	
		boolean mergeFlag = false;
		do {
			mergeFlag = false;
			PointRectangle new_rect = null, old_rect1 = null, old_rect2 = null;
			int size = rectangles.size; // must calc here is it's changing often
			A: for (int i=0; i < size; i++) {
				for (int j=0; j < size; j++) { // can j start at i+1?
					if (i != j) {
						PointRectangle r1 = rectangles.get(i);
						PointRectangle r2 = rectangles.get(j);
						if (withinNUnits(r1.start_x, r2.end_x, blockSize) || withinNUnits(r1.end_x, r2.start_x, blockSize)) {
							if (r1.start_y == r2.start_y && r1.end_y == r2.end_y) {
								// combine the walls
								int sx = min(r1.start_x, min(r1.end_x, min(r2.start_x, r2.end_x)));
								int sy = min(r1.start_y, min(r1.end_y, min(r2.start_y, r2.end_y)));
								int ex = max(r1.start_x, max(r1.end_x, max(r2.start_x, r2.end_x)));
								int ey = max(r1.start_y, max(r1.end_y, max(r2.start_y, r2.end_y)));
								new_rect = new PointRectangle(sx, sy, ex, ey);
								old_rect1 = r1;
								old_rect2 = r2;
								mergeFlag = true;
								break A;
							}
						}
					}
				}
			}
			if (new_rect != null) {
				rectangles.removeValue(old_rect1, true);
				rectangles.removeValue(old_rect2, true);
				rectangles.add(new_rect);
			}
		} while (mergeFlag);
		
		/*
		 * Needlessly tall bodies can cause coll errors in box2d, we need to shrink out areas not affecting the map
		 */
		
//		ObjectSet<PointRectangle> markedForDeletion = new ObjectSet<PointRectangle>(rectangles.size / 5);
		A: for (PointRectangle r : rectangles) {
			int max_y = max(r.start_y, r.end_y); // will change and must be recomputed per iteration
			int min_y = min(r.start_y, r.end_y); // will change and must be recomputed per iteration
			int max_x = max(r.start_x, r.end_x); // constant
			int min_x = min(r.start_x, r.end_x); // constant
			
			
			// shrink top down
			boolean canShrink = true;
			while (canShrink) {
				
//				if (max_y - min_y < blockSize) {
//					markedForDeletion.add(r);
//					canShrink = false;
//					continue A;
//				}
				
//				if (this.getPotentialRoomsInArea(new Rectangle(min_x, max_y - blockSize, max_x - min_x, blockSize)).size == 0
				if (this.getPotentialRoomsInArea(new Rectangle(min_x, max_y - blockSize, max_x - min_x, blockSize)).size == 0
						&& min_y >= 0){
					if (r.end_y > r.start_y) {
						r.end_y -= blockSize;
					} else {
						r.start_y -= blockSize;
					}
					max_y = max(r.start_y, r.end_y);
					min_y = min(r.start_y, r.end_y);
				} else {
					canShrink = false;
				}
			}
			
			// shrink bottom up
			canShrink = true;
			while (canShrink) {
				
//				if (max_x - min_x < blockSize) {
//					markedForDeletion.add(r);
//					canShrink = false;
//					continue A;
//				}
				
//				if (this.getPotentialRoomsInArea(new Rectangle(min_x, min_y, max_x - min_x, blockSize)).size == 0
				if (this.getPotentialRoomsInArea(new Rectangle(min_x, min_y, max_x - min_x, blockSize)).size == 0
						&& max_y <= HEIGHT){
					if (r.start_y < r.end_y) {
						r.start_y += blockSize;
					} else {
						r.end_y += blockSize;
					}
					max_y = max(r.start_y, r.end_y);
					min_y = min(r.start_y, r.end_y);
				} else {
					canShrink = false;
				}
			}
		}
		
		// delete any rectangles whose size is too small after shrinking
//		for (PointRectangle r : markedForDeletion) {
//			rectangles.removeValue(r, true);
//		}
		
		
		/*
		 * Any box2d objects bigger than the permitted size need to be cut into half until they are small enough
		 */

		
		for (int i=0; i < rectangles.size; i++) {
			PointRectangle r = rectangles.get(i);
			int sx = min(r.start_x, r.end_x);
			int sy = min(r.start_y, r.end_y);
			int ex = max(r.start_x, r.end_x);
			int ey = max(r.start_y, r.end_y);
			int height = max(r.start_y, r.end_y) - sy;
			int width = max(r.start_x, r.end_x) - sx;
			
			if (height > MAX_BOX2D_STATIC_BODY_SIZE && height > 2*blockSize) {
				int halfway = (ey - sy + 1)/2;
				PointRectangle top_half = new PointRectangle(sx, sy + halfway, ex, ey);
				PointRectangle bottom_half = new PointRectangle(sx, sy, ex, sy + halfway);
				rectangles.removeIndex(i);
				i--;
				rectangles.add(top_half);
				rectangles.add(bottom_half);
			} else if (width > MAX_BOX2D_STATIC_BODY_SIZE && height > 2*blockSize) {
				int halfway = (ex - sx)/2;
				PointRectangle left_half = new PointRectangle(sx, sy, sx + halfway, ey);
				PointRectangle right_half = new PointRectangle(sx + halfway, sy, ex, ey);
				rectangles.removeIndex(i);
				i--;
				rectangles.add(left_half);
				rectangles.add(right_half);
			}
		}
		
		/*
		 * Tiles are now merged, time to box2dify them.
		 * Also, remove any unused partitions here.
		 */
		for (PointRectangle r : rectangles) {
			int sx = min(r.start_x, r.end_x);
			int sy = min(r.start_y, r.end_y);
			int height = max(r.start_y, r.end_y) - sy;
			int width = max(r.start_x, r.end_x) - sx;
			
			if (this.getPotentialRoomsInArea(new Rectangle(sx, sy, width, height)).size == 0) {
				continue; // skip any rooms with no use
			}
			if (height == 0) height = blockSize;
			if (width == 0) width = blockSize;
			
			// add "just a bit" to the size of the walls
//			width += blockSize*2;
//			height += blockSize*2;
//			sx -= blockSize;// / 2.0f;
//			sy -= blockSize;// / 2.0f;
			
			// prints size of big rects
//			if (max(width, height) > MAX_BOX2D_STATIC_BODY_SIZE) {
//				System.out.format("sx: %d, sy: %d, width: %d, height: %d%n", sx, sy, width, height);
//			}
			Box2dBodyFactory.createStaticRectangle(sx, sy, width, height, world);
		}
		
		/*
		 * NO LONGER RELEVANT WITH BOX HEIGHT REDUCTION
		 * Optional, make a border around the entire dungeon preventing escape if.
		 * 
		 * We might not want this actually, it could be cool to just put water/lava/height around the edges instead,
		 * light could bleed in and it would be a tactical spot for players with knockback abilities to kite enemy mobs to.
		 * (also light might hurt mobs, meaning kiting mobs towards the light is smart if the mob's weakness is known.
		 */
//		Vector2 bottom_left = Vector2.Zero;
//		Vector2 bottom_right = new Vector2(WIDTH, 0);
//		Vector2 top_left = new Vector2(0, HEIGHT);
//		Vector2 top_right = new Vector2(WIDTH, HEIGHT);
//		Box2dBodyFactory.createEdge(top_left, top_right, world); // top
//		Box2dBodyFactory.createEdge(bottom_left, bottom_right, world); // bottom
//		Box2dBodyFactory.createEdge(bottom_left, top_left, world); // left
//		Box2dBodyFactory.createEdge(bottom_right, top_right, world); // right
		
	}
	
	ShapeRenderer sr = new ShapeRenderer();
	public void render(Matrix4 combined) {
		sr.setProjectionMatrix(combined);
	    sr.begin(ShapeRenderer.ShapeType.Filled);
	    
	    
	    sr.setColor(Color.BROWN);
	    for (Room corridor : getCriticalRooms()) {
	    	sr.rect(corridor.getLeft(), corridor.getBottom(), corridor.getWidth(), corridor.getHeight());
	    }
	    sr.setColor(Color.SALMON);
		for (Room rooms : getOptionalRooms()) {
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
		int biggest = max(HALL_WIDTH, MIN_SIDE_LENGTH);
		if (area.width > biggest || area.height > biggest) {
//			try {
//				throw new Exception();
//			} catch (Exception e) {
//				System.out.println("WARNING: Area -> Room algorithm has no case for entities larger than rooms");
//				e.printStackTrace();
//			}
//			Gdx.app.error("getPotentialRoomsInArea",
//					"WARNING: Area -> Room algorithm has no case for entities larger than rooms", new IllegalArgumentException());
		}
		
		// create a list of rooms that area could potentially have from spatial partition
		ObjectSet<Room> potentialRooms = new ObjectSet<Room>(ESTIMATED_MAX_ROOMS_PER_PARTITION);
		
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
		
		int biggest = max(HALL_WIDTH, MIN_SIDE_LENGTH);
		ObjectSet<Room> potentialRooms = null;
		/*
		 * If the area we're looking for is bigger than that of the spatial partition we need to subdivide it
		 */
		if (area.width > biggest || area.height > biggest) {
//			Gdx.app.error("getRoomsInArea", "areas larger than UNITS_PER_PARTITION might have problems");
			int sizeFactor = (max(1, (int)area.width/UNITS_PER_PARTITION))*max(1, (int)area.height/UNITS_PER_PARTITION);
			potentialRooms = new ObjectSet<Room>(ESTIMATED_MAX_ROOMS_PER_PARTITION*sizeFactor);
			
//			potentialRooms = new ObjectSet<Room>();
			Rectangle r = new Rectangle(0.0f, 0.0f, UNITS_PER_PARTITION, UNITS_PER_PARTITION); // not problem?
			
			for (float y=area.y; y <= area.y + area.height; y += this.UNITS_PER_PARTITION) { // not problem
				for (float x=area.x; x <= area.x + area.width; x += this.UNITS_PER_PARTITION) { // not problem
					r.setX(x);
					r.setY(y);
					
//					ObjectSet<Room> pRooms = this.getPotentialRoomsInArea(r);
					Array<Room> pRooms = this.getAllPotentialRoomsAtPoint(x, y);
					if (pRooms != null) {
						potentialRooms.addAll(pRooms);
					}
				}
			}
						
		} else {
			/*
			 * If the area we're looking for is smaller then we use 4 corners
			 */
			// create a list of rooms that area could potentially have from spatial partition
			potentialRooms = this.getPotentialRoomsInArea(area);
			
		}
			// perform a bounds check on the potential rooms
			Array<Room> rooms = new Array<Room>(potentialRooms.size);
			for (Room room : potentialRooms) {			
				if (room.getBounds().overlaps(area)) {
					rooms.add(room);
				}
			}
			
			return rooms;
	}
	
	/*
	 * Returns a list of rooms that could potentially be in our area from our spatial partition
	 */
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
	
	public Array<Room> getAllRoomsAtPoint(Vector2 point) {
		Array<Room> rooms = spatialPartition.get(calculatePartitionKey(point.x, point.y));
		if (rooms != null) {
			for (Room room : rooms) {
				if (!room.getBounds().contains(point)) {
					rooms.removeValue(room, true);
				}
			}
		}
		return rooms;
	}
	
	public Array<Room> getAllPotentialRoomsAtPoint(Vector2 point) {
		return getAllPotentialRoomsAtPoint(point.x, point.y);
	}
	
	public Array<Room> getAllPotentialRoomsAtPoint(int x, int y) {
		return spatialPartition.get(calculatePartitionKey(x, y)); // don't call other method, this is faster
	}
	
	public Array<Room> getAllPotentialRoomsAtPoint(float x, float y) {
		return spatialPartition.get(calculatePartitionKey(x, y));
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
	
	private int min(int a, int b) {
		return a < b ? a : b;
	}
	
	private int max(int a, int b) {
		return a > b ? a : b;
	}
	
	private boolean withinNUnits(int a, int b, int range) {
		return Math.abs(a - b) <= range;
	}
}
