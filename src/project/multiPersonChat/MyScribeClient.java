package project.multiPersonChat;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import project.ConfChat;
import project.exception.NotLoggedInException;
import project.statisticsForGraphs.LoggerForGraphs;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.CancellableTask;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeClient;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.Topic;
import rice.pastry.commonapi.PastryIdFactory;

public class MyScribeClient

implements ScribeClient, Application {
	static Logger logger = Logger.getLogger(ConfChat.class);

	private Scribe myScribe;
	private ArrayList<UniqueTopic> myTopics;
	protected Endpoint endpoint;
	private final ConfChat confChat;
	private final Node node;



	public MyScribeClient(Node node, ConfChat confchat) {
		this.endpoint = node.buildEndpoint(this, "thisEndPoint");
		myScribe = new ScribeImpl(node, "thisScribeImpl");
		endpoint.register();
		this.confChat = confchat;
		this.node = node;
		myTopics = new ArrayList<UniqueTopic>();
	}


	/**
	 * Subscribes to a unique Topic
	 */
	public void subscribe(String topic, String uniqueTopic) {
		Topic myTopic = new Topic(new PastryIdFactory(node.getEnvironment()), uniqueTopic);
		this.myScribe.subscribe(myTopic, this);
		synchronized (myTopics) {
			this.myTopics.add(new UniqueTopic(topic, uniqueTopic));
		}
	}

	/**
	 * Create a Specific Topic
	 */
	public void create(String topic, Node remoteNode) {
		String uniqueTopic = topic + remoteNode.getId();
		Topic myTopic = new Topic(new PastryIdFactory(node.getEnvironment()), uniqueTopic);
		this.myScribe.subscribe(myTopic, this);
		synchronized (myTopics) {
			this.myTopics.add(new UniqueTopic(topic, uniqueTopic));
		}
	}

	/**
	 * Unsubscribes from a specific Topic
	 */
	public void unsubscribe(String topic) {
		UniqueTopic uniqueTopic = this.getUniqueTopic(topic);
		Topic unTopic = new Topic(new PastryIdFactory(node.getEnvironment()), uniqueTopic.getUniqueTopicName());
		this.myScribe.unsubscribe(unTopic, this);
		synchronized (myTopics) {
			this.myTopics.remove(uniqueTopic);
		}
	}

	/**
	 * Return List of Subscribed Topics
	 */
	public ArrayList<UniqueTopic> getMyTopics() {
		synchronized (myTopics) {
			return (ArrayList<UniqueTopic>) myTopics.clone();
		}
	}


	/**
	 * Retrurns the UniqueTopic Object given the topic Name
	 */
	public UniqueTopic getUniqueTopic(String topic) {

		ArrayList<UniqueTopic> Topics = getMyTopics();

		for (UniqueTopic uniqueTopic : Topics) {
			if (uniqueTopic.getTopicName().equals(topic)) {
				return uniqueTopic;
			}
		}
		return null;
	}



	/**
	 * Send Message to Topic subscribed Members
	 */
	public void sendMulticastMessage(String topicName, String messageToSend) throws NotLoggedInException {
		sendMulticast(topicName, messageToSend, MulticastContentType.message);
	}


	/**
	 * Notify Entrance of this user into the room
	 */
	public void notifyEntrance(String topicName) throws NotLoggedInException {
		sendMulticast(topicName, null, MulticastContentType.entrance);
	}


	/**
	 * Notify Entrance of this user into the room
	 */
	public void notifyDeparture(String topicName) throws NotLoggedInException {
		sendMulticast(topicName, null, MulticastContentType.departure);
	}


	/**
	 * Notify Existence of the user in the room
	 */
	public void notifyKeepAlive(String topicName) throws NotLoggedInException {
		sendMulticast(topicName, null, MulticastContentType.keepAlive);
	}

	/**
	 * Notify failure of a user
	 * @throws NotLoggedInException 
	 */
	public void notifyFailureOfNode(String topicName) throws NotLoggedInException{
		sendMulticast(topicName, null, MulticastContentType.nodeFailure);
	}


	/**
	 * Send Message to Topic subscribed Members
	 */
	private void sendMulticast(String topicName, String messageToSend, MulticastContentType status) throws NotLoggedInException {

		UniqueTopic uniqueTopicName = getUniqueTopic(topicName);
		String myUsername = this.confChat.getUserManager().getMe().getUsername();
		MulticastContent myMessage = new MulticastContent(myUsername, messageToSend, topicName, status);
		Topic myTopic = new Topic(new PastryIdFactory(node.getEnvironment()), uniqueTopicName.getUniqueTopicName());

		// For Logger Graphs
		if (myMessage.getStatus().equals(MulticastContentType.message)) {
			myMessage.setMultiChatMessageID(LoggerForGraphs.getNextGlobalMultiChatMessageIDGenerator());
			LoggerForGraphs.logMultiChat(myMessage);
		}

		synchronized (myScribe) {
			myScribe.publish(myTopic, myMessage);
		}
	}






	/**
	 * Called whenever we receive a published message.
	 */
	@SuppressWarnings("serial")
	class PublishContent
	implements Message {
		@Override
		public int getPriority() {
			return MAX_PRIORITY;
		}
	}

	@Override
	public void deliver(Topic topic, ScribeContent content) {
		try {
			String message = ((MulticastContent) content).getMessage();
			String messagetopic = ((MulticastContent) content).getTopic();
			String from = ((MulticastContent) content).getFrom();

			// if it is a message add it to the display array
			MulticastContentType opt = ((MulticastContent) content).getStatus();

			switch (opt) {
			case message:
				// add message only if it is not mine
				if (!this.confChat.getUserManager().getMe().getUsername().equals(from)) {
					this.confChat.addPendentNotifications("Room[" + messagetopic + "][" + from + "] " + message);
					this.getUniqueTopic(messagetopic).addUser(from);
					// For Logger Graphs
					LoggerForGraphs.logMultiChat(((MulticastContent) content));
				}
				break;
			case entrance:
				this.confChat.addPendentNotifications("Room[" + messagetopic + "] " + from + " entered the room :)");
				this.getUniqueTopic(messagetopic).addUser(from);
				Thread.sleep(1000);
				notifyKeepAlive(messagetopic);
				break;

			case keepAlive:
				this.getUniqueTopic(messagetopic).addUser(from);
				break;

			case departure:
				this.confChat.addPendentNotifications("Room[" + messagetopic + "] " + from + " left the room :(");
				this.getUniqueTopic(messagetopic).delUser(from);
				break;

			case nodeFailure:
				this.getUniqueTopic(messagetopic).delUserAll();
				Thread.sleep(1000);
				this.notifyKeepAlive(messagetopic);
				break;

			default:
				break;
			}
		} catch (NotLoggedInException e) {
			logger.info("User isn't logged in");
		} catch (InterruptedException e) {}
	}

	/**
	 * Disables Anycating
	 */
	@Override
	public boolean anycast(Topic topic, ScribeContent content) {
		return true;
	}


	/**
	 * Enables Forwarding
	 */
	@Override
	public boolean forward(RouteMessage message) {
		return true;
	}


	/************ Some passthrough accessors for the myScribe *************/
	@Override
	public void update(NodeHandle arg0, boolean arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void childAdded(Topic arg0, NodeHandle arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void childRemoved(Topic arg0, NodeHandle arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void subscribeFailed(Topic arg0) {

		System.out.println("--------------------------Falha-------------- subscribe");

		@SuppressWarnings("serial")
		class PublishContent1
		implements Message {
			@Override
			public int getPriority() {
				return MAX_PRIORITY;
			}
		}
	}

	@Override
	public void deliver(Id arg0, Message arg1) {
		// TODO Auto-generated method stub

	}



	/**
	 * Update each topic gossip ID if it is lower than current.
	 */
	public long countChatRooms() {

		long counter = 0;
		synchronized (myTopics) {
			for (UniqueTopic topic : myTopics) {

				Topic myTopic = new Topic(new PastryIdFactory(node.getEnvironment()), topic.getUniqueTopicName());
				if (myScribe.isRoot(myTopic)) {
					logger.debug("Im root of topic:" + topic.getUniqueTopicName());
				//notUsed	//verifyIfChatRoomHasNoDeadPeers(topic, myTopic);
					counter++;
				}
			}
		}
		return counter;
	}

	/**
	 * Verifies if the number of participants coresponds with the number of childs in a topic
	 * if not sends a multicast notifiyng everyone to keppAlive
	 */
	private void verifyIfChatRoomHasNoDeadPeers(UniqueTopic ut, Topic topic){

		long numOfTeoreticalParticipants = ut.getUsers().size();
		long numOfActualParticipants = this.myScribe.numChildren(topic)+1;

		if(numOfActualParticipants != numOfTeoreticalParticipants){
			logger.info("SCRIBE Node Failure Detected, Renewing Participant List ...");

			try {
				this.notifyFailureOfNode(ut.getTopicName());
			} catch (NotLoggedInException e) {}
		}
	}


}
