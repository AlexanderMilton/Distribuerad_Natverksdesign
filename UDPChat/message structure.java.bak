

--- MESSAGE TYPES ---

1: connect
2: disconnect
3: list
4: whisper
5: broadcast
6: heartbeat
7: ackn



--- CHAT MESSAGE STRUCTURE ---

ADDRESS		PORT	TYPE	SENDER		TIMESTAMP	PARAMETER	TEXT

public ChatMessage(InetAddress address, int port, int type, String sender, long timeStamp, String parameter, String text)
{
	String message = type + dl + sender + dl + timeStamp + dl + parameter + dl + text;
	packet = new DatagramPacket(message.getBytes(), message.length(), address, port);
}