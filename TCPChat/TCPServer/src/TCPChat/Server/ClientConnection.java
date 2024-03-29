package TCPChat.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

	private boolean disconnected;
	private final String m_name;
	private final Socket m_socket;
	private PrintWriter m_writer;
	private BufferedReader m_reader;

	public ClientConnection(String name, Socket socket)
	{
		m_name = name;
		m_socket = socket;
		disconnected = false;

		try
		{
			m_writer = new PrintWriter(m_socket.getOutputStream(), true);
			m_reader = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
		} catch (IOException e)
		{
			System.out.println("Error: IO exception creating client  connection printwriter");
			e.printStackTrace();
		}
	}

	public void sendMessage(String message)
	{
		ChatMessage chatMessage = new ChatMessage("", "message", "", message);
		m_writer.println(chatMessage.getString());
	}

	public void sendHeartbeat()
	{
		ChatMessage chatMessage = new ChatMessage("", "heartbeat", "", "");
		m_writer.println(chatMessage.getString());
	}

	public String getName()
	{
		return m_name;
	}

	public Socket getSocket()
	{
		return m_socket;
	}

	public BufferedReader getReader()
	{
		return m_reader;
	}

	public boolean isDisconnected()
	{
		return disconnected;
	}

	// Check if the tested name matches this client (regardless of case and spaces)
	public boolean hasName(String name)
	{
		String testName = name.trim().toLowerCase();
		String clientName = getName().trim().toLowerCase();
		return testName.equals(clientName);
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
