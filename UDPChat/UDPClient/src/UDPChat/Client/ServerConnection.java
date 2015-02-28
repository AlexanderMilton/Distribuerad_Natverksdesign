package UDPChat.Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;

import UDPChat.Shared.ChatMessage;

public class ServerConnection
{

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;
	static int MAX_ATTEMPTS = 10;

	private String m_name = null;
	private DatagramSocket m_socket = null;
	private int m_serverPort = -1;
	private InetAddress m_serverAddress = null;
	private long acknowledgedTimeStamp = 0;

//	private BufferedReader m_reader;
//	private PrintWriter m_writer;

	public ServerConnection(String hostName, int port, String name)
	{
		m_name = name;

		try
		{
			m_serverPort = port;
			m_serverAddress = InetAddress.getByName(hostName);
			m_socket = new DatagramSocket(m_serverPort, m_serverAddress);
//			m_writer = new PrintWriter(m_socket.getOutputStream(), true);
//			m_reader = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
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

	// 1 CONNECT
	public boolean connect(String name) throws IOException
	{
		// Create varaibles
		ChatMessage connectionRequest = new ChatMessage(m_serverAddress, m_serverPort, 6, m_name, getTimeStamp(), "", "");
		byte[] buf = new byte[256];
		DatagramPacket received = new DatagramPacket(buf, buf.length);

		while (true)
		{
			// Send request
			send(connectionRequest);
			System.out.println("Sending connection request...");

			// Receive response
			m_socket.receive(received);

			// Parse response
			connectionRequest = new ChatMessage(received);
			String response = connectionRequest.getText();

			// If a heartbeat is received before acknowledgment, simply respond to it
			if (connectionRequest.getType() == 6)
			{
				heartbeat();
			}

			if (response.equals("OK"))
			{
				// Connection approved
				System.out.println("Connection approved by server");
				return true;
			} else if (response.equals("NAME"))
			{
				// Name already taken
				System.err.println("Error: that name is already taken by another user");
				return false;
			} else
			{
				// Unknown response
				System.err.println("Error: unknown connection response: " + response);
				System.exit(1);
				return false;
			}
		}
	}

	// 2 DISCONNECT
	public void disconnect()
	{
		ChatMessage message = new ChatMessage(m_serverAddress, m_serverPort, 2, m_name, getTimeStamp(), "", "");
		System.out.println("Sending disconnect request");
		resendUntilAcknowledged(message);
	}

	// 3 LIST
	public void list()
	{
		ChatMessage message = new ChatMessage(m_serverAddress, m_serverPort, 3, m_name, getTimeStamp(), "", "");
		System.out.println("Sending user list request");
		resendUntilAcknowledged(message);
	}

	// 4 WHISPER
	public void whisper(String recepient, String msg)
	{
		ChatMessage message = new ChatMessage(m_serverAddress, m_serverPort, 4, m_name, getTimeStamp(), recepient, msg);
		System.out.println("Send whisper to " + recepient);
		resendUntilAcknowledged(message);
	}

	// 5 BROADCAST
	public void broadcast(String msg)
	{
		ChatMessage message = new ChatMessage(m_serverAddress, m_serverPort, 5, m_name, getTimeStamp(), "", msg);
		System.out.println("Send broadcast");
		resendUntilAcknowledged(message);
	}

	public void resendUntilAcknowledged(ChatMessage message)
	{
		for (int i = MAX_ATTEMPTS; i > 0; i--)
		{
			send(message);
			if (message.getTimeStamp() <= acknowledgedTimeStamp)
			{
				return;
			}
		}

		System.err.println("Error: failed to send message");
	}

	public void send(ChatMessage message)
	{

		Random generator = new Random();
		double failure = generator.nextDouble();

		if (failure > TRANSMISSION_FAILURE_RATE)
		{
			try
			{
				m_socket.setSoTimeout(1000);
				m_socket.send(message.getPacket());
			} catch (SocketTimeoutException e)
			{
				System.out.println("Socket timed out");
			} catch (IOException e)
			{
				System.err.println("Error: IO Exception sending message");
				System.exit(1);
			}
		} else
		{
			System.out.println("Transmission failure");
		}
	}

	public String receiveChatMessage()
	{
		ChatMessage chatMessage = null;
		DatagramPacket packet = null;

		try
		{
			System.out.println("Receiving message...");
			m_socket.receive(packet);
			chatMessage = new ChatMessage(packet);
		} catch (IOException e)
		{
			System.err.println("Error: failed to receive message");
			e.printStackTrace();
		}
		
		// TODO: handle acknowledgments and time stamps
		
//		// Message already received
//		if (acknowledgedTimeStamp == chatMessage.getTimeStamp());
//		{
//			
//		}
		
		// Update latest timestamp
		acknowledgedTimeStamp = chatMessage.getTimeStamp();

		switch(chatMessage.getType())
		{
		case 2:		// Disconnect
			System.exit(0);
			break;
		case 3:		// List
		case 4:		// Whisper
		case 5:		// Broadcast
			return chatMessage.getText();
		case 6:		// Heartbeat
			heartbeat();
			break;
		default:	// Unknown type
			System.err.println("Error: unknown message type: " + chatMessage.getType());
			break;
		}
		
		return "";
	}

	private void heartbeat()
	{
		// TODO
		ChatMessage message = new ChatMessage(m_serverAddress, m_serverPort, 6, m_name, getTimeStamp(), "", "");
		System.out.println("Sending heartbeat");
		send(message);
	}

	private long getTimeStamp()
	{
		return System.currentTimeMillis();
	}
}
