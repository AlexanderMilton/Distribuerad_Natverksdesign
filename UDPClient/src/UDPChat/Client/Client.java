package UDPChat.Client;

import java.awt.event.*;

//import java.io.*;

public class Client implements ActionListener
{

	private String m_name = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;

	public static void main(String[] args)
	{
		if (args.length < 3)
		{
			System.err
					.println("Usage: java -jar Client.jar <serverhostname> <serverportnumber> <username>");
			System.exit(-1);
		}

		try
		{
			Client instance = new Client(args[2]);
			instance.connectToServer(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e)
		{
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Client(String userName)
	{
		m_name = userName;

		// Start up GUI (runs in its own thread)
		m_GUI = new ChatGUI(this, m_name);
	}

	private void connectToServer(String hostName, int port)
	{
		// Create a new server connection
		m_connection = new ServerConnection(hostName, port, m_name);
		if (m_connection.handshake(m_name))
		{
			listenForServerMessages();
		} else
		{
			System.err.println("Unable to connect to server");
		}
	}

	private void listenForServerMessages()
	{
		// Use the code below once m_connection.receiveChatMessage() has been
		// implemented properly.
		do
		{	
			String message = m_connection.receiveChatMessage();
			
			if(!message.isEmpty())
				m_GUI.displayMessage(message);
			
			// Terminate client if socket has been closed
			if (m_connection.m_socket.isClosed())
			{
				try {
					Thread.sleep(1500);
				} catch(InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				System.exit(0);
			}
		} while (true);
	}

	// Sole ActionListener method; acts as a callback from GUI when user hits
	// enter in input field

	@Override
	public void actionPerformed(ActionEvent e)
	{
		// Get input from GUI window
		String input = m_GUI.getInput();

		System.out.println("1) Got input, incrementing clients MC");
		System.out.println("Input: " + input);

		// Increment message counter
//		m_connection.m_messageCounter++;

		// Forward slash expects a command with our without argument and message
		if (input.startsWith("/"))
		{
			String[] stringArray = input.split(" ", 3);
			String message = null;
			stringArray[0].toLowerCase();

			switch (stringArray[0])
			{

			// Connect to server - OBSOLETE
			/*
			 * case "/connect": case "/c": message = "01" + "|" + m_name; break;
			 */

			// Send private message
			case "/whisper":
			case "/w":
				
				message = "02" + "|" + 1 + "|"
						+ m_name + "|" + stringArray[1] + "|" + stringArray[2];
				break;

			// Request user list
			case "/list":
			case "/l":
				message = "03" + "|" + 1 + "|"
						+ m_name;
				break;

			// Request disconnect
			case "/leave":
			case "/quit":
			case "/exit":
			case "/dc":

				message = "04" + "|" + 1 + "|"	+ m_name;

				break;

			default:
				System.err.println("Error: invalid command");
//				m_connection.m_messageCounter--;
				m_GUI.clearInput();
				return;
			}

			m_connection.sendChatMessage(message);

		}

		// Messages without commands are treated as broadcasts
		else if (input.length() > 0)
		{
			String message = "00" + "|" + 1 + "|"
					+ m_name + "|" + input;
			m_connection.sendChatMessage(message);
		}

		else
		{
			// Decrement message counter
//			m_connection.m_messageCounter--;
		}

		m_GUI.clearInput();
	}
}
