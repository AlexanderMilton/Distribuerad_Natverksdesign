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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ServerConnection
{

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;
	static int MAX_SEND_ATTEMPTS = 10;

	int m_serverPort = -1;
	private InetAddress m_serverAddress = null;
	public DatagramSocket m_socket = null;
	
//	private int m_ackPort = -1;
//	private InetAddress m_ackAddress = null;
//	public DatagramSocket m_ackSocket = null;

	private String m_name;
	private int message = 0;
	private int previousMessageID = 0;
//	private boolean isAcked = false;
//	private boolean recentlyReceived = false;

	public boolean handshake(String name)
	{
		
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
		
		byte[] data = new byte[256];
		DatagramPacket handshakeResponse = new DatagramPacket(data, data.length);
		String message = new String("01" + "|" + getMessageID() + "|" + name);

		// Send connection request (handshake)
		System.out.println("Sending handshake to server...");
		sendMessage(message, m_socket, m_serverPort);

		// Receive handshake 
		try 
		{ 
			System.out.println("Receiving handshake from server...");
			m_socket.receive(handshakeResponse);
		} catch (IOException e) {
			System.err.println("Failed to receive packet");
			e.printStackTrace();
		}

		message = unpack(handshakeResponse);
		System.out.println("Unpacked handshake response: " + message);
		
		// Handshake successful
		if (message.equals("OK"))
		{
			System.out.println("Successfully connected to server\n");
			return true;
		}

		// Handshake unsuccessful, name was taken
		else if (message.equals("NAME"))
		{
			System.err.println("Error: that username is already taken");
			m_socket.close();
			return false;
		}

		System.err.println("Error: unknown handshake return: " + message);
		return false;
	}
	
	public ServerConnection(String hostName, int port, String name)
	{
		m_name = name;
		m_serverPort = port;
		
		// Get host address by name
		try
		{
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e)
		{
			e.printStackTrace();
			System.err.println("Error: unknown host.");
		}

		// Create sockets
		try
		{
			m_socket = new DatagramSocket();
		} catch (SocketException e)
		{
			e.printStackTrace();
			System.err.println("Error: invalid port.");
		}

		System.out.println("m_socket port: " + m_socket.getLocalPort());
		System.out.println("Server address: " + m_serverAddress);
		System.out.println("Server port: " + m_serverPort + "\n");
	}

	public void send(String msg, DatagramSocket socket, int port)
	{
		// Randomize a failure variable
		Random generator = new Random();
		double failure = generator.nextDouble();

		// Trim message
		String message = msg;
		message.trim();
		
		// Pack message with the given type
		DatagramPacket packet = packMsg(message, m_serverAddress, port);
		
		if (failure > TRANSMISSION_FAILURE_RATE)
		{
			// Send message
			try
			{
				System.out.println("Sending from port " + socket.getLocalPort() + " to port " + packet.getPort() + ", message " + message);
				m_socket.send(packet);
			} catch (IOException e)
			{
				System.err.println("Error: failed to connect to server");
				e.printStackTrace();
			}
		}
		// Message failed to send
		System.err.println("Error: failed to send message");
	}
	
	public DatagramPacket sendMessage(String msg, DatagramSocket socket, int port)
	{

		byte[] data = new byte[256];
		DatagramPacket acknowledgment = new DatagramPacket(data, data.length);
				
		try
		{
			socket.setSoTimeout(1000);
		} catch (SocketException e)
		{
			e.printStackTrace();
		}
		
		for (int i = 0; i <= MAX_SEND_ATTEMPTS; i++)
    	{
			try
			{
				// Send message
				System.out.println("Sending message: " + msg);
				send(msg, socket, port);
				
				// Wait for acknowledgment
				System.out.println("Awaiting acknowledgment...");
				socket.receive(acknowledgment);

				// Received acknowledgment, return packet
				System.out.println("Received acknowledgment");
				return acknowledgment;
			} catch (SocketTimeoutException e)
			{ 
				// Socket timed out
				System.out.println("Socket timed out, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
			} catch (IOException e)
			{
				System.err.println("Error: I/O-exception");
			}
    	}
		System.err.println("Error: failed to receive message");
		System.exit(1);
		return acknowledgment;
	}
	
	public DatagramPacket receivePacket(DatagramSocket socket)
	{
		byte[] data = new byte[256];
		DatagramPacket message = new DatagramPacket(data, data.length);
		
		try
		{
			socket.receive(message);
		} catch (IOException e)
		{
			System.err.println("Error: failed to receive message");
			e.printStackTrace();
		}
		return message;
	}
	
	public String receiveMessage()
	{
		String message = null;
		String[] messageComponent = null;
		int receivedMessageID = 0;
		
		while (true)
		{
			// Receive and unpack a server distributed message
			message = unpack(receivePacket(m_socket));
			messageComponent = message.split("\\|");
			
			// Check the ID of the message
			receivedMessageID = Integer.parseInt(messageComponent[0]);
			
			if (receivedMessageID <= previousMessageID)
			{
//				sendAcknowledgment();
				return "";
			}
			else
			{
				previousMessageID = receivedMessageID;			
			}
			
			if (message.equals("%ACK%"))
			{
				System.out.println("Received ack");
				return "";
			}
			else if (message.equals("%DC%"))
			{
				m_socket.close();
				return "You have been disconnected";
			}
//			else if (message.equals("%POLL%"))
//			{
//				sendChatMessage("%ACK%", m_socket);
//				return "";
//			}
			else 
			{
//				sendAcknowledgment();
				return messageComponent[1];
			}
		}
	}
	
	public String getMessageID()
	{
		message++;
		return (m_name + message);
	}

	public DatagramPacket packMsg(String msg, InetAddress address, int port)
	{
		System.out.println("Packing message: " + msg);
		byte[] data = new byte[256];
		data = msg.getBytes();
		return new DatagramPacket(data, msg.length(), address, port);
	}
	
	public String unpack(DatagramPacket packet)
	{
		return new String(packet.getData(), 0, packet.getLength());
	}
	
	


	
	
//	public void sendAcknowledgment()
//	{
//		// Randomize a failure variable
//		Random generator = new Random();
//
//		// Pack ack
//		DatagramPacket packet = packAck();
//			
//		double failure = generator.nextDouble();
//
//		// Send ack
//		try
//		{
//			System.out.println("Sending ack");
//			if (failure > TRANSMISSION_FAILURE_RATE)
//			{
//				m_ackSocket.send(packet);
//				return;
//			}
//		} catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//		
//		// Message failed to send
//		System.err.println("Error: failed to send ack");
//	}
//
//	public String receiveChatMessage()
//	{
//		byte[] buf = new byte[256];
//		DatagramPacket packet = new DatagramPacket(buf, buf.length);
//		
//		try
//		{
//			m_socket.receive(packet);
//		} catch (IOException e)
//		{
//			System.err.println("Error: client failed to receive packet.");
//			e.printStackTrace();
//		}
//
//		// Unpack and split message
//		String message = unpack(packet);
//		String[] messageComponent = message.split("\\|");
//		
//		System.out.println("Received message: " + message);
//		
//		int receivedMessageID = Integer.parseInt(messageComponent[0]);
//		message = messageComponent[1];
//		
//		if (receivedMessageID <= previousMessageID)
//		{
//			sendAcknowledgment();
//			return "";
//		}
//		
//		else
//		{
//			previousMessageID = receivedMessageID;			
//		}
//		
//		if (message.equals("%ACK%"))
//		{
//			System.out.println("Received ack");
//			return "";
//		}
//		
//		else if (message.equals("%DC%"))
//		{
//			m_socket.close();
//			return "You have been disconnected";
//		}
//		
////		else if (message.equals("%POLL%"))
////		{
////			sendChatMessage("%ACK%", m_socket);
////			return "";
////		}
//		
//		else 
//		{
//			sendAcknowledgment();
//			return messageComponent[1];
//		}
//	}
	
	

}
