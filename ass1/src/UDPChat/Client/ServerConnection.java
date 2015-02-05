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

public class ServerConnection 
{

	// Artificial 30% packet loss
	private static double TRANSMISSION_FAILURE_RATE = 0.3;
	private static final boolean DEBUG = false;
	private static final int RETRANSMISSIONS = 9;
	private static final int TIMEOUT = 500; // in ms
	
	// For communication initiated by the server
	private DatagramSocket m_socket = null;
	private InetAddress m_serverAddress = null;
	private int m_serverPort = -2;
	private String m_token = "0"; // stores authentication token
	private int m_localSessionNum = 1;
	private int m_remoteSessionNum = 0;

	public ServerConnection(String hostName, int port) throws SocketException, UnknownHostException 
	{
		m_serverPort = port;
		m_serverAddress = InetAddress.getByName(hostName);
		m_socket = new DatagramSocket();
	}
	
	private void send(String message, DatagramSocket socket, int port)
	{
		Random generator = new Random();
		double failure = generator.nextDouble();

		byte[] data = message.getBytes();	
		DatagramPacket packet = new DatagramPacket(data, data.length, m_serverAddress, port);
		if (failure > TRANSMISSION_FAILURE_RATE) 
		{
			try
			{
				if(DEBUG){ System.out.println("Sending from port "+socket.getLocalPort()+" to port "+packet.getPort()+", message "+message); }
				socket.send(packet);
			} 
			catch (IOException e)
			{
				System.err.println("Error: Unable to connect to server");
				System.err.println("IOException");
				System.exit(-4);
			}
		} 
		else 
		{
			// Message got lost
			if(DEBUG){ System.err.println("Message lost during transmission."); }
		}
	}
	
	private DatagramPacket resendUntilReply(String message, DatagramSocket from, int to_port) throws SocketTimeoutException
    {
		byte[] buffer = new byte[1024]; // max size 1kiB
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
    	try
		{
    		from.setSoTimeout(TIMEOUT);
		} 
    	catch (SocketException e)
		{
			System.err.println("Error setting socket timeout");
			e.printStackTrace();
			System.exit(-4);
		}
    	
    	int tries = 0;
    	while(true)
    	{
			try
			{
				if(DEBUG){ System.out.println("Sending "+message); }
				// Send message
				send(message, from, to_port);
				if(DEBUG){ System.out.println("Waiting for ack"); }
				// Wait for response
				from.receive(packet);
				if(DEBUG){ System.out.println("Got ack"); }
				// If packet was received then return its content
				return packet;
			}
			catch (SocketTimeoutException e)
			{ // Timeout
				if(tries <= RETRANSMISSIONS)
				{ // Try again
					if(DEBUG){ System.out.println("Wait timeout, trying again"); }
					++tries;
				}
				else
				{ // No more retransmissions
					if(DEBUG){ System.out.println("Wait timeout, giving up"); }
					throw e;
				}
			}
			catch (IOException e)
			{
				System.err.println("Error: IO error while receiving packet");
				System.exit(-3);
			}
    	}
    }
	
	private DatagramPacket receivePacket(DatagramSocket socket) throws SocketTimeoutException
	{
		byte[] buffer = new byte[1024]; // max size 1kiB
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		try
		{
			if(DEBUG){ System.out.println("Receiving on port "+socket.getLocalPort()); }
			socket.receive(packet);
		}
		catch(SocketTimeoutException e)
		{
			throw e;
		}
		catch (IOException e)
		{
			System.err.println("Error: IOException while receiving.");
			System.exit(-3);
		}
		return packet;
	}
	
	private String getMessage(DatagramPacket packet)
	{
		String data = new String(packet.getData(), 0, packet.getLength());
		return data;
	}
	
	// Returns any server messages. Automatically acks when appropriate
	public String waitForServerMessage()
	{
		DatagramPacket packet = null;
		String msg_type = null;
		String data = null;
		String[] msg = null;
		
		while(true)
		{
			try
			{
				packet = receivePacket(m_socket);
				data = getMessage(packet);
				if(DEBUG){ System.out.println("Received message "+data); }
				msg_type = data.split("/", 2)[0];
				
				if(msg_type.equals("070"))
				{ // server message
					// Get first three fields and remainder
					msg = getMessage(packet).split("/", 4);
					if(msg.length < 4)
					{
						System.err.println("Received malformed server message.");
					}
					else
					{
						// Check token
						if(msg[1].equals(m_token))
						{
							// Send ack
							send("071", m_socket, packet.getPort());
							
							// Check session number
							if(Integer.parseInt(msg[2]) > m_remoteSessionNum)
							{ // Not already received
								// Update session number
								m_remoteSessionNum = Integer.parseInt(msg[2]);
								// Return message body
								return msg[3];
							}
						}
						else
						{
							System.err.println("Invalid token in server message!");
						}

					}
				}
				else if(msg_type.equals("030"))
				{ // ping request
					// Send ping response
					send("031", m_socket, packet.getPort());
				}
				else if(msg_type.equals("011") || msg_type.equals("012"))
				{ // connection successful || connection failed
					// Send ack
					send("013", m_socket, packet.getPort());
				}
				else
				{
					System.err.println("Unknown message type received.");
				}
			} 
			catch (SocketTimeoutException e)
			{ // Try again
			}
		}
	}

