package TCPChat.Shared;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ChatMessage{
		
	private JSONObject obj = new JSONObject();
		
	@SuppressWarnings("unchecked")
	public ChatMessage(String sender, String command, String parameter, String message){
		obj.put("sender", sender);
		obj.put("command", command);
		obj.put("parameter", parameter);
		obj.put("timestamp", System.currentTimeMillis());
		obj.put("message", message);
	}
	
	public ChatMessage(String JSONstring){
		JSONParser parser = new JSONParser();
		try
		{
			obj = (JSONObject)parser.parse(JSONstring);
		} catch (ParseException e)
		{
			System.err.println("Error: parse exception");
			e.printStackTrace();
		}
	}

	public String getSender(){
		return (String)obj.get("sender");	
	}

	public String getCommand(){
		return (String)obj.get("command");	
	}
	
	public String getParameter(){
		return (String)obj.get("parameter");	
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
