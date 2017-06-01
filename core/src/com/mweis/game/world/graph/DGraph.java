package com.mweis.game.world.graph;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.Graph;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Keys;

public class DGraph<N> implements IndexedGraph<N> {
	private ObjectMap<N, Array<Connection<N>>> nodes;
	private ObjectIntMap<N> indices;
	private int NEXT_INDEX = 0;
	private int DEFAULT = -1;
	
	public DGraph() {
		this.nodes = new ObjectMap<N, Array<Connection<N>>>();
		this.indices = new ObjectIntMap<N>();
	}

	@Override
	public Array<Connection<N>> getConnections(N fromNode) {
		return nodes.get(fromNode);
	}
	
	public void addKey(N key) {
		nodes.put(key, new Array<Connection<N>>());
		indices.put(key, NEXT_INDEX++);
	}
	
//	public void removeKey(N key) {
//		nodes.remove(key);
//		indices.remove(key, DEFAULT);
//	}
	
	public void addConnection(N key, Connection<N> connection) {
		nodes.get(key).add(connection);
	}
	
	public Keys<N> getKeys() {
		return nodes.keys();
	}
	
	public boolean hasKey(N key) {
		return nodes.containsKey(key);
	}

	@Override
	public int getIndex(N node) {
		return indices.get(node, DEFAULT);
	}

	@Override
	public int getNodeCount() {
		return nodes.size;
	}
}
