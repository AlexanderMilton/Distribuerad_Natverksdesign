/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPChat.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ClientConnection
{

	static double TRANSMISSION_FAILURE_RATE = 0.3;
	static int MAX_SEND_ATTEMPTS = 10;

	private final String m_name;
	private final InetAddress m_clientAddress;
	private final int m_clientPort;
	public DatagramSocket m_ackSocket = null;
	
	public boolean isAcked = false;
	public boolean markedForDeath = false;

	public ClientConnection(String name, InetAddress address, int port)
	{
		m_name = name;
		m_clientAddress = address;
		m_clientPort = port;
		
		System.out.println("\nm_name = " + m_name);
		System.out.println("m_clientAddress = " + m_clientAddress);
		System.out.println("m_clientPort = " + m_clientPort);
		
		// Create ack socket
		try
		{
			m_ackSocket = new DatagramSocket();
		} catch (SocketException e)
		{
			e.printStackTrace();
			System.err.println("Error: invalid port.");
		}
		
		// Set socket timeout
		try
		{
			m_ackSocket.setSoTimeout(10);
		} catch (SocketException e)
		{
			System.err.println("Error: failed to set socket timeout");
			e.printStackTrace();
		}
	}
	
	// For initiating communication
	public void sendMessage(String msg, int messageID, DatagramSocket socket)
	{
		send(msg, messageID, socket, m_clientPort);
	}
	
	// For sending replies to incoming communications
	public void sendReply(String msg, int messageID, DatagramSocket socket, DatagramPacket reply)
	{
		send(msg, messageID, socket, reply.getPort());
	}

	private void send(String msg, int messageID, DatagramSocket socket, int port)
	{
		// Randomize a failure variable
		Random generator = new Random();
		double failure = generator.nextDouble();

		System.out.println("port: " + port);
		System.out.println("msg: " + msg);
		
		DatagramPacket packet = pack(messageID, msg);
		
		if (failure > TRANSMISSION_FAILURE_RATE)
		{
			try
			{
				System.out.println("Sending message: " + msg + " with message ID: " + messageID);
				socket.send(packet);
			} catch (IOException e)
			{
				System.err.println("Error: failed to send message");
				System.exit(1);
			}
		}
		else
		{
			// Message lost
			System.err.println("Message lost on server side");
		}
		
	}
		
		
		
		
//		// Make a number of attempts to send the message
//		for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++)
//		{
//			double failure = generator.nextDouble();
//			
//			// Set latch to 1
//			isAcked = false;
//			
//			// Send message
//			try
//			{
//				if (failure > TRANSMISSION_FAILURE_RATE)
//					socket.send(packet);
//				//return true; // TODO REMOVE RETURN
//			} catch (IOException e)
//			{
//				System.err.println("Error: failed to send message to client");
//				e.printStackTrace();
//			}
//			
//			try
//			{
//				m_ackSocket.receive(packet);
//			} catch (IOException e)
//			{
//				System.out.println("Failed to receive ack from client");
//			}
//			
//			String ack = unpack(packet);
//			System.out.println("CC received ack: " + ack);
//			
//			if (ack.equals("%ACK%"))
//			{
//				return true;
//			}
//
//		}
//		// Message failed to send
//		System.err.println("Message never arrived, client presumed disconnected");
//		return false;
//	}
//
//	public void returnAck()
//	{		
//		// Randomize a failure variable
//		Random generator = new Random();
//		DatagramPacket packet = packAck();
//
//		System.out.println("CC sending ack to port: " + packet.getPort());
//		System.out.println("CC sending ack address: " + packet.getAddress());
//
//		double failure = generator.nextDouble();
//
//		if (failure > TRANSMISSION_FAILURE_RATE)
//		{
//
//			// Send message
//			try
//			{
//				m_ackSocket.send(packet);
//				return;
//			} catch (IOException e)
//			{
//				System.err.println("Error: failed to send ack to client");
//				e.printStackTrace();
//			}
//
//		} else
//		{
//			// Message got lost
//			System.out.println("returnAck lost on server side");
//		}
//		
//		// Message failed to send
//		System.err.println("Error: failed to return ack");
//	}
	
	public DatagramPacket pack(int messageID, String msg)
	{
		String message = messageID + "|" + msg;
		byte[] data = message.getBytes();
		DatagramPacket packet = new DatagramPacket(data, message.length(), m_clientAddress, m_clientPort);

		return packet;
	}

	public DatagramPacket packAck()
	{
		String msg = "%ACK%";
		System.out.println("Packing message: " + msg);
		byte[] data = new byte[256];
		data = msg.getBytes();
		return new DatagramPacket(data, msg.length(), m_clientAddress, m_clientPort);
	}
	
	public String unpack(DatagramPacket packet)
	{
		return new String(packet.getData(), 0, packet.getLength());
	}

	public boolean hasName(String testName)
	{
		return testName.equals(m_name);
	}

	public InetAddress getAddress()
	{
		return m_clientAddress;
	}

	public String getName()
	{
		return m_name;
	}

	public int getPort()
	{
		return m_clientPort;
	}

	public int getAckPort()
	{
		return m_ackSocket.getLocalPort();
	}

}
