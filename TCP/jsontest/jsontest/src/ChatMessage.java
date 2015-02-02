import org.json.simple.JSONObject;

public class ChatMessage{
		
	private JSONObject obj = new JSONObject();
		
	public ChatMessage(String command, String parameters){
		obj.put("command", command);
		obj.put("parameters", parameters);
		obj.put("timestamp", System.currentTimeMillis());
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
}
