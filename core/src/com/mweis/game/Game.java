package com.mweis.game;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.graphics.GL20;
import com.mweis.game.util.InputHandler;
import com.mweis.game.view.ScreenManager;
import com.mweis.game.view.screens.GameScreen;

/*
 * roadmap
 * ==========================
 * Setup base w/ Messages (DONE) 
 * Setup b2d WSAD world (DONE)
 * Setup b2d from gen world
 * Setup base entity (only what is needed for tests, NO STEERING YET)
 * setup colls w/ world
 * setup ai pathing (using gdx ai scheduling)
 * setup shadows, and vision
 * setup ai logic with state machines where enemies are selected based on Steer[] getVisibleTargets()
 (recall Seek needs a Location<Vector<T>> and not a SteeringBehavior)
 */

public class Game implements ApplicationListener {
	
	private static final float DELTA_TIME = 1 / 60f;
	private float accumulator = 0.0f;
	
	@Override
	public void create() {
		Gdx.graphics.setTitle("Game");
		ScreenManager.setScreen(new GameScreen());
		Gdx.input.setInputProcessor(new InputHandler());
	}

	@Override
	public void resize(int width, int height) {
		ScreenManager.getCurrentScreen().resize(width, height);
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		float deltaTime = Gdx.graphics.getDeltaTime(); // this is the "real" dt, used to allow constant DT steps
		
		if (deltaTime > 0.25f) { deltaTime = 0.25f; }
		
		accumulator += deltaTime;
		
		while (accumulator >= DELTA_TIME) {
			GdxAI.getTimepiece().update(DELTA_TIME);
			ScreenManager.getCurrentScreen().update();
			MessageManager.getInstance().update();
			accumulator -= DELTA_TIME;
		}
		
		ScreenManager.getCurrentScreen().render();
	}

	@Override
	public void pause() {
		ScreenManager.getCurrentScreen().pause();
	} 

	@Override
	public void resume() {
		ScreenManager.getCurrentScreen().resume();
	}

	@Override
	public void dispose() {
		// NOTE: ScreenManager handles disposal of screens on change. 
		// This only exists for the final screen to close (Graphic resource dump on close)
		ScreenManager.getCurrentScreen().dispose();
	}
}