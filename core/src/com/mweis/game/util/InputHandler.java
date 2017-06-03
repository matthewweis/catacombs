package com.mweis.game.util;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.math.Vector2;

/*
 * Conveys input as a message to any subscribers.
 * For simplicity the InputHandler will convert input into commands (i.e. press A = move_left), this will eventually be
 * abstracted away
 */
public class InputHandler implements InputProcessor {
	
	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Keys.W || keycode == Keys.UP) {
			MessageManager.getInstance().dispatchMessage(Messages.INPUT.UP);
			return true;
		} else if (keycode == Keys.S || keycode == Keys.DOWN) {
			MessageManager.getInstance().dispatchMessage(Messages.INPUT.DOWN);
			return true;
		} else if (keycode == Keys.A || keycode == Keys.LEFT) {
			MessageManager.getInstance().dispatchMessage(Messages.INPUT.LEFT);
			return true;
		} else if (keycode == Keys.D || keycode == Keys.RIGHT) {
			MessageManager.getInstance().dispatchMessage(Messages.INPUT.RIGHT);
			return true;
		} else if (keycode == Keys.NUM_1) {
			MessageManager.getInstance().dispatchMessage(Messages.INPUT.ONE);
			return true;
		} else if (keycode == Keys.NUM_2) {
			MessageManager.getInstance().dispatchMessage(Messages.INPUT.TWO);
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
	
	Vector2 mousePooled = new Vector2();
	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		mousePooled.set(screenX, screenY);
		MessageManager.getInstance().dispatchMessage(Messages.INPUT.MOUSE_MOVED, mousePooled);
		return true;
	}

	@Override
	public boolean scrolled(int amount) {
		MessageManager.getInstance().dispatchMessage(Messages.INPUT.SCROLLED, (Integer) amount);
		return true;
	}
}
