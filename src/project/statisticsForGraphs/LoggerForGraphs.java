package project.statisticsForGraphs;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import project.MsgSenders.msgTypes.TextMsg;
import project.multiPersonChat.MulticastContent;


public class LoggerForGraphs {
	static Logger logger = Logger.getLogger(LoggerForGraphs.class);

	private static long GlobalMessageOneOnOneIDGenerator = 0; 
	private static long GlobalMultiChatMessageIDGenerator = 0;

	//Section Need to Log data to make graphs on the speed of two person chat later
	public static void logMessageOneOnOne(String sender, String receiver, TextMsg textMessage){
		//logger.info(LogTypes.Message101 + "  " +" MessageID: "+ textMessage.getMessageID() +" Sender: "+ sender + " Receiver: "+ receiver +" TimeStamp: " + getTimeStamp() );
	}

	public static long getNextGlobalMessageOneOnOneIDGenerator(){
		GlobalMessageOneOnOneIDGenerator = GlobalMessageOneOnOneIDGenerator + 1;
		return GlobalMessageOneOnOneIDGenerator;
	}
	//-----------------------------------------------------------------------------
	
	public static void logStorageLookUp(Date startTime, Date finishTime){
		long differenceInMiliseconds = finishTime.getTime() - startTime.getTime();
		//logger.info(LogTypes.StorageLookup + "  " + "Time: " + differenceInMiliseconds );
	}
	
	public static void logStoragePut(Date startTime, Date finishTime){
		long differenceInMiliseconds = finishTime.getTime() - startTime.getTime();
	//	logger.info(LogTypes.StoragePut + "  " + "Time: " + differenceInMiliseconds );
	}
	//-----------------------------------------------------------------------------
	
	public static void logMultiChat(MulticastContent myMessage){
		//logger.info(LogTypes.MultiChat + "  " + "Message ID: " + myMessage.getMultiChatMessageID() + " Sender: " + myMessage.getFrom() + " Topic: " + myMessage.getTopic() + " TimeStamp: " + getTimeStamp());
	}
	
	public static long getNextGlobalMultiChatMessageIDGenerator(){
		GlobalMultiChatMessageIDGenerator = GlobalMultiChatMessageIDGenerator + 1;
		return GlobalMultiChatMessageIDGenerator;
	}
	//-----------------------------------------------------------------------------
	
	
	
	private static String getTimeStamp(){
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//YYYY-MM-DD HH:MI:Sec.Ms)
		Date now = new Date();
		String timeStamp = sdfDate.format(now);
		SimpleDateFormat sdfDateMiliSeconds = new SimpleDateFormat("SSS");//YYYY-MM-DD HH:MI:Sec.Ms)
		timeStamp = timeStamp + " Miliseconds: "+ sdfDateMiliSeconds.format(now);
		return timeStamp;
	}

}
