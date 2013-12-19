package project.MsgSenders.msgTypes;

import project.MsgSenders.Msg;
import project.MsgSenders.MsgType;
import project.multiPersonChat.UniqueTopic;

public class InviteTopic extends Msg{

	private static final long serialVersionUID = 1L;
	
	private UniqueTopic uniqueTopic;
	private String from;
	
	public InviteTopic(MsgType type, UniqueTopic uniqueTopic, String from) {
		super(type);
		this.uniqueTopic = uniqueTopic;
		this.from = from;
	}

	public UniqueTopic getUniqueTopic() {
		return uniqueTopic;
	}

	public String getFrom() {
		return from;
	}

	
}
