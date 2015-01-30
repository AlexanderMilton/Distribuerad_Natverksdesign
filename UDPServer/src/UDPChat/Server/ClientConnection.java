/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPChat.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author brom
 */
public class ClientConnection
{

	static double TRANSMISSION_FAILURE_RATE = 0.3;
	static int MAX_SEND_ATTEMPTS = 10;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;

	public CountDownLatch acknowledgement;
	public int m_messageCounter = 1; // Account for handshake
	public int m_ackCounter = 0;

	public ClientConnection(String name, InetAddress address, int port)
	{
		m_name = name;
		m_address = address;
		m_port = port;
		acknowledgement = new CountDownLatch(1);
	}

	public void sendMessage(DatagramPacket message, DatagramSocket socket)
	{

		// Randomize a failure variable
		Random generator = new Random();

		DatagramPacket packet = message;

		m_ackCounter++;

		System.out.println("Sending on socket at port: " + socket.getLocalPort());

		// Make a number of attempts to send the message
		for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++)
		{

			double failure = generator.nextDouble();

			if (failure > TRANSMISSION_FAILURE_RATE)
			{

				// Send message
				try
				{
					socket.send(packet);
				} catch (IOException e)
				{
					System.err.println("Error: failed to send message to client");
					e.printStackTrace();
				}

				// Receive acknowledgment from Client via Server
				try
				{
					socket.receive(packet);
					String clientResponse = new String(packet.getData(), 0, packet.getLength());
					//String[] clientResponseComponent = clientResponse.split("\\|");
					
					socket.getLocalAddress();
					socket.getLocalPort();

					System.out.println("client connection address: " + socket.getLocalAddress());
					System.out.println("client connection port: " + socket.getLocalPort());
					
					System.out.println("Client connection recieved message: " + clientResponse);

					if (clientResponse.equals("ACK"))
					{
						// Message was successfully sent and acknowledged by
						// client
						System.out.println("Ack received from client to client connection");
						return;
					} else
					{
						// Non-ack message was received
						System.err.println("Error: expected ack from client: " + clientResponse);
						continue;
					}
				} catch (IOException e)
				{
					e.printStackTrace();
					System.err.println("Error: failed to receive acknowledgement");
				}

			} else
			{
				// Message got lost
				System.out.println("Message lost on server side");
			}
		}
		// Message failed to send, decrement ack counter
		m_ackCounter--;
		System.err.println("Error: failed to send message");
	}

	public void returnAck(DatagramPacket message, DatagramSocket socket)
	{
		// Randomize a failure variable
		Random generator = new Random();
		DatagramPacket packet = message;

		System.out.println("Sending on socket at port: " + socket.getLocalPort());

		// Make a number of attempts to send the message
		for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++)
		{

			double failure = generator.nextDouble();

			if (failure > TRANSMISSION_FAILURE_RATE)
			{

				// Send message
				try
				{
					socket.send(packet);
					return;
				} catch (IOException e)
				{
					System.err.println("Error: failed to send ack to client");
					e.printStackTrace();
				}

			} else
			{
				// Message got lost
				System.out.println("Message lost on server side");
			}
		}
		// Message failed to send
		System.err.println("Error: failed to return ack");
	}

	public boolean hasName(String testName)
	{
		return testName.equals(m_name);
	}

	public InetAddress getAddress()
	{
		return m_address;
	}

	public String getName()
	{
		return m_name;
	}

	public int getPort()
	{
		return m_port;
	}

	public int getMessageCounter()
	{
		return m_messageCounter;
	}

	public int getAckCounter()
	{
		return m_ackCounter;
	}

}
