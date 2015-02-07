package UDPChat.Server;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class Server 
{
	private static final boolean DEBUG = false;
	private static final String SERVERNAME = "Server";
	private static final String BANNER = "Welcome to the DND UDP Chat Server\nBe nice and have fun.";
	private static final int RETRANSMISSIONS = 9;
	private static final int TRANSMISSION_TIMEOUT = 300; // in ms
	private static final long ACTIVITY_TIMEOUT = 20; // in seconds
	
	private Map<String, ClientConnection> m_connectedClients = new HashMap<String, ClientConnection>();
    private DatagramSocket m_socket;

    public static void main(String[] args)
    {
		if(args.length < 1) 
		{
		    System.err.println("Usage: java Server portnumber");
		    System.exit(-1);
		}
		try 
		{
		    Server instance = new Server(Integer.parseInt(args[0]));
		    instance.listen();
		} 
		catch(NumberFormatException e) 
		{
		    System.err.println("Error: Port number must be an integer.");
		    System.exit(-1);
		} 
		catch (SocketException e)
		{
			System.err.println("Error: Could not create socket.");
		    System.exit(-2);
		}
    }

    private Server(int portNumber) throws SocketException 
    {
    	m_socket = new DatagramSocket(portNumber);
    	m_socket.setSoTimeout(TRANSMISSION_TIMEOUT);
    }

    private void listen() 
    {
		System.out.println("Server running");
		String msg = null, msg_type = "000";
		byte[] buffer = new byte[1024]; // max size 1kiB
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		while(true)
		{
			// Ping inactive clients
			{
				Date timeoutTime = new Date(new Date().getTime() - ACTIVITY_TIMEOUT*1000l);
				
				Iterator<Map.Entry<String, ClientConnection>> i;
				i = m_connectedClients.entrySet().iterator();
				ClientConnection c;
	
				while (i.hasNext())
				{
					c = i.next().getValue();
					if(!c.activeSince(timeoutTime) && !ping(c))
					{
						String username = c.getName();
						i.remove();
						sendStatus(username+" has been disconnected, ping timeout.");						
					}
				}
			}
			
			// Process incoming message
			try
			{
				m_socket.receive(packet);

				msg = new String(packet.getData(), 0, packet.getLength());
				
				msg_type = msg.split("/", 2)[0];
				switch(msg_type)
				{
				case "010": // connect
					if(DEBUG){ System.out.println("Processing incoming connection"); }
					processConnect(msg, packet);
					break;
				case "020": // disconnect
					if(DEBUG){ System.out.println("Processing incoming disconnect"); }
					processDisconnect(msg, packet);
					break;
				case "040": // public message
					if(DEBUG){ System.out.println("Processing incoming public message"); }
					processPublic(msg, packet);
					break;
				case "050": // private message
					if(DEBUG){ System.out.println("Processing incoming private message"); }
					processPrivate(msg, packet);
					break;
				case "060": // list request
					if(DEBUG){ System.out.println("Processing incoming list request"); }
					processList(msg, packet);
					break;
				default:
					System.err.println("Unknown message type, message dropped.");
					if(DEBUG){ System.out.println(msg); }
					break;
				}
			}
			catch(SocketTimeoutException e)
			{ // No packet received
			}
			catch (IOException e)
			{
				System.err.println("Error: IO error while receiving packet");
				System.exit(-3);
			}
		}
    }
    
    private String resendUntilReply(ClientConnection to, DatagramSocket from, String message) throws SocketTimeoutException
    {
		byte[] buffer = new byte[1024]; // max size 1kiB
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
    	try
		{
    		from.setSoTimeout(TRANSMISSION_TIMEOUT);
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
				to.sendMessage(message, from);
				if(DEBUG){ System.out.println("Waiting for ack"); }
				// Wait for response
				from.receive(packet);
				if(DEBUG){ System.out.println("Got ack"); }
				// If packet was received then return its content
				return new String(packet.getData(), 0, packet.getLength());
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
    
	private void processConnect(String data, DatagramPacket sender)
    {
		// Create connection for client
    	ClientConnection client = new ClientConnection(data.split("/", 2)[1], sender.getAddress(), sender.getPort());
    	
		for(Map.Entry<String, ClientConnection> i : m_connectedClients.entrySet())
		{
		    if(i.getValue().hasName(client.getName()))
		    { // There already exists a user with this name
				try
				{
					resendUntilReply(client, new DatagramSocket(), "012/Username already in use.");
				} 
				catch (SocketTimeoutException e)
				{ // No ack received
					return;
				}
				catch (SocketException e)
				{
					System.err.println("Error: Socket exception");
					if(DEBUG){ e.printStackTrace();	}
					return;
				}
				// Ack received
				return;
		    }
		}
		// Name is not taken
		try
		{
			// Create token
			String token = "";
			do
			{
				String tokenSeed = client.getName()+System.currentTimeMillis()+new Random().nextDouble();
				byte[] hash = MessageDigest.getInstance("SHA1").digest(tokenSeed.getBytes());
				
				StringBuffer hex = new StringBuffer();
				for (int i = 0; i < hash.length; i++) 
				{
					if ((0xff & hash[i]) < 0x10) 
					{
						hex.append("0"+Integer.toHexString((0xFF & hash[i])));
					}
					else 
					{
						hex.append(Integer.toHexString(0xFF & hash[i]));
					}
				}
				token = new String(hex);
			} // If hash collision, go again
			while(m_connectedClients.get(token) != null);
			// Set token
			client.setToken(token);
			
			// Send connection successful
			try
			{
				resendUntilReply(client, new DatagramSocket(), "011/"+token+"/"+BANNER);
			} 
			catch (SocketTimeoutException e)
			{ // No ack received
				System.err.println("Timeout when sending connection successful.");
				return;
			} 
			catch (SocketException e)
			{
				System.err.println("Error: SocketException");
				if(DEBUG) { e.printStackTrace(); }
				return;
			}
			
			// Store client
			m_connectedClients.put(token, client);
			// Broadcast join
			sendStatus(client.getName()+" has joined");
		} 
		catch (NoSuchAlgorithmException e)
		{
			System.err.println("Error: Could not find digest algoritm");
			if(DEBUG){ e.printStackTrace();	}
			System.exit(-5);
		}
		
    }
    
    private void processDisconnect(String data, DatagramPacket packet)
	{
    	String[] msg = data.split("/", 4);
    	if(msg.length < 4)
    	{
    		System.err.println("Malformed disconnect received.");
    		return;
    	}
    	
    	// Remove client
    	ClientConnection sender = m_connectedClients.remove(msg[1]);
    	if(sender != null)
    	{ // User disconnecting
        	// Broadcast quit message
        	sendStatus(sender.getName()+" has quit: "+msg[3]);
    	}
    	else
    	{ // Invalid token, or user already disconnected
    		// ack must go out anyway
    		sender = new ClientConnection("", packet.getAddress(), packet.getPort());
    	}
    	// Send disconnect ack
    	sender.sendReplyMessage("021", m_socket, packet);
	}

    private void processPrivate(String data, DatagramPacket packet)
	{
    	String[] msg = data.split("/", 5);
    	if(msg.length != 5)
    	{
    		System.err.println("Malformed private message received.");
    		return;
    	}
    	// Check token and get username
    	ClientConnection sender = m_connectedClients.get(msg[1]);
    	if(sender == null)
    	{
    		System.err.println("Invalid token received in private message.");
    		return;
    	}
    	sender.updateActivityTime();
    	
		ClientConnection to = getClientByName(msg[3]);
		if(to == null)
		{ // No such user
			// Send private message error
			sender.sendReplyMessage("052/Could not send message. No user "+msg[3], m_socket, packet);
			return;
		}
		
		if(sender.checkSession(Integer.parseInt(msg[2])))
    	{ // Message not already received
			sendPrivate(to, sender, msg[4]);
    	}
		
		// Send private message ack
		sender.sendReplyMessage("051", m_socket, packet);
	}
    
    private void sendPrivate(ClientConnection to, ClientConnection from, String message) 
    {
    	String data = "070/"+to.getToken()+"/"+to.newSession()+"/"+from.getName()+"/1/"+message;
    	try
    	{
    		if(DEBUG){ System.out.println("Sending message "+data); }
    		resendUntilReply(to, new DatagramSocket(), data);
        	// ack received
        	to.updateActivityTime();
    	} 
    	catch (SocketTimeoutException e)
    	{ // No ack received
    	}
    	catch (SocketException e)
    	{
    		System.err.println("Error: Socket exception");
    		if(DEBUG){ e.printStackTrace();	}
    	}
    }
    
    private void processList(String data, DatagramPacket packet)
	{
    	String[] msg = data.split("/", 3);
    	if(msg.length < 3)
    	{
    		System.err.println("Malformed list request received.");
    		return;
    	}
    	// Check token and get username
    	ClientConnection sender = m_connectedClients.get(msg[1]);
    	if(sender == null)
    	{
    		System.err.println("Invalid token received in list request.");
    		return;
    	}
    	sender.updateActivityTime();
    	
    	// Create list
    	StringBuffer list = new StringBuffer();
    	for(Map.Entry<String, ClientConnection> i : m_connectedClients.entrySet())
    	{
    		list.append(i.getValue().getName()+"\n");
    	}
    	
    	// Send list response
    	sender.sendReplyMessage("061/"+list, m_socket, packet);
	}
    
    private ClientConnection getClientByName(String name)
    {
		for(Map.Entry<String, ClientConnection> i : m_connectedClients.entrySet())
		{
		    if(i.getValue().hasName(name)) 
		    {
		    	return i.getValue();
		    }
		}
		// No such user
		return null;
    }
    
    private void processPublic(String data, DatagramPacket packet)
	{
    	String[] msg = data.split("/", 4);
    	if(msg.length < 4)
    	{
    		System.err.println("Malformed public message received.");
    		return;
    	}
    	// Check token and get username
    	ClientConnection sender = m_connectedClients.get(msg[1]);
    	if(sender == null)
    	{
    		System.err.println("Invalid token received in public message.");
    		return;
    	}
    	sender.updateActivityTime();
    	
    	try
    	{
			if(sender.checkSession(Integer.parseInt(msg[2])))
			{ // Message not already received
				sendBroadcast(sender, msg[3]);
			}
			// Send public message ack
			sender.sendReplyMessage("041", m_socket, packet);
    	}
    	catch (NumberFormatException e) 
    	{
    		System.err.println("Error converting received session number.");
    		return;
    	}
	}

    private void sendBroadcast(ClientConnection from, String message) 
    {
    	String data = null;
    	if(DEBUG){ System.out.println("Broadcasting "+message+" from "+from); }
    	for(Map.Entry<String, ClientConnection> i : m_connectedClients.entrySet()) 
		{
    		ClientConnection c = i.getValue();
    		data = "070/"+c.getToken()+"/"+c.newSession()+"/"+from.getName()+"/0/"+message;
        	try
        	{
        		if(DEBUG){ System.out.println("Sending message "+data); }
        		resendUntilReply(c, new DatagramSocket(), data);
            	// ack received
            	c.updateActivityTime();
        	} 
        	catch (SocketTimeoutException e)
        	{ // No ack received
        	}
        	catch (SocketException e)
        	{
        		System.err.println("Error: Socket exception");
        		if(DEBUG){ e.printStackTrace();	}
        	}
		}
    }

    private void sendStatus(String message) 
    {
		String data = null;
		if(DEBUG){ System.out.println("Sending status "+message); }
		for(Map.Entry<String, ClientConnection> i : m_connectedClients.entrySet()) 
		{
			ClientConnection c = i.getValue();
			data = "070/"+c.getToken()+"/"+c.newSession()+"/"+SERVERNAME+"/2/"+message;
	    	try
	    	{
	    		if(DEBUG){ System.out.println("Sending message "+data); }
	    		resendUntilReply(c, new DatagramSocket(), data);
	        	// ack received
	        	c.updateActivityTime();
	    	} 
	    	catch (SocketTimeoutException e)
	    	{ // No ack received
	    	}
	    	catch (SocketException e)
	    	{
	    		System.err.println("Error: Socket exception");
	    		if(DEBUG){ e.printStackTrace();	}
	    	}
		}
    }
    
    private boolean ping(ClientConnection client) 
    {
    	// Send ping request
		String data = "030/"+client.getToken();
		if(DEBUG){ System.out.println("Sending ping"); }
		try
    	{
    		resendUntilReply(client, new DatagramSocket(), data);
        	// ack received
        	client.updateActivityTime();
        	return true;
    	} 
    	catch (SocketTimeoutException e)
    	{ // No ack received
    		return false;
    	}
    	catch (SocketException e)
    	{
    		System.err.println("Error: Socket exception");
    		if(DEBUG){ e.printStackTrace();	}
    		return false;
    	}
    }
}