package socs.network.node;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.io.*;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;


public class Router {


	private boolean isStart;

	protected LinkStateDatabase lsd;

	RouterDescription rd = new RouterDescription();

	//assuming that all routers are with 4 ports
	Link[] ports = new Link[4];

	public Router(Configuration config) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processIPAddress = "0.0.0.0";
		lsd = new LinkStateDatabase(rd);


		String port_str =rd.simulatedIPAddress;
		port_str = port_str.replaceAll("\\.", "");
		port_str = port_str.substring(6, port_str.length()-1);
		int port = Integer.parseInt(port_str);
		port += 1024;
		rd.processPortNumber = (short) port;
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
		//check IP is valid or not
		if(lsd._store.get(destinationIP).links.size() == 1 || lsd._store.get(rd.simulatedIPAddress).links.size() == 1){
			System.out.println("the destinationIP is not in the network!");
			return;
		}
		String path = lsd.getShortestPath(destinationIP);
		System.out.println(path);
	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 *
	 * @param portNumber the port number which the link attaches at
	 */
	private void processDisconnect(short portNumber) {
		if(ports[portNumber] == null){
			System.out.println("ports["+portNumber+"] is null!");
			return;
		}
		//remove link from own links
		for (LinkDescription ld : lsd._store.get(rd.simulatedIPAddress).links){
			if(ld.linkID.compareTo(ports[portNumber].router2.simulatedIPAddress)==0){
				lsd._store.get(rd.simulatedIPAddress).links.remove(ld);
			}
		}
		//remove own link from the target links' database
		for (LinkDescription ld : lsd._store.get(ports[portNumber].router2.simulatedIPAddress).links){
			if(ld.linkID.compareTo(rd.simulatedIPAddress)==0){
				lsd._store.get(ports[portNumber].router2.simulatedIPAddress).links.remove(ld);
			}
		}
		//write new lsa update
		lsd._store.get(rd.simulatedIPAddress).lsaSeqNumber++;
		lsd._store.get(ports[portNumber].router2.simulatedIPAddress).lsaSeqNumber++;
		// Create the vector of LSA to be send in the update packet
		Vector<LSA> lsaUpdate = new Vector<LSA>();
		for (Map.Entry<String, LSA> entry : lsd._store.entrySet())
		{
			lsaUpdate.add(entry.getValue());
		}
		//send to all neighors

		for(int i=0;i<ports.length;i++){
			if(ports[i]!= null &&ports[i].router2.status==RouterStatus.TWO_WAY){
				try{
					//create a new socket for each neighbor
					Socket target_socket = new Socket(ports[i].router2.processIPAddress,ports[i].router2.processPortNumber);
					OutputStream outToServer = target_socket.getOutputStream();
					ObjectOutputStream new_out = new ObjectOutputStream(outToServer);
					//write the out packet
					SOSPFPacket LSA_packet = new SOSPFPacket();
					LSA_packet.srcProcessIP = rd.processIPAddress;
					LSA_packet.srcProcessPort = rd.processPortNumber;
					LSA_packet.srcIP = rd.simulatedIPAddress;
					LSA_packet.dstIP = ports[i].router2.simulatedIPAddress;
					LSA_packet.sospfType = 1 ;
					LSA_packet.routerID = rd.simulatedIPAddress;

					LSA_packet.lsaArray = new Vector<LSA>(lsaUpdate);

					new_out.writeObject( LSA_packet);
					new_out.flush();

					target_socket.close();

				}
				catch(IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}

		//delete target from ports
		ports[portNumber] = null;
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
		boolean duplicate = false;
		for(int i=0; i<ports.length;i++){
			if(ports[i] != null ){
				if(ports[i].router2.simulatedIPAddress.equals(simulatedIP)){
					System.out.println("Error, this router is already a neighbor! ");
					duplicate = true;
					break;
				}
			}
		}

		if(!duplicate){
			for(int i=0; i<ports.length;i++){
				// Locate an empty port, and instantiate the link
				if(ports[i] == null){
					RouterDescription r2 = new RouterDescription();
					r2.processIPAddress = processIP;
					r2.processPortNumber = processPort;
					r2.simulatedIPAddress = simulatedIP;
					ports[i] = new Link(this.rd, r2);

					// Create the LSA for the link and add it to the database
					LSA lsa = lsd._store.get(rd.simulatedIPAddress);
					LinkDescription ld = new LinkDescription();
					ld.linkID = simulatedIP;
					ld.portNum = processPort;
					ld.tosMetrics = weight;
					lsa.links.add(ld);
					lsd._store.put(rd.simulatedIPAddress, lsa);

					break;
				}else if(i==ports.length-1){
					System.out.println("No more slots for new router");
				}
			}
		}
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {
		//check this router has already started or not
		if(isStart){
			System.out.println("This router has already started!");
			return;
		}else{
			isStart = true;
		}

		// Increment LSA sequence number for self and target
		lsd._store.get(rd.simulatedIPAddress).lsaSeqNumber++;
		// Create the vector of LSA to be send in the update packet
		Vector<LSA> lsaUpdate = new Vector<LSA>();
		for (Map.Entry<String, LSA> entry : lsd._store.entrySet())
		{
			lsaUpdate.add(entry.getValue());
		}

		for(int i=0;i<ports.length;i++){
			// If the port is already two-way, print a notification
			if(ports[i]!=null && ports[i].router2.status == RouterStatus.TWO_WAY){
				System.out.println("port "+ ports[i].router2.simulatedIPAddress +" is TWO_WAY already!");
			}
			if(ports[i]!=null){
				try{
					Socket target_socket = new Socket(ports[i].router2.processIPAddress,ports[i].router2.processPortNumber);
					OutputStream outToServer = target_socket.getOutputStream();
					ObjectOutputStream out = new ObjectOutputStream(outToServer);
					InputStream inFromServer = target_socket.getInputStream();
					ObjectInputStream in = new ObjectInputStream(inFromServer);
					// If the current port is not yet two-way, prepare to send HELLO
					if(ports[i].router2.status != RouterStatus.TWO_WAY){
						SOSPFPacket packet = new SOSPFPacket();
						packet.srcProcessIP = rd.processIPAddress;
						packet.srcProcessPort = rd.processPortNumber;
						packet.srcIP = rd.simulatedIPAddress;
						packet.dstIP = ports[i].router2.simulatedIPAddress;
						packet.sospfType = 0 ;
						packet.routerID = rd.simulatedIPAddress;
						int temp_weight = 0;
						// Obtain link weight from the link in the router's database
						for(int j = 0 ;j<lsd._store.get(rd.simulatedIPAddress).links.size();j++){
							if(lsd._store.get(rd.simulatedIPAddress).links.get(j)!=null&&lsd._store.get(rd.simulatedIPAddress).links.get(j).linkID.equals(ports[i].router2.simulatedIPAddress)){
								temp_weight = lsd._store.get(rd.simulatedIPAddress).links.get(j).tosMetrics;
							}
						}
						packet.weight = temp_weight;

						// Send first HELLO
						out.writeObject(packet);
						out.flush();

						// Wait for response
						try {
							packet = (SOSPFPacket)in.readObject();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}

						// Received response HELLO
						if (packet.sospfType == 0)
						{
							System.out.println("received HELLO from " + packet.srcIP + ";");
							ports[i].router2.status = RouterStatus.TWO_WAY;
							System.out.println("set " + packet.srcIP + " state to TWO_WAY");
						}

						// Send second hello
						packet = new SOSPFPacket();
						packet.srcProcessIP = rd.processIPAddress;
						packet.srcProcessPort = rd.processPortNumber;
						packet.srcIP = rd.simulatedIPAddress;
						packet.dstIP = ports[i].router2.simulatedIPAddress;
						packet.sospfType = 0;
						packet.routerID = rd.simulatedIPAddress;
						packet.neighborID = ports[i].router2.simulatedIPAddress;
						packet.weight = temp_weight;

						out.flush();
						out.writeObject(packet);
					}

					if(ports[i].router2.status == RouterStatus.TWO_WAY)
					{
						// Populate the LSAupdate packet
						SOSPFPacket packet = new SOSPFPacket();
						packet.srcProcessIP = rd.processIPAddress;
						packet.srcProcessPort = rd.processPortNumber;
						packet.srcIP = rd.simulatedIPAddress;
						packet.dstIP = ports[i].router2.simulatedIPAddress;
						packet.sospfType = 1;
						packet.routerID = rd.simulatedIPAddress;
						packet.lsaArray = new Vector<LSA>(lsaUpdate);

						out.flush();
						out.writeObject(packet);
					}


					// Close the streams and the socket
					in.close();
					out.close();
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

		//check the router has started or not
		if(!isStart){
			System.out.println("This router has not started yet!");
			return;
		}

		int portNumber = 0;
		//attach part
		boolean duplicate = false;
		for(int i=0; i<ports.length;i++){
			if(ports[i] != null ){
				if(ports[i].router2.simulatedIPAddress.equals(simulatedIP)){
					System.out.println("Error, this router is already a neighbor! ");
					duplicate = true;
					break;
				}
			}
		}

		if(!duplicate){
			for(int i=0; i<ports.length;i++){
				// Locate an empty port, and instantiate the link
				if(ports[i] == null){
					portNumber = i;
					RouterDescription r2 = new RouterDescription();
					r2.processIPAddress = processIP;
					r2.processPortNumber = processPort;
					r2.simulatedIPAddress = simulatedIP;
					ports[i] = new Link(this.rd, r2);

					// Create the LSA for the link and add it to the database
					LSA lsa = lsd._store.get(rd.simulatedIPAddress);
					LinkDescription ld = new LinkDescription();
					ld.linkID = simulatedIP;
					ld.portNum = processPort;
					ld.tosMetrics = weight;
					lsa.links.add(ld);
					lsd._store.put(rd.simulatedIPAddress, lsa);

					break;
				}else if(i==ports.length-1){
					System.out.println("No more slots for new router");
				}
			}
		}

		//start part
		// Increment LSA sequence number for self every time connect() is called
		lsd._store.get(rd.simulatedIPAddress).lsaSeqNumber++;
		// Create the vector of LSA to be send in the update packet
		Vector<LSA> lsaUpdate = new Vector<LSA>();
		for (Map.Entry<String, LSA> entry : lsd._store.entrySet())
		{
			lsaUpdate.add(entry.getValue());
		}

		try{
			Socket target_socket = new Socket(ports[portNumber].router2.processIPAddress,ports[portNumber].router2.processPortNumber);
			OutputStream outToServer = target_socket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outToServer);
			InputStream inFromServer = target_socket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(inFromServer);
			// If the current port is not yet two-way, prepare to send HELLO
			if(ports[portNumber].router2.status != RouterStatus.TWO_WAY){
				SOSPFPacket packet = new SOSPFPacket();
				packet.srcProcessIP = rd.processIPAddress;
				packet.srcProcessPort = rd.processPortNumber;
				packet.srcIP = rd.simulatedIPAddress;
				packet.dstIP = ports[portNumber].router2.simulatedIPAddress;
				packet.sospfType = 0 ;
				packet.routerID = rd.simulatedIPAddress;
				int temp_weight = 0;
				// Obtain link weight from the link in the router's database
				for(int j = 0 ;j<lsd._store.get(rd.simulatedIPAddress).links.size();j++){
					if(lsd._store.get(rd.simulatedIPAddress).links.get(j)!=null&&lsd._store.get(rd.simulatedIPAddress).links.get(j).linkID.equals(ports[portNumber].router2.simulatedIPAddress)){
						temp_weight = lsd._store.get(rd.simulatedIPAddress).links.get(j).tosMetrics;
					}
				}
				packet.weight = temp_weight;

				// Send first HELLO
				out.writeObject(packet);
				out.flush();

				// Wait for response
				try {
					packet = (SOSPFPacket)in.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

				// Received response HELLO
				if (packet.sospfType == 0)
				{
					System.out.println("received HELLO from " + packet.srcIP + ";");
					ports[portNumber].router2.status = RouterStatus.TWO_WAY;
					System.out.println("set " + packet.srcIP + " state to TWO_WAY");
				}

				// Send second hello
				packet = new SOSPFPacket();
				packet.srcProcessIP = rd.processIPAddress;
				packet.srcProcessPort = rd.processPortNumber;
				packet.srcIP = rd.simulatedIPAddress;
				packet.dstIP = ports[portNumber].router2.simulatedIPAddress;
				packet.sospfType = 0;
				packet.routerID = rd.simulatedIPAddress;
				packet.neighborID = ports[portNumber].router2.simulatedIPAddress;
				packet.weight = temp_weight;

				out.flush();
				out.writeObject(packet);
			}

			if(ports[portNumber].router2.status == RouterStatus.TWO_WAY)
			{
				// Populate the LSAupdate packet
				SOSPFPacket packet = new SOSPFPacket();
				packet.srcProcessIP = rd.processIPAddress;
				packet.srcProcessPort = rd.processPortNumber;
				packet.srcIP = rd.simulatedIPAddress;
				packet.dstIP = ports[portNumber].router2.simulatedIPAddress;
				packet.sospfType = 1;
				packet.routerID = rd.simulatedIPAddress;
				packet.lsaArray = new Vector<LSA>(lsaUpdate);

				out.flush();
				out.writeObject(packet);
			}


			// Close the streams and the socket
			in.close();
			out.close();
			target_socket.close();


		}catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		for(int i =0;i<ports.length;i++){
			if(ports[i]!=null){
				if(ports[i].router2.status == RouterStatus.TWO_WAY ){
					System.out.println("IP Address of the neighbor"+(i+1)+": "+ports[i].router2.simulatedIPAddress);
				}
			}
		}
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {
		for(int i =0 ;i < 4 ; i++){
			if(ports[i] != null){
				processDisconnect((short)i);
			}
		}

		isStart = false;
		System.exit(0);

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
				} else if (command.startsWith("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
							cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					//output neighbors
					processNeighbors();
				} else if (command.equals("store"))
				{
					System.out.println("_store: "+lsd.toString());
				}
				else {
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
