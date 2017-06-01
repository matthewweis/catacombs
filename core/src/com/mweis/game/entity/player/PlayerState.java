package com.mweis.game.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.physics.box2d.Body;
import com.mweis.game.util.Messages;

public enum PlayerState implements State<PlayerAgent> {
	DEFAULT() {
		@Override
		public void enter(PlayerAgent entity) {
			MessageManager.getInstance().dispatchMessage(null, entity, Messages.Entity.UPDATE);
		}

		@Override
		public void update(PlayerAgent entity) {
			
//			System.out.println(GdxAI.getTimepiece().getDeltaTime());
//			System.out.println(1/60.0f == GdxAI.getTimepiece().getDeltaTime());
			
			float vx = 0.0f, vy = 0.0f, scale = 20.0f;
			if (Gdx.input.isKeyPressed(Keys.W)) {
				vy += 1.0f * scale;
			}
			if (Gdx.input.isKeyPressed(Keys.S)) {
				vy -= 1.0f * scale;
			}
			if (Gdx.input.isKeyPressed(Keys.A)) {
				vx -= 1.0f * scale;
			}
			if (Gdx.input.isKeyPressed(Keys.D)) {
				vx += 1.0f * scale;
			}
//			System.out.println(entity.body.getPosition());
			
			entity.body.setLinearVelocity(vx, vy);
			
			MessageManager.getInstance().dispatchMessage(1/60.0f, null, entity, Messages.Entity.UPDATE);
		}

		@Override
		public void exit(PlayerAgent entity) {
			
		}

		@Override
		public boolean onMessage(PlayerAgent entity, Telegram telegram) {
			if (telegram.message == Messages.Entity.UPDATE) {
				entity.fsm.update();
				return true;
			}
			return false;
		}
	};
}