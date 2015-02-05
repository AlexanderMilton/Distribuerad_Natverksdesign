package UDPChat.Client;

public class ConnectFailedException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3742647716318871157L; // lolwut
	
	public ConnectFailedException(String msg)
	{
		super(msg);
	}
}
