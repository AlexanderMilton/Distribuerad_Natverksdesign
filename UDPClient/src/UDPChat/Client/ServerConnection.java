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
import java.util.concurrent.Semaphore;
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

	int m_serverPort = -1;
	private InetAddress m_serverAddress = null;
	public DatagramSocket m_socket = null;
	private Semaphore ackLock = new Semaphore(1);
	// private int m_ackPort = -1;
	// private InetAddress m_ackAddress = null;
	// public DatagramSocket m_ackSocket = null;

	private String m_name;
	private int message = 0;
	private int latestMessageID = 0;

	// private boolean isAcked = false;
	// private boolean recentlyReceived = false;

	public boolean connect(String name)
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

		String message = new String("01" + "|" + getMessageID() + "|" + name);

		// Send connection request (handshake)
		System.out.println("Sending handshake to server...");
		String response = new String(unpack(sendMessage(message, m_socket, m_serverPort)));

		System.out.println("Unpacked handshake response: " + response);
		
		// Handshake successful
		if (response.split("\\|")[1].equals("ACK"))
		{
			ackLock.release();
			return true;
		}

		// Handshake successful
		if (response.split("\\|")[1].equals("OK"))
		{
			System.out.println("Successfully connected to server\n");
			ackLock.release();
			return true;
		}

		// Handshake unsuccessful, name was taken
		else if (response.split("\\|")[1].equals("NAME"))
		{
			System.err.println("Error: that username is already taken");
			m_socket.close();
			return false;
		}

		System.err.println("Error: unknown handshake return: " + response);
		return false;
	}

	public ServerConnection(String hostName, int port, String name)
	{
		m_name = name;
		m_serverPort = port;
		ackLock.drainPermits();

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
		System.out.println("Server port: " + m_serverPort);
		System.out.println("Acknowledgement semaphore permits: " + ackLock.availablePermits() + "\n");
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
		else
		{
			// Message failed to send
			System.err.println("Error: failed to send message");
		}
	}

	public DatagramPacket sendMessage(String msg, DatagramSocket socket, int port)
	{

		byte[] data = new byte[256];
		DatagramPacket acknowledgment = new DatagramPacket(data, data.length);
		
//		try
//		{
//			socket.setSoTimeout(1000);
//		} catch (SocketException e)
//		{
//			e.printStackTrace();
//		}

		for (int i = 0; i <= MAX_SEND_ATTEMPTS; i++)
		{
			try
			{
				// Send message
				System.out.println("Sending message: " + msg);
				send(msg, socket, port);

				ackLock.tryAcquire(1, 1000, TimeUnit.MILLISECONDS);

//				// Wait for acknowledgment
//				System.out.println("Awaiting acknowledgment...");
//				socket.receive(acknowledgment);
//				// TODO: get shit together
//				
//				// Received acknowledgment, return packet
//				System.out.println("Received acknowledgment");
//				return acknowledgment;

			}
//			catch (SocketTimeoutException e)
//			{
//				// Socket timed out
//				System.out.println("Socket timed out, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
//			}
			catch (InterruptedException e)
			{
				// Socket timed out
				System.out.println("Socket timed out, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
			}
//			catch (IOException e)
//			{
//				System.err.println("Error: I/O-exception");
//			}
		}
		System.err.println("Error: failed to receive message");
		System.exit(1);
		return acknowledgment;
	}

	public DatagramPacket receivePacket(DatagramSocket socket) throws SocketTimeoutException
	{
		byte[] data = new byte[256];
		System.out.println("Buffer size: " + data.length);
		DatagramPacket packet = new DatagramPacket(data, data.length);

		try
		{
			socket.receive(packet);
			return packet;
		} catch (SocketTimeoutException e)
		{
			// Timeout
		} catch (IOException e)
		{
			System.err.println("Error: failed to receive message");
			e.printStackTrace();
		}
		return packet;
	}

	public String receiveMessage()
	{
		DatagramPacket packet = null;
		String message = null;
		String[] messageComponent = null;
		int receivedMessageID = 0;

		while (true)
		{
			try
			{
				// Receive and unpack a server distributed message
				packet = receivePacket(m_socket);
				message = unpack(packet);
				
				System.out.println("Received and unpacked message: " + message + " from port " + packet.getPort());

				switch (message)
				{
				case "OK":
				case "NAME":
					return message;
					
				case "ACK":
					ackLock.release();
					
				case "DC":
					m_socket.close();
					System.exit(0);
					break;
					
				case "":
					return "";
				}

				messageComponent = message.split("\\|");

				System.out.println(message);

				// Check the ID of the message
				receivedMessageID = Integer.parseInt(messageComponent[0]);

				if(messageComponent[1].equals("OK"))
				{
					
				}
//				if (receivedMessageID <= latestMessageID)
//				{
//					// Acknowledge reception
//					send("%ACK%", m_socket, packet.getPort());
//					return "";
//				} else
//				{
//					latestMessageID = receivedMessageID;
//				}

				// Acknowledge reception
				send("ACK", m_socket, packet.getPort());
				return messageComponent[1];

			} catch (SocketTimeoutException e)
			{
				// Socket timed out
			}
		}
	}

	public int getMessageID()
	{
		message++;
		return (message);
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
		return new String(packet.getData(), 0, packet.getLength()).trim();
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
//		// else if (message.equals("%POLL%"))
//		// {
//		// sendChatMessage("%ACK%", m_socket);
//		// return "";
//		// }
//
//		else
//		{
//			sendAcknowledgment();
//			return messageComponent[1];
//		}
//	}

}
