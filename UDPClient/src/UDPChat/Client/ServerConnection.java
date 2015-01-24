/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPChat.Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ServerConnection {

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private int m_serverPort = -1;
	private InetAddress m_serverAddress = null;
	private DatagramSocket m_socket = null;
	

	public ServerConnection(String hostName, int port) {
		// Assign port
		m_serverPort = port;
		
		// Get host address by name
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.err.println("Error: unknown host.");
		}
		
		// Create socket at assigned port
		try {
			m_socket = new DatagramSocket(m_serverPort, m_serverAddress);
		} catch (SocketException e) {
			e.printStackTrace();
			System.err.println("Error: invalid port.");
		}

		System.out.println("Created socket at port " + m_serverPort + " and address " + m_serverAddress);
		
		// TODO:
		// - get address of host based on parameters and assign it to m_serverAddress
		// - set up socket and assign it to m_socket

	}

	public boolean handshake(String name) {
		
		// Verify valid name length
		if (name.length() > 20)
		{
			System.err.println("Error: username can be at most 20 characters.");
			return false;
		}
		else if (name.length() < 1)
		{
			System.err.println("Error: username must be at least one character.");
			return false;
		}

		// Pack a message with code 01 (handshake) and separator |
		DatagramPacket handshake = pack("01" + "|" + name);
		
		// Attempt to send and receive handshake		
		try {
			m_socket.send(handshake);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error: failed to establish connection.");
			return false;
			
		}
		
		// Receive handshake
		try {
			m_socket.receive(handshake);
		} catch (IOException e) {
			System.out.println("Failed to receive packet");
			e.printStackTrace();
		}
		
		String message = unpack(handshake);
		
		// Handshake successful
		if (message.equals("OK"))
			return true;
		
		// Handshake unsuccessful, name was taken
		else if (message.equals("NAME"))
			System.err.println("Error: that username is already taken.");
			return false;
		
		// TODO:
		// - marshal connection message containing user name
		// - send message via socket
		// - receive response message from server
		// - unmarshal response message to determine whether connection was successful
		// - return false if connection failed (e.g., if user name was taken)
		
		
	}

	public String receiveChatMessage() {

		byte[] buf = new byte[256];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		
		try {
			m_socket.receive(packet);
		} catch (IOException e) {
			System.err.println("Error: client failed to receive packet.");
			e.printStackTrace();
		}
		
		String message = unpack(packet);
		
		return message;
		
		// TODO:
		// - receive message from server
		// - unmarshal message if necessary
		// - return message contents
		// Note that the main thread can block on receive here without
		// problems, since the GUI runs in a separate thread

	}
	
	public void sendChatMessage(String name, String comm, String arg, String msg) {
		// if an argument is included, append and forward it

		String type = null;
		String message = comm + "|" + arg + "|" + msg + "|" + name;
		
		switch(comm){
		
		// Connect to server - OBSOLETE
		case "/connect":	case "/Connect":	case "/CONNECT":
			type = "01";
			break;

		// Send private message
		case "/whisper":	case "/Whisper":	case "/WHISPER":
			type = "02";
			break;

		// Request user list
		case "/list":	case "/List":	case "/LIST":
			type = "03";
			break;

		// Request disconnect
		case "/leave":	case "/Leave":	case "/LEAVE":
		case "/quit":	case "/Quit":	case "/QUIT":
		case "/exit":	case "/Exit": 	case "/EXIT":
		case "/dc":		case "/DC":
			type = "04";
			break;
		
		default:
			System.err.println("Error: invalid command");
			return;
		}
		
		message = type + "|" + arg + "|" + msg;
		
		sendChatMessage(name, message);
	}

	public void sendChatMessage(String name, String msg) {
		// Randomize a failure variable
		Random generator = new Random();
		double failure = generator.nextDouble();
		
		// Copy the user's message, trim it and split it at each separator
		String message = "00" + "|" + msg + "|" + name;	// Broadcast code "00"
		message.trim();

		if (failure > TRANSMISSION_FAILURE_RATE) {
			
			// Pack message with the given type
			DatagramPacket packet = pack(message);
			
			// Send message
			try {
				m_socket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Error: failed to send message");
			}
			
			// TODO:
			// - marshal message if necessary
			// - send a chat message to the server
		} else {
			// Message got lost
			System.err.println("Message was lost.");
		}
	}
	
	public DatagramPacket pack(String msg){
		// Append message code and name to message, marshal packet and send it to assigned address and port
		byte[] data = new byte[256];
		data = msg.getBytes();

		System.out.println("Packed message :" + msg);
		
		return new DatagramPacket(data, msg.length(), m_serverAddress, m_serverPort);
	}
	
	public String unpack(DatagramPacket packet) {
		String receivedPacket = new String(packet.getData(), 0, packet.getLength());
		String[] message = receivedPacket.split("\\|");
		
		System.out.println("Unpacked packet containing: " + receivedPacket);
		
		return message[0];
	}

}


// packet contains either:		UPDATED: NO!
//	type 	sender_name 	message 	address 		port
//	type 	argument 		message 	sender_name 	address 	port
