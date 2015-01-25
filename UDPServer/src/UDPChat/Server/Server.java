package UDPChat.Server;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class Server {

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private DatagramSocket m_socket;

	public static void main(String[] args) {
		if(args.length < 1) {
			System.err.println("Usage: java -jar Server.jar <portnumber>");
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
		// TODO: 
		// - create a socket, attach it to port based on portNumber, and assign it to m_socket
		try {
	        m_socket = new DatagramSocket(portNumber);
		} catch (SocketException e) {
			System.err.println("Error: failed to create socket.");
			e.printStackTrace();
		}
		
		System.out.println("Created socket at port " + portNumber);

	}

	private void listenForClientMessages() {
		System.out.println("Waiting for client messages... ");
		
		// Check if all clients are still connected
		pollClientConnectionStatus();
		
		do {

			byte[] buf = new byte[256];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			try {
				m_socket.receive(packet);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// Unpack message
			String message = unpack(packet);
			
			// Split message into segments containing type, message and/or arguments
			String[] messageComponent = message.split("\\|");

			// Read packet sender address and port
			InetAddress address = packet.getAddress();
			int port = packet.getPort();
			
			switch(messageComponent[0]) {

			case "00":		// Broadcast global message
				broadcast(messageComponent[2] + ": " + messageComponent[1]);
				break;
				
			case "01":		// Handshake
				
				if (addClient(messageComponent[1], address, port))
				{
					String response = "OK";
					buf = response.getBytes();
					packet = new DatagramPacket(buf, buf.length, address, port);
					try {
						m_socket.send(packet);
					} catch (IOException e) {
						System.err.println("Error: failed to send handshake response");
						e.printStackTrace();
					}
				}
				else
				{
					System.err.println("Error: failed to add client");
				}
					
				break;
				
			case "02":		// Private message
				System.out.println("Sending private message: " + messageComponent[3] + " to client " + messageComponent[2]);
				sendPrivateMessage(messageComponent[3], messageComponent[2], address, port);
				break;
				
			case "03":		// List request
				// Send a list of all clients
				printClientList(messageComponent[1], address, port);
				break;
				
			case "04":		// Leave request
				// Disconnect user
				disconnectClient(messageComponent[2]);
				break;
				
			default:
				System.err.println("Error: unknown message type: " + messageComponent[0]);
			}
			
			
			
				
			
			// TODO: Listen for client messages.
			// On reception of message, do the following:
			// - Unmarshal message
			// * Depending on message type, either
			//    - Try to create a new ClientConnection using addClient(), send 
			//      response message to client detailing whether it was successful
			//    - Broadcast the message to all connected users using broadcast()
			//    - Send a private message to a user using sendPrivateMessage()
			
		} while (true);
	}

	public void broadcast(String msg) {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			DatagramPacket message = pack(msg, c.getAddress(), c.getPort());
			c.sendMessage(message, m_socket);
		}
	}

	public boolean addClient(String name, InetAddress address, int port) {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if(c.hasName(name)) {
				return false;	// Already exists a client with this name
			}
		}
		m_connectedClients.add(new ClientConnection(name, address, port));
		System.out.println("Added client " + name + " sucessfully");
		return true;
	}

	public void sendPrivateMessage(String name, String msg, InetAddress address, int port) {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if(c.hasName(name)) {
				
				String whisper = new String(name + " whispers: " + msg);
				DatagramPacket message = pack(whisper, address, port);
				
				c.sendMessage(message, m_socket);
			}
		}
	}
	
	public void printClientList(String name, InetAddress address, int port) {
		String clientList = new String();
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			clientList += (c.getName() + System.getProperty("line.separator"));
		}
		sendPrivateMessage(clientList.toString(), name, address, port);
	}
	
	public void disconnectClient(String name) {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if(c.hasName(name)) {
				c = null;
				broadcast(c.getName() + " left the chat");
				return;
			}
		}
	}
	
	public void pollClientConnectionStatus() {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			try {
				c.getAddress().isReachable(10);
			} catch (IOException e) {
				broadcast(c.getName() + " timed out");
				c = null;
			}
		}
	}
	
	public DatagramPacket pack(String msg, InetAddress iadd, int port){
		// Append message code and name to message, marshal packet and send it to assigned address and port
		String message = msg;
		byte[] data = message.getBytes();
		DatagramPacket packet = new DatagramPacket(data, message.length(), iadd, port);
		
		return packet;
	}
	
	public String unpack(DatagramPacket packet) {
		return new String(packet.getData(), 0, packet.getLength());
	}
}
