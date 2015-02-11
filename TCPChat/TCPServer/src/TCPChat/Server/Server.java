package TCPChat.Server;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import TCPChat.Shared.ChatMessage;

public class Server
{
	private static String HELP_STRING = "[Help and commands]\n> Type a message to broadcast it to all users\n> Type /help or /h to show this message\n> Type /list or /l to view a list of all active users\n> Type /whisper or /w followed by a username and a message to send a private message to that user\n> Type /cat and squint for some awesome ASCII art\n> Type /disconnect or /dc to leave the chat";
	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private ServerSocket m_serverSocket;
	private int m_port = -1;
	private PrintWriter m_writer;
	private BufferedReader m_reader;

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("Usage: java Server portnumber");
			System.exit(-1);
		}
		try
		{
			Server instance = new Server(Integer.parseInt(args[0]));
		} catch (NumberFormatException e)
		{
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		} catch (IOException e)
		{
			System.err.println("Error: failed to create server socket");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private Server(int portNumber) throws IOException
	{
		m_port = portNumber;
		m_serverSocket = new ServerSocket(m_port);
		
		Thread clientHandler = new Thread(new ClientConnectionThread());
		Thread messageHandler = new Thread(new ClientMessageThread());

		clientHandler.start();
		messageHandler.start();
	}

	public class ClientConnectionThread implements Runnable
	{
		@Override
		public void run()
		{
			boolean doubleBreak = false;
			
			while (true)
			{
				// Wait for a new client to connect to the server
				Socket clientSocket = new Socket();
				ChatMessage clientMessage = null;
				doubleBreak = false;
				
				try
				{
					// Accept client socket
					clientSocket = m_serverSocket.accept();
					System.out.println("Accepted client connection");

					// Open streams to the socket
					m_writer = new PrintWriter(clientSocket.getOutputStream(), true);
					m_reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

					// Parse the request
					clientMessage = new ChatMessage(m_reader.readLine());
					
					// Check for name availability
					ClientConnection c;
					for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
					{
						c = itr.next();
						if (c.hasName(clientMessage.getSender()))
						{
							// There already exists a client with this name
							System.out.println("Name already exists, denying request and closing socket");
							m_writer.println(new ChatMessage("Server", "Response", "0", "NAME").getString());
							clientSocket.close();
							doubleBreak = true;
							break;
						}
					}
				} catch (IOException e)
				{
					System.err.println("Error: failed to open and accept client socket");
					e.printStackTrace();
				}
				
				if (doubleBreak)
					break;
				
				// Add the client
				System.out.println("Responding with acknowledgment to client");
				m_connectedClients.add(new ClientConnection(clientMessage.getSender(), clientSocket));
				m_writer.println(new ChatMessage("Server", "Response", "0", "OK").getString());
				sendPublicMessage(clientMessage.getSender() + " joined the chat");
			}
		}
	}

	private class ClientMessageThread implements Runnable
	{
		@Override
		public void run()
		{	
			while (true)
			{
				ClientConnection c;
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
				{
					c = itr.next();
					// Skip crashed clients
					if (c.isCrashed())
						continue;
					
					try
					{
						// Read messages from clients, parse them and forward to the message handler
						handleMessage(new ChatMessage(new BufferedReader(new InputStreamReader(c.getSocket().getInputStream())).readLine()));
					} catch (IOException e)
					{
						System.err.println("Client " + c.getName() + " crashed");
						c.markAsCrashed();
					}
				}
			}
		}
	}
	
	private void handleMessage(ChatMessage message)
	{
		switch(message.getCommand())
		{
		case "broadcast":
			broadcast(message.getSender(), message.getMessage());
			break;
			
		case "disconnect":
			disconnect(message.getSender());
			break;

		case "whisper":
			whisper(message.getParameter(), message.getSender(), message.getMessage());
			break;

		case "list":
			list(message.getSender());
			break;

		case "help":
			help(message.getSender());
			break;

		case "cat":
			cat(message.getSender());
			break;

		default:
			System.out.println("Unknown command received: " + message.getCommand());
			break;
		}
	}

	private void broadcast(String sender, String message)
	{
		sendPublicMessage(sender + ": " + message);
	}

	private void disconnect(String sender)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if(c.hasName(sender))
			{
				sendPrivateMessage(sender, "You have been disconnected");
				c.markAsDisconnected();
			}
		}
		sendPublicMessage(sender + " disconnected");
	}

	private void whisper(String recepient, String sender, String message)
	{
		sendPrivateMessage(recepient, sender + " whispers: " + message);
	}

	private void list(String sender)
	{
		String list = "[List of all active clients]";
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			list += ("\n> " + itr.next().getName());
		}
		sendPrivateMessage(sender, list);
	}

	private void help(String sender)
	{
		sendPrivateMessage(sender, HELP_STRING);
	}

	private void cat(String sender)
	{
		sendPrivateMessage(sender, "\n=~======================~~~=========,,,,,,:~~====++++????????????????????++=::~= \n=~==================================,,,,,::~~~===++++???????????????????+++=~~~~ \n=~===========~======================,,.,,::~~~~===+++???????????????????+++=~~~~ \n=~==============+==================~,.,,,::~~~~====+++?????????????????++++=~~~= \n=~========+=====+?=================~..,,::~~~=======+++++????+???????+?++++==:~~ \n=~========+=====+?=================:..,,:~~~~========+++++++??++++++++++++++=:~~ \n=========~+=====+++============:,,:=..,,:~~~~=~~~~~~========+++++++++========:== \n=========++=====++?==========:.,:::~..,::~::~~~::,,,,:::~~~======~~:::::~~~==~== \n========+++==++=++?++======~..,::~::..,:~::::::::::,,,,:::~===~~::::::::~~~~=~== \n========++++=+???????+?+==,.,:~=~=~:,,,~~::::,:::,,,,,,,,:=+??=::,,,,,,,::~~=:~= \n~=======??+I?????+=~~..:II++~~~~~=~,,,,~~~~:,,.:,...,..,,~+????:,,,..,,,:::~=~== \n~=======+??II??+~:,.,..=77?+++==~~,:,,:~~===~~~~~==~:::,~=+??I?~,,:,,,::~=~===== \n~=====+?IIIII?+:,,,,.:~I77I??++==~::,.:~~=====~:~~~:~~====+??I?++:::~=====+++=== \n======??IIII?+=,,..~=?I777IIII?+==::,~~~~==++????+++++====+?II?+++??++++????+=== \n======+????I??7~:.:.??I77I??II??+,:::::~~==++??????+++===++?II?+++??????????+=== \n======+++?????I7?=~,III77?+::=I?,.,,~,:~~==+++++?+++~~~=++??II??+=++????????+=== \n=======+????+?II77IIII77II~.:=I?:..~~::~~~====++++=~~=+==+???II??+~=++?+?++++=== \n=====+==++++==+?II77??I77I?I?7I?=..,~:~~~~~~~~===~~=~==~~=++????+=+~===++++===== \n======+=====++=+IIII7++????77I?+:.....:~~::::~:::::::,::,,::~~~::~~~::~~~~~~~=== \n=======+++====+?????II~~:II??++=......:~~::::::::::~:,,,,,,,,,,,,~~~:,,:::::~=== \n========++~?+=~==+++=~~=+??I=++,......,~~~:::::::::::,,,,,,,,,,,:~~~::,,:::~~=== \n================~~~:::::==+++==.,......:~~~~~~~::,.,,,,,,,,,,,::~~:::::::~~~==== \n==========+==~=~=~~~~~=~~=~====..,.....::~~~:~===~~~~::,,,,,,,:~~~~:~~~~~~~~==== \n==============~~~~~~~~~~===++=.,:.,.....:::~~~~=~==~~~~~~::::~=======~~~~~~===== \n===++========~~~~~~~~~~==++++=........,.,:::~~~~~~~~~~~~~~::~===~===~~:~::====== \n==+++++===+======~=~===+++++++,..,.....,,,::::~~~~~~~~~~~~~~~~~~===~=~:::======= \n=++++?+++++==========++?+????+~.....,...,,,:::~~~~~~~~~~~~~~=~======~::~======== \n==+++?++++++++==+++++????????+==,.......,,,,:::~~~~~======++++=====~~:========== \n+++++?+???+++++?++???????I???++~,....,,,,,,,,::~~~~~~~~===+++++++==~~=========== \n+++++????????+++???????IIII???++,....,,,,,,,,,,:~~~~~~~====++++++=~~============ \n++++????????????????????II?????+=:....,,,,,,,,,,::~~~~~~~~~===++==~============= \n+==++??????++??????????????II?+=:.....,,,,,,,,,,,,:::::::::~~===~=============== \n==++++????????????????????????+~,,....,,,,,,,,,,,,,,,:::::::::::=======~======== \n===++?+????+++++????++++++++++~,.....,,,,,,,,,,,,,,,,,,,::::::::~==========++=== \n===++++++++++++++++++++++++===~,.....,::,,,,,,,,,,,,,,,,:::::::::=============== \n==~~==+++++++=~======+++++=,,,,.....,:::,,,,,,,,,,,,,,,::::::::~:==~============ \n+++??+=~~=====????????????+:,.......:::::,,,,,,,,,,,,,,,:::::::~~~~~...,======== \n+?IIIII?~:,~IIIIIIIII???++=:,....:.~.,:::::,,,,,,,,:,,:::::::::~~~~=~=.......=== \nIIIIIII?=,?IIIIIIIIII??+=~.......+.....~:::::,,,,,,,,,,,:::::::~~~~====~,......= \nIIIIIIIIIIIIIIIIIII??+=:,...............,~:::,,,,,,,,,,::::::::~~~==~====~...... \nIIIIIIIIIIIIIIIIII??=......................=~:,,,,,,,,,:::::::::~====~===~:..... \nIIIII?IIIIIIIIIII?=:,.......................~~~:,:::,,,:::::::~~~=~======~~~.... \nIIIII????I?IIIII?:,........................~.~~~~:::::,::::~~~=~=======+===~,... \nIII?+??I+II????+:............................~~=~::::::::~~~~=~==============... \nI?+=,,+?????+I+,...........................:,~~==~:~::::~~~==========+=======...");
	}

	private void sendPublicMessage(String message)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			c.sendMessage(message);
		}
	}

	private void sendPrivateMessage(String recepient, String message)
	{	
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(recepient))
			{
				c.sendMessage(message);
			}
		}
	}
}
