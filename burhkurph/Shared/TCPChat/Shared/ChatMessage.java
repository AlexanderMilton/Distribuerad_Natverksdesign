package TCPChat.Shared;
import org.json.simple.JSONObject;

public class ChatMessage{
		
	private JSONObject obj = new JSONObject();
		
	public ChatMessage(String sender, String command, String parameters, String message){
		obj.put("sender", sender);
		obj.put("command", command);
		obj.put("parameters", parameters);
		obj.put("timestamp", System.currentTimeMillis());
		obj.put("message", message);
	}

	public String getSender(){
		return (String)obj.get("sender");	
	}

	public String getCommand(){
		return (String)obj.get("command");	
	}
	
	public String getParameters(){
		return (String)obj.get("parameters");	
	}
	
	public String getTimeStamp(){
		return obj.get("timestamp").toString();
	}

	public String getMessage(){
		return (String)obj.get("message");	
	}
	
	public String getString() {
		return obj.toJSONString();
	}
}
