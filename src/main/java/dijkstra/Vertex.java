package dijkstra;

public class Vertex {
	final private String name;
	
	public Vertex(String ip_addr)
	{
		this.name = ip_addr;
	}
	
	public String getName()
	{
		return name;
	}
	
	public boolean equals(Vertex v)
	{
		return this.name.equals(v.name);
	}
}
