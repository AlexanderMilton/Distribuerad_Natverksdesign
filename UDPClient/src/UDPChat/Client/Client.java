package UDPChat.Client;

import java.awt.event.*;

public class Client implements ActionListener
{

	private String m_name = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;

	public static void main(String[] args)
	{
		if (args.length < 3)
		{
			System.err.println("Usage: java -jar Client.jar <serverhostname> <serverportnumber> <username>");
			System.exit(-1);
		}

		// try
		// {
		Client instance = new Client(args[2]);
		instance.connectToServer(args[0], Integer.parseInt(args[1]));
		// } //catch (NumberFormatException e)
		// {
		// System.err.println("Error: port number must be an integer: " +
		// Integer.parseInt(args[1]));
		// System.exit(-1);
		// }
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
		if (m_connection.connect(m_name))
		{
			listenForServerMessages();
		} else
		{
			System.err.println("Error: unable to connect to server");
			System.exit(1);
		}
	}

	private void listenForServerMessages()
	{
		do
		{
			String message = m_connection.receiveMessage();

			if (!message.isEmpty())
				m_GUI.displayMessage(message);

			// Terminate client if socket has been closed
			if (m_connection.m_socket.isClosed())
			{
				try
				{
					Thread.sleep(1500);
				} catch (InterruptedException e)
				{
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

		// Forward slash expects a command with our without argument and message
		if (input.startsWith("/"))
		{
			String[] stringArray = input.split(" ", 3);
			String message = null;
			stringArray[0].toLowerCase();

			switch (stringArray[0])
			{

			// // Connect to server - OBSOLETE
			// case "/connect": case "/c": message = "01" + "|" + m_name;
			// message = "01" + "|" + m_connection.getMessageID() + "|" +
			// m_name;
			// break;

			// Send private message
			case "/whisper":
			case "/w":
				message = "02" + "|" + m_connection.getMessageID() + "|" + m_name + "|" + stringArray[1] + "|" + stringArray[2];
				break;

			// Request user list
			case "/list":
			case "/l":
				message = "03" + "|" + m_connection.getMessageID() + "|" + m_name;
				break;

			// Request disconnect
			case "/leave":
			case "/quit":
			case "/exit":
			case "/dc":
				message = "04" + "|" + m_connection.getMessageID() + "|" + m_name;
				break;

			default:
				System.err.println("Error: invalid command");
				m_GUI.clearInput();
				return;
			}

			m_connection.sendMessage(message, m_connection.m_socket, m_connection.m_serverPort);

		}

		// Messages without commands are treated as broadcasts
		else if (input.length() > 0)
		{
			String message = "00" + "|" + m_connection.getMessageID() + "|" + m_name + "|" + input;
			m_connection.sendMessage(message, m_connection.m_socket, m_connection.m_serverPort);
		}

		m_GUI.clearInput();
	}
}
