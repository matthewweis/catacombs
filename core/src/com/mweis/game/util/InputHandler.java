package com.mweis.game.util;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.MessageManager;

/*
 * Conveys input as a message to any subscribers.
 * For simplicity the InputHandler will convert input into commands (i.e. press A = move_left), this will eventually be
 * abstracted away
 */
public class InputHandler implements InputProcessor {
	
	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Keys.W || keycode == Keys.UP) {
			MessageManager.getInstance().dispatchMessage(Messages.Input.UP);
			return true;
		} else if (keycode == Keys.S || keycode == Keys.DOWN) {
			MessageManager.getInstance().dispatchMessage(Messages.Input.DOWN);
			return true;
		} else if (keycode == Keys.A || keycode == Keys.LEFT) {
			MessageManager.getInstance().dispatchMessage(Messages.Input.LEFT);
			return true;
		} else if (keycode == Keys.D || keycode == Keys.RIGHT) {
			MessageManager.getInstance().dispatchMessage(Messages.Input.RIGHT);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}
