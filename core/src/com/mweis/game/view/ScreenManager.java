package com.mweis.game.view;

import java.util.Stack;


public class ScreenManager {
		
	private static Stack<Screen> screens = new Stack<Screen>();
	
	public static Screen getCurrentScreen() {
		return screens.peek();
	}
	
	/*
	 * Attempts to return the previous screen, if there is no previous screen nothing will change.
	 */
	public static Screen previousScreen() {
		Screen curr = screens.peek();
		Screen last = screens.pop();
		
		if (last != null) {
			curr.dispose();
			last.resume();
		} else {
			setScreen(curr);
		}
		
		return screens.peek();
	}
	
	public static void setScreen(Screen screen) {
		screens.push(screen);
		screen.show();
	}
	
	private ScreenManager() { } // PREVENT INSTANTIATION OF THIS CLASS
}
