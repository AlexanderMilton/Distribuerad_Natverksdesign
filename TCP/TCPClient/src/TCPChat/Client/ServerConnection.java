/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package TCPChat.Client;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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

	private DatagramSocket m_socket = null;
	private InetAddress m_serverAddress = null;
	private int m_serverPort = -1;

	public ServerConnection(String hostName, int port)
	{
		m_serverPort = port;
		
		try
		{
			m_serverAddress.getByName(hostName);
		} catch (UnknownHostException e)
		{
			System.err.println("Error: unknown host");
			e.printStackTrace();
			System.exit(1);
		}
		
		try
		{
			m_socket = new DatagramSocket();
		} catch (SocketException e)
		{
			System.err.println("Error: failed to create socket");
			e.printStackTrace();
			System.exit(1);
		}

	}

	public boolean connect(String name)
	{
		// TODO:
		// * marshal connection message containing user name
		// * send message via socket
		// * receive response message from server
		// * unmarshal response message to determine whether connection was
		// successful
		// * return false if connection failed (e.g., if user name was taken)
		return true;
	}
	
	public void disconnect()
	{
		pack();
	}
	
	public void list()
	{
		pack();
	}
	
	public void help()	// DOes this go in the client?
	{
		pack();
	}
	
	public void whisper()
	{
		pack();
	}
	
	public void broadcast()
	{
		pack();
	}
	
	public void aux()	// Determine what this does
	{
		pack();
	}

	public void sendChatMessage(String message)
	{
		Random generator = new Random();
		double failure = generator.nextDouble();

		if (failure > TRANSMISSION_FAILURE_RATE)
		{
			// TODO:
			// * marshal message if necessary
			// * send a chat message to the server
		} else
		{
			// Message got lost
		}
	}

	public String receiveChatMessage()
	{
		// TODO:
		// * receive message from server
		// * unmarshal message if necessary

		// Note that the main thread can block on receive here without
		// problems, since the GUI runs in a separate thread

		// Update to return message contents
		return "";
	}

}
