package UDPChat.Client;

import java.awt.event.*;
import java.io.IOException;


public class Client implements ActionListener {

    private String m_name = null;
    private final ChatGUI m_GUI;
    private ServerConnection m_connection = null;

    public static void main(String[] args) throws IOException {
	if(args.length < 3) {
	    System.err.println("Usage: java Client serverhostname serverportnumber username");
	    System.exit(-1);
	}

	try {
	    Client instance = new Client(args[2]);
	    instance.connectToServer(args[0], Integer.parseInt(args[1]));
	} catch(NumberFormatException e) {
	    System.err.println("Error: port number must be an integer.");
	    System.exit(-1);
	}
    }

    private Client(String userName) {	
	m_name = userName;

	// Start up GUI (runs in its own thread)
	m_GUI = new ChatGUI(this, m_name);
    }

    private void connectToServer(String hostName, int port) throws IOException {
		//Create a new server connection
		m_connection = new ServerConnection(hostName, port);

		if(m_connection.handshake(m_name)) {
			m_GUI.displayMessage("Connected successfully to the server.");
		    listenForServerMessages();
		}
		else {
			m_GUI.displayMessage("Unable to connect to server.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.exit(0);
		}
    }

    private void listenForServerMessages() throws IOException {
    	do {
    		String r = m_connection.receiveChatMessage();
    		r.trim();
    		
    		/* QUICK DIRTY WORKAROUND FOR PRINTING LATE HANDSHAKE MESSAGES */
    		/* NOTE: We will never receive a pure true/false message in any other case so this is safe. */
    		if (!r.equals("false") && !r.equals("true")) {
    			m_GUI.displayMessage(r);
    		}
    		
    		
    		if (r.equals("You have been disconnected from the server.")) {
    			break;
    		}
    	} while(true);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
    }

    // Sole ActionListener method; acts as a callback from GUI when user hits enter in input field
    @Override
    public void actionPerformed(ActionEvent e) {
		// Since the only possible event is a carriage return in the text input field,
		// the text in the chat input field can now be sent to the server.
		m_connection.sendChatMessage(m_GUI.getInput());
		m_GUI.clearInput();
    }
}
