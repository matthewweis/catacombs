package com.mweis.game.util;

public final class Messages {
	
	public final static class Input {
		public final static int UP = next();
		public final static int DOWN = next();
		public final static int LEFT = next();
		public final static int RIGHT = next();
		
		public final static int SCROLLED = next(); // extraInfo holds amount
	}
	
	public static final class Dungeon {
		/*
		 * if "needs return reciept" is true then will return Body
		 */
		public final static int SPAWN_ENTITY = next();
	}
	
	public static final class Entity {
		public final static int UPDATE = next();
	}
	
	
	/*
	 * A method to return the next available integer value for the next declared message.
	 * This allows easy insertion of messages without constantly juggling the numbers.
	 */
	private static int next_int = Integer.MIN_VALUE;
	private final static int next() {
		return next_int++;
	}
	
	private Messages() { };
}
