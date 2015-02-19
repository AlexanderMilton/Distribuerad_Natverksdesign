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
import java.util.Iterator;

public class Server
{

	static double TRANSMISSION_FAILURE_RATE = 0.3;
	static int MAX_SEND_ATTEMPTS = 10;
//	static private String WELCOME = ("Welcome to the Chat Room");

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private ArrayList<String> m_messageList = new ArrayList<String>();
	private DatagramSocket m_socket;
//	private boolean receivedPoll = false;
	private int message = 0;

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
		// new Thread(new pollClientStatus()).start();

		byte[] data = new byte[256];
		DatagramPacket packet = new DatagramPacket(data, data.length);

		System.out.println("Waiting for client messages... ");

		do
		{

			try
			{
				m_socket.receive(packet);

				// Unpack message
				String message = unpack(packet);

				System.out.println("\nReceived message: " + message);

				// Split message into segments containing type, message and/or
				// arguments
				/*
				 * messageComponent[0] = type XX messageComponent[1] = messageID
				 * messageComponent[2] = name messageComponent[3] =
				 * message/argument messageComponent[4] = message (if argument
				 * present)
				 */

				String[] messageComponent = message.split("\\|");

				System.out.println("messageComponent[0]: " + messageComponent[0]);
				System.out.println("messageComponent[1]: " + messageComponent[1]);
				System.out.println("messageComponent[2]: " + messageComponent[2]);

				// Read packet sender address and port
//				InetAddress address = packet.getAddress();
//				int port = packet.getPort();

				// Read message type, message count and sender name
				String type = messageComponent[0];
				String messageID = messageComponent[1];
				String sender = messageComponent[2];

				// Check if message has already been interpreted
				if (messageAlreadyInterpreted(messageID))
				{
					System.out.println("Message already interpreted");
					acknowledgeMessage("OK", sender, packet);
					continue;
				}

				switch (type)
				{
				case "00":
					// Broadcast request
					broadcast(messageComponent[3], getClient(sender));
					break;

				case "01":
					// Connection request
					connect(sender, packet);
					break;

				case "02":
					// Whisper request
					String recepient = messageComponent[3];
					whisper(getClient(recepient), getClient(sender), messageComponent[4]);
					break;

				case "03":
					// List request
					list(packet, getClient(sender));
					break;

				case "04":
					// Leave request
					disconnect(getClient(sender), packet);
					break;
				}
			} catch (SocketTimeoutException e)
			{
				// No packet was received, reiterating
			} catch (IOException e1)
			{
				System.err.println("Error: failed to receive packet");
				e1.printStackTrace();
				System.exit(1);
			}
			// Remove clients who have disconnected
			removeDisconnectedClients();
		} while (true);
	}

	private String send(ClientConnection recepient, DatagramSocket socket, String msg, int messageID)
	{
		byte[] data = new byte[256];
		DatagramPacket packet = new DatagramPacket(data, data.length);

		try
		{
			socket.setSoTimeout(1000);
		} catch (SocketException e)
		{
			System.err.println("Error: failed to set socket timeout");
			e.printStackTrace();
			System.exit(1);
		}

		for (int i = 0; i <= MAX_SEND_ATTEMPTS; i++)
		{

			// Send message
			recepient.send(msg, messageID, socket, getMessageID());

			try
			{
				// Await response, return message on receive
				socket.receive(packet);
				return unpack(packet);
			} catch (SocketTimeoutException e)
			{
				System.out.println("Acknowledgment timed out, " + (MAX_SEND_ATTEMPTS - i) + (" attempts left"));
			} catch (IOException e)
			{
				System.err.println("Error: failed to receive response");
				e.printStackTrace();
				System.exit(1);
			}
		}
		// Failed to receive response, return error
		return new String("ERROR");
	}

	private void broadcast(String msg, ClientConnection sender)
	{
		String message = new String(sender.getName() + ": " + msg);
		System.out.println("Broadcasting message: " + message);
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			try
			{
				// Send on a fresh socket
				send(c, new DatagramSocket(), message, getMessageID());
			} catch (SocketException e)
			{
				System.err.println("Error: socket exception");
				e.printStackTrace();
			}
		}
	}

	// Validate name and connect a new client
	private void connect(String sender, DatagramPacket packet)
	{
		ClientConnection newClient = new ClientConnection(sender, packet.getAddress(), packet.getPort());
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(sender))
			{
				// The name is already taken by another client
				acknowledgeMessage("NAME", sender, packet);

				// Client response acquired
				return;
			}
		}
		m_connectedClients.add(newClient);

		// Send acknowledgment
		acknowledgeMessage("OK", sender, packet);
	}

	private void whisper(ClientConnection recepient, ClientConnection sender, String msg)
	{
		String message = new String(sender.getName() + " whispers: " + msg);

		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c == recepient)
			{
				try
				{
					send(c, new DatagramSocket(), message, getMessageID());
				} catch (SocketException e)
				{
					System.err.println("Error: socket exception");
					return;
				}
				// Client response acquired
				return;
			} else
			{
				// Recepient not found
				tell(sender, "User not found");
			}
		}
	}

	private String tell(ClientConnection recepient, String msg)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c == recepient)
			{
				try
				{
					return send(recepient, new DatagramSocket(), msg, getMessageID());
				} catch (SocketException e)
				{
					System.err.println("Error: socket exception");
					e.printStackTrace();
				}
			}
		}
		// Client not found
		System.err.println("Recepient not found");
		return "ERROR";
	}

	private void list(DatagramPacket pkt, ClientConnection sender)
	{
		// List all active clients
		ClientConnection c;
		String list = new String("[List of connected clients]");
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			list += ("\n> " + c.getName());
		}
		tell(sender, list);
	}

	private void disconnect(ClientConnection sender, DatagramPacket pkt)
	{
		if (sender.getName() == null)
		{
			return;
		}

		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(sender.getName()))
			{
				// Send disconnect instruction to client
				tell(sender, "%DC%");

				// Mark client as dead to remove
				c.markedForDeath = true;

				System.out.println("Client " + c.getName() + " " + c.markedForDeath);
			}
		}
	}

	private void removeDisconnectedClients()
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.markedForDeath)
			{
				System.out.println(c.getName() + " was marked for death and disconnected");
				announce(c.getName() + " disconnected");
				m_connectedClients.remove(c);
				c = null;
			}
		}
	}

	private void announce(String msg)
	{
		System.out.println("Announcing message: " + msg);
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			try
			{
				// Send on a fresh socket
				send(c, new DatagramSocket(), msg, getMessageID());
			} catch (SocketException e)
			{
				System.err.println("Error: socket exception");
				e.printStackTrace();
			}
		}
	}

	public boolean messageAlreadyInterpreted(String messageID)
	{
		String c;
		for (Iterator<String> itr = m_messageList.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.equals(messageID))
			{
				return true;
			}
		}
		// Message not already interpreted
		m_messageList.add(messageID);
		return false;
	}

	public void acknowledgeMessage(String msg, String name, DatagramPacket packet)
	{
		System.out.println("Acknowledging message from " + name + "...");
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(name))
			{
				c.sendReply(msg, getMessageID(), m_socket, packet);
			}
		}
	}

	private ClientConnection getClient(String username)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c = itr.next();
			if (c.hasName(username))
			{
				return c;
			}
		}
		return null;
	}

	public String unpack(DatagramPacket packet)
	{
		return new String(packet.getData(), 0, packet.getLength());
	}

	// case "00": // Broadcast global message
	// broadcast(name + ": " + messageComponent[3]);
	// break;
	//
	// case "01": // Handshake
	//
	// System.out.println("Adding client...");
	//
	// // Add client with name, address & port
	// if (addClient(name, address, port))
	// {
	// String response = ("OK");
	// buf = response.getBytes();
	// packet = new DatagramPacket(buf, buf.length, address, port);
	// } else
	// {
	// System.err.println("Error: name already taken");
	// String response = ("NAME");
	// buf = response.getBytes();
	// packet = new DatagramPacket(buf, buf.length, address, port);
	// }
	//
	// try
	// {
	// m_socket.send(packet);
	// } catch (IOException e)
	// {
	// System.err.println("Error: failed to send handshake response");
	// e.printStackTrace();
	// }
	//
	// broadcast(name + " joined the chat");
	//
	// break;
	//
	// case "02": // Private message
	//
	// String recepient = messageComponent[3];
	// String whisper = name + " whispers: " + messageComponent[4];
	// System.out.println("Sending private message: " + whisper + " to client "
	// + recepient);
	// sendPrivateMessage(recepient, whisper);
	// break;
	//
	// case "03": // List request
	// // Send a list of all clients
	// printClientList(name, address, port);
	// break;
	//
	// case "04": // Leave request
	// // Disconnect user
	// disconnectClient(name);
	// break;
	//
	// case "05": // Message delivery acknowledgement // Ack message
	// receivedAck(name);
	// System.out.println("Client reception acknowledged");
	// break;
	//
	// case "06": // Responding to poll
	// receivedPoll = true;
	// break;
	//
	// default:
	// System.err.println("Error: unknown message type: " +
	// messageComponent[0]);
	// break;

	// public void broadcast(String msg)
	// {
	// System.out.println("Broadcasting message: " + msg);
	//
	// ClientConnection c;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// c = itr.next();
	// DatagramPacket message = pack(getMessageID(), msg + "|" +
	// c.m_ackSocket.getLocalPort(), c.getAddress(), c.getPort());
	// if(!c.sendMessage(message, getMessageID(), m_socket))
	// c.markedForDeath = true;
	// }
	// }
	//
	// public boolean addClient(String name, InetAddress address, int port)
	// {
	// ClientConnection c = null;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// c = itr.next();
	// if (c.hasName(name))
	// {
	// return false; // Already exists a client with this name
	// }
	// }
	// m_connectedClients.add(new ClientConnection(name, address, port));
	// return true;
	// }
	//
	// public void sendPrivateMessage(String recepient, String whisper)
	// {
	// ClientConnection c;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// c = itr.next();
	// if (c.hasName(recepient))
	// {
	// DatagramPacket message = pack(getMessageID(), whisper, c.getAddress(),
	// c.getPort());
	// if(!c.sendMessage(message, m_socket))
	// c.markedForDeath = true;
	// }
	// }
	// }
	//
	// public boolean messageAlreadyInterpreted(String messageID)
	// {
	// String c;
	// for (Iterator<String> itr = m_messageList.iterator(); itr.hasNext();)
	// {
	// c = itr.next();
	// if (c.equals(messageID))
	// {
	// return true;
	// }
	// }
	// // Message not already interpreted
	// m_messageList.add(messageID);
	// return false;
	// }
	//
	// public void acknowledgeMessage(String name, InetAddress address, int
	// port)
	// {
	// System.out.println("Acknowledging message from " + name + "...");
	// ClientConnection c;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// c = itr.next();
	// if (c.hasName(name))
	// {
	// c.returnAck();
	// return;
	// }
	// }
	// }
	//
	// public void receivedAck(String name)
	// {
	// ClientConnection c;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// c = itr.next();
	// if (c.hasName(name))
	// {
	// System.out.println("Acking message reception to client " + name);
	// c.isAcked = true;
	// }
	// }
	// }
	//
	// public void printClientList(String name, InetAddress address, int port)
	// {
	// String clientList = new String("[List of all active clients]");
	// ClientConnection c;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// c = itr.next();
	// clientList += (System.getProperty("line.separator") + "> " +
	// c.getName());
	// }
	// sendPrivateMessage(name, clientList.toString());
	// }
	//
	// public void disconnectClient(String name)
	// {
	// if(name == null) {
	// return;
	// }
	// ClientConnection c;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// c = itr.next();
	// System.out.println("Client " + c.getName() + " " + c.markedForDeath);
	// if (c.hasName(name))
	// {
	// System.out.println("BEFORE: No of clients: " +
	// m_connectedClients.size());
	// sendPrivateMessage(name, "%DC%"); //TODO handle DC
	// m_connectedClients.remove(c);
	// c = null; // May be unnecessary
	// broadcast(name + " disconnected");
	// System.out.println("AFTER: No of clients: " + m_connectedClients.size());
	// return;
	// }
	// else if (c.markedForDeath)
	// {
	// System.out.println(c.getName() +
	// " was marked for death and disconnected");
	// broadcast(c.getName() + " disconnected");
	// m_connectedClients.remove(c);
	// c = null; // May be unnecessary
	// }
	// }
	// }
	//
	// public int getAckPort(String name)
	// {
	// ClientConnection c;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// c = itr.next();
	// if (c.hasName(name))
	// {
	// return c.getAckPort();
	// }
	// }
	// return 0;
	// }

	public int getMessageID()
	{
		return ++message;
	}

	// public DatagramPacket pack(int ackCounter, String msg, InetAddress iadd,
	// int port)
	// {
	// // Append message code and name to message, marshal packet and send it to
	// assigned address and port
	// String message = ackCounter + "|" + msg;
	// byte[] data = message.getBytes();
	// DatagramPacket packet = new DatagramPacket(data, message.length(), iadd,
	// port);
	//
	// return packet;
	// }
	//

	//
	// // Every few seconds, all clients are polled to see if they are still
	// connected
	// public class pollClientStatus implements Runnable
	// {
	// String currentClient = null;
	//
	// @Override
	// public void run()
	// {
	// System.out.println("Starting poll thread");
	// while(true)
	// {
	// try
	// {
	// Thread.sleep(5000);
	// } catch (InterruptedException e1)
	// {
	// System.err.println("Error: failed to pause poll thread");
	// e1.printStackTrace();
	// }
	//
	// ClientConnection c = null;
	// for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
	// itr.hasNext();)
	// {
	// try
	// {
	// c = itr.next();
	// currentClient = c.getName();
	// } catch (ConcurrentModificationException e)
	// {
	// if(!currentClient.equals(null))
	// disconnectClient(currentClient);
	// continue;
	// }
	//
	// sendPrivateMessage(c.getName(), "%POLL%");
	//
	// receivedPoll = false;
	//
	// // Sleep between checks
	// try
	// {
	// Thread.sleep(50);
	// } catch (InterruptedException e)
	// {
	// System.err.println("Error: failed to sleep");
	// e.printStackTrace();
	// }
	//
	// if(receivedPoll)
	// {
	// System.out.println("Received client poll");
	// continue;
	// }
	// else
	// {
	// System.err.println("Client acknowledgment timed out");
	// }
	// }
	// }
	// }
	// }
}
