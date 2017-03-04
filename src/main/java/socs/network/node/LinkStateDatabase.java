package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    String min_next_node = new String();
    int min_weight=Integer.MAX_VALUE;
	HashMap<String, LSA> tentative_list = new HashMap<String, LSA>();
	HashMap<String, LSA> confirmed_list = new HashMap<String, LSA>();
	
	//add itself to confirmed_list at first step
	confirmed_list.put(rd.simulatedIPAddress,_store.get(rd.simulatedIPAddress));
	//add neighbors to tentative list
	//loop through _stores
	for( Map.Entry<String, LSA> entry: _store.entrySet()){
		//if it is itself, skip
		if(entry.getKey() == rd.simulatedIPAddress){
			continue;
		}
		tentative_list.put(entry.getKey(), entry.getValue());
	}
	
	//choose minimum weight from tentative list
	for( Map.Entry<String, LSA> entry: tentative_list.entrySet()){
		if(entry.getValue().links.peekLast().tosMetrics < min_weight){
			min_weight = entry.getValue().links.peekLast().tosMetrics;
			min_next_node = entry.getKey();
		}
	}
	//add the minimum weight node to confirmed list
	confirmed_list.put(min_next_node,_store.get(min_next_node));
	
	  
    return null;
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
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
