package UDPChat.Server;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.io.IOException;
import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import UDPChat.Shared.Message;
import UDPChat.Shared.MessageID;

public class Server {
	
    private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
    private DatagramSocket m_socket;
    private long currentPacketID = 0;

    public static void main(String[] args){
		if(args.length < 1) {
		    System.err.println("Usage: java Server portnumber");
		    System.exit(-1);
		}
		try {
		    Server instance = new Server(Integer.parseInt(args[0]));
		    instance.listenForClientMessages();
		} catch(NumberFormatException e) {
		    System.err.println("Error: port number must be an integer: " + args[0]);
		    System.exit(-1);
		}
    }

    private Server(int portNumber) {
    	// TODO: create a socket, attach it to port based on portNumber, and assign it to m_socket
    	try {
    		m_socket = new DatagramSocket(portNumber);
    		m_socket.setSoTimeout(1000);
    	} catch (SocketException e) {
    		System.err.println("Error: Failed to create a socket with port " + portNumber);
    	}
    }
    
    private void listenForClientMessages() {
		System.out.println("Waiting for client messages... ");
	
		do {
			/* Listen and retrive the next incoming packet. */
			byte[] buf = new byte[512];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			try {
				m_socket.receive(packet);
			} catch (SocketTimeoutException e) {
				/* We need to increase it, as it might send a new PING-request. */
				currentPacketID++;
				
				/* Checks if a client has been pinged more than 10 times without answering, if yes, remove it. */
				updateClients();
				continue;
			} catch (IOException e) {
				System.err.println("Error: Failed to listen for incoming packages.");
				System.exit(-1);
			}
			
			Message message = new Message(new String(packet.getData(), 0, packet.getLength()));
			
			/* Don't handle any badly formated messages */
			if (!message.isValid()) continue;
		
			System.out.println("Recieved following message: " + message.getText());
			
			if (message.getText().equals("ACK")) {
				ClientConnection c = getClient(message.getUsername());
				if (c != null) {
					c.advancePacketSenderQueue(Long.toString(message.getUID()));
					
				}
				continue;
			}
			

			/* Increase the ID of the sent packet so clients can handle it properly */
			currentPacketID++;
			
			/* Acknowledge the packet. */
			ackPacket(packet, message.getUID());
			
			
			/* Delegate our message to the right handler. */
			handleMessage(message, packet);
			
		} while (true);
    }
    
    public void updateClients() {
    	for (int i = m_connectedClients.size() - 1; i >= 0; i--) {
    		ClientConnection c = m_connectedClients.get(i);
    		if (c.getCurrentPacketSentCount() >= 10) {
    			String username = c.getUsername();
    			boolean valid = c.getValidFlag();
    			System.out.println("Found crashed client, removing: " + username);
    			c.kill();
    			m_connectedClients.remove(i);
    			if (valid) {
    				broadcast(username + " has disconnected. Reason[CLIENT WAS UNRESPONSIVE]");
    			}
    		} else if (c.isPacketSenderEmpty()) {
    			/* If there is no queued packet, add a ping request */
    			currentPacketID++;
    			c.sendMessage("PING", currentPacketID, MessageID.PING);
    		}
    	}
    }

