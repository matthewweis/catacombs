package com.mweis.game.view.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.mweis.game.entity.player.PlayerAgent;
import com.mweis.game.util.Constants;
import com.mweis.game.util.Messages;
import com.mweis.game.view.Screen;
import com.mweis.game.world.Dungeon;
import com.mweis.game.world.DungeonFactory;

public class GameScreen implements Screen, Telegraph {
	
	private Box2DDebugRenderer renderer = new Box2DDebugRenderer();
	private Dungeon dungeon = DungeonFactory.generateDungeon();
	private OrthographicCamera cam;
	private PlayerAgent player;
	
	@Override
	public void show() {
		player = new PlayerAgent();
		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		cam = new OrthographicCamera();
//		cam.setToOrtho(false, 90 * Constants.PPM, 90 * Constants.PPM * (h / w));
		cam.setToOrtho(false, 90, 90 * (h / w));
		cam.update();
		
		MessageManager.getInstance().addListener(this, Messages.Input.SCROLLED);
	}

	@Override
	public void render() {
		Vector3 position = cam.position;
//		position.x = player.body.getPosition().x * Constants.PPM;
//		position.y = player.body.getPosition().y * Constants.PPM;
		position.x = player.body.getPosition().x;
		position.y = player.body.getPosition().y;
		cam.position.set(position);
		cam.update();
		
		dungeon.render(cam.combined);
//		renderer.render(dungeon.world, cam.combined.scl(Constants.PPM));
		renderer.render(dungeon.world, cam.combined);
	}

	@Override
	public void update() {
		dungeon.world.step(GdxAI.getTimepiece().getDeltaTime(), 8, 3);
	}

	@Override
	public void resize(int width, int height) {
//		cam.viewportWidth = 90f * Constants.PPM;
//		cam.viewportHeight = 90f * Constants.PPM * height / width;
		cam.viewportWidth = 90f;
		cam.viewportHeight = 90f * height / width;
		cam.update();
	}

	@Override
	public void pause() {
		
	}

	@Override
	public void resume() {
		
	}

	@Override
	public void hide() {
		
	}

	@Override
	public void dispose() {
		dungeon.world.dispose();
		renderer.dispose();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Messages.Input.SCROLLED) {
			int amount = (Integer) msg.extraInfo;
			cam.zoom += amount * cam.zoom * 0.2;
		}
		return false;
	}
}
