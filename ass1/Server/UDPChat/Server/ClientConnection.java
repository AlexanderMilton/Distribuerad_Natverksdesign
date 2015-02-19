/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPChat.Server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

import UDPChat.Shared.MessageID;
import UDPChat.Shared.PacketSenderThread;

/**
 * 
 * @author brom
 */
public class ClientConnection {
	
	static double TRANSMISSION_FAILURE_RATE = 0.3;
	
	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private long lastPacketUID;
	private PacketSenderThread packetSender = null;
	private int lastIDAdded = -1;
	private boolean m_valid = true;
	
	public ClientConnection(String name, DatagramSocket socket, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
		
		lastPacketUID = -99;
		
		packetSender = new PacketSenderThread(socket);
		packetSender.start();
	}

	public void sendMessage(String message, long pid, int ptype) {
		if (lastIDAdded == MessageID.PING && ptype == MessageID.PING) return;
		
		String formatedMessage = pid + " " + ptype + " " + "SERVER" + " " + message;
		byte[] buf = new byte[512];
		buf = formatedMessage.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, m_address, m_port);
		packetSender.add(packet);
	}
	
	public static void sendAckPacket(String message, long pid, int ptype, DatagramSocket socket, InetAddress address, int port) {
		Random generator = new Random();
		double failure = generator.nextDouble();
		
		if (failure <= TRANSMISSION_FAILURE_RATE) return;
		
		String formatedMessage = pid + " " + ptype + " " + "SERVER" + " " + message;
		byte[] buf = new byte[512];
		buf = formatedMessage.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);

		try {
			socket.send(packet);
		} catch (IOException e) {
			System.err.println("Failed to send ack to client.");
		}
	}

	public int getCurrentPacketSentCount() {
		return packetSender.getCurrentPacketSentCount();
	}
	
	public boolean isPacketSenderEmpty() {
		if (packetSender.getCurrentPacketType() == -1) return true;
		return false;
	}
	
	public String getUsername() {
		return m_name;
	}
	
	public void advancePacketSenderQueue(String id) {
		packetSender.advanceQueue(id);
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}
	
	public InetAddress getAddress() {
		return m_address;
	}
	
	public int getPort() {
		return m_port;
	}
	
	public void setLastPacketUID(long uid) {
		lastPacketUID = uid;
	}
	
	public long getLastSocketUID() {
		return lastPacketUID;
	}
	
	public void kill() {
		packetSender.kill();
	}
	
	public void setValidFlag(boolean valid) {
		m_valid = valid;
	}
	
	public boolean getValidFlag() {
		return m_valid;
	}
}
