package com.mweis.game.box2d;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

public class Box2dBodyFactory {
	
	public static Body createDynamicSquare(World world) {
		BodyDef bodyDef = new BodyDef();
		
		bodyDef.type = BodyType.DynamicBody;
		
//		bodyDef.position.set(position);

		Body body = world.createBody(bodyDef);
		
		PolygonShape polygon = new PolygonShape();
		polygon.setAsBox(2.0f, 2.0f);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygon;
//		fixtureDef.density = density;
//		fixtureDef.isSensor = isSensor;
		
		Fixture fixture = body.createFixture(fixtureDef);

		polygon.dispose();
		
		return body;
	}
	
	public static Body createStaticSquare(Vector2 position, World world) {
		BodyDef bodyDef = new BodyDef();
		
		bodyDef.type = BodyType.StaticBody;
		
		bodyDef.position.set(position);

		Body body = world.createBody(bodyDef);
		
		PolygonShape polygon = new PolygonShape();
		polygon.setAsBox(3.5f, 3.5f);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygon;
//		fixtureDef.density = density;
//		fixtureDef.isSensor = isSensor;
		
		Fixture fixture = body.createFixture(fixtureDef);

		polygon.dispose();
		
		return body;
	}
	
	private Box2dBodyFactory() { };
}
