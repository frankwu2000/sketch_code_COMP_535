package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dijkstra.*;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  String getShortestPath(String destinationIP) {
	  List<Vertex> vertices = new ArrayList<Vertex>();
	  List<Edge> edges = new ArrayList<Edge>();
	  
	  // Add all vertices into the graph
	  for (Map.Entry<String, LSA> entry : _store.entrySet())
	  {
		  vertices.add(new Vertex(entry.getKey()));
		  /*
		  for (int i = 0; i < entry.getValue().links.size(); i++)
		  {
			  Vertex source = new Vertex(entry.getKey());
			  Vertex dest = new Vertex(entry.getValue().links.get(i).linkID);
			  if (!source.equals(dest))
			  {
				  Edge new_edge = new Edge(source,dest,entry.getValue().links.get(i).tosMetrics);
				  edges.add(new_edge);
			  }
		  }*/
	  }
	  // Populate the graph with edges, now that the vertices are present
	  for (Map.Entry<String, LSA> entry : _store.entrySet())
	  {
		  for (int i = 0; i < entry.getValue().links.size(); i++)
		  {
			  Vertex source = new Vertex("");
			  Vertex dest = new Vertex("");
			  for (int j = 0; j < vertices.size(); j++)
			  {
				  if (vertices.get(j).getName().equals(entry.getKey()))
				  {
					  source = vertices.get(j);
				  }
				  if (vertices.get(j).getName().equals(entry.getValue().links.get(i).linkID))
				  {
					  dest = vertices.get(j);
				  }
			  }
			  
			  if (!source.equals(dest))
			  {
				  Edge new_edge = new Edge(source,dest,entry.getValue().links.get(i).tosMetrics);
				  edges.add(new_edge);
			  }
		  }
	  }
	  /*
	  // Remove duplicates
	  for (int i = 0; i < edges.size(); i++)
	  {
		  for (int j = edges.size()- 1; j >= 0; j--)
		  {
			  if (edges.get(j).equals(edges.get(i).reverse()))
			  {
				  edges.remove(j);
			  }
		  }
	  }*/
	  
	  Graph network = new Graph(vertices, edges);
	  DijkstraAlgorithm algo = new DijkstraAlgorithm(network);
	  
	  // Convert source & dest to Vertex class
	  algo.execute(rd.simulatedIPAddress);
	  
	  LinkedList<Vertex> shortestPath = algo.getPath(destinationIP);
	  
	  String return_string = new String();
	  for (int i = 0; i < shortestPath.size() - 1; i++)
	  {
		  return_string += shortestPath.get(i).getName();
		  return_string += " ->(";
		  return_string += network.weightByVertices(shortestPath.get(i), shortestPath.get(i+1));
		  return_string += ") ";
	  }
	  return_string += shortestPath.get(shortestPath.size() - 1).getName();

	  return return_string;
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = 0;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.tosMetrics).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
