/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package TCPChat.Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import TCPChat.Shared.ChatMessage;

/**
 * 
 * @author brom
 */
public class ServerConnection
{

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private Socket m_socket = null;
	private int m_serverPort = -1;
	private InetAddress m_serverAddress = null;

	private BufferedReader m_reader;
	private PrintWriter m_writer;

	public ServerConnection(String hostName, int port)
	{
		
		try
		{
			m_serverPort = port;
			m_serverAddress = InetAddress.getByName(hostName);
			m_socket = new Socket(m_serverAddress, m_serverPort);
			m_writer = new PrintWriter(m_socket.getOutputStream());
			m_reader = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
		} catch (UnknownHostException e)
		{
			System.err.println("Error: unknown host");
			e.printStackTrace();
		} catch (IOException e)
		{
			System.err.println("Error: IOException");
			e.printStackTrace();
			System.exit(1);
		}

	}

	public void handleInput(String input)
	{

	}

	public boolean connect(String name)
	{
			
		String received = new String();
		
		ChatMessage cm = new ChatMessage("Peter Sjöberg", "Datorgrafik", "Examination", "Ser det där rätt ut?");
		System.out.println(cm.getString());
		
		
		
		
		System.out.println("Received " + received);
		
		try 
		{
			m_socket.close();
		} catch (IOException e)
		{
			System.err.println("Error: IOException closing socket");
			e.printStackTrace();
			System.exit(1);
		}
		
		// TODO:
		// * marshal connection message containing user name
		// * send message via socket
		// * receive response message from server
		// * unmarshal response message to determine whether connection was
		// successful
		// * return false if connection failed (e.g., if user name was taken)
		
		
		return false;
	}

	public void disconnect()
	{
		// pack();
	}

	public void list()
	{
		// pack();
	}

	public void help() // TODO: Does this go in the client or throught the
						// server?
	{
		// pack();
	}

	public void whisper(ChatMessage msg)
	{
		// pack();
	}

	public void broadcast()
	{
		// pack();
	}

	public void aux() // Determine what this does
	{
		// pack();
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

	private DatagramPacket pack(String message)
	{
		byte[] data = new byte[256];
		data = message.getBytes();

		System.out.println("Packing message: " + message);
		return new DatagramPacket(data, message.length(), m_serverAddress, m_serverPort);
	}

	private String unpack(DatagramPacket packet)
	{
		String message = new String(packet.getData(), 0, packet.getLength());

		System.out.println("Unpacked message: " + message);
		return message;
	}
}