	public String connect(String username) throws ConnectFailedException 
	{
		if (username == null)
		{
			throw new ConnectFailedException("Null username provided");
		}
		
		// Send connect on the socket that the server will use 
		try
		{
			DatagramPacket reply = resendUntilReply("010"+"/"+username, m_socket, m_serverPort);
			
			String data = getMessage(reply);
			String[] msg = data.split("/", 3);
			if(msg.length < 2)
			{
				if(DEBUG){ System.err.println("Invalid message: "+data); }
				throw new ConnectFailedException("Invalid response from server");
			}
			
			if (msg[0].equals("012")) // Connection failed
			{
				if(DEBUG){ System.err.println("Got 012 connection failed"); }
				send("013", m_socket, reply.getPort());
				throw new ConnectFailedException(msg[1]);
			}
			else if(msg[0].equals("011")) // Connection successful
			{
				if(DEBUG){ System.err.println("Got 011 connection successful"); }
				send("013", m_socket, reply.getPort());
				m_token = msg[1];
				if (msg.length < 3) // No banner set
				{
					return "";
				}
				else
				{
					return msg[2];
				}
			}
			else
			{
				if(DEBUG){ System.err.println("Invalid message: "+data); }
				throw new ConnectFailedException("Invalid response from server");
			}
		} 
		catch (SocketTimeoutException e)
		{
			throw new ConnectFailedException("Connection timeout");
		}
	}
	
	public void disconnect(String message)
	{
		try
		{
			DatagramSocket socket = new DatagramSocket();
			resendUntilReply("020/"+m_token+"/"+m_localSessionNum+"/"+message, socket, m_serverPort);
		} 
		catch (SocketException | SocketTimeoutException e)
		{
			if(DEBUG){ System.err.println("Could not disconnect."); }
			++m_localSessionNum;
			return;
		}
		++m_localSessionNum;
	}

	public boolean sendPublic(String message)
	{
		try
		{
			DatagramSocket socket = new DatagramSocket();
			resendUntilReply("040/"+m_token+"/"+m_localSessionNum+"/"+message, socket, m_serverPort);
		} 
		catch (SocketException | SocketTimeoutException e)
		{
			if(DEBUG){ System.err.println("Could not send public."); }
			++m_localSessionNum;
			return false;
		}
		++m_localSessionNum;
		return true;
	}
	
	public String sendPrivate(String to, String message)
	{
		try
		{
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket reply = resendUntilReply("050/"+m_token+"/"+m_localSessionNum+"/"+to+"/"+message, socket, m_serverPort);
			++m_localSessionNum;
			
			String[] msg = getMessage(reply).split("/", 2);
			if(msg[0].equals("051"))
			{ // private message ack
				return null;
			}
			
			if(msg[0].equals("052") && msg.length == 2)
			{ // private message error
				return msg[1];
			}
			
			if(DEBUG){ System.err.println("Malformed server response to private message.\n"+getMessage(reply)); }
			return "Malformed server response.";
		} 
		catch (SocketException | SocketTimeoutException e)
		{
			if(DEBUG){ System.err.println("Could not send private."); }
			++m_localSessionNum;
			return "Connection timeout";
		}
	}
	
	public String requestList()
	{
		try
		{
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket reply = resendUntilReply("060/"+m_token+"/"+m_localSessionNum, socket, m_serverPort);
			++m_localSessionNum;
			
			String[] msg = getMessage(reply).split("/", 2);
			if(msg[0].equals("061") && msg.length == 2)
			{ // list response
				return msg[1];
			}
			
			System.err.println("Malformed server response to list request.");
			if(DEBUG){ System.err.println(getMessage(reply)); }
			return null;
		} 
		catch (SocketException | SocketTimeoutException e)
		{
			if(DEBUG){ System.err.println("Could not request list."); }
			++m_localSessionNum;
			return null;
		}
	}
}
