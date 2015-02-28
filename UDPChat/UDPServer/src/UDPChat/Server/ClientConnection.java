package UDPChat.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

import UDPChat.Shared.ChatMessage;

public class ClientConnection
{
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private boolean disconnected;
	private final String m_name;
	private final DatagramSocket m_socket;
//	private PrintWriter m_writer;
//	private BufferedReader m_reader;

	public ClientConnection(String name, DatagramSocket socket)
	{
		m_name = name;
		m_socket = socket;
		disconnected = false;
	}

	public void sendMessage(ChatMessage message)
	{
		DatagramPacket packet = message.getPacket();
		m_socket.send(packet);
		ChatMessage chatMessage = new ChatMessage(m_serverAddress, m_serverPort, 6, m_name, getTimeStamp(), "", "");
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

	public DatagramSocket getSocket()
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
		m_socket.close();
	}
}
