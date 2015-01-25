/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPChat.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ClientConnection {

	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	
	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
		
	}

	public void sendMessage(DatagramPacket message, DatagramSocket socket) {

		Random generator = new Random();
		double failure = generator.nextDouble();

		if (failure > TRANSMISSION_FAILURE_RATE) {
			// TODO: send a message to this client using socket.
			try {
				socket.send(message);
			} catch (IOException e) {
				System.err.println("Error: failed to send message to client!");
				e.printStackTrace();
			}
			
		} else {
			// Message got lost
			System.out.println("Message lost on server side");
		}

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

}
