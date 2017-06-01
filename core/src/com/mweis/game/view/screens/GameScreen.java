package com.mweis.game.view.screens;

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.mweis.game.entity.player.PlayerAgent;
import com.mweis.game.view.Screen;
import com.mweis.game.world.Dungeon;

public class GameScreen implements Screen {
	
	private Box2DDebugRenderer renderer = new Box2DDebugRenderer();
	private Dungeon dungeon = new Dungeon();
	private OrthographicCamera cam;
	private PlayerAgent player;
	
	@Override
	public void show() {
		player = new PlayerAgent();
		
//		float w = Gdx.graphics.getWidth();
//		float h = Gdx.graphics.getHeight();
//		cam = new OrthographicCamera();
//		cam.setToOrtho(false, w / 2, h / 2);
//		cam.update();
		float w = 720.0f/8.0f; //  /8 = 90
		float h = 480.0f/8.0f; //  /8 = 60
		cam = new OrthographicCamera(w, h);
	}

	@Override
	public void render() {
		cam.position.set(player.body.getPosition(), 0.0f);
		cam.update();
		
		renderer.render(dungeon.world, cam.combined);
	}

	@Override
	public void update() {
		dungeon.world.step(GdxAI.getTimepiece().getDeltaTime(), 8, 3);
	}

	@Override
	public void resize(int width, int height) {
		
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
}
