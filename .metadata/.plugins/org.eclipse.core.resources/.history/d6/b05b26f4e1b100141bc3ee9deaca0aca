/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package TCPChat.Client;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import TCPChat.Shared.ChatMessage;

/**
 * 
 * @author brom
 */
public class ServerConnection
{

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private String m_name = null;
	private Socket m_socket = null;
	private int m_serverPort = -1;
	private InetAddress m_serverAddress = null;

	private BufferedReader m_reader;
	private PrintWriter m_writer;

	public ServerConnection(String hostName, int port, String name)
	{
		m_name = name;

		try
		{
			m_serverPort = port;
			m_serverAddress = InetAddress.getByName(hostName);
			m_socket = new Socket(m_serverAddress, m_serverPort);
			m_writer = new PrintWriter(m_socket.getOutputStream(), true);
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

	public boolean connect(String name) throws IOException
	{
		// Send connection request
		ChatMessage sendThis = new ChatMessage(m_name, "connect", "0", "0");
		m_writer.println(sendThis.getString());
		System.out.println("Sending ChatMessage: " + sendThis.getString());

		// Receive response		
		String received = m_reader.readLine();
		String response = new ChatMessage(received).getMessage();

		if (response.equals("OK"))
		{
			// Connection approved
			System.out.println("Connection approved by server");
			return true;
		}
		else if (response.equals("NAME"))
		{
			// Name already taken
			System.err.println("Error: that name is already taken by another user");
			return false;
		}
		else
		{
			// Unknown response
			System.err.println("Error: unknown connection response: " + response);
			System.exit(1);
			return false;
		}
	}

	/*
	 * String sender String command String parameters String message
	 */

	// DISCONNECT
	public void disconnect()
	{
		ChatMessage message = new ChatMessage(m_name, "disconnect", "", "");
		System.out.println("Sending disconnect request");
		m_writer.println(message.getString());
	}

	// LIST
	public void list()
	{
		ChatMessage message = new ChatMessage(m_name, "list", "", "");
		System.out.println("Sending user list request");
		m_writer.println(message.getString());
	}

	// HELP
	public void help() // TODO: Does this go in the client or through the
						// server?
	{
		ChatMessage message = new ChatMessage(m_name, "help", "", "");
		System.out.println("Sending help request");
		m_writer.println(message.getString());
	}

	// WHISPER
	public void whisper(String recepient, String msg)
	{
		ChatMessage message = new ChatMessage(m_name, "whisper", recepient, msg);
		System.out.println("Send whisper to " + recepient);
		m_writer.println(message.getString());
	}
	
	// BROADCAST
	public void broadcast(String msg)
	{
		ChatMessage message = new ChatMessage(m_name, "broadcast", "", msg);
		System.out.println("Send broadcast");
		m_writer.println(message.getString());
	}

	// EMOTE
	public void emote(String msg) // Determine what this does
	{
		ChatMessage message = new ChatMessage(m_name, "emote", "", msg);
		m_writer.println(message.getString());
	}

	public String receiveChatMessage()
	{
		String message = "";
		try
		{
			System.out.println("Receiving message...");
			message = m_reader.readLine();
		} catch (IOException e)
		{
			System.err.println("Error: failed to read from Buffered Reader");
			e.printStackTrace();
		}
		return message;
	}
}
