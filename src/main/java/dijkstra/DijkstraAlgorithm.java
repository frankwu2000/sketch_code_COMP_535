package dijkstra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DijkstraAlgorithm {
	
	private final List<Vertex> nodes;
	private final List<Edge> edges;
	private Set<Vertex> solved;
	private Set<Vertex> processing;
	private Map<Vertex, Vertex> predecessors;
	private Map<Vertex, Integer> distance;
	
	public DijkstraAlgorithm(Graph graph)
	{
		this.nodes = new ArrayList<Vertex>(graph.getVertices());
		this.edges = new ArrayList<Edge>(graph.getEdges());
	}
	
	// This executes Dijkstra's algorithm on source vertex with identifier sourceName
	public void execute(String sourceName)
	{
		Vertex source = null;
		for (Vertex v : this.nodes)
		{
			if (v.getName().equals(sourceName))
				source = v;
		}
		solved = new HashSet<Vertex>();
		processing = new HashSet<Vertex>();
		predecessors = new HashMap<Vertex, Vertex>();
		distance = new HashMap<Vertex, Integer>();
		distance.put(source, 0);
		processing.add(source);
		while (processing.size() > 0)
		{
			Vertex node = getMin(processing);
			solved.add(node);
			processing.remove(node);
			findMinDist(node);
		}
	}
	
	private void findMinDist(Vertex node)
	{
		List<Vertex> neighbors = getNeighbors(node);
		for (Vertex v : neighbors)
		{
			if (getShortestDistance(v) > getShortestDistance(node) + getDist(node,v))
			{
<<<<<<< HEAD
				//2017-04-03 debug
=======
>>>>>>> origin/master
				distance.put(v, getDist(node,v));
				predecessors.put(v, node);
				processing.add(v);
			}
		}
	}
	
	private int getDist(Vertex node, Vertex target)
	{
		for (Edge edge : edges)
		{
			if (edge.getSource().equals(node) && edge.getDest().equals(target))
			{
				return edge.getWeight();
			}
		}
		throw new RuntimeException("Should not happen");
	}
	
	private List<Vertex> getNeighbors(Vertex node)
	{
		List<Vertex> neighbors = new ArrayList<Vertex>();
		for (Edge edge : edges)
		{
			if (edge.getSource().equals(node) && !isSolved(edge.getDest()))
			{
				neighbors.add(edge.getDest());
			}
		}
		return neighbors;
	}
	
	private Vertex getMin(Set<Vertex> vertices)
	{
		Vertex min = null;
		for (Vertex vertex : vertices)
		{
			if (min == null)
			{
				min = vertex;
			}
			else if (getShortestDistance(vertex) < getShortestDistance(min))
			{
				min = vertex;
			}
		}
		return min;
	}
	
	private boolean isSolved(Vertex vertex)
	{
		return solved.contains(vertex);
	}
	
	private int getShortestDistance(Vertex dest)
	{
		Integer d = distance.get(dest);
		if (d == null)
		{
			return Integer.MAX_VALUE;
		}
		else
		{
			return d;
		}
	}
	
	// This returns the shortest path from source to target as a LinkedList of vertices
	public LinkedList<Vertex> getPath(String targetName)
	{
		Vertex target = null;
		for (Vertex v : this.nodes)
		{
			if (v.getName().equals(targetName))
				target = v;
		}
		if (target == null)
		{
			throw new RuntimeException("Something went wrong finding target Vertex");
		}
		LinkedList<Vertex> path = new LinkedList<Vertex>();
		Vertex step = target;
		if (predecessors.get(step) == null)
			return null;
		path.add(step);
		while (predecessors.get(step) != null)
		{
			step = predecessors.get(step);
			path.add(step);
		}
		Collections.reverse(path);
		return path;
	}
}
