package socs.network.node;
import java.net.*;
import java.io.*;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];

  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    lsd = new LinkStateDatabase(rd);
    String port_str =rd.simulatedIPAddress;
    port_str = port_str.replaceAll("\\.", "");
    port_str = port_str.substring(6, port_str.length()-1);
    int port = Integer.parseInt(port_str);
    port += 1024;
    //create server socket 
   try {
	   Thread t1 = new Server_socket( port , this );
	   t1.start();
   } catch (IOException e) {
	   e.printStackTrace();
   } 
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {
	  
	  for(int i=0; i<ports.length;i++){
		  //instantiate links of the router
		  if(ports[i] == null){
			  RouterDescription r2 = new RouterDescription();
			  r2.processIPAddress = processIP;
			  r2.processPortNumber = processPort;
			  r2.simulatedIPAddress = simulatedIP;
			  ports[i] = new Link(this.rd, r2); 
			  //weight is not used
			  
			  break;
		  }
	  }
	  
	  //
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
	  //client socket
	  for(int i=0;i<ports.length;i++){
		  if(ports[i]!=null){
			  try{
				 // System.out.println("Connecting to " + ports[i].router2.processIPAddress  + " on port " + ports[i].router2.processPortNumber);
				  Socket target_socket = new Socket(ports[i].router2.processIPAddress,ports[i].router2.processPortNumber);
				  
				//  System.out.println("Just connected to " + target_socket.getRemoteSocketAddress());
				  OutputStream outToServer = target_socket.getOutputStream();
			      ObjectOutputStream out = new ObjectOutputStream(outToServer);
			      InputStream inFromServer = target_socket.getInputStream();
			      ObjectInputStream in = new ObjectInputStream(inFromServer);
			      
			      //SOSPFPacket
			      SOSPFPacket packet = new SOSPFPacket();
			      packet.srcProcessIP = rd.processIPAddress;
			      packet.srcProcessPort = rd.processPortNumber;
			      packet.srcIP = rd.simulatedIPAddress;
			      packet.dstIP = ports[i].router2.simulatedIPAddress;
			      packet.sospfType = 0 ;
//			      packet.routerID;
//			      packet.neighborID;
//			      packet.lsaArray;
			      out.writeObject(packet);
			      
			      
			      //read input packet from server
			      try {
					packet = (SOSPFPacket)in.readObject();
			      } catch (ClassNotFoundException e) {
					e.printStackTrace();
			      }

			      if (packet.sospfType == 0)
			      {
			    	  System.out.println("received HELLO from " + packet.srcIP + ";");
			    	  ports[i].router2.status = RouterStatus.TWO_WAY;
			    	  System.out.println("set " + packet.srcIP + " state to TWO_WAY");
			      }
					
			      packet.srcProcessIP = rd.processIPAddress;
			      packet.srcProcessPort = rd.processPortNumber;
			      packet.srcIP = rd.simulatedIPAddress;
			      packet.dstIP = ports[i].router2.simulatedIPAddress;
			      packet.sospfType = 0;
			      packet.routerID = rd.simulatedIPAddress;
			      packet.neighborID = ports[i].router2.simulatedIPAddress;
					
			      out.writeObject(packet);
			      
				  target_socket.close();
				  
			  }catch (IOException e) {
				  e.printStackTrace();
			  }
			  
			  
		  }
		 
		  
		  
	  
	  
		  
	  }
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
	  for(int i =0;i<ports.length;i++){
		  if(ports[i]!=null){
			  System.out.println("IP Address of the neighbor"+(i+1)+": "+ports[i].router2.simulatedIPAddress);
			  
		  }
	  }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
