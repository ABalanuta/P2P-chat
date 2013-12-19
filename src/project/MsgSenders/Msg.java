package project.MsgSenders;

import rice.p2p.commonapi.Message;

public class Msg
        implements Message {

    private static final long serialVersionUID = -3522917750191143661L;

    private final MsgType type;
    private String message;
    
    public Msg(MsgType type) {
        this.type = type;
    }
    
    public Msg(MsgType type, String message) {
        this.type = type;
        this.setMessage(message);
        
    }

    @Override
    public int getPriority() {
        return Message.LOW_PRIORITY;
    }

    public MsgType getType() {
        return type;
    }

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

}
