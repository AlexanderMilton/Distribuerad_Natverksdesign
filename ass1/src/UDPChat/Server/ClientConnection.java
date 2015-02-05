package UDPChat.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.Random;

public class ClientConnection 
{
	private static boolean DEBUG = false;
	
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private final String m_name;
	private String m_token;
	private final InetAddress m_address;
	private final int m_port;
	private int m_sessionNum;
	private int m_remoteSessionNum;
	public Date m_lastActivity;

	public ClientConnection(String name, InetAddress address, int port)
	{
		m_name = name;
		m_token = "0";
		m_address = address;
		m_port = port;
		m_sessionNum = 1;
		m_remoteSessionNum = 0;
		m_lastActivity = new Date();
	}

	public boolean hasName(String testName) 
	{
		return testName.equals(m_name);
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public String getToken()
	{
		return m_token;
	}
	
	public void setToken(String token)
	{
		m_token = token;
	}
	
	// Returns whether this is a new session, if so also updates the session number
	public boolean checkSession(int num)
	{
		if(num > m_remoteSessionNum)
		{
			m_remoteSessionNum = num;
			return true;
		}
		else
		{
			return false;
		}
	}
	
	// Returns a new (higher) session number
	public int newSession()
	{
		return ++m_sessionNum;
	}
	
	public void updateActivityTime()
	{
		m_lastActivity = new Date();
		if(DEBUG){ System.out.println("Updating activity time"); }
	}
	
	public boolean activeSince(Date time)
	{
		return m_lastActivity.after(time);
	}
	
	// For initiating communication
	public void sendMessage(String message, DatagramSocket socket)
	{
		sendMessage(message, socket, m_port);
	}
	
	// For sending replies to incoming communications
	public void sendReplyMessage(String message, DatagramSocket socket, DatagramPacket reply)
	{
		sendMessage(message, socket, reply.getPort());
	}
	
	private void sendMessage(String message, DatagramSocket socket, int port)
	{

		Random generator = new Random();
		double failure = generator.nextDouble();

		byte[] data = message.getBytes();	
		DatagramPacket packet = new DatagramPacket(data, data.length, m_address, port);
		if (failure > TRANSMISSION_FAILURE_RATE) 
		{
			try
			{
				if(DEBUG) { System.out.println("Sending from port "+socket.getLocalPort()+" to port "+packet.getPort()+", message "+message); }
				socket.send(packet);
			} 
			catch (IOException e)
			{
				System.err.println("Error: IOException while sending message");
				System.exit(-4);
			}
		} 
		else
		{
			// Message got lost
			if(DEBUG)
			{
				System.err.println("Message lost during transmission.");
			}
		}

	}
}
