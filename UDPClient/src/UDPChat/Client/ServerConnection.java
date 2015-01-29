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
import java.util.concurrent.CountDownLatch;

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
	//private DatagramSocket m_clientAckSocket = null;
	//private int id;

	public CountDownLatch acknowledgement;
	public int m_messageCounter = 0;
	private int m_ackCounter = 0;
	

	public ServerConnection(String hostName, int port, String name) {
		
		m_name = name;
		m_serverPort = port;
		acknowledgement = new CountDownLatch(1);
		
		// Get host address by name
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.err.println("Error: unknown host.");
		}
		
		// Create sockets
		try {
			m_clientSocket = new DatagramSocket();
			//m_clientAckSocket = new DatagramSocket();
			//m_serverSocket = new DatagramSocket(m_serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
			System.err.println("Error: invalid port.");
		}

		System.out.println("m_clientSocket port: " + m_clientSocket.getLocalPort());
		//System.out.println("m_clientAckSocket: " + m_clientAckSocket.getLocalPort());
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

		m_messageCounter++;
		
		sendChatMessage("01" + "|" + m_messageCounter + "|" + name);
		
		String handshakeResponse = receiveChatMessage();
		
		// Handshake successful
		if (handshakeResponse.equals("OK"))
		{
			return true;
		}
			
		// Handshake unsuccessful, name was taken
		else if (handshakeResponse.equals("NAME"))
		{
			System.err.println("Error: that username is already taken");
			return false;
		}
				
		/*
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
		String[] messageComponent = message.split("\\|");
		
		// Handshake successful
		if (messageComponent[1].equals("OK"))
		{
			return true;
		}
			
		// Handshake unsuccessful, name was taken
		else if (messageComponent[1].equals("NAME"))
		{
			System.err.println("Error: that username is already taken");
			return false;
		}
		*/
		
		System.err.println("Error: unknown handshake return: " + handshakeResponse);
		return false;
	}

	public void sendChatMessage(String msg) {
		
		// Randomize a failure variable
		Random generator = new Random();
		double failure = generator.nextDouble();
		
		String message = msg;
		message.trim();
		
		// Pack message with the given type
		DatagramPacket packet = pack(message);

		System.out.println("m_messageCounter: " + m_messageCounter);
		System.out.println("(unpacked) message: " + message);
		System.out.println("MAX_SEND_ATTEMPTS: " + MAX_SEND_ATTEMPTS);
			
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
				
				// Receive acknowledgement from server via clientconnection
				try {
					System.out.println("Receiving on clientSocket...");
					m_clientSocket.receive(packet);
				} catch (IOException e) {
					System.err.println("Error: client failed to receive ack.");
					e.printStackTrace();
				}
				
				// Unpack and split message
				message = unpack(packet);
				String[] messageComponent = message.split("\\|");
				
				System.out.println("Received: " + message + " on clientSocket");
				
				if (messageComponent[1].equals("ACK")) {
					if (Integer.parseInt(messageComponent[0]) > m_ackCounter)
						m_ackCounter++;
					
					try {
						System.out.println("Sending final ack to server...");
						m_clientSocket.send(pack("ACK"));
					} catch (IOException e) {
						System.err.println("Error: failed to send final ack to server");
						e.printStackTrace();
					}	// Ack to server
					return;
				}
				
			} else {
				// Message got lost
				System.err.println("Ack lost on client side, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
			}
		}
		// Message failed to send, decrement message counter
		m_messageCounter--;
		System.err.println("Error: failed to send message");
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
		String[] messageComponent = message.split("\\|");

		System.out.println("Successfully received and unpacked packet: " + message);
		System.out.println("Printing message: " + messageComponent[1]);
		
		return messageComponent[1];
	}
	
	public DatagramPacket pack(String msg){
		// Append message code and name to message, marshal packet and send it to assigned address and port
		byte[] data = new byte[256];
		data = msg.getBytes();
		System.out.println("Packed message :" + msg);
		return new DatagramPacket(data, msg.length(), m_serverAddress, m_serverPort);
	}
	
	public String unpack(DatagramPacket packet) {
		String message = new String(packet.getData(), 0, packet.getLength());
		System.out.println("Unpacked packet containing: " + message);
		return message;
	}

}