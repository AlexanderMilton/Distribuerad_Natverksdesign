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

public class Server {

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private ServerSocket m_serverSocket;
	private int m_port = -1;
	private PrintWriter m_writer;
	private BufferedReader m_reader;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: java Server portnumber");
			System.exit(-1);
		}
		try {
			Server instance = new Server(Integer.parseInt(args[0]));
			instance.listenForClientMessages();
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Server(int portNumber) {
		try
		{
			m_port = portNumber;
			m_serverSocket = new ServerSocket(m_port);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void listenForClientMessages() {
		do {
			Socket clientSocket = new Socket();
			
			try
			{
				System.out.println("Waiting to accept new client connection...");
				clientSocket = m_serverSocket.accept();
				System.out.println("Accepted client connection");
				
				m_writer = new PrintWriter(clientSocket.getOutputStream(), true);
				m_reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				System.out.println("Reading from client socket buffer");
				ChatMessage clientMessage = new ChatMessage(m_reader.readLine());
			} catch (IOException e)
			{
				System.err.println("Error: failed to accept client socket");
				e.printStackTrace();
			}
			
			m_writer.println(new ChatMessage("Server", "Ack", "Something", "Hey Joe").getString());
			
			// TODO: Listen for client messages.
			// On reception of message, do the following:
			// * Unmarshal message
			// * Depending on message type, either
			// - Try to create a new ClientConnection using addClient(), send
			// response message to client detailing whether it was successful
			// - Broadcast the message to all connected users using broadcast()
			// - Send a private message to a user using sendPrivateMessage()
		} while (true);
	}

	public boolean addClient(String name, InetAddress address, int port) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr
				.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				return false; // Already exists a client with this name
			}
		}
		m_connectedClients.add(new ClientConnection(name, address, port));
		return true;
	}

	public void sendPrivateMessage(String message, String name) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr
				.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
//				c.sendMessage(message, m_socket);
			}
		}
	}

	public void broadcast(String message) {
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr
				.hasNext();) {
//			itr.next().sendMessage(message, m_socket);
		}
	}
}
