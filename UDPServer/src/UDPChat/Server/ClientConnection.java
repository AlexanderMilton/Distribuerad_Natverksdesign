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
import java.util.concurrent.TimeUnit;

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

	public CountDownLatch acknowledgment;
	//public int m_messageCounter = 0;
	//public int m_ackCounter = 0;

	public ClientConnection(String name, InetAddress address, int port)
	{
		m_name = name;
		m_address = address;
		m_port = port;
		acknowledgment = new CountDownLatch(1);
	}

	public void sendMessage(DatagramPacket message, DatagramSocket socket)
	{

//		System.out.println("messageCounter: " + m_messageCounter);
//		System.out.println("ackCounter: " + m_ackCounter);
		
		// Randomize a failure variable
		Random generator = new Random();

		acknowledgment = new CountDownLatch(1);

		DatagramPacket packet = message;

//		m_ackCounter++;

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
					return;	// TODO: Remove if receiving in same function
				} catch (IOException e)
				{
					System.err.println("Error: failed to send message to client");
					e.printStackTrace();
				}

				// Receive acknowledgment from Client via Server
				
					/*try
					{
						acknowledgment.await();
						System.out.println("Received client acknowledgment message after " + i + " attempts");
						return;
					} catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					
					

			} else
			{
				// Message got lost
				System.out.println("Message lost on server side, " + (MAX_SEND_ATTEMPTS - i) + " attempts left");
			}
		}
		// Message failed to send, decrement ack counter
//		m_ackCounter--;
		System.err.println("Error: failed to send message");
	}

	public void returnAck(DatagramPacket message, DatagramSocket socket)
	{

//		System.out.println("messageCounter: " + m_messageCounter);
//		System.out.println("ackCounter: " + m_ackCounter);
		
		// Randomize a failure variable
		Random generator = new Random();
		DatagramPacket packet = message;

		System.out.println("Sending on socket at port: " + socket.getLocalPort());

		// Make a number of attempts to send the message
		//for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++)
		{

			double failure = generator.nextDouble();

			if (failure > TRANSMISSION_FAILURE_RATE)
			{

				// Send message
				try
				{
					socket.send(packet);
					System.out.println("6) Only-once acknowledgment was sent to client from client connection");
					return;
				} catch (IOException e)
				{
					System.err.println("Error: failed to send ack to client");
					e.printStackTrace();
				}

			} else
			{
				// Message got lost
				//System.out.println("Message lost on server side");
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
		return 1;
	}

	public int getAckCounter()
	{
		return 1;
	}

}
