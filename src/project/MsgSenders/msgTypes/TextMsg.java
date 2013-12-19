package project.MsgSenders.msgTypes;

import project.MsgSenders.Msg;
import project.MsgSenders.MsgType;

public class TextMsg extends
        Msg {

    private static final long serialVersionUID = -3522917750191143661L;

    private final String message;
    private final String sourceUsername;
    private final String destinationUsername;
	
	private long messageID;
	
    public TextMsg(String message, String sourceUsername, String destinationUsername, long messageID) {
        super(MsgType.TextMsg);
        this.message = message;
        this.sourceUsername = sourceUsername;
        this.destinationUsername = destinationUsername;
        this.messageID = messageID;
    }

    public String getMessage() {
        return this.message;
    }

    public String getSourceUsername() {
        return sourceUsername;
    }

    public String getDestinationUsername() {
        return destinationUsername;
    }

	/**
	 * @return the messageID
	 */
	public long getMessageID() {
		return this.messageID;
	}

	@Override
	public String toString() {
		return "TextMsg [message=" + message + ", SourceUsername=" + sourceUsername + ", DestinationUsername=" + destinationUsername + "]";
	}



}
