package com.mweis.game.util;

/*
 * MESSAGE CONVENTION
 * ==================
 * All messages prefixed with one of the following: ANNOUNCE_, NOTIFY_, REQUEST_
 * The prefix should indicate the behavior that message will entail.
 * 
 * ANNOUNCE - send to EVERYONE who is subscribed to this type of message. All subscribers should get this.
 * SOLICIT - send to EVERYONE who is subscribed, asking for a REPLY (although it might not be fulfilled)
 * 
 * NOTIFY - send to ONE person, they do not need to be subscribed. YOU MUST HAVE A REFRENCE TO WHO THE MESSAGE IS GOING TO.
 * REQUEST - send to ONE person, and request a REPLY. they do not need to be subscribed.
 * 
 * NOTES ABOUT MESSAGES
 * ======================
 * Returning true in handleMessage(..) does NOT mean that other subscribers won't get a chance to see that message
 * (so return true whenever a message is handled)
 * Classes implementing Telegraph WILL NOT BE GARBAGE COLLECTED until unsubscribed from that message
 * 
 */
public final class Messages {
	
	/*
	 * EVENTUALLY INPUT HANDLER WILL CONVERT THESE TO ACTIONS!
	 * InputHandler will eventually need an instance of the current screen.
	 */
	public static final class INPUT {
		public static final int UP = next();
		public static final int DOWN = next();
		public static final int LEFT = next();
		public static final int RIGHT = next();
		
		public static final int ONE = next();
		public static final int TWO = next();
		
		public static final int SCROLLED = next(); // extraInfo holds amount
		public static final int MOUSE_MOVED = next(); // extraInfo holds Vector2 w/ ScreenX, ScreenY
	}
	
	/*
	 * 
	 */
	public static final class ANNOUNCE {
		public static final int DUNGEON_NEW = next(); // sent when a new Dungeon is wanted.
		public static final int DUNGEON_LOADED = next(); // sent when Dungeon is done loading
		public static final int LIGHT_PLACED = next(); // extraInfo: Light
	}
	
	/*
	 * 
	 */
	public static final class SOLICIT {
		public static final int ENTITY_SPAWNED = next(); // extraInfo holds AgentBuilder
	}
	
	/*
	 * 
	 */
	public static final class NOTIFY {
		public static final int UPDATE = next();
	}
	
	/*
	 * 
	 */
	public static final class REQUEST {
		
	}
	
//	public static final class DUNGEON {
//		public static final int ANNOUNCE_LOADED = next();
//		public static final int ANNOUNCE_CLOSED = next();
//	}
//	
//	public static final class ENTITY {
//		public static final int SOLICIT_SPAWN = next(); // extraInfo holds AgentBuilder
//		public static final int NOTIFY_UPDATE = next();
//	}
//	
//	/*
//	 * Generic Messages. These can come from any class.
//	 */
//	public static final class ACTION {
//		public static final int ANNOUNCE_NEW_LIGHT = next(); // extraInfo holds Light
////		public static final int ANNOUNCE_EXTINGUISH_LIGHT = next();
//	}
//	
//	public static final class EVENT {
//		public static final int ENTER_DUNGEON = next();
//		public static final int EXIT_DUNGEON = next();
//	}
	
	/*
	 * A method to return the next available integer value for the next declared message.
	 * This allows easy insertion of messages without constantly juggling the numbers.
	 */
	private static int next_int = 0;//Integer.MIN_VALUE;
	private static final int next() {
		return next_int++;
	}
	
	private Messages() { };
}
