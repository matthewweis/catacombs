package com.mweis.game.util;

/*
 * A list of all messages that can be broadcast/listened too be the Telegram system.
 * As a convention, all messages should be self-describing:
 *  - example: use "Entity.Spawn" instead of "Dungeon.Spawn_Entity"
 *  - use "use Entity.FindPath" instead of "Pathfinder.GetPath"s-u[8hn
 */
public final class Messages {
	
	public final static class INPUT {
		public final static int UP = next();
		public final static int DOWN = next();
		public final static int LEFT = next();
		public final static int RIGHT = next();
		
		public final static int SCROLLED = next(); // extraInfo holds amount
	}
	
	public static final class DUNGEON {
		public final static int LOADED = next();
//		public final static int CLOSED = next();
	}
	
	public static final class ENTITY {
		public final static int SPAWN = next(); // extraInfo holds AgentBuilder
		public final static int UPDATE = next();
	}
	
	public static final class EVENT {
		public final static int ENTER_DUNGEON = next();
		public final static int EXIT_DUNGEON = next();
	}
	
	
	/*
	 * A method to return the next available integer value for the next declared message.
	 * This allows easy insertion of messages without constantly juggling the numbers.
	 */
	private static int next_int = 0;//Integer.MIN_VALUE;
	private final static int next() {
		return next_int++;
	}
	
	private Messages() { };
}
