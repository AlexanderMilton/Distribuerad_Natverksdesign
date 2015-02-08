/*
 * Author: Petter Henriksson & András Márki
 * 
 */

class JsonTest 
{
   public static void main(String[] args) 
   {	   
	   ChatMessage c = new ChatMessage("whisper", "alex");
	   System.out.println(c.getCommand());
	   System.out.println(c.getParameters());
	   System.out.println(c.getTimeStamp());
	}
}