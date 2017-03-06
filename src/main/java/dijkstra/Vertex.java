package dijkstra;

public class Vertex {
	final private String name;
	
	public Vertex(String identifier)
	{
		this.name = identifier;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public boolean equals(Vertex v)
	{
		return this.name.equals(v.name);
	}
}
