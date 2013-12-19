package project;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import project.admin.StatisticsSummary;
import project.exception.ConfChatException;
import project.exception.NotLoggedInException;
import project.management.Friend;
import project.multiPersonChat.UniqueTopic;

public class ConfChatAPI {
    static Logger logger = Logger.getLogger(ConfChatAPI.class);

    ConfChat confChat = null;


    public ConfChatAPI(String bindPort, String bootstrapIP, String bootstrapPort) throws IOException, InterruptedException {
        confChat = new ConfChat(bindPort, bootstrapIP, bootstrapPort);
    }


    /**
     * Exit program
     */
    public void exit() {
        confChat.finishConfChat();
    }

    /**
     * Show administrator statitics
     */
    public StatisticsSummary show_status() throws ConfChatException {
        return confChat.showStatistics();
    }


    /**
     * Register new user
     */
    public void register(String username, String password, String fullname) throws ConfChatException {
        confChat.getUserManager().register(username, password, fullname);
    }

    /**
     * Login user
     */
    public void login(String username, String passwd) throws ConfChatException, NotLoggedInException {
        confChat.getUserManager().login(username, passwd);
    }

    /**
     * Logout user
     */
    public void logout() throws ConfChatException, NotLoggedInException {
        confChat.logout();
    }



    /**
     * Change user password
     */
    public void changePassword(String username, String oldPassword, String newPassword) throws ConfChatException {
        confChat.getUserManager().changePassword(username, oldPassword, newPassword);
    }


    /**
     * Search username retriving the fullname. Only username whose full name has all name
     * parts of searched fullname. For each partname, get all usernames and check if them
     * fullname contains all names which are being searched
     */
    public ArrayList<Friend> searchByPartOfName(String fullName) {
        return confChat.getUserManager().searchByPartOfName(fullName);
    }

    /**
     * List Current Friends
     */
    public ArrayList<Friend> listFriends() throws ConfChatException, NotLoggedInException {
        return confChat.getUserManager().listFriends();
    }


    /**
     * Send friend request
     */
    public void sendFriendRequest(String username) throws ConfChatException, NotLoggedInException {
        confChat.getUserManager().sendFriendRequest(username);
    }


    /**
     * Extract the friend request, move it to my friends and notify destination
     */
    public void acceptFriendRequest(String username) throws ConfChatException, NotLoggedInException {
        confChat.getUserManager().acceptFriendRequest(username);
    }


    /**
     * Accept all friends
     * 
     * @throws NotLoggedInException
     * @throws ChatException alreadyFriend invalidUsername
     */
    public void acceptALLFriendRequest() throws NotLoggedInException, ConfChatException {
        confChat.getUserManager().acceptALLFriendRequest();
    }


    /**
     * List users whose request your friendship
     * 
     * @return Array of friends
     * @throws NotLoggedInException
     */
    public ArrayList<Friend> listFriendRequest() throws NotLoggedInException {
        return confChat.getUserManager().listFriendRequest();
    }



    public void sendMessage(String username, String message) throws NotLoggedInException {
        confChat.getChatOneToOne().sendMessage(username, message);
    }

    public void createChatRoom(String roomName) throws ConfChatException, NotLoggedInException {
        confChat.getChatRoomManager().createChatRoom(roomName);
    }

    public void sendMessageChatRoom(String roomName, String message) throws NotLoggedInException, ConfChatException {
        confChat.getChatRoomManager().sendMessage(roomName, message);
    }

    public ArrayList<UniqueTopic> listChatRoomTopics() throws NotLoggedInException {
        return confChat.getChatRoomManager().listChatRoomTopics();
    }

    public void inviteToChatRoom(String username, String topic) throws ConfChatException, NotLoggedInException {
        confChat.getChatRoomManager().inviteToChatRoom(username, topic);
    }

    public void unsubscribeRoom(String topic) throws ConfChatException, NotLoggedInException {
        confChat.getChatRoomManager().unsubscribeRoom(topic);
    }

    public ArrayList<String> getPendentNotifications() {
        return confChat.getPendentNotifications();
    }

    public void restartPendentNotifications() {
        confChat.restartPendentNotifications();
    }
}
