package project.MsgSenders;

import org.apache.log4j.Logger;

import project.ConfChat;
import project.exception.ConfChatException;
import project.exception.ExceptionEnum;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.DeliveryNotification;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.MessageReceipt;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.pastry.PastryNode;

/**
 * Pure FreePastry Routing. Nodes must have the same instanceName to exchange messages.
 * This class is abstract because each Sender Instance could have different actions when
 * send or receive message but provides a base class to send message by ID or straight to
 * destination.
 */
public abstract class MsgSender extends
Thread
implements Application {
	static Logger logger = Logger.getLogger(MsgSender.class);


	protected Endpoint endpoint;
	private final PastryNode pastryNode;

	private final ConfChat confchat;

	boolean debug = false;

	public MsgSender(ConfChat confchat, String instanceName) {
		this.confchat = confchat;
		// We are only going to use one instance of this application on each PastryNode
		this.pastryNode = confchat.getPastryNode();
		this.endpoint = pastryNode.buildEndpoint(this, instanceName);
		// now we can receive messages
		this.endpoint.register();
	}






	/**
	 * Called to route a message to the id.
	 */
	public MessageReceipt routeMsgId(Id id, Msg msg) {
		logger.debug(this + " sending to " + id);
		return endpoint.route(id, msg, null);
	}


	/**
	 * Called to route a message to the id.
	 */
	public MessageReceipt routeMsgId(Id id, Msg msg, DeliveryNotification notification) {
		logger.debug(this + " sending to " + id);
		return endpoint.route(id, msg, null, notification);
	}

	/**
	 * Called to directly send a message to the node, not by ring
	 * @throws ConfChatException 
	 */
	public MessageReceipt routeMsgDirect(NodeHandle nh, Msg msg) throws ConfChatException {
		if(nh != null){
			logger.debug(this + " sending direct to " + nh.getId());
			return endpoint.route(nh.getId(), msg, nh);
		}else
			throw new ConfChatException(ExceptionEnum.userNotOnline);
	}

	/**
	 * Called to directly send a message to the node, not by ring
	 */
	public MessageReceipt routeMsgDirect(NodeHandle nh, Msg msg, DeliveryNotification notification) {
		logger.debug(this + " sending direct to " + nh.getId());
		return endpoint.route(nh.getId(), msg, nh, notification);
	}


	/**
	 * Called when we receive a message.
	 */
	public abstract void deliver(Id id, Msg message);


	@Override
	public void deliver(Id arg0, Message arg1) {
		if (arg1 instanceof Msg) {
			Msg msg = (Msg) arg1;
			deliver(arg0, msg);
		}
	}







	/**
	 * Called when you hear about a new neighbor
	 */
	@Override
	public void update(NodeHandle handle, boolean joined) {
	}

	/**
	 * Called a message travels along your path
	 */
	@Override
	public boolean forward(RouteMessage message) {
		return true;
	}

	/**
	 * @return the confchat to use at ChatOneToOne
	 */
	public ConfChat getConfchat() {
		return confchat;
	}


	@Override
	public String toString() {
		return "ConfChat " + endpoint.getId();
	}
}
