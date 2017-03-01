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
				
				in_packet.srcProcessIP = router.rd.processIPAddress;
				in_packet.srcProcessPort = router.rd.processPortNumber;
				in_packet.dstIP = in_packet.srcIP;
				in_packet.srcIP = router.rd.simulatedIPAddress;
				in_packet.sospfType = 0;
				in_packet.neighborID = in_packet.routerID;
				in_packet.routerID = router.rd.simulatedIPAddress;
				
				out.writeObject(in_packet);
				
				try {
					in_packet = (SOSPFPacket) in.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				
				if (in_packet.sospfType == 0)
				{
					System.out.println("received HELLO from " + in_packet.srcIP + ";");
					router.ports[linkPort].router2.status = RouterStatus.TWO_WAY;
					System.out.println("set " + in_packet.srcIP + " state to TWO_WAY");
				}
				//attach the client router to this router
				//router.ports[linkPort] = null;
				
				//receive LSA packet
				if (in_packet.sospfType == 1){
					//receive broadcast of LSAupdate
					Vector<LSA> lsaUpdate = in_packet.lsaArray;
					
					//save the broadcast to linkstate database
					for(int i = 0 ;i<lsaUpdate.size();i++){
						if(router.lsd._store.containsKey(lsaUpdate.get(i).linkStateID) ){
							//if the incoming lsa is already stored in the linkstate database
							//compare their sequence number
							if(router.lsd._store.get(lsaUpdate.get(i).linkStateID).lsaSeqNumber < lsaUpdate.get(i).lsaSeqNumber){
								//update lsa to the lsdb
								router.lsd._store.put(lsaUpdate.get(i).linkStateID, lsaUpdate.get(i));
							}
							//else skip this lsa
						}else{
							//update lsa to the lsdb
							router.lsd._store.put(lsaUpdate.get(i).linkStateID, lsaUpdate.get(i));
						}
					}
					
					//broadcast current linkstate database to all neighbors except the sender of the packet
					
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
