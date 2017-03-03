package socs.network.node;

import java.net.*;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;

import java.io.*;

public class Server_socket extends Thread {
	private ServerSocket server_socket;
	private Router router;
	public Server_socket(int port , Router router) throws IOException{
			this.router = router;
			server_socket = new ServerSocket(port);
	}
	
	public void run(){
		while(true){
			try{
				System.out.println(server_socket.toString());
				
				
				//System.out.println("Waiting for client on port " + server_socket.getLocalPort() + "...");
				Socket client_socket = server_socket.accept();
				
				//System.out.println("Just connected to " + client_socket.getRemoteSocketAddress());
				ObjectInputStream in = new ObjectInputStream(client_socket.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(client_socket.getOutputStream());
				
				SOSPFPacket in_packet = new SOSPFPacket();
				try {
					in_packet = (SOSPFPacket)in.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				
				if (in_packet.sospfType == 0)
				{
					int linkPort = 0;
					//build a link between of client router on the server router
					for(int i = 0;i< router.ports.length ; i++){
						if(router.ports[i] == null){
							RouterDescription source_rd = new RouterDescription();
							source_rd.processIPAddress = in_packet.srcProcessIP;
							source_rd.processPortNumber = in_packet.srcProcessPort;
							source_rd.simulatedIPAddress = in_packet.srcIP;
							
							router.ports[i] = new Link(router.rd, source_rd); 
							linkPort = i;
							break;
						}
					}
					System.out.println("received HELLO from " + in_packet.srcIP + ";");
					router.ports[linkPort].router2.status = RouterStatus.INIT;
					System.out.println("set " + in_packet.srcIP + " state to INIT");
					
					SOSPFPacket out_packet = new SOSPFPacket();
					out_packet.srcProcessIP = router.rd.processIPAddress;
					out_packet.srcProcessPort = router.rd.processPortNumber;
					out_packet.dstIP = in_packet.srcIP;
					out_packet.srcIP = router.rd.simulatedIPAddress;
					out_packet.sospfType = 0;
					out_packet.neighborID = in_packet.routerID;
					out_packet.routerID = router.rd.simulatedIPAddress;
					
					out.writeObject(out_packet);
					
					try {
						in_packet = (SOSPFPacket) in.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					
					System.out.println("received HELLO from " + in_packet.srcIP + ";");
					router.ports[linkPort].router2.status = RouterStatus.TWO_WAY;
					System.out.println("set " + in_packet.srcIP + " state to TWO_WAY");
				}
				else if (in_packet.sospfType == 1)
				{
					//receive broadcast of LSAupdate
					Vector<LSA> lsaUpdate = new Vector<LSA>(in_packet.lsaArray);					
					//save the broadcast to linkstate database
					for(int i = 0 ;i<lsaUpdate.size();i++){
						if(router.lsd._store.containsKey(lsaUpdate.get(i).links.getLast().linkID)){
							//if the incoming lsa is already stored in the linkstate database
							//compare their sequence number
							if(router.lsd._store.get(lsaUpdate.get(i).links.getLast().linkID).lsaSeqNumber < lsaUpdate.get(i).lsaSeqNumber){
								//update lsa to the lsdb
								router.lsd._store.put(lsaUpdate.get(i).links.getLast().linkID, lsaUpdate.get(i));
							}
							//else skip this lsa
						}else{
							//update lsa to the lsdb
							router.lsd._store.put(lsaUpdate.get(i).links.getLast().linkID, lsaUpdate.get(i));
						}
					}
					
					//save the current linkstate to the vector new_lsaUpdate
					Vector<LSA> new_lsaUpdate = new Vector<LSA>();
				    for(int i = 0; i< router.ports.length ; i++){
				    	  if(router.ports[i]!=null){
				    		  new_lsaUpdate.add(router.lsd._store.get(router.ports[i].router2.simulatedIPAddress));
				    	  }
				    }
				    
					//broadcast current linkstate database to all neighbors except the sender of the packet
					for(int i=0;i<router.ports.length;i++){
						if(router.ports[i]!= null && !(router.ports[i].router2.simulatedIPAddress.equals(in_packet.srcIP))){
							System.out.println("Port IP"+router.ports[i].router2.simulatedIPAddress+"SourceIP: "+in_packet.srcIP);
							//create a new socket for each neighbor
							Socket target_socket = new Socket(router.ports[i].router2.processIPAddress,router.ports[i].router2.processPortNumber);
							OutputStream outToServer = target_socket.getOutputStream();
						    ObjectOutputStream new_out = new ObjectOutputStream(outToServer);
						    //write the out packet
						    SOSPFPacket LSA_packet = new SOSPFPacket();
						    LSA_packet.srcProcessIP = router.rd.processIPAddress;
						    LSA_packet.srcProcessPort = router.rd.processPortNumber;
						    LSA_packet.srcIP = router.rd.simulatedIPAddress;
						    LSA_packet.dstIP = router.ports[i].router2.simulatedIPAddress;
						    LSA_packet.sospfType = 1 ;
						    LSA_packet.routerID = router.rd.simulatedIPAddress;
						    //increment sequence number
						    for(int j = 0 ; j<new_lsaUpdate.size();j++){
						    	if (new_lsaUpdate.get(j) != null)
						    		new_lsaUpdate.get(j).lsaSeqNumber++;
						    }
						    LSA_packet.lsaArray = new Vector<LSA>(new_lsaUpdate);
						      
						    new_out.writeObject( LSA_packet);
						    new_out.flush();
						      
						    target_socket.close();
						}
					}
				}
				client_socket.close();
			}
			catch(IOException e) {
	            e.printStackTrace();
	            break;
			}
		}
	}
}
