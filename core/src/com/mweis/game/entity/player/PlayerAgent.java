package com.mweis.game.entity.player;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.mweis.game.entity.Agent;
import com.mweis.game.entity.AgentBuilder;
import com.mweis.game.util.Messages;

public class PlayerAgent extends Agent<PlayerAgent, PlayerState> {
	
	public PlayerAgent(AgentBuilder<PlayerAgent, PlayerState> builder) {
		super(builder);
		super.setStateMachine(new DefaultStateMachine<PlayerAgent, PlayerState>(this));
		super.getStateMachine().changeState(PlayerState.DEFAULT);
//		MessageManager.getInstance().addListener(this, Messages.ENTITY.UPDATE); // ONLY SUBSCRIBE TO UNTARGETED MESSAGES
	}	
}
