package com.mweis.game.world;

import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.mweis.game.box2d.Box2dBodyFactory;
import com.mweis.game.util.Messages;

public class Dungeon implements Telegraph {
	
//	private final static Dungeon instance = new Dungeon();
	
	public World world;
	
	public Dungeon() {
		world = new World(Vector2.Zero, true);
		Box2dBodyFactory.createStaticSquare(new Vector2(5, 5), world);
		Box2dBodyFactory.createStaticSquare(new Vector2(5, 25), world);
		Box2dBodyFactory.createStaticSquare(new Vector2(25, 5), world);
		Box2dBodyFactory.createStaticSquare(new Vector2(25, 25), world);
		MessageManager.getInstance().addListener(this, Messages.Dungeon.SPAWN_ENTITY);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Messages.Dungeon.SPAWN_ENTITY) {
			msg.extraInfo = Box2dBodyFactory.createDynamicSquare(world);
			return true;
		}
		return false;
	}
}
