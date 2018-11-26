package spy.g6;

import java.util.ArrayList;
import java.util.HashMap;

import spy.sim.Point;


// A* pathfinding
public class PathFinder {
	
	private class Node implements Comparable<Object>{
		int x, y;
		int g = -1;
		int h;
		Node parent = null;
		
		Node(Point p){
			x = p.x;
			y = p.y;
		}
		
		Node(int x, int y){
			this.x = x;
			this.y = y;
		}
		
		@Override
		public int compareTo(Object obj) {
			Node objnode = (Node)obj;
			int f = g + h;
			int fobj = objnode.g + objnode.h;
			return (f > fobj) ? 1 : ((f == fobj) ? 0 : -1);
		}
		
		@Override
		public boolean equals(Object obj) {
			try {
				Node objnode = (Node)obj;
				return objnode.x == x && objnode.y == y;
			}
			catch(Exception e) {
				return false;
			}
		}
	}
	
	protected ArrayList<Node> openList;
	protected ArrayList<Node> closedList;
	protected ArrayList<ArrayList<Node>> nodes;
	protected int[][] map;
	protected Point start, end;
	
	/**
	 * 0 for unknown, -1 for water, 1 for normal and 2 for muddy
	 * @param map
	 */
	public PathFinder(int[][] map) {
		// TODO dynamic
		this.map = map;
	}
	
	public void setObjective(Point start, Point end) {
		this.start = start;
		this.end = end;
		openList = new ArrayList<Node>();
		closedList = new ArrayList<Node>();
		nodes = new ArrayList<ArrayList<Node>>(map.length);
		for (int i = 0; i < map.length; ++i) {
			ArrayList<Node> row = new ArrayList<Node>(map[0].length);
			for (int j = 0; j < map[0].length; ++j)
				row.set(j, new Node(i, j));
			nodes.set(i, row);
		}
	}
	
	public boolean startSearch(){
		addToOpenList(new Node(start));
		// Find path
		return search();
	}
	
	protected boolean search(){
		Node node;	// Searching one node at a time
		while (!openList.isEmpty()){
			// Get best node from Open list (least f(n))
			node = openList.get(0);
			
			// Place parent on the closed List
			openList.remove(node); closedList.add(node);
			
			if (node.x == end.x && node.y == end.y){
				return true;
			}
			
			// Expand parent to all adjacent nodes
			if (isValid(node.x, node.y - 1)) updateAdjNode(nodes.get(node.x).get(node.y - 1), node, false);
			if (isValid(node.x, node.y + 1)) updateAdjNode(nodes.get(node.x).get(node.y + 1), node, false);
			if (isValid(node.x + 1, node.y)) updateAdjNode(nodes.get(node.x - 1).get(node.y), node, false);
			if (isValid(node.x - 1, node.y)) updateAdjNode(nodes.get(node.x + 1).get(node.y), node, false);
			
			if (isValid(node.x, node.y - 1)) updateAdjNode(nodes.get(node.x - 1).get(node.y - 1), node, true);
			if (isValid(node.x, node.y + 1)) updateAdjNode(nodes.get(node.x - 1).get(node.y + 1), node, true);
			if (isValid(node.x + 1, node.y)) updateAdjNode(nodes.get(node.x + 1).get(node.y - 1), node, true);
			if (isValid(node.x - 1, node.y)) updateAdjNode(nodes.get(node.x + 1).get(node.y + 1), node, true);
		}
		return false;
	}
	
	/**
	 * To test whether the node corresponding to the given coordinates is on the grid and not a barrier
	 * @param x the x coordinate of the node
	 * @param y the y coordinate of the node
	 * @return <b>TRUE</b> if the node is accessible <br>
	 * 		   <b>FALSE</b> if the node does not exist or it represents a barrier
	 */
	protected boolean isValid(int x, int y){
		return ((x >= 0) && (x < map.length) && (y >= 0) && (y < map[0].length) && (map[x][y] != -1));
	}
	
	/**
	 * Update one adjacent node from the current node.
	 * @param adjNode the adjacent node
	 * @param node the current node
	 * @param diag diagonally adjacent
	 */
	protected void updateAdjNode(Node adjNode, Node node, boolean diag){
		if (closedList.contains(adjNode))
			return;
		else {
			// Travel time between two nodes
			int cost = (diag) ? 3 : 2;
			boolean isMuddy = map[node.x][node.y] == 2 || map[adjNode.x][adjNode.y] == 2;
			// Add exploring
			if (isMuddy)
				cost *= 2;
			
			if (openList.contains(adjNode)){
				// If this path is better than previous path, discard the previous path
				if (adjNode.g > node.g + cost){
					adjNode.g = node.g + cost;
					adjNode.parent = node;
				}
			}
			else{
				adjNode.g = node.g + cost;
				adjNode.parent = node;
				adjNode.h = findH(adjNode);
				openList.add(adjNode);
				addToOpenList(adjNode);
			}
		}
	}
	
	/**
	 * Add a node into open list and insertion-sort the list.
	 * @param node the node to be added
	 */
	protected void addToOpenList(Node node){
		for (int i = 0; i < openList.size(); ++i)
			if (node.compareTo(openList.get(i)) < 0) {
				openList.add(i, node);
				return;
			}
		openList.add(openList.size(), node);
	}
	
	/**
	 * Find the heuristic of a node, currently using Manhattan distance. TODO
	 * @param node the node to be tested
	 * @return the Manhattan distance between that node and the goal
	 */
	protected int findH(Node node){
		return Math.abs(node.x - end.x) + Math.abs(node.y - end.y);
	}
	
}
