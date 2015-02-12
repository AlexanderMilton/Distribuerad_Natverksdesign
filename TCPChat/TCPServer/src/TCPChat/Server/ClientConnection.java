/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package TCPChat.Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import TCPChat.Shared.ChatMessage;

/**
 * 
 * @author brom
 */
public class ClientConnection
{

	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private final String m_name;
	private final Socket m_socket;
	private boolean crashed;
	private boolean disconnected;
	private PrintWriter m_writer;

	public ClientConnection(String name, Socket socket)
	{
		m_name = name;
		m_socket = socket;
		crashed = false;
		disconnected = false;
		
		try
		{
			m_writer = new PrintWriter(m_socket.getOutputStream(), true);
		} catch (IOException e)
		{
			System.out.println("Error: IO exception creating client  connection printwriter");
			e.printStackTrace();
		}
	}

	public void sendMessage(String message)
	{
		if (crashed)
			return;
		
		ChatMessage chatMessage = new ChatMessage("", "message", "", message);
		m_writer.println(chatMessage.getString());
	}

	public String getName()
	{
		if (crashed)
			return m_name + " (disconnected)";
		else
			return m_name;
	}

	public Socket getSocket()
	{
		return m_socket;
	}
	
	public boolean isDisconnected()
	{
		return disconnected;
	}
	
	public boolean isCrashed()
	{
		return crashed;
	}

	// Check if the tested name matches this client (regardless of case and spaces)
	public boolean hasName(String name)
	{
		String testName = name.trim().toLowerCase();
		String clientName = getName().trim().toLowerCase();
		return testName.equals(clientName);
	}

	// Mark connection as crashed
	public void markAsCrashed()
	{
		crashed = true;
	}

	// Mark connection as ready for deletion
	public void markAsDisconnected()
	{
		disconnected = true;
	}

	// Close the connection socket
	public void closeSocket()
	{
		try
		{
			m_socket.close();
		} catch (IOException e)
		{
			System.err.println("Error: failed to close client connection socket");
			e.printStackTrace();
		}
	}
}
