/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPChat.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author brom
 */
public class ClientConnection {

	static double TRANSMISSION_FAILURE_RATE = 0.3;
	static int MAX_SEND_ATTEMPTS = 10;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private final int m_ackPort;

	public CountDownLatch acknowledgement;
	public int m_messageCounter = 0;
	public int m_ackCounter = 0;
	
	public ClientConnection(String name, InetAddress address, int port, int ackPort) {
		m_name = name;
		m_address = address;
		m_port = port;
		m_ackPort = ackPort;
		acknowledgement = new CountDownLatch(1);
	}

	public void sendMessage(DatagramPacket message, DatagramSocket socket) {
		
		// Randomize a failure variable
		Random generator = new Random();
		
		DatagramPacket packet = message;
		
		System.out.println("Sending on socket at port: " + socket.getLocalPort());
		
		// Make a number of attempts to send the message
		for (int i = 1; i <= MAX_SEND_ATTEMPTS; i++) {

			double failure = generator.nextDouble();
			
			if (failure > TRANSMISSION_FAILURE_RATE) {
				
				// Send message
				try {
					socket.send(packet);
				} catch (IOException e) {
					System.err.println("Error: failed to send message to client");
					e.printStackTrace();
				}
				
				// Receive acknowledgment from Client via Server
				try {
					//socket.RESET TIMER
					socket.receive(packet);
					if (packet.getData().equals("ACK"))
					{
						System.out.println("Ack received by client connection");
						// Message was successfully sent and acknowledged by client
						return;
					}
					else
					{
						// Non-ack message was received
						System.err.println("Error: server-side message transmission failure");
						continue;
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Error: failed to receive acknowledgement");
				}
				
				/*
				// Receive acknowledgement from client via server
				try {
					acknowledgement.await();
				} catch (InterruptedException e) {
					System.err.println("Error: failed to receive acknowledgement");
					e.printStackTrace();
				}
				*/
				
			} else {
				// Message got lost
				System.out.println("Message lost on server side");
			}
		}
		// Message failed to send, decrement ack counter
		m_ackCounter--;
		System.err.println("Error: failed to send message");
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}
	
	public InetAddress getAddress() {
		return m_address;
	}
	
	public String getName() {
		return m_name;
	}
	
	public int getPort() {
		return m_port;
	}
	
	public int getAckPort() {
		return m_ackPort;
	}
	
	public int getMessageCounter() {
		return m_messageCounter;
	}
	
	public int getAckCounter() {
		return m_ackCounter;
	}

}
