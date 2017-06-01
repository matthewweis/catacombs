package com.mweis.game.world;

import java.util.Random;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.mweis.game.world.graph.DGraph;
import com.mweis.game.world.graph.Edge;

/*
 * From tutorial implementation, but with changes for this project:
 * https://github.com/fisherevans/ProceduralGeneration/blob/master/dungeons/src/main/java/com/fisherevans/procedural_generation/dungeons/DungeonGenerator.java
 * 
 * Corridors are the amount of "critical" rooms which will be passed through.
 * All intermediate "non-critical" dungeon rooms are included iff the halls pass through them.
 */
public class DungeonFactory {
	
	private static Random random = new Random();
	
	private static final int padding = -1;
	
	/*
	 * All dungeons hold their own copy of these, because eventually we may eventually want variance between dungeons.
	 */
	private static int mapSize = 35*2;
	private static int scale = 1;
	private static int minSideLength = 250;
	private static int maxSideLength = 550;
	private static int hallWidth = 75;
	private static int corridorCount = 20;
	private static int roomCount = 80;
	private static float minRatio = 1.0f;
	private static float maxRatio = 1.5f;
	private static float touchedRoomChance = 1.0f;
	
	private static Room start, end;
	
	public static Dungeon generateDungeon() {
		Array<Room> rooms = createRooms();
		seperateRooms(rooms);
		Array<Room> corridors = findCorridors(rooms);
		centerCorridors(rooms, corridors);
		DGraph<Room> graph = connectRooms(corridors);
		Array<Room> halls = createHalls(graph);
		for (Room hall : halls) {
			hall.expand(1);
		}
		Array<Room> untouched = removeUntouched(rooms, halls);
		
		findStartAndEnd(corridors);
				
		return new Dungeon(start, end, rooms, corridors, halls, graph, minSideLength,
				maxSideLength, hallWidth, minRatio, maxRatio);
	}
	
	private static Array<Room> createRooms() {
		int width, height, x, y;
		double ratio;
		Room room;
		Array<Room> rooms = new Array<Room>(roomCount);
		for (int i=0; i < roomCount; i++) {
			do {
				width = getRandomSide();
				height = getRandomSide();
				ratio = Room.getRatio(width, height);
			} while (ratio < minRatio || ratio > maxRatio);
			x = getRandomGausInt(mapSize * 2) - mapSize - width/2;
			y = getRandomGausInt(mapSize * 2) - mapSize - height/2;
			room = new Room(x*scale, y*scale, width*scale, height*scale);
			rooms.add(room);
		}
		return rooms;
	}
	
	private static void seperateRooms(Array<Room> rooms) {
		Room a, b;
		int dx, dxa, dxb, dy, dya, dyb;
		boolean touching;
		do {
			touching = false;
			for(int i = 0; i < rooms.size; i++) {
				a = rooms.get(i);
				for(int j = i+1; j < rooms.size; j++) {
					b = rooms.get(j);
					if(a.touches(b, padding)) {
						touching = true;
						dx = Math.min(a.getRight()-b.getLeft()+padding, a.getLeft()-b.getRight()-padding);
						dy = Math.min(a.getBottom()-b.getTop()+padding, a.getTop()-b.getBottom()-padding);
						if(Math.abs(dx) < Math.abs(dy)) dy = 0;
						else dx = 0;
						
						dxa = -dx/2;
						dxb = dx+dxa;
						
						dya = -dy/2;
						dyb = dy+dya;

						a.shift(dxa,  dya);
						b.shift(dxb,  dyb);
					}
				}
			}
		} while(touching);
	}
	
    private static Array<Room> findCorridors(Array<Room> rooms) {
    	rooms.sort();
    	Array<Room> corridors = new Array<Room>();
        for(int i = 0; i < corridorCount; i++) {
        	corridors.add(rooms.removeIndex(0));
        }
        return corridors;
    }
	
	private static void centerCorridors(Array<Room> rooms, Array<Room> corridors) {
        int left = Integer.MAX_VALUE, right = Integer.MIN_VALUE;
        int top = Integer.MIN_VALUE, bottom = Integer.MAX_VALUE;
        for(Room corridor : corridors) {
            left = Math.min(left, corridor.getLeft());
            right = Math.max(right, corridor.getRight());
            top = Math.max(top, corridor.getTop());
            bottom = Math.min(bottom, corridor.getBottom());
        }
        int shiftX = (right+left)/2;
        int shiftY = (top+bottom)/2;
        for(Room corridor:corridors)
            corridor.shift(-shiftX, -shiftY);
        for(Room room : rooms)
            room.shift(-shiftX, -shiftY);
    }

