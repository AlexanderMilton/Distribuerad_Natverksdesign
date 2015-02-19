package UDPChat.Shared;

public class Message {
	private String username;
	private long uid;
	private int type;
	private String message_text;
	private boolean valid = true;
	
	public Message(String data) {	
		/* 64kb packet data is the maximum theoretical size. */
		assert(data.getBytes().length > 64000);
		
		
		/* Extract Header */
		data.trim();
		
		System.out.println("Creating a message out of: " + data);
		
		try {
			String[] header_tok = data.split(" ", 4);
			
			
			System.out.println("UID\t" + header_tok[0]);
			System.out.println("TYPE\t" + header_tok[1]);
			System.out.println("USR\t" + header_tok[2]);
			System.out.println("MSG\t" + header_tok[3]);
			
			/* Extract Chat-message */
			uid 			= Long.parseLong(header_tok[0].trim());
			type 			= Integer.parseInt(header_tok[1].trim());
			username 		= header_tok[2].trim();
			message_text	= header_tok[3].trim();
		} catch(ArrayIndexOutOfBoundsException e) {
			System.out.println("Message failed to be constructed correctly, might be corrupted or badly formated.");
			valid = false;
		}
	}
	
	public String getUsername() {
		return username;
	}
	
	public int getType() {
		return type;
	}
	
	public String getText() {
		return message_text;
	}
	
	public long getUID() {
		
		return uid;
	}
	
	public byte[] getBytes() {
		return getRawString().getBytes();
	}
	
	public String getRawString() {
		return "[" + Long.toString(uid) + "|" + type + "]" + message_text;
	}
	
	public boolean isValid() {
		return valid;
	}
}
