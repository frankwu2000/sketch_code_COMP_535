package dijkstra;

public class Edge {
	private final Vertex source;
	private final Vertex dest;
	private final int weight;
	
	public Edge(Vertex source, Vertex dest, int weight)
	{
		this.source = source;
		this.dest = dest;
		this.weight = weight;
	}
	
	public Vertex getSource()
	{
		return source;
	}
	
	public Vertex getDest()
	{
		return dest;
	}
	
	public int getWeight()
	{
		return weight;
	}
	
	public Edge reverse()
	{
		return new Edge(this.dest, this.source, this.weight);
	}
	
	public boolean equals(Edge e)
	{
		return (this.source.equals(e.source) && this.dest.equals(e.dest) && this.weight == e.weight);
	}
}
