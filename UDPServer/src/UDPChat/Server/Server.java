package UDPChat.Server;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Server
{

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private DatagramSocket m_socket;

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("Usage: java -jar Server.jar <portnumber>");
			System.exit(-1);
		}
		try
		{
			Server instance = new Server(Integer.parseInt(args[0]));
			instance.listenForClientMessages();
		} catch (NumberFormatException e)
		{
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Server(int portNumber)
	{
		try
		{
			m_socket = new DatagramSocket(portNumber);
		} catch (SocketException e)
		{
			System.err.println("Error: failed to create socket.");
			e.printStackTrace();
		}

		System.out.println("Created socket at port " + m_socket.getLocalPort());

	}

	private void listenForClientMessages()
	{
		// Concurrently check if all clients are still connected
		new Thread(new pollClientStatus()).start();
		
		System.out.println("Waiting for client messages... ");

		do
		{
			byte[] buf = new byte[256];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			try
			{
				m_socket.receive(packet);
			} catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			System.out.println("4) Server received message");

			// Unpack message
			String message = unpack(packet);

			System.out.println("Unpacked message: " + message);

			// Split message into segments containing type, message and/or arguments
			/*
			 * messageComponent[0] = type XX 
			 * messageComponent[1] = messageCounter 
			 * messageComponent[2] = name 
			 * messageComponent[3] = message/argument 
			 * messageComponent[4] = message (if argument present)
			 */
			String[] messageComponent = message.split("\\|");

			// Read packet sender address and port
			InetAddress address = packet.getAddress();
			int port = packet.getPort();

			// Read message type, message count and sender name
			String type = messageComponent[0];
			//int messageCounter = Integer.parseInt(messageComponent[1]);
			String name = messageComponent[2];

//			System.out.println("address: " + address);
//			System.out.println("port: " + port);
//			System.out.println("type: " + type);
//			System.out.println("messageCounter: " + messageCounter);
//			System.out.println("name: " + name);

			/*// Check if message has already been interpreted
			if (!type.equals("01") && messageAlreadyInterpreted(name, messageCounter))
			{
				System.out.println("Message already interpreted");
				//acknowledgeMessage(name, "ACK", address, port);
				continue;
			}

			// Uninterpreted, non-connection request messages are acknowledged
			else*/ if (!type.equals("01"))
			{
				acknowledgeMessage(name, "%ACK%", address, port);
			}
			
			switch (type)
			{

			case "00": // Broadcast global message
				broadcast(name + ": " + messageComponent[3]);
				break;

			case "01": // Handshake

				System.out.println("Adding client...");

				// Add client with name, address & port
				if (addClient(name, address, port))
				{
					String response = ("OK");
					buf = response.getBytes();
					packet = new DatagramPacket(buf, buf.length, address, port);
				} else
				{
					System.err.println("Error: name already taken");
					String response = ("NAME");
					buf = response.getBytes();
					packet = new DatagramPacket(buf, buf.length, address, port);
				}

				try
				{
					m_socket.send(packet);
				} catch (IOException e)
				{
					System.err.println("Error: failed to send handshake response");
					e.printStackTrace();
				}

				break;

			case "02": // Private message
				
				String recepient = messageComponent[3];
				String whisper = name + " whispers: " + messageComponent[4];
				System.out.println("Sending private message: " + whisper + " to client " + recepient);
				sendPrivateMessage(recepient, whisper);
				break;

			case "03": // List request
				// Send a list of all clients
				printClientList(name, address, port);
				break;

			case "04": // Leave request
				// Disconnect user
				disconnectClient(name);
				break;
				
			case "05": // Message delivery acknowledgement // Ack message
				receivedAck(name);
				System.out.println("Client reception acknowledged"); 
				break;
				
			case "06": // Responding to poll
				Latch.poll.countDown();
				break;

			default:
				System.err.println("Error: unknown message type: " + messageComponent[0]);

			}

		} while (true);
	}

	public void broadcast(String msg)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			DatagramPacket message = pack(c.getAckCounter(), msg, c.getAddress(), c.getPort());
			c.sendMessage(message, m_socket);
		}
	}

	public boolean addClient(String name, InetAddress address, int port)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(name))
			{
				return false; // Already exists a client with this name
			}
		}
		broadcast(name + " joined the chat");
		m_connectedClients.add(new ClientConnection(name, address, port));
		System.out.println("Added client " + name + " sucessfully");
		return true;
	}

	public void sendPrivateMessage(String recepient, String whisper)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(recepient))
			{
				DatagramPacket message = pack(c.getAckCounter(), whisper, c.getAddress(), c.getPort());
				c.sendMessage(message, m_socket);
			}
		}
	}

	public boolean messageAlreadyInterpreted(String name, int clientMessageCounter)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(name))
			{
				System.out.println("clientConnection MC: " + c.getMessageCounter());
				System.out.println("serverConnection MC: " + clientMessageCounter);
				if (c.getMessageCounter() <= clientMessageCounter)
				{
					// Message has not been interpreted
					c.m_messageCounter++; // = clientMessageCounter;
					return false;
				} else
				{
					// Message has already been interpreted
					return true;
				}
			}
		}
		// Client not yet stored
		return false;
	}

	public void acknowledgeMessage(String name, String msg, InetAddress address, int port)
	{
		System.out.println("5) Message is being acknowledged by server");
		
		System.out.println("Acknowledging message from " + name + "...");
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(name))
			{
				DatagramPacket message = pack(c.getAckCounter(), msg, address, port);
				c.returnAck(message, m_socket);
			}
		}
	}

	public void receivedAck(String name)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(name))
			{
				c.acknowledgment.countDown();
			}
		}
	}

	public void printClientList(String name, InetAddress address, int port)
	{
		String clientList = new String("[List of all active clients]");
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			clientList += (System.getProperty("line.separator") + "> " + c.getName());
		}
		sendPrivateMessage(name, clientList.toString());
	}

	public void disconnectClient(String name)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(name))
			{
				sendPrivateMessage(name, "%DC%");
				broadcast(name + " disconnected");
				m_connectedClients.remove(c);
				return;
			}
		}
	}

	public DatagramPacket pack(int ackCounter, String msg, InetAddress iadd, int port)
	{
		// Append message code and name to message, marshal packet and send it
		// to assigned address and port
		String message = ackCounter + "|" + msg;
		System.out.println("Packed message: " + message);
		byte[] data = message.getBytes();
		DatagramPacket packet = new DatagramPacket(data, message.length(), iadd, port);

		return packet;
	}

	public String unpack(DatagramPacket packet)
	{
		return new String(packet.getData(), 0, packet.getLength());
	}

	// Every few seconds, all clients are polled to see if they are still connected
	public class pollClientStatus implements Runnable 
	{
		String currentClient = null;
		
		@Override
		public void run()
		{
			System.out.println("Starting poll thread");
			while(true)
			{
				try
				{
					Thread.sleep(5000);
				} catch (InterruptedException e1)
				{
					System.err.println("Error: failed to pause poll thread");
					e1.printStackTrace();
				}
				
				ClientConnection c = null;
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
				{
					try
					{
						c = itr.next();
						currentClient = c.getName();
					} catch (ConcurrentModificationException e)
					{
						if(!currentClient.equals(null))
							disconnectClient(currentClient);
						continue;
					}
					
					sendPrivateMessage(c.getName(), "%POLL%");

					Latch.poll = new CountDownLatch(1);
					
					try
					{
						System.out.println("Polling client " + currentClient);
						if(Latch.poll.await(500, TimeUnit.MILLISECONDS))
							continue;
						else
						{
							disconnectClient(currentClient);
						}
					} catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
}
