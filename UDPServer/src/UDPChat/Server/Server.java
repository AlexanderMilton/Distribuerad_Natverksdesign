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
		
		
		do {

			// Check if all clients are still connected
			pollClientConnectionStatus();
			
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
			
			System.out.println("Unpacked message: " + message);
			
			// Split message into segments containing type, message and/or arguments
			/*
			 * messageComponent[0] = type XX
			 * messageComponent[1] = messageCounter
			 * messageComponent[2] = name
			 * messageComponent[3] = message/argument
			 * messageComponent[4] = message (if argument present)
			 */
			String[] messageComponent = message.split("\\|");

			// Read packet sender address and port
			InetAddress address = packet.getAddress();
			int port = packet.getPort();
			
			// Read message type, message count and sender name
			String type = messageComponent[0];
			int messageCounter = Integer.parseInt(messageComponent[1]);
			String name = messageComponent[2];
			
			// Check if message has already been interpreted
			if (messageAlreadyInterpreted(name, messageCounter)) {				
				sendPrivateMessage(name, "ACK", address, port);
				continue;
			}

			
			switch(type) {

			case "00":		// Broadcast global message
				broadcast(name + ": " + messageComponent[3]);
				break;
				
			case "01":		// Handshake
				
				if (addClient(name, address, port))
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
				String recepient = messageComponent[3];
				String whisper = messageComponent[4];
				System.out.println("Sending private message: " + whisper + " to client " + recepient);
				sendPrivateMessage(recepient, whisper, address, port);
				break;
				
				/* ^ old code, on ICE ^
				String whisperer = messageComponent[3];
				System.out.println("Sending private message: " + messageComponent[3] + " to client " + whisperer);
				sendPrivateMessage(messageComponent[3], whisperer, address, port);
				break;
				*/
				
			case "03":		// List request
				// Send a list of all clients
				printClientList(name, address, port);
				break;
				
			case "04":		// Leave request
				// Disconnect user
				disconnectClient(name);
				break;
				
			default:
				System.err.println("Error: unknown message type: " + messageComponent[0]);
			}
			
		} while (true);
	}

	public void broadcast(String msg) {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			DatagramPacket message = pack(c.getAckCounter(), msg, c.getAddress(), c.getPort());
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
				
				DatagramPacket message = pack(c.getAckCounter(), msg, address, port);
				
				c.sendMessage(message, m_socket);
			}
		}
	}
	
	public boolean messageAlreadyInterpreted(String name, int clientMessageCounter)
	{
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if(c.hasName(name)) {
				if (c.getMessageCounter() < clientMessageCounter) {
					// Message has not been interpreted
					c.m_messageCounter++;
					return false;
				}
			}
		}
		// Message has already been interpreted
		return true;
	}

	/*public void acknowledgeMessage(String name, InetAddress address, int port) {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if(c.hasName(name)) {
				
				DatagramPacket message = pack("ACK", address, port);
				
				c.sendMessage(message, m_socket);
			}
		}
	}*/
	
	public void printClientList(String name, InetAddress address, int port) {
		String clientList = new String("[List of all active clients]\n");
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			clientList += ("> " + c.getName() + System.getProperty("line.separator"));
		}
		sendPrivateMessage(name, clientList.toString(), address, port);
	}
	
	public void disconnectClient(String name) {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if(c.hasName(name)) {
				broadcast(name + " left the chat");
				c = null;
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
	
	public DatagramPacket pack(int ackCounter, String msg, InetAddress iadd, int port){
		// Append message code and name to message, marshal packet and send it to assigned address and port
		String message = ackCounter + "|" + msg;
		byte[] data = message.getBytes();
		DatagramPacket packet = new DatagramPacket(data, message.length(), iadd, port);
		
		return packet;
	}
	
	public String unpack(DatagramPacket packet) {
		return new String(packet.getData(), 0, packet.getLength());
	}
}
