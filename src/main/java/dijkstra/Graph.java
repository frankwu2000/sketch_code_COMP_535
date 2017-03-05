package dijkstra;

import java.util.List;

public class Graph {
	private final List<Vertex> vertices;
	private final List<Edge> edges;
	
	public Graph(List<Vertex> vertices, List<Edge> edges)
	{
		this.vertices = vertices;
		this.edges = edges;
	}
	
	public List<Vertex> getVertices()
	{
		return vertices;
	}
	
	public List<Edge> getEdges()
	{
		return edges;
	}
	
	public int weightByVertices(Vertex a, Vertex b)
	{
		for (Edge e : edges)
		{
			if ((e.getSource().equals(a) && e.getDest().equals(b)) || (e.getSource().equals(b) && e.getDest().equals(a)))
			{
				return e.getWeight();
			}
		}
		return -1;
	}
}
