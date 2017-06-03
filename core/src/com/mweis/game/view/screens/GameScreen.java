package com.mweis.game.view.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.mweis.game.entity.AgentBuilder;
import com.mweis.game.entity.player.PlayerAgent;
import com.mweis.game.entity.player.PlayerState;
import com.mweis.game.gfx.LightSystem;
import com.mweis.game.util.Messages;
import com.mweis.game.view.Screen;
import com.mweis.game.world.Dungeon;
import box2dLight.PointLight;
import box2dLight.RayHandler;

public class GameScreen implements Screen, Telegraph {
	
	private Box2DDebugRenderer renderer = new Box2DDebugRenderer();
	private Dungeon dungeon;
	private OrthographicCamera cam;
	private PlayerAgent player;
	private RayHandler rayHandler;
//	ConeLight light;
	
	@Override
	public void show() {
		MessageManager.getInstance().dispatchMessage(Messages.ANNOUNCE.DUNGEON_NEW);
		rayHandler = new RayHandler(dungeon.getWorld());
		player = new PlayerAgent(new AgentBuilder<PlayerAgent, PlayerState>(dungeon.getStartRoom().getCenter()));
		
		
		PointLight light = new PointLight(rayHandler, 1000); // attached to player
		
//		Array<PointLight> lights = new Array<PointLight>(dungeon.getRooms().size);
//		for (Room room : dungeon.getRooms()) {
//			PointLight l = new PointLight(rayHandler, 5000);
//			l.setDistance(30.0f);
//			l.setPosition(room.getCenter());
//			l.setStaticLight(true);
//			lights.add(new PointLight(rayHandler, 30));
//		}
		
//		light = new ConeLight(rayHandler, 100, Color.WHITE, 60.0f, 0.0f, 0.0f, 0.0f, 60.0f);
		
		light.setDistance(60.0f);
		light.attachToBody(player.getBody());
		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		cam = new OrthographicCamera();
//		cam.setToOrtho(false, 90 * Constants.PPM, 90 * Constants.PPM * (h / w));
		cam.setToOrtho(false, 90, 90 * (h / w));
		cam.update();
		
		MessageManager.getInstance().addListener(this, Messages.INPUT.SCROLLED);
		MessageManager.getInstance().addListener(this, Messages.INPUT.MOUSE_MOVED);
//		MessageManager.getInstance().addListener(this, Messages.INPUT.ONE);
//		MessageManager.getInstance().addListener(this, Messages.INPUT.TWO);
	}

	@Override
	public void render() {
		Vector3 position = cam.position;
//		position.x = player.body.getPosition().x * Constants.PPM;
//		position.y = player.body.getPosition().y * Constants.PPM;
		
		
//		position.x = cam.position.x + (player.getBody().getPosition().x - cam.position.x) * 0.1f;
//		position.y = cam.position.y + (player.getBody().getPosition().y - cam.position.y) * 0.1f;
//		position.lerp(new Vector3(player.getBody().getPosition(), 0.0f), 0.1f);
		position.interpolate(new Vector3(player.getBody().getPosition(), 0.0f), 0.175f, Interpolation.smooth);
		
		cam.position.set(position);
		cam.update();
		
		dungeon.render(cam.combined);
//		renderer.render(dungeon.world, cam.combined.scl(Constants.PPM));
		renderer.render(dungeon.getWorld(), cam.combined);
		rayHandler.setCombinedMatrix(cam);
		rayHandler.render();
	}

	@Override
	public void update() {
		dungeon.getWorld().step(GdxAI.getTimepiece().getDeltaTime(), 8, 3);
		rayHandler.update();
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
		dungeon.getWorld().dispose();
		renderer.dispose();
		rayHandler.dispose();
	}
	
	int passes = 0;
	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Messages.INPUT.SCROLLED) {
			int amount = (Integer) msg.extraInfo;
			cam.zoom += amount * cam.zoom * 0.2;
		} else if (msg.message == Messages.INPUT.MOUSE_MOVED) {
			
			// point light
//			Vector3 sp3 = cam.unproject(new Vector3((Vector2)msg.extraInfo, 0));
//			Vector2 sp2 = new Vector2(sp3.x, sp3.y);
//			Vector2 a = player.getBody().getPosition();
//			Vector2 d = sp2.sub(a);
//			player.getBody().setTransform(player.getBody().getPosition(), d.angleRad());
		}
//		} else if (msg.message == Messages.INPUT.ONE) {
//			passes++;
//			System.out.println(passes);
//			rayHandler.setBlurNum(passes);
//		} else if (msg.message == Messages.INPUT.TWO) {
//			passes--;
//			System.out.println(passes);
//			rayHandler.setBlurNum(passes);
//		}
		return false;
	}
}
