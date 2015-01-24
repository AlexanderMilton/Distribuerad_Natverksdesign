package UDPChat.Server;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {
	
    private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
    private DatagramSocket m_socket;

    public static void main(String[] args){
	if(args.length < 1) {
	    System.err.println("Usage: java Server portnumber");
	    System.exit(-1);
	}
	try {
	    Server instance = new Server(Integer.parseInt(args[0]));
	    instance.listenForClientMessages();
	} catch(NumberFormatException e) {
	    System.err.println("Error: port number must be an integer.");
	    System.exit(-1);
	}
    }

    private Server(int portNumber) {
	// TODO: create a socket, attach it to port based on portNumber, and assign it to m_socket
    }

    private void listenForClientMessages() {
	System.out.println("Waiting for client messages... ");

	do {
	    // TODO: Listen for client messages.
	    // On reception of message, do the following:
	    // * Unmarshal message
	    // * Depending on message type, either
	    //    - Try to create a new ClientConnection using addClient(), send 
	    //      response message to client detailing whether it was successful
	    //    - Broadcast the message to all connected users using broadcast()
	    //    - Send a private message to a user using sendPrivateMessage()
	} while (true);
    }

    public boolean addClient(String name, InetAddress address, int port) {
	ClientConnection c;
	for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
	    c = itr.next();
	    if(c.hasName(name)) {
		return false; // Already exists a client with this name
	    }
	}
	m_connectedClients.add(new ClientConnection(name, address, port));
	return true;
    }

    public void sendPrivateMessage(String message, String name) {
	ClientConnection c;
	for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
	    c = itr.next();
	    if(c.hasName(name)) {
		c.sendMessage(message, m_socket);
	    }
	}
    }

    public void broadcast(String message) {
	for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
	    itr.next().sendMessage(message, m_socket);
	}
    }
}
