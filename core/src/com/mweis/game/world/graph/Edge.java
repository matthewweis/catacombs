package com.mweis.game.world.graph;

import com.badlogic.gdx.ai.pfa.Connection;

public class Edge<N> implements Connection<N> {
	private N fromNode, toNode;
	private float cost;
	
	public Edge(N fromNode, N toNode, float cost) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.cost = cost;
	}

	@Override
	public float getCost() {
		return cost;
	}

	@Override
	public N getFromNode() {
		return fromNode;
	}

	@Override
	public N getToNode() {
		return toNode;
	}
}
