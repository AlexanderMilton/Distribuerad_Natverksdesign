package TCPChat.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import TCPChat.Shared.ChatMessage;

public class Server
{
	private static String HELP_STRING = "[Help and commands]\n> Type a message to broadcast it to all users\n> Type /help or /h to show this message\n> Type /list or /l to view a list of all active users\n> Type /whisper or /w followed by a username and a message (delimited by spaces) to send a private message to that user\n> Type /cat and squint for some awesome ASCII art\n> Type /disconnect or /dc to leave the chat";
	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private ServerSocket m_serverSocket;
	private int m_port = -1;
	private PrintWriter m_writer;
	private BufferedReader m_reader;
	private Semaphore criticalSection = new Semaphore(1, true);
	private Semaphore secondBeat = new Semaphore(1, true);

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("Usage: java Server portnumber");
			System.exit(-1);
		}
		try
		{
			@SuppressWarnings("unused")
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
		
		System.out.println("Server successfully started at port " + m_port);

		secondBeat.drainPermits();

		Thread clientHandler = new Thread(new ClientConnectionThread());
		Thread messageHandler = new Thread(new ClientMessageThread());
		Thread heartbeatHandler = new Thread(new HeartbeatThread());
		Thread cartHandler = new Thread(new BringOutYerDeadThread());

		clientHandler.start();
		messageHandler.start();
		heartbeatHandler.start();
		cartHandler.start();
	}

	// Thread concurrently accepts incoming client connections
	private class ClientConnectionThread implements Runnable
	{
		@Override
		public void run()
		{
			while (true)
			{
				// Wait for a new client to connect to the server
				Socket clientSocket = null;
				ChatMessage clientMessage = null;

				try
				{
					// Accept client socket
					clientSocket = m_serverSocket.accept();
					System.out.println("Received client connection request");

					// Open streams to the socket
					m_writer = new PrintWriter(clientSocket.getOutputStream(), true);
					m_reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

					// Parse the request
					clientMessage = new ChatMessage(m_reader.readLine());

					addClient(clientSocket, clientMessage);

				} catch (IOException e)
				{
					System.err.println("Error: failed to open and accept client socket");
					e.printStackTrace();
				}
			}
		}

		private void addClient(Socket socket, ChatMessage chatMessage) throws IOException
		{
			while (true)
			{
				try
				{
					criticalSection.acquire();
				} catch (InterruptedException e)
				{
					continue;
				}

				// Check for name availability
				ClientConnection c;
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
				{
					c = itr.next();
					if (c.hasName(chatMessage.getSender()))
					{
						// There already exists a client with this name
						System.out.println("Name already exists, denying request and closing socket");
						m_writer.println(new ChatMessage("Server", "Response", "0", "NAME").getString());
						socket.close();
						criticalSection.release();
						return;
					}
				}

				// Add the client
				System.out.println("Responding with acknowledgment to client");
				m_connectedClients.add(new ClientConnection(chatMessage.getSender(), socket));
				m_writer.println(new ChatMessage("Server", "Response", "0", "OK").getString());
				sendPublicMessage(chatMessage.getSender() + " joined the chat");
				criticalSection.release();
				return;
			}
		}
	}

	// Thread handling incoming client messages
	private class ClientMessageThread implements Runnable
	{
		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					criticalSection.acquire();

					ClientConnection c;
					for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
					{
						c = itr.next();

						// Skip disconnected clients
						if (c.isDisconnected())
							continue;

						// String newString = null;
						try
						{
							
							while (c.getReader().ready())
							{
								handleMessage(new ChatMessage(c.getReader().readLine()));
							}
						} catch (SocketException e)
						{
							System.err.println("Client " + c.getName() + " disconnected");
							disconnect(c.getName());
						} catch (IOException e)
						{
							System.err.println("Client " + c.getName() + " disconnected");
							disconnect(c.getName());
						}
					}

					criticalSection.release();

				} catch (InterruptedException e)
				{
					System.err.println("Error: failed to acquire criticalSection");
					e.printStackTrace();
				}
			}
		}
	}
	
	// Check the status of all connections
	public class HeartbeatThread implements Runnable
	{
		@Override
		public void run()
		{			
			while (true)
			{
				// Drain all available permits
				secondBeat.drainPermits();
				
				// Heartbeat every few seconds
				try
				{
					Thread.sleep(5000);
				} catch (InterruptedException e)
				{
					System.err.println("Error: failed to sleep");
					e.printStackTrace();
				}
				
				System.out.println("Sending heartbeat to clients");
				
				ClientConnection c;
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
				{
					c = itr.next();

					// Skip already disconnected clients
					if (c.isDisconnected())
						continue;
					
					try
					{
						// Send heartbeat and wait
						c.sendHeartbeat();
						
						if(!secondBeat.tryAcquire(2000, TimeUnit.MILLISECONDS))
						{
							// No heartbeat received
							System.out.println("Failed to receive heartbeat from " + c.getName() + ", disconneting");
							disconnect(c.getName());
							continue;
						}
					} catch (InterruptedException e)
					{
						System.err.println("Error: heart skipped a beat");
						e.printStackTrace();
						continue;
					}

					// Second heartbeat acquired
					System.out.println("Received heartbeat from " + c.getName());
				}
			}
		}
	}

	// Remove disconnected clients
	public class BringOutYerDeadThread implements Runnable
	{
		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					// Sleep for a few seconds
					Thread.sleep(1000);

					// Skip if empty
					if (m_connectedClients.isEmpty())
						continue;

					ArrayList<ClientConnection> deadClients = new ArrayList<ClientConnection>();
					ClientConnection c;
					for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
					{
						c = itr.next();
						if (c.isDisconnected())
						{
							deadClients.add(c);
						}
					}

					// Skip if empty
					if (deadClients.isEmpty())
						continue;

					// Lock critical section
					criticalSection.acquire();

					// Remove clients
					for (Iterator<ClientConnection> itr = deadClients.iterator(); itr.hasNext();)
					{
						c = itr.next();
						String name = c.getName();
						m_connectedClients.remove(c);
						sendPublicMessage(name + " disconnected");
					}

					// Release critical section
					criticalSection.release();

				} catch (InterruptedException e)
				{
					System.err.println("Error: disconnection handler thread interrupted");
					e.printStackTrace();
				}
			}
		}
	}

	private void handleMessage(ChatMessage message)
	{
		switch (message.getCommand())
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

		case "heartbeat":
			secondBeat.release();
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
			if (c.hasName(sender))
			{
				sendPrivateMessage(sender, "You have been disconnected");
				c.markAsDisconnected();
			}
		}
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
		sendPrivateMessage(
				sender,
				"\n=~======================~~~=========,,,,,,:~~====++++????????????????????++=::~= \n=~==================================,,,,,::~~~===++++???????????????????+++=~~~~ \n=~===========~======================,,.,,::~~~~===+++???????????????????+++=~~~~ \n=~==============+==================~,.,,,::~~~~====+++?????????????????++++=~~~= \n=~========+=====+?=================~..,,::~~~=======+++++????+???????+?++++==:~~ \n=~========+=====+?=================:..,,:~~~~========+++++++??++++++++++++++=:~~ \n=========~+=====+++============:,,:=..,,:~~~~=~~~~~~========+++++++++========:== \n=========++=====++?==========:.,:::~..,::~::~~~::,,,,:::~~~======~~:::::~~~==~== \n========+++==++=++?++======~..,::~::..,:~::::::::::,,,,:::~===~~::::::::~~~~=~== \n========++++=+???????+?+==,.,:~=~=~:,,,~~::::,:::,,,,,,,,:=+??=::,,,,,,,::~~=:~= \n~=======??+I?????+=~~..:II++~~~~~=~,,,,~~~~:,,.:,...,..,,~+????:,,,..,,,:::~=~== \n~=======+??II??+~:,.,..=77?+++==~~,:,,:~~===~~~~~==~:::,~=+??I?~,,:,,,::~=~===== \n~=====+?IIIII?+:,,,,.:~I77I??++==~::,.:~~=====~:~~~:~~====+??I?++:::~=====+++=== \n======??IIII?+=,,..~=?I777IIII?+==::,~~~~==++????+++++====+?II?+++??++++????+=== \n======+????I??7~:.:.??I77I??II??+,:::::~~==++??????+++===++?II?+++??????????+=== \n======+++?????I7?=~,III77?+::=I?,.,,~,:~~==+++++?+++~~~=++??II??+=++????????+=== \n=======+????+?II77IIII77II~.:=I?:..~~::~~~====++++=~~=+==+???II??+~=++?+?++++=== \n=====+==++++==+?II77??I77I?I?7I?=..,~:~~~~~~~~===~~=~==~~=++????+=+~===++++===== \n======+=====++=+IIII7++????77I?+:.....:~~::::~:::::::,::,,::~~~::~~~::~~~~~~~=== \n=======+++====+?????II~~:II??++=......:~~::::::::::~:,,,,,,,,,,,,~~~:,,:::::~=== \n========++~?+=~==+++=~~=+??I=++,......,~~~:::::::::::,,,,,,,,,,,:~~~::,,:::~~=== \n================~~~:::::==+++==.,......:~~~~~~~::,.,,,,,,,,,,,::~~:::::::~~~==== \n==========+==~=~=~~~~~=~~=~====..,.....::~~~:~===~~~~::,,,,,,,:~~~~:~~~~~~~~==== \n==============~~~~~~~~~~===++=.,:.,.....:::~~~~=~==~~~~~~::::~=======~~~~~~===== \n===++========~~~~~~~~~~==++++=........,.,:::~~~~~~~~~~~~~~::~===~===~~:~::====== \n==+++++===+======~=~===+++++++,..,.....,,,::::~~~~~~~~~~~~~~~~~~===~=~:::======= \n=++++?+++++==========++?+????+~.....,...,,,:::~~~~~~~~~~~~~~=~======~::~======== \n==+++?++++++++==+++++????????+==,.......,,,,:::~~~~~======++++=====~~:========== \n+++++?+???+++++?++???????I???++~,....,,,,,,,,::~~~~~~~~===+++++++==~~=========== \n+++++????????+++???????IIII???++,....,,,,,,,,,,:~~~~~~~====++++++=~~============ \n++++????????????????????II?????+=:....,,,,,,,,,,::~~~~~~~~~===++==~============= \n+==++??????++??????????????II?+=:.....,,,,,,,,,,,,:::::::::~~===~=============== \n==++++????????????????????????+~,,....,,,,,,,,,,,,,,,:::::::::::=======~======== \n===++?+????+++++????++++++++++~,.....,,,,,,,,,,,,,,,,,,,::::::::~==========++=== \n===++++++++++++++++++++++++===~,.....,::,,,,,,,,,,,,,,,,:::::::::=============== \n==~~==+++++++=~======+++++=,,,,.....,:::,,,,,,,,,,,,,,,::::::::~:==~============ \n+++??+=~~=====????????????+:,.......:::::,,,,,,,,,,,,,,,:::::::~~~~~...,======== \n+?IIIII?~:,~IIIIIIIII???++=:,....:.~.,:::::,,,,,,,,:,,:::::::::~~~~=~=.......=== \nIIIIIII?=,?IIIIIIIIII??+=~.......+.....~:::::,,,,,,,,,,,:::::::~~~~====~,......= \nIIIIIIIIIIIIIIIIIII??+=:,...............,~:::,,,,,,,,,,::::::::~~~==~====~...... \nIIIIIIIIIIIIIIIIII??=......................=~:,,,,,,,,,:::::::::~====~===~:..... \nIIIII?IIIIIIIIIII?=:,.......................~~~:,:::,,,:::::::~~~=~======~~~.... \nIIIII????I?IIIII?:,........................~.~~~~:::::,::::~~~=~=======+===~,... \nIII?+??I+II????+:............................~~=~::::::::~~~~=~==============... \nI?+=,,+?????+I+,...........................:,~~==~:~::::~~~==========+=======...");
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
