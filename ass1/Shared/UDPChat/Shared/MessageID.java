package UDPChat.Shared;

public class MessageID {
	public static final int JOIN 	= 0;
	public static final int LEAVE	= 1;
	public static final int TELL	= 2;
	public static final int LIST	= 3;
	public static final int SHOUT	= 4;
	
	public static final int ACK		= 5; // Unused so far.
	
	/* Client/Server connected beat. */
	public static final int PING	= 6;
	public static final int PONG	= 7;
}
