package com.mweis.game.entity;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.math.Vector2;

/*
 * Has no build method. Instead the information here is spread to different systems.
 */
public class AgentBuilder<A extends Agent<A, S>, S extends State<A>> {
	private Vector2 spawnPosition; // required
	
	public AgentBuilder(Vector2 spawnPosition) {
		this.spawnPosition = spawnPosition;
	}
		
}
