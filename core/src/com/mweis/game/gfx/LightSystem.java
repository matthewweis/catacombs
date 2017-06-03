package com.mweis.game.gfx;

import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.mweis.game.util.Messages;

public class LightSystem implements Telegraph {
	
	public static LightSystem instance = new LightSystem();
	
	private LightSystem() {
//		MessageManager.getInstance().addListener(this, Messages.);
	}
	
	
	@Override
	public boolean handleMessage(Telegram msg) {
		
		return false;
	}
}
