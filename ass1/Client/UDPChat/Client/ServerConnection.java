/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPChat.Client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


import UDPChat.Shared.Message;
import UDPChat.Shared.MessageID;
import UDPChat.Shared.PacketSenderThread;

/**
 *
 * @author brom
 */
public class ServerConnection {	
    private DatagramSocket m_socket = null;
    private InetAddress m_serverAddress = null;
    private int m_serverPort = -1;
    private long currentPacketID = 0;
    private long lastServerPacketID = -1;
    private String username;
    private PacketSenderThread packetSender;

    public ServerConnection(String hostName, int port) {
		m_serverPort = port;
		
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			System.err.println("Error: Unknown host.");
		}
	
		try {
			m_socket 		= new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("Error: Failed to create socket.");
		}
		
		/* Create the packetSender thread, it will (re-)send all the messages */
		packetSender = new PacketSenderThread(m_socket);
		packetSender.start();
    }

    public boolean handshake(String name) {
    	username = name;
	    
	    sendChatMessage("/join GENERIC_JOIN_REQUEST");
	    String data = "false";
		
	    try {
			data = receiveChatMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   
	    System.out.println("Handshake resulted in " + data);
	    boolean result = Boolean.parseBoolean(data);
	    
		return result;
    }

    public String receiveChatMessage() throws IOException {
    	DatagramPacket packet = null;
    	Message message = null;
    	do {
        	byte[] buffer = new byte[512];
        	packet = new DatagramPacket(buffer, buffer.length, m_serverAddress, m_serverPort);
        	
            m_socket.receive(packet);

        	message = new Message(new String(packet.getData(), 0, packet.getLength()));
        	
   
        	packetSender.sendAckPacket(packet, username);
        	
        	
        	if (message.getText().equals("ACK")) {
        		int ptype = packetSender.getCurrentPacketType();
        		packetSender.advanceQueue(Long.toString(message.getUID()));
        		
        		/* WORKAROUND: We need to handle the leave in some way, since after a leave message has been handled, we can't send back packages. */
        		if (ptype == MessageID.LEAVE) {
        			return "You have been disconnected from the server.";
        		}
        	}

        	System.out.println("Last packet ID from Server was: " + lastServerPacketID);
        	System.out.println("Got message from server with pid of " + message.getUID());
        	
    	} while(message.getText().equals("ACK") || message.getUID() <= lastServerPacketID || message.getType() == MessageID.PING);
    	
    	lastServerPacketID = message.getUID();
    	
    	return message.getText();
    }
    
    private int fetchMessageID(String message) {
    	String[] tok = message.split(" ", 2);
    	tok[0].trim();
    	
    	switch(tok[0].toLowerCase()) {
    	case "/join":
    		return MessageID.JOIN;
    	case "/leave":
    		return MessageID.LEAVE;
    	case "/tell":
    		return MessageID.TELL;
    	case "/list":
    		return MessageID.LIST;
    		default:
    			return MessageID.SHOUT;
    	}
    }

    public void sendChatMessage(String message) {
    	/* Get the message-type id, this will determine how we build the message. */
    	int id = fetchMessageID(message);
    	switch(id) {
    	case MessageID.SHOUT:
    	case MessageID.LIST:
    	case MessageID.TELL:
    			message = Long.toString(currentPacketID) + " " + id + " " + username + " " + message;
    			break;
    			
    	case MessageID.LEAVE:
    		String lm = "";
    		try {
    			lm = message.split(" ", 2)[1].trim();
    		} catch(ArrayIndexOutOfBoundsException e) {
    			lm = "GENERIC_EXIT_MESSAGE";
    		}
    		message = Long.toString(currentPacketID) + " " + id + " " + username + " " + lm;
    		break;
    		
    			default:
    				message = Long.toString(currentPacketID) + " " + id + " " + username + " " + message.split(" ", 2)[1];
    				break;
    	}
    	
    	
    	byte[] buf = new byte[512];
    	buf = message.getBytes();
    		
    	DatagramPacket packet = new DatagramPacket(buf, buf.length, m_serverAddress, m_serverPort);
    	
    	packetSender.add(packet);

		currentPacketID++;
    }
}
