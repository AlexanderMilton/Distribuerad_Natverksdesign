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
	static int MAX_SEND_ATTEMPTS = 10;

	private int m_serverPort = -1;
	private InetAddress m_serverAddress = null;
	private DatagramSocket m_clientSocket = null;
	//private int id;
	
	public int m_messageCounter = 0;
	private int m_ackCounter = 0;
	

	public ServerConnection(String hostName, int port, String name) {
				
		// Get host address by name
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.err.println("Error: unknown host.");
		}
		
		// Assign port
		m_serverPort = port;
		
		// Create socket
		try {
			m_clientSocket = new DatagramSocket();
			//m_serverSocket = new DatagramSocket(m_serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
			System.err.println("Error: invalid port.");
		}

		System.out.println("Local port: " + m_clientSocket.getLocalPort());
		System.out.println("Server address: " + m_serverAddress);
		System.out.println("Server port: " + m_serverPort);
		
		// TODO:
		// - get address of host based on parameters and assign it to m_serverAddress
		// - set up socket and assign it to m_clientSocket

	}

	public boolean handshake(String name) {
		
		// Verify valid name length
		if (name.length() > 20)
		{
			System.err.println("Error: username can be at most 20 characters.");
			return false;
		}
		else if (name.length() < 3)
		{
			System.err.println("Error: username must be at least 3 characters.");
			return false;
		}

		// Pack a message with code 01 (handshake) and separator |
		byte[] buf = new byte[256];
		DatagramPacket handshake = new DatagramPacket(buf, buf.length);
		handshake = pack("01" + "|" + name);
		
		// Attempt to send and receive handshake		
		try {
			m_clientSocket.send(handshake);
			m_clientSocket.setSoTimeout(250);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error: failed to establish connection.");
			return false;
			
		}
		
		buf = new byte[256];
		DatagramPacket handshakeResponse = new DatagramPacket(buf, buf.length);
		
		// Receive handshake
		try {
			m_clientSocket.receive(handshakeResponse);
		} catch (IOException e) {
			System.err.println("Failed to receive packet");
			e.printStackTrace();
		}
		
		String message = unpack(handshakeResponse);
		
		// Handshake successful
		if (message.equals("OK"))
		{
			return true;
		}
			
		// Handshake unsuccessful, name was taken
		else if (message.equals("NAME"))
		{
			System.err.println("Error: that username is already taken");
			return false;
		}
		
		System.err.println("Error: unknown handshake return");
		return false;
	}

	public String receiveChatMessage() {

		byte[] buf = new byte[256];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		
		try {
			m_clientSocket.receive(packet);
		} catch (IOException e) {
			System.err.println("Error: client failed to receive packet.");
			e.printStackTrace();
		}
		
		String[] messageComponents = unpack(packet).split("|");
		
		if (messageComponents[0] > m_ackCounter) {
			return message;
		}
		
	}
	
	/* discarded ack concept
	  for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++) {
				
				if (failure > TRANSMISSION_FAILURE_RATE) {
					
					// Pack message with the given type
					DatagramPacket packet = pack(message);
					
					// Send message
					try {
						m_clientSocket.send(packet);
					} catch (IOException e) {
						e.printStackTrace();
						System.err.println("Error: failed to send message");
					}
					
					// Receive acknowledgement
					try {
						m_clientSocket.receive(packet);
						return;
					} catch (IOException e) {
						e.printStackTrace();
						System.err.println("Error: failed to receive acknowledgement");
					}
					
				} else {
					// Message got lost
					System.err.println("Message lost on client side");
				}
			}
	*/

	public void sendChatMessage(String msg) {
		// Randomize a failure variable
		Random generator = new Random();
		double failure = generator.nextDouble();
		
		String message = msg;
		message.trim();
		
		// Increment message counter
		m_messageCounter++;
		
		
		// Pack message with the given type
		DatagramPacket packet = pack(message);
		
		// Make a number of attempts to send the message
		for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++) {
			
			if (failure > TRANSMISSION_FAILURE_RATE) {
				
				// Send message
				try {
					m_clientSocket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to send, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
				}
				
				//m_clientSocket.RESET TIMER;
				message = receiveChatMessage();
				if (message.equals("ACK"))
				{
					// Message was successfully sent and acknowledged by server
					System.err.println("Error: message transmission failure");
					return;
				}
				else
				{
					// Non-ack message was received
					continue;
				}
				
			} else {
				// Message got lost
				System.err.println("Message lost on client side");
			}
		}
		// Message failed to send, decrement message counter
		m_messageCounter--;
		System.err.println("Error: failed to send message");
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