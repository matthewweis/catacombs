package com.mweis.game.box2d;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.mweis.game.util.Constants;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.EdgeShape;

public class Box2dBodyFactory {
	
	public static Body createDynamicSquare(Vector2 position, World world) {
		BodyDef bodyDef = new BodyDef();
		
		bodyDef.type = BodyType.DynamicBody; // could be kinematic
		
//		bodyDef.position.set(position.cpy().scl(Constants.MPP));
		bodyDef.position.set(position);

		Body body = world.createBody(bodyDef);
		
		PolygonShape polygon = new PolygonShape();
//		polygon.setAsBox(32.0f / Constants.PPM, 32.0f / Constants.PPM); // this is half-width and half-height, thus a 2x2 meter box
		polygon.setAsBox(1.0f, 1.0f); // this is half-width and half-height, thus a 2x2 meter box

		
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygon;
//		fixtureDef.density = density;
//		fixtureDef.isSensor = isSensor;
		
		Fixture fixture = body.createFixture(fixtureDef);

		polygon.dispose();
		
		return body;
	}
	
	
	
	public static Body createStaticSquare(Vector2 position, float size, World world) {
		BodyDef bodyDef = new BodyDef();
		
		bodyDef.type = BodyType.StaticBody;
		
//		bodyDef.position.set(position.cpy().scl(Constants.MPP));
		bodyDef.position.set(position);


		Body body = world.createBody(bodyDef);
		
		PolygonShape polygon = new PolygonShape();
//		polygon.setAsBox(size / 2 / Constants.PPM, size / 2 / Constants.PPM);
		polygon.setAsBox(size / 2, size / 2);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygon;
//		fixtureDef.density = density;
//		fixtureDef.isSensor = isSensor;
		
		Fixture fixture = body.createFixture(fixtureDef);

		polygon.dispose();
		
		return body;
	}
	
	public static Body createStaticRectangle(float x, float y, float width, float height, World world) {
		BodyDef bodyDef = new BodyDef();
		
		bodyDef.type = BodyType.StaticBody;
		
//		bodyDef.position.set(position.cpy().scl(Constants.MPP));
		bodyDef.position.set(x + width/2, y + height/2);


		Body body = world.createBody(bodyDef);
		
		PolygonShape polygon = new PolygonShape();
//		polygon.setAsBox(size / 2 / Constants.PPM, size / 2 / Constants.PPM);
		polygon.setAsBox(width / 2, height / 2);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygon;
//		fixtureDef.density = density;
//		fixtureDef.isSensor = isSensor;
		
		Fixture fixture = body.createFixture(fixtureDef);

		polygon.dispose();
		
		return body;
	}
	
	public static Body createEdge(Vector2 v1, Vector2 v2, World world) {
		BodyDef bodyDef = new BodyDef();
		
		bodyDef.type = BodyType.StaticBody;
		
		// center of line segment
//		float posx = (p1.x + p2.x)/2f;
//		float posy = (p1.y + p2.y)/2f;
//		
//		// calculate length of line segment
//		float len = (float) Math.sqrt((p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y));
		
//		Vector2 v1c = new Vector2(v1);
//		v1c.x /= Constants.PPM;
//		v1c.y /= Constants.PPM;
//		
//		Vector2 v2c = new Vector2(v2);
//		v2c.x /= Constants.PPM;
//		v2c.y /= Constants.PPM;
		
//		bodyDef.position.set(v1);

		Body body = world.createBody(bodyDef);
		
		EdgeShape edge = new EdgeShape();
		
//		v1.scl(Constants.MPP);
//		v1.scl(Constants.MPP;
//		edge.set(v1.cpy().scl(Constants.MPP), v2.cpy().scl(Constants.MPP));
		edge.set(v1, v2);

//		edge.set(v1c, v2c);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = edge;
//		fixtureDef.density = density;
//		fixtureDef.isSensor = isSensor;
		
		Fixture fixture = body.createFixture(fixtureDef);

		edge.dispose();
		
		return body;
	}
	
	private Box2dBodyFactory() { };
}