    public boolean addClient(String name, InetAddress address, int port) {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
		    c = itr.next();
		    if(c.hasName(name)) {
		    	return false; // Already exists a client with this name
		    }
		}
		m_connectedClients.add(new ClientConnection(name, m_socket, address, port));
		return true;
    }

    public ClientConnection getClient(String name) {
		ClientConnection c = null;
    	for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
		    c = itr.next();
		    if(c.hasName(name)) {
		    	return c; // Already exists a client with this name
		    }
		}
    	return null;
    }
    
    /* This function finds a close match, used to find none valid users */
    public ClientConnection findMatch(String name, InetAddress address, int port) {
    	for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
		    ClientConnection c = itr.next();
		    
		    InetAddress clientAddress = c.getAddress();
		    int clientPort = c.getPort();
		    String clientName = c.getUsername();
		    
		    if( clientAddress == address && clientPort == port && clientName.startsWith(name) ) {
		    	return c;
		    }
		}
    	return null;
    }
    
    public void sendPrivateMessage(String message, String name) {
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
		    ClientConnection c = itr.next();
		    if(c.hasName(name) && c.getValidFlag()) {
		    	c.sendMessage(message, currentPacketID, 0);
		    }
		}
    }

    public void broadcast(String message) {
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
		    ClientConnection c = itr.next();
		    if (c.getValidFlag()) {
		    	c.sendMessage(message, currentPacketID, 0);
		    }
		}
    }
    
    protected void ackPacket(DatagramPacket packet, long id) {
    	ClientConnection.sendAckPacket("ACK", id, 0, m_socket, packet.getAddress(), packet.getPort());
    }
    
    /* Handlers for all message types. */
    
    protected void handleMessage(Message message, DatagramPacket packet) {
    	System.out.println("Handling message...");
    	
    	/* Get the user. */
    	ClientConnection client = null; 
    	client = getClient(message.getUsername());
    	
    	System.out.println("Client data was " + client);
    	
    	/* Stop handling if not a registered user, but do handle if not registered and requesting a join. */
    	if (client == null && message.getType() != MessageID.JOIN) return; 
    	
    	
    	System.out.println("Passed first sanity-check!");
    	
    	/* If we expected a newer packet from a registered user, stop handling the packet. */
    	if (client != null && message.getUID() <= client.getLastSocketUID() && message.getType() != MessageID.JOIN) return;
    	
    	System.out.println("Passed second sanity-check!");
    	
    	/* Handle ACK-messages seperatly */
    	if (message.getText().equals("ACK")) {
    		ClientConnection c = getClient(message.getUsername());
    		if (c != null) {
    			c.advancePacketSenderQueue(Long.toString(message.getUID()));
    		}
    		return;
    	}
    	
    	
    	int t = message.getType();
    	System.out.println(t);
		switch(t) {
		case MessageID.JOIN:
			System.out.println("Running handler for JOIN.");
			join_handler(message, packet);
			break;
			
		case MessageID.LEAVE:
			leave_handler(message, packet);
			break;
			
		case MessageID.SHOUT:
			System.out.println("Running SHOUT");
			shout_handler(message, packet);
			break;
			
		case MessageID.TELL:
			tell_handler(message, packet);
			break;
			
		case MessageID.LIST:
			list_handler(message, packet);
			break;
			
			default:
				System.err.println("Error: Got unhandled message.");
				break;
			
		}
		
		/* Register the latest UID we handled, to avoid handing copies of the same message. */
		client = getClient(message.getUsername());
		if (client != null && message.getUID() > client.getLastSocketUID()) {
			client.setLastPacketUID(message.getUID());
		}
    }
    
    protected void join_handler(Message message, DatagramPacket packet) {
    	String usrname = message.getUsername();
    	
    	boolean unregistred 	= (getClient(usrname) == null) ? true : false ;
    	boolean validUsrname	= !(usrname.length() < 3 || usrname.length() > 30 || usrname.equals(" "));
    	
    	
    	ClientConnection c = null;
    	if (validUsrname && unregistred) {
    		broadcast(usrname + " has joined the chat.");
    		addClient(usrname, packet.getAddress(), packet.getPort());
    		c = getClient(usrname);
    	} else {
    		System.err.println("Tried adding user " + usrname + " but failed.");
    		
    		/* If we fail, we still need to make a psuedo-connection to send fail-packages. */
    		/* They exist in the list but are marked as not valid, ping-detection will handle and remove them when the messaging is done. */
    		addClient(message.getUsername() + " FAILED_PID " + currentPacketID, packet.getAddress(), packet.getPort());
    		c = findMatch(usrname, packet.getAddress(), packet.getPort());
    		c.setValidFlag(false);
    	}
    	
		c.sendMessage(Boolean.toString(validUsrname && unregistred), currentPacketID, 0);
    
    	System.out.println("Tried to add user, response is " + (unregistred && validUsrname));
    }
    
    protected void leave_handler(Message message, DatagramPacket packet) {
    	System.out.println("Running leave handler.");
    	for(int i = 0; i < m_connectedClients.size(); i++) {
    		if (m_connectedClients.get(i).hasName(message.getUsername())) {
    			m_connectedClients.remove(i);
    			broadcast(message.getUsername() + " has disconnected. Reason[" + message.getText() + "]");
    			return;
    		}
    	}
    }
    
    protected void shout_handler(Message message, DatagramPacket packet) {
    	System.out.println("Running shout handler.");
    	broadcast("[" + message.getUsername() + "]: " + message.getText());
    }
    
    protected void tell_handler(Message message, DatagramPacket packet) {
    	System.out.println("Running tell handler.");
    	ClientConnection c = getClient(message.getUsername());
    	try {
    		String[] tok = message.getText().split(" ", 3);
    		if (tok.length < 3) {
    			throw new ArrayIndexOutOfBoundsException();
    		} else {
        		String who = tok[1].trim();
        		String msg = tok[2].trim(); 
        		ClientConnection target = getClient(who);
        		if (target == null || !target.getValidFlag()) {
        			c.sendMessage("[SERVER]: No such user connected.", currentPacketID, 0);
        		} else {
        			target.sendMessage(message.getUsername() + " says: " + msg ,currentPacketID, 0);
        		}
    		}
    	} catch(ArrayIndexOutOfBoundsException e) {
    		c.sendMessage("[SERVER]: Invalid number of arguments.", currentPacketID, 0);
    	}
    }
    
    protected void list_handler(Message message, DatagramPacket packet) {
    	System.out.println("Running list handler.");
    	ClientConnection c = getClient(message.getUsername());
    	
    	String listOfUsers = "== CONNECTED USERS ==\n";
    	for(int i = 0; i < m_connectedClients.size(); i++) {
    		ClientConnection tc = m_connectedClients.get(i);
    		if (tc.getValidFlag())
    			listOfUsers += m_connectedClients.get(i).getUsername() + "\n";
    	}
    	
    	c.sendMessage(listOfUsers, currentPacketID, 0);
    }
}
