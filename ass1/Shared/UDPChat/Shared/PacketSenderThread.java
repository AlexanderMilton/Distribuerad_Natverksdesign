package UDPChat.Shared;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class PacketSenderThread extends Thread {
	private final double TRANSMISSION_FAILURE_RATE = 0.3;
	
	private DatagramSocket m_socket;
	private Queue<DatagramPacket> m_packetQueue;
	private DatagramPacket m_current = null;
	private boolean running = true;
	private int currentPacketSentCount = 0;
	
	public PacketSenderThread(DatagramSocket socket) {
		m_socket 		= socket;
		m_packetQueue 	= new LinkedList<DatagramPacket>();
	}
	
	public void run() {
		Random generator = new Random();
		
		while(running) {
			System.out.print("");
			if (!m_packetQueue.isEmpty() && m_current == null) {
				m_current = m_packetQueue.poll();
				if (m_current != null) currentPacketSentCount = 0;
			}
			
			if (m_current == null) continue;
			
			System.out.println("Sending message.");
			
			double failure = generator.nextDouble();
		    if (failure > TRANSMISSION_FAILURE_RATE) {
		    	try {
		    		currentPacketSentCount++;
					m_socket.send(m_current);
				} catch (IOException e) {
					System.err.println("Failed to send packet to server.");
				}
		    }
			
			try {
				sleep(500);
			} catch (InterruptedException e) {
				System.err.println("PacketSenderThread was interrupted.");
			}
		}
	}
	
	public void add(DatagramPacket packet) {
		System.out.println(m_current);
		m_packetQueue.add(packet);
	}
	
	public int getCurrentPacketSentCount() {
		return currentPacketSentCount;
	}
	
	public boolean advanceQueue(String id) {
		if (m_current == null) return false;
		String tok[] = new String(m_current.getData(), 0, m_current.getLength()).split(" ", 4);
		tok[0].trim();
		if (id.equals(tok[0])) {
			m_current = null;
			return true;
		}
		return false;
	}
	
	public void sendAckPacket(DatagramPacket packet, String username) {
		Random generator = new Random();
		double failure = generator.nextDouble();
		
		/* If we have "failed" a transmission, just exit */
		if (failure <= TRANSMISSION_FAILURE_RATE) return;
		
		String[] ptok = new String(packet.getData(), 0, packet.getLength()).split(" ", 4);
		ptok[0].trim();
		
		String pData = ptok[0] + " " + 0 + " " + username + " " + "ACK";
		
		byte[] buf = new byte[512];
		buf = pData.getBytes();
		
		DatagramPacket ackPacket = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
		
		try {
			m_socket.send(ackPacket);
		} catch (IOException e) {
			System.err.println("Failed to send ack-packet.");
		}
	}
	
	public void sendPongPacket(DatagramPacket packet, String username) {
		Random generator = new Random();
		double failure = generator.nextDouble();
		
		/* If we have "failed" a transmission, just exit */
		if (failure <= TRANSMISSION_FAILURE_RATE) return;
		
		String[] ptok = new String(packet.getData(), 0, packet.getLength()).split(" ", 4);
		ptok[0].trim();
		
		String pData = ptok[0] + " " + MessageID.PONG + " " + username + " " + "PING_RESPONSE";
		
		byte[] buf = new byte[512];
		buf = pData.getBytes();
		
		DatagramPacket ackPacket = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
		
		try {
			m_socket.send(ackPacket);
		} catch (IOException e) {
			System.err.println("Failed to send ack-packet.");
		}
	}
	
	public int getCurrentPacketType() {
		if (m_current == null) return -1;
		
		String s = new String(m_current.getData(), 0, m_current.getLength());
		Message m = new Message(s);
		return m.getType();
	}
	
	public void kill() {
		running = false;
	}
}
