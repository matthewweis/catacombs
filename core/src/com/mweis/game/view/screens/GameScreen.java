package com.mweis.game.view.screens;

import com.badlogic.gdx.Gdx;
import com.mweis.game.view.Screen;

public class GameScreen implements Screen {

	@Override
	public void show() {
		Gdx.app.log("show", "called");
	}

	@Override
	public void render() {
		Gdx.app.log("render", "called");
	}

	@Override
	public void update() {
		Gdx.app.log("update", "called");
	}

	@Override
	public void resize(int width, int height) {
		Gdx.app.log("resize", "called");
	}

	@Override
	public void pause() {
		Gdx.app.log("pause", "called");
	}

	@Override
	public void resume() {
		Gdx.app.log("resume", "called");
	}

	@Override
	public void hide() {
		Gdx.app.log("hide", "called");
	}

	@Override
	public void dispose() {
		Gdx.app.log("dispose", "");
	}
}
