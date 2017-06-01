package com.mweis.game.entity.player;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.physics.box2d.Body;
import com.mweis.game.util.Messages;

public class PlayerAgent implements Telegraph {
	
	public Body body;
	public StateMachine<PlayerAgent, PlayerState> fsm;
	
	public PlayerAgent() {
		this.body = null;
		MessageManager.getInstance().dispatchMessage(this, Messages.Dungeon.SPAWN_ENTITY, true);
		
		this.fsm = new DefaultStateMachine<PlayerAgent, PlayerState>(this);
		fsm.changeState(PlayerState.DEFAULT);
		MessageManager.getInstance().addListener(this, Messages.Entity.UPDATE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Messages.Dungeon.SPAWN_ENTITY) {
			this.body = (Body) msg.extraInfo;
			return true;
		}
		return fsm.handleMessage(msg);
	}
	
}
