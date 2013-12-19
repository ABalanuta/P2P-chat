package project.multiPersonChat;


import java.util.ArrayList;

import org.apache.log4j.Logger;

import project.ConfChat;
import project.MsgSenders.Msg;
import project.MsgSenders.MsgType;
import project.MsgSenders.msgTypes.InviteTopic;
import project.exception.ConfChatException;
import project.exception.ExceptionEnum;
import project.exception.NotLoggedInException;
import project.exception.UserOfflineException;
import project.management.Friend;
import project.storage.Objects.Username;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.PastryNode;

public class ChatRoomManager {
    static Logger logger = Logger.getLogger(ChatRoomManager.class);


    private final PastryNode pastryNode;
    private final ConfChat confchat;
    private final MyScribeClient chatRooms;


    public ChatRoomManager(ConfChat confchat) {
        this.pastryNode = confchat.getPastryNode();
        this.confchat = confchat;
        this.chatRooms = new MyScribeClient(pastryNode, confchat);
    }


    /**
     * Creates a ChatRoom with the especified Topic Name
     */
    public void createChatRoom(String topicToSubscribe) throws ConfChatException, NotLoggedInException {

        checkIfLoggedIn();
        if (haveTopic(topicToSubscribe))
            throw new ConfChatException(ExceptionEnum.TopicAlreadyExists);

        chatRooms.create(topicToSubscribe, pastryNode);
        chatRooms.notifyEntrance(topicToSubscribe);
        logger.info("Chatroom " + topicToSubscribe + " was been created");
    }

    /**
     * Unsubscribes user from target Topic ChatRoom
     */
    public void unsubscribeRoom(String topic) throws ConfChatException, NotLoggedInException {

        checkIfLoggedIn();
        if (!haveTopic(topic))
            throw new ConfChatException(ExceptionEnum.RoomNotSubscribed);

        chatRooms.notifyDeparture(topic);
        chatRooms.unsubscribe(topic);
    }

    /**
     * Unsubscribes user All Topics
     */
    public void unsubscribeFromAll() throws ConfChatException, NotLoggedInException {

        checkIfLoggedIn();

        for (UniqueTopic u : chatRooms.getMyTopics()) {
            logger.info("Unsubscribing From: " + u.getTopicName());
            chatRooms.notifyDeparture(u.getTopicName());
            chatRooms.unsubscribe(u.getTopicName());
        }
    }

    /**
     * Subscribes to an Existing ChatRoom
     */
    public void subscribeChatRoom(String topicName, String topicUniqueName, String from) throws NotLoggedInException {

        chatRooms.subscribe(topicName, topicUniqueName);
        chatRooms.notifyEntrance(topicName);
        this.confchat.addPendentNotifications("Now you Participate in ChatRoom:" + topicName + " with uniqueID " + topicUniqueName);
    }


    /**
     * Invites Target user to Join my Topic ChatRoom (ChatRooms are private you ned to be
     * invited)
     */
    public void inviteToChatRoom(String username, String topic) throws ConfChatException, NotLoggedInException {

        checkIfLoggedIn();

        // Verify Friendship and Online Status
        Friend friend = confchat.getUserManager().getMe().getFriend(username);
        Username user = confchat.getUserManager().getUsernameObject(friend.getUsername());

        NodeHandle userHandle;
        try {
            userHandle = user.getUserNode();
        } catch (UserOfflineException e) {
            throw new ConfChatException(ExceptionEnum.userNotOnline);
        }

        checkIfSubscribedToTopic(topic);

        UniqueTopic uniqueTopic = chatRooms.getUniqueTopic(topic);
        Msg toSend = new InviteTopic(MsgType.InviteTopic, uniqueTopic, confchat.getUserManager().getMe().getUsername());

        confchat.getUserManager().routeMsgDirect(userHandle, toSend);
    }

    /**
     * List all the Rooms that you subscribed or created
     */
    public ArrayList<UniqueTopic> listChatRoomTopics() throws NotLoggedInException {

        checkIfLoggedIn();
        return chatRooms.getMyTopics();
    }

    /**
     * Sends a message to a specific Room
     */
    public void sendMessage(String topic, String message) throws NotLoggedInException, ConfChatException {

        checkIfLoggedIn();
        checkIfSubscribedToTopic(topic);
        String myUsername = this.confchat.getUserManager().getMe().getUsername();
        chatRooms.sendMulticastMessage(topic, message);

    }


    /**
     * Verifies if the user is logged in
     */
    private void checkIfLoggedIn() throws NotLoggedInException {
        if (confchat.getUserManager().getMe() == null) {
            throw new NotLoggedInException();
        }
    }


    /**
     * Verifies if user is subscribed to this room
     */
    private void checkIfSubscribedToTopic(String topic) throws ConfChatException {

        if (!haveTopic(topic))
            throw new ConfChatException(ExceptionEnum.RoomNotSubscribed);

    }


    /**
     * Verifies if user is subscribed to this room
     */
    public boolean haveTopic(String topic) {

        if (chatRooms.getUniqueTopic(topic) != null) {
            return true;
        }
        return false;
    }


    /**
     * Update each topic gossip ID if it is lower than current.
     * 
     * @throws NotLoggedInException
     */
    public long countRootRoom() throws NotLoggedInException {
        return chatRooms.countChatRooms();
    }


}
