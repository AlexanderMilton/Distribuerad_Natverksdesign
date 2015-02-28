package UDPChat.Shared;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class ChatMessage
{
	static String dl = "/"; // Delimiter
	private byte[] buf = new byte[256];
	private DatagramPacket packet = new DatagramPacket(buf, buf.length);

	private InetAddress address 	= null;
	private int 		port 		= 0;
	private int 		type 		= 0;
	private String 		sender 		= null;
	private long 		timeStamp	= 0;
	private String 		parameter 	= null;
	private String 		text 		= null;

	public ChatMessage(InetAddress address, int port, int type, String sender, long timeStamp, String parameter, String text)
	{
		String message = type + dl + sender + dl + timeStamp + dl + parameter + dl + text;
		packet = new DatagramPacket(message.getBytes(), message.length(), address, port);
	}

	public ChatMessage(DatagramPacket packet)
	{
		String message = new String(packet.getData(), 0, packet.getLength()).trim();
		String messageComponent[] = message.split(dl);
		
		type 		= Integer.parseInt(	messageComponent[0]);
		sender 		= 					messageComponent[1];
		timeStamp	= Long.parseLong(	messageComponent[2]);
		parameter 	=					messageComponent[3];
		text 		= 					messageComponent[4];
	}
	
	public InetAddress getAddress()
	{
		return address;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public DatagramPacket getPacket()
	{
		return packet;
	}

	public int getType()
	{
		return type;
	}

	public String getSender()
	{
		return sender;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public String getParameter()
	{
		return parameter;
	}

	public String getText()
	{
		return text;
	}
}
