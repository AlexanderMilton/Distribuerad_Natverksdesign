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

	private String m_name;
	private int m_serverPort = -1;
	private InetAddress m_serverAddress = null;
	private DatagramSocket m_clientSocket = null;
	private DatagramSocket m_clientAckSocket = null;
	//private int id;
	
	public int m_messageCounter = 0;
	private int m_ackCounter = 0;
	

	public ServerConnection(String hostName, int port, String name) {
		
		// Copy name of client to connection
		m_name = name;
		
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
			m_clientAckSocket = new DatagramSocket();
			//m_serverSocket = new DatagramSocket(m_serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
			System.err.println("Error: invalid port.");
		}

		System.out.println("m_clientSocket port: " + m_clientSocket.getLocalPort());
		System.out.println("m_clientAckSocket: " + m_clientAckSocket.getLocalPort());
		System.out.println("Server address: " + m_serverAddress);
		System.out.println("Server port: " + m_serverPort);
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
		handshake = pack("01" + "|" + m_messageCounter + "|" + name + "|" + m_clientAckSocket.getLocalPort());
		
		// Attempt to send and receive handshake		
		try {
			m_clientSocket.send(handshake);
			//m_clientSocket.setSoTimeout(250);
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
		
		System.err.println("Error: unknown handshake return: " + message);
		return false;
	}

	public void sendChatMessage(String msg, boolean reqAck) {
		
		// Randomize a failure variable
		Random generator = new Random();
		double failure = generator.nextDouble();
		
		String message = msg;
		message.trim();
		
		// Pack message with the given type
		DatagramPacket packet = pack(message);
		

		System.out.println("m_messageCounter: " + m_messageCounter);
		System.out.println("(unpacked) message: " + message);
		System.out.println("requires acknowledgement: " + reqAck);
		System.out.println("MAX_SEND_ATTEMPTS: " + MAX_SEND_ATTEMPTS);
		
		// Messages require acknowledgments
		if (reqAck) {
			
			// Make a number of attempts to send the message
			for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++) {
				
				failure = generator.nextDouble();
				
				if (failure > TRANSMISSION_FAILURE_RATE) {
					
					// Send message
					try {
						m_clientSocket.send(packet);
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Failed to send, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
					}
					
					//m_clientSocket.RESET TIMER;
					if (handleAck())
					{
						// Message was successfully sent and acknowledged by server
						return;	// TODO: leave for loop on ack
					}
					else
					{
						// Non-ack message was received or error occurred
						System.err.println("Error: failed to receive ack, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
						continue;
					}
					
				} else {
					// Message got lost
					System.err.println("Ack lost on client side, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
				}
			}
			// Message failed to send, decrement message counter
			//m_messageCounter--;
			System.err.println("Error: failed to send message");
		}
		
		
		
		// Acknowledgments do not require their own acks
		else {
			if (failure > TRANSMISSION_FAILURE_RATE) {
				// Send message
				try {
					m_clientSocket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to send ack");
				}
			} else {
				// Message got lost
				System.err.println("Message lost on client side");
			}
		}
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
		
		// Unpack and split message
		String message = unpack(packet);
		
		System.out.println("Successfully received and unpacked message: " + message);
		
		return message;
	}
	
	public boolean handleAck() {
		byte[] buf = new byte[256];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		
		try {
			System.out.println("Receiving on clientAckSocket...");
			m_clientAckSocket.receive(packet);
		} catch (IOException e) {
			System.err.println("Error: client failed to receive ack.");
			e.printStackTrace();
		}
		
		// Unpack and split message
		String message = unpack(packet);
		String[] messageComponents = message.split("|");
		
		System.out.println("Received: " + message + " on clientAckSocket");
		
		if (messageComponents[1].equals("ACK")) {
			if (Integer.parseInt(messageComponents[0]) > m_ackCounter) {
				
				// Increment ack-counter
				m_ackCounter++;		// = messageComponents[0];
				
				// Send ack response
				sendChatMessage("05" + "|" + m_ackCounter + "|" + m_name, false);	// Ack to server
				return true;
			}
			else {
				// Message already interpreted, send ack
				sendChatMessage("05" + "|" + m_messageCounter + "|" + m_name, false);	// Ack to server
				return true;
			}
		}
		else {
			// Failed to handle acks
			return false;
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
		
		return message[1];
	}

}


// packet contains either:		UPDATED: NO!
//	type 	sender_name 	message 	address 		port
//	type 	argument 		message 	sender_name 	address 	port