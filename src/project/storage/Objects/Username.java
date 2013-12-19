package project.storage.Objects;

import java.util.ArrayList;
import java.util.Date;

import project.exception.ConfChatException;
import project.exception.ExceptionEnum;
import project.exception.UserOfflineException;
import project.management.Friend;
import rice.pastry.NodeHandle;

public class Username extends
PastObject {
	private static final long serialVersionUID = 1L;

	private final String username;
	private String password;
	private final String fullName;
	private NodeHandle nodeHandle;
	/** Simple timestamp. We don't care about signed timestamp and clock sync */
	private Date timestampHandle;
	private ArrayList<Friend> friends;



	/** Friends whose are waiting for our response */
	private ArrayList<Friend> requestFriendsList;
	/** Friends whose confirmed our request but we didn't added yet */
	private ArrayList<Friend> acceptedFriendsList;

	/** Handler is valid during expireTime milisecounds */
	//    private final static long EXPIRE_TIME = 300000;


	private final ArrayList<String> messagesReceivedWhileOffline;


	public Username(String user, String fullname, String pass) {
		super(PastObjectTypes.USERNAME);
		this.username = user;
		this.password = pass;
		this.fullName = fullname;
		this.friends = new ArrayList<Friend>();
		this.requestFriendsList = new ArrayList<Friend>();
		this.acceptedFriendsList = new ArrayList<Friend>();
		this.timestampHandle = new Date();
		this.messagesReceivedWhileOffline = new ArrayList<String>();
	}







	public void addFriendRequest(Friend friendRequest) {
		this.requestFriendsList.add(friendRequest);
	}


	public Friend extractFriendRequest(String username) throws ConfChatException {
		for (Friend friend : requestFriendsList) {
			if (friend.getUsername().equals(username)) {
				requestFriendsList.remove(friend);
				return friend;
			}
		}
		throw new ConfChatException(ExceptionEnum.invalidUsername);
	}

	public ArrayList<Friend> getFriendsRequests() {
		return requestFriendsList;
	}

	public Friend getFriend(String username) throws ConfChatException {
		for (Friend friend : friends) {
			if (friend.getUsername().equals(username)) {
				return friend;
			}
		}
		throw new ConfChatException(ExceptionEnum.invalidUsername);
	}


	public void addFriend(Friend friend) {
		this.friends.add(friend);
	}



	public void addAcceptedFriendRequest(Friend friend) {
		this.acceptedFriendsList.add(friend);
	}







	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * If this handle was not renew, set null
	 * 
	 * @throws UserOfflineException
	 */
	 public NodeHandle getUserNode() throws UserOfflineException {
		//        if (new Date().getTime() > this.timestampHandle.getTime() || nodeHandle == null) {
		//            throw new UserOfflineException(username);
		//        }
		 if ( nodeHandle == null) {
			 throw new UserOfflineException(username);
		 }
		 return nodeHandle;
	 }


	 public void setUserOffline() {
		 this.nodeHandle = null;
	 }


	 public void setNodeHandle(NodeHandle nodeHandle) {
		 //        long timestamp = new Date().getTime() + EXPIRE_TIME;
		 //        this.timestampHandle = new Date(timestamp);
		 this.nodeHandle = nodeHandle;
	 }




	 public String getUsername() {
		 synchronized (username) {
			 return username;
		 }
	 }

	 public String getFullName() {
		 synchronized (fullName) {
			 return fullName;
		 }
	 }



	 @SuppressWarnings("unchecked")
	 public ArrayList<Friend> getFriends() {

		 if (friends != null) {
			 synchronized (friends) {
				 return (ArrayList<Friend>) friends.clone();
			 }
		 }
		 return null;
	 }



	 public ArrayList<Friend> getRequestFriendsList() {
		 if (requestFriendsList != null) {
			 synchronized (requestFriendsList) {
				 return (ArrayList<Friend>) requestFriendsList.clone();
			 }
		 }
		 return null;
	 }


	 public ArrayList<Friend> getAcceptedFriendsList() {
		 if (acceptedFriendsList != null) {
			 synchronized (acceptedFriendsList) {
				 return (ArrayList<Friend>) acceptedFriendsList.clone();
			 }
		 }
		 return null;
	 }



	 public void setFriends(ArrayList<Friend> friends) {

		 synchronized (friends) {
			 this.friends = friends;
		 }
	 }

	 public void setRequestFriendsList(ArrayList<Friend> requestFriendsList) {
		 synchronized (requestFriendsList) {
			 this.requestFriendsList = requestFriendsList;
		 }
	 }

	 public void setAcceptedFriendsList(ArrayList<Friend> acceptedFriendsList) {
		 synchronized (acceptedFriendsList) {
			 this.acceptedFriendsList = acceptedFriendsList;
		 }
	 }


	 public void cleanAcceptedFriendsList() {
		 synchronized (acceptedFriendsList) {
			 this.acceptedFriendsList = new ArrayList<Friend>();
		 }
	 }

	 public void cleanRequestFriendsList() {
		 synchronized (requestFriendsList) {
			 this.requestFriendsList = new ArrayList<Friend>();
		 }

	 }

	 public boolean isMyFriend(String username) {
		 for (Friend currentFriend : getFriends()) {
			 if (currentFriend.getUsername().equals(username)) {
				 return true;
			 }
		 }
		 return false;
	 }




	 public ArrayList<String> getMessagesReceivedWhileOffline() {
		 return messagesReceivedWhileOffline;
	 }

	 public void addMessageReceivedWhileOffline(String message) {
		 this.messagesReceivedWhileOffline.add(message);
	 }

	 public void resetMessagesReceivedWhileOffline() {
		 this.messagesReceivedWhileOffline.clear();
	 }

}
