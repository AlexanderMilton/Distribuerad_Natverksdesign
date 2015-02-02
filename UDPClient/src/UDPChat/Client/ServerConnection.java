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
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author brom
 */
public class ServerConnection
{

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;
	static int MAX_SEND_ATTEMPTS = 10;

	private String m_name;
	private int m_serverPort = -1;
	private InetAddress m_serverAddress = null;
	public DatagramSocket m_socket = null;

	public CountDownLatch acknowledgment;
//	public int m_messageCounter = 0;	// Start at one to be ahead of server
//	private int m_ackCounter = 0;

	public ServerConnection(String hostName, int port, String name)
	{

		m_name = name;
		m_serverPort = port;
		acknowledgment = new CountDownLatch(1);

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
			// m_clientAckSocket = new DatagramSocket();
			// m_serverSocket = new DatagramSocket(m_serverPort);
		} catch (SocketException e)
		{
			e.printStackTrace();
			System.err.println("Error: invalid port.");
		}

		System.out.println("m_socket port: " + m_socket.getLocalPort());
		System.out.println("Server address: " + m_serverAddress);
		System.out.println("Server port: " + m_serverPort + "\n");
	}

	public boolean handshake(String name)
	{
		
		// Verify valid name length
		if (name.length() > 20)
		{
			System.err.println("Error: username can be at most 20 characters.");
			return false;
		} else if (name.length() < 3)
		{
			System.err.println("Error: username must be at least 3 characters.");
			return false;
		}

		try
		{
			System.out.println("Sending handshake to server...");
			m_socket.send(pack("01" + "|" + 1 + "|" + name));
		} catch (IOException e)
		{
			System.err.println("Failed to send handshake from client");
			e.printStackTrace();
		}

		byte[] buf = new byte[256];
		DatagramPacket handshakeResponse = new DatagramPacket(buf, buf.length);

		// Receive handshake 
		try 
		{ 
			System.out.println("Receiving handshake from server...");
			m_socket.receive(handshakeResponse);
		} catch (IOException e) {
			System.err.println("Failed to receive packet"); e.printStackTrace();
		}

		String message = unpack(handshakeResponse);
		
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
			return false;
		}

		System.err.println("Error: unknown handshake return: " + handshakeResponse);
		return false;
	}

	public void sendChatMessage(String msg)
	{
//		System.out.println("messageCounter: " + m_messageCounter);
//		System.out.println("ackCounter: " + m_ackCounter);

		
		System.out.println("2) sendChatMessage(" + msg + ")");

		// Randomize a failure variable
		Random generator = new Random();

		// Trim message
		String message = msg;
		message.trim();

		// Pack message with the given type
		DatagramPacket packet = pack(message);
		
		// Set latch to 1
		acknowledgment = new CountDownLatch(1);

		// Make a number of attempts to send the message
		for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++)
		{
			double failure = generator.nextDouble();

			if (failure > TRANSMISSION_FAILURE_RATE)
			{

				// Send message
				try
				{
					System.out.println("Sending message: " + msg);
					m_socket.send(packet);
					System.out.println("3) Message was successfullt sent");
					return;	// TODO: Remove if receiving in same function
				} catch (IOException e)
				{
					e.printStackTrace();
				}

				// Receive server acknowledgment
				try
				{
					if(acknowledgment.await(1000, TimeUnit.MILLISECONDS))
					{

						System.out.println("9) CDL is released in sendChatMessage");
						System.out.println("Received server acknowledgment message after " + i + " attempts");
						return;
					}
					else
					{
						System.err.println("Server acknowledgment timed out");
					}
				} catch (InterruptedException e)
				{
					System.err.println("Failed to receive server acknowledgment");
					e.printStackTrace();
				}
			}
			
			else
			{
				// Message got lost
				System.err.println("Message lost on client side, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
			}
		}
		// Message failed to send, decrement message counter
//		m_messageCounter--;
		System.err.println("Error: failed to send message");
	}

	public String receiveChatMessage()
	{
		System.out.println("1.5) Blocking message receive");

		byte[] buf = new byte[256];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		try
		{
			m_socket.receive(packet);
			System.out.println("7) Acknowledgment should be recieved by client in receiveChatMessage");
		} catch (IOException e)
		{
			System.err.println("Error: client failed to receive packet.");
			e.printStackTrace();
		}

		// Unpack and split message
		String message = unpack(packet);
		String[] messageComponent = message.split("\\|");

		System.out.println("Received message: " + message);
		
		if (messageComponent[1].equals("%ACK%"))// && Integer.parseInt(messageComponent[0]) >= m_ackCounter)
		{

			System.out.println("8) Message is \"ACK\", AC is incremented and CDL released");
			System.out.println("Received ack, incrementing ack counter");
//			m_ackCounter++;
			acknowledgment.countDown();
			return "";
		}
		
		else if (messageComponent[1].equals("%DC%"))
		{
			m_socket.close();
			return "You have been disconnected";
		}
		
		else if (messageComponent[1].equals("%POLL%"))
		{
			sendChatMessage("06|" + 1 + "|" + m_name);
			return "";
		}
		
		else 
		{
			//returnAck();
			return messageComponent[1];
		}
	}

	public void returnAck()
	{

//		System.out.println("messageCounter: " + m_messageCounter);
//		System.out.println("ackCounter: " + m_ackCounter);

		System.out.println("11) A message has been received and acknowledgment will be returned");
		// Randomize a failure variable
		Random generator = new Random();
		DatagramPacket packet = pack("05" + "|" + 1 + "|" + m_name);

		System.out.println("Sending on socket at port: " + m_socket.getLocalPort());

		// Make a number of attempts to send the message
		//for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++)
		{

			double failure = generator.nextDouble();

			if (failure > TRANSMISSION_FAILURE_RATE)
			{

				// Send message
				try
				{
					m_socket.send(packet);
					return;
				} catch (IOException e)
				{
					System.err.println("Error: failed to send ack to server");
					e.printStackTrace();
				}

			} else
			{
				// Message got lost
				System.out.println("Message lost on client side");
			}
		}
		// Message failed to send
		System.err.println("Error: failed to return ack");
	}

	public DatagramPacket pack(String msg)
	{
		// Append message code and name to message, marshal packet and send it
		// to assigned address and port
		byte[] data = new byte[256];
		data = msg.getBytes();
		System.out.println("Packed message: " + msg);
		return new DatagramPacket(data, msg.length(), m_serverAddress, m_serverPort);
	}

	public String unpack(DatagramPacket packet)
	{
		String message = new String(packet.getData(), 0, packet.getLength());
		System.out.println("Unpacked packet containing: " + message);
		return message;
	}

}
