package com.mweis.game.entity;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.physics.box2d.Body;
import com.mweis.game.util.Messages;

public abstract class Agent<A extends Agent<A, S>, S extends State<A>> implements Telegraph {
	private Body body;
	private StateMachine<A, S> stateMachine;
	

	public Agent(AgentBuilder<A, S> builder) {
		MessageManager.getInstance().dispatchMessage(this, Messages.SOLICIT.ENTITY_SPAWNED, builder, true);
	}

	public Body getBody() {
		return this.body;
	}
	
	protected void setBody(Body body) {
		this.body = body;
	}
	
	public StateMachine<A, S> getStateMachine() {
		return this.stateMachine;
	}
	
	protected void setStateMachine(StateMachine<A, S> stateMachine) {
		this.stateMachine = stateMachine;
	}
	
	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Messages.SOLICIT.ENTITY_SPAWNED) {
			if (msg.extraInfo instanceof Body) { // reply from dungeon
				this.body = (Body) msg.extraInfo;
			}
			return true;
		}
		return stateMachine.handleMessage(msg);
	}
}