    private static DGraph<Room> connectRooms(Array<Room> corridors) {
        Room a, b, c;
        DGraph<Room> graph = new DGraph<Room>();
        double abDist, acDist, bcDist;
        boolean skip;
        for(int i = 0; i < corridors.size; i++) {
            a = corridors.get(i);
            for(int j = i+1; j < corridors.size; j++) {
                skip = false;
                b = corridors.get(j);
                abDist = Math.pow(a.getCenterX()-b.getCenterX(), 2) + Math.pow(a.getCenterY()-b.getCenterY(), 2);
                for(int k = 0;k < corridors.size;k++) {
                    if(k == i || k == j)
                        continue;
                    c = corridors.get(k);
                    acDist = Math.pow(a.getCenterX()-c.getCenterX(), 2) + Math.pow(a.getCenterY()-c.getCenterY(), 2);
                    bcDist = Math.pow(b.getCenterX()-c.getCenterX(), 2) + Math.pow(b.getCenterY()-c.getCenterY(), 2);
                    if(acDist < abDist && bcDist < abDist)
                        skip = true;
                    if(skip)
                        break;
                }
                if(!skip) {
                	if (!graph.hasKey(a)) {
                		graph.addKey(a);
                	}
					float dist = a.getCenter().dst(b.getCenter());						
                	graph.addConnection(a, new Edge<Room>(a, b, dist));
//                    if(graph.get(a) == null)
//                        graph.put(a, new Array<Room>());
//                    graph.get(a).add(b);
                }
            }
        }
        return graph;
    }

    private static Array<Room> createHalls(DGraph<Room> graph) {
        int dx, dy, x, y;
        Room a, b;
        Array<Room> keys = new Array<Room>();
        Array<Room> halls = new Array<Room>();
        keys.addAll(graph.getKeys().toArray());
        keys.sort();
        for(Room key : keys) {
            for(Connection<Room> edge : graph.getConnections(key)) {
            	Room outer = edge.getFromNode();
            	Room inner = edge.getToNode();
            	// make sure starting point is to the left
            	if(outer.getCenterX() < inner.getCenterX()) {
                    a = outer;
                    b = inner;
                } else {
                    a = inner;
                    b = outer;
                }
                x = (int) a.getCenterX();
                y = (int) a.getCenterY();
                dx = (int) b.getCenterX()-x;
                dy = (int) b.getCenterY()-y;
                
                if(random.nextInt(1) == 1) {
                	Room h1 = new Room(x, y, dx+1, hallWidth);
                	Room h2 = new Room(x+dx, y, hallWidth, dy);
                	
                    halls.add(h1);
                    halls.add(h2);
                } else {
                	Room h1 = new Room(x, y+dy, dx+1, hallWidth);
                	Room h2 = new Room(x, y, hallWidth, dy);
                	
                    halls.add(h1);
                    halls.add(h2);
                }
            }
        }
        return halls;
    }
    
    private static Array<Room> removeUntouched(Array<Room> rooms, Array<Room> halls) {
    	Room room;
    	Array<Room> untouched = new Array<Room>();
    	boolean touched;
    	int i = 0;
    	while (i < rooms.size) {
    		room = rooms.get(i);
    		touched = false;
    		for(Room hall : halls) {
    			if(room.touches(hall) && random.nextDouble() <= touchedRoomChance) {
    				touched = true;
    				break;
    			}
    		}
    		if(!touched) {
    			untouched.add(rooms.removeIndex(i));
    		} else {
    			i++;
    		}
    	}
    	return untouched;
    }
    
    /*
     * Send list of corridors, and this will set the starting and ending rooms.
     */
    private static void findStartAndEnd(Array<Room> corridors) {
        Room a, b;
        double maxDist = Double.MIN_VALUE, dist;
        for(int i = 0;i < corridors.size;i++) {
            a = corridors.get(i);
            for(int j = i+1;j < corridors.size;j++) {
                b = corridors.get(j);
                dist = Math.pow(a.getCenterX()-b.getCenterX(), 2) + Math.pow(a.getCenterY()-b.getCenterY(), 2);
                if(dist > maxDist) {
                    maxDist = dist;
                    if(random.nextBoolean()) {
                        start = a;
                        end = b;
                    } else {
                        start = b;
                        end = a;
                    }
                }
            }
        }
}
	
	
	
	private static int getRandomGausInt(int size) {
        double r = random.nextGaussian();
        r *= size/5;
        r += size/2;
        if(r < 0 || r > size)
            return getRandomGausInt(size);
        else
            return (int)r;
    }

    private static int getRandomGausSmallInt(int size) {
        double r = random.nextGaussian();
        r *= size/1.5;
        if(r < 0)
            r *= -1;
        if(r > size)
            return getRandomGausSmallInt(size);
        else
            return (int)r;
    }
	
	private static int getRandomSide() {
		return getRandomGausSmallInt(maxSideLength - minSideLength) + minSideLength;
	}
	
	private DungeonFactory() { } // factories need no instantiation
}
