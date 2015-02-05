package UDPChat.Client;

import java.awt.event.*;
import java.net.SocketException;
import java.net.UnknownHostException;


public class Client implements ActionListener 
{

	private String m_username = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;

	public static void main(String[] args) 
	{
		if (args.length < 3) 
		{
			System.err.println("Usage: java Client hostname port username");
			System.exit(-1);
		}

		try 
		{
			Client instance = new Client(args[2]);
			instance.connect(args[0], Integer.parseInt(args[1]));
			instance.listen();
		} 
		catch (NumberFormatException e) 
		{
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Client(String userName) 
	{
		m_username = userName;

		// Start up GUI (runs in its own thread)
		m_GUI = new ChatGUI(this, m_username);
	}

	private void connect(String hostName, int port) 
	{
		// Create a new server connection
		try 
		{
			m_connection = new ServerConnection(hostName, port);
		} 
		catch (SocketException e1) 
		{
			System.err.println("Error: Unable to connect to server");
			System.err.println("Socket exception");
			System.exit(-2);
		} 
		catch (UnknownHostException e1) 
		{
			System.err.println("Error: Unable to connect to server");
			System.err.println("Unable to resolve hostname");
			System.exit(-3);
		}
		String connect_msg = null;
		try
		{
			connect_msg = m_connection.connect(m_username);
		}
		catch (ConnectFailedException e)
		{
			System.err.println("Error: Unable to connect to server");
			if (e.getMessage() != null)
			{
				System.err.println(e.getMessage());
			}
			else 
			{
				System.err.println("Could not establish connection.");
			}
			System.exit(-3);
		}
		m_GUI.displayMessage(connect_msg);
	}

	
 	private void listen() 
	{		
		String[] msg = null;
		
		while(true)
		{
			String data = m_connection.waitForServerMessage();
			if(data != null)
			{
				msg = data.split("/", 3);
				if(msg.length != 3)
				{
					System.err.println("Incorrect number of fields in server message.");
				}
				else
				{
					if(msg[1].equals("0"))
					{ // Public message
					  // Display as "username   messagetext"
						m_GUI.displayMessage(msg[0]+"   "+msg[2]);
					}
					else if(msg[1].equals("1"))
					{ // Private message
					  // Display as "username ->messagetext"
						m_GUI.displayMessage(msg[0]+"-->"+msg[2]);
					}
					else // assumed to be 2
					{ // Status message
					  // Display as "--- username:  messagetext"
						m_GUI.displayMessage("--- "+msg[0]+":  "+msg[2]);
					}
					
				}
			}
		}
	}

	// Sole ActionListener method; acts as a callback from GUI when user hits
	// enter in input field
	@Override
	public void actionPerformed(ActionEvent e) 
	{
		String text = m_GUI.getInput();
		if(text.startsWith("/msg"))
		{
			String[] args = text.split(" ", 3);
			if(args.length < 3)
			{
				m_GUI.displayMessage("Usage: /msg username message");
				return;
			}
			String retval = m_connection.sendPrivate(args[1], args[2]);
			if(retval == null)
			{
				m_GUI.displayMessage(args[1]+"<--"+args[2]);
			}
			else
			{
				m_GUI.displayMessage("--- Error sending message: "+retval);
			}
		}
		else if(text.startsWith("/list") || text.startsWith("/who"))
		{
			String retval = m_connection.requestList();
			if(retval == null)
			{
				m_GUI.displayMessage("--- Error requesting user list");
			}
			else
			{
				m_GUI.displayMessage("--- User list\n"+retval+"--- End of user list");
			}
		}
		else if(text.startsWith("/disconnect") || text.startsWith("/quit") || text.startsWith("/part"))
		{
			String[] args = text.split(" ", 2);
			String message = "Disconnecting";
			if(args.length > 1)
			{
				message = args[1];
			}
			m_connection.disconnect(message);
			m_GUI.displayMessage("--- Disconnected");
			System.exit(0);
		}
		else if(text.startsWith("/"))
		{
			m_GUI.displayMessage("--- Unknown command");
			return;
		}
		else if(text.equals(""))
		{ // Don't send empty strings
		}
		else
		{ // Send public message
			if(!m_connection.sendPublic(text))
			{
				m_GUI.displayMessage("--- Error sending message");
			}
		}
		m_GUI.clearInput();
	}
}
