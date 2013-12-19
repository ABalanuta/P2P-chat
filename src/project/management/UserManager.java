package project.management;



import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

import org.apache.log4j.Logger;

import project.ConfChat;
import project.MsgSenders.Msg;
import project.MsgSenders.MsgSender;
import project.MsgSenders.MsgType;
import project.MsgSenders.msgTypes.FriendMsg;
import project.MsgSenders.msgTypes.InviteTopic;
import project.exception.ConfChatException;
import project.exception.ExceptionEnum;
import project.exception.NotLoggedInException;
import project.exception.UserOfflineException;
import project.storage.Objects.PartOfFullNames;
import project.storage.Objects.PastObject;
import project.storage.Objects.PastObjectTypes;
import project.storage.Objects.Username;
import rice.p2p.commonapi.CancellableTask;
import rice.p2p.commonapi.DeliveryNotification;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.MessageReceipt;
import rice.pastry.NodeHandle;

public class UserManager extends
MsgSender {
	static Logger logger = Logger.getLogger(UserManager.class);


	private Username me;
	private final ConfChat confchat;
	CancellableTask messageToSelfTask;
	/* HANDLER REFRESH INTERVAL must be smaller than handler expire time */
	private final static int REFRESH_INTERVAL = 180000;
	private boolean running = false;

	public UserManager(ConfChat confchat) {
		// Create instance of Sender to exchange message with other
		super(confchat, "UserManager");
		this.confchat = confchat;
		this.me = null;
	}

	@Override
	public void run() {
		// Update my handler from time to time and check my friends status
		while (running) {
			Random dice = new Random();
			// Refresh between 3minutes and 3 minutes and half
			long time = REFRESH_INTERVAL + (dice.nextInt(30000));
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
				logger.debug("Usermanager thread update, interrupted");
			}
			logger.debug("Lets refresh handle");
			refreshHandle();

			try {
				updateOnlineFriends();
			} catch (NotLoggedInException e) {
				// If i'm not logged in, ok relax
			}
		}

	}

	/**
	 * User registry in application
	 */
	public void register(String username, String password, String fullname) throws ConfChatException {
		if (confchat.getStorageAPI().getObject(username, PastObjectTypes.USERNAME) != null) {
			throw new ConfChatException(ExceptionEnum.usernameAlreadyTaken);
		}

		String hashPasswd = hashString(password);
		Username usernameObj = new Username(username, fullname, hashPasswd);
		usernameObj.setNodeHandle(null); // Not login yet. Still offline
		boolean inserted = confchat.getStorageAPI().insertObject(username, usernameObj);

		if (!inserted) {
			throw new ConfChatException(ExceptionEnum.storageError);
		}

		for (String partName : fullname.split(" ")) {

			if (partName.length() < 1)
				continue;

			// first verify if the object exists
			PastObject obj = confchat.getStorageAPI().getObject(partName, PastObjectTypes.PARTOFNAME);
			PartOfFullNames partNameObj;
			if (obj != null) {
				if (obj.getType() != PastObjectTypes.PARTOFNAME) {
					logger.error("Usermanager:register: object type with wrong id");
					throw new ConfChatException(ExceptionEnum.storageError);
				}
				partNameObj = (PartOfFullNames) obj;
			} else {
				// if not exists, create new
				partNameObj = new PartOfFullNames(partName);
			}
			// add the new Username (username is unique and struct is a set)
			partNameObj.getUsernames().add(username);
			confchat.getStorageAPI().insertObject(partName, partNameObj);
		}
		logger.info("Success! Registed!");
	}


	/**
	 * Login user
	 */
	public void login(String username, String password) throws ConfChatException, NotLoggedInException {

		boolean loggedIn = true;
		try {
			getMe();
		} catch (NotLoggedInException e) {
			loggedIn = false;
		}

		//        if (loggedIn) {
		//            throw new ConfChatException(ExceptionEnum.alreadyLoggedIn);
		//        }

		Username usernameObj = getUsernameObject(username);
		if (usernameObj == null) {
			throw new ConfChatException(ExceptionEnum.invalidUsername);
		}
		String hashPasswd = hashString(password);

		if (usernameObj.getPassword().equals(hashPasswd)) {
			try {
				usernameObj.getUserNode();
				//throw new ConfChatException(ExceptionEnum.alreadyLoggedIn);
				logger.info("You may be logged in in multiple places");
			} catch (UserOfflineException e) {
			}
			// Set online
			NodeHandle myHandle = confchat.getPastryNode().getLocalHandle();
			usernameObj.setNodeHandle(myHandle);


			// Keep copy local copy of object username
			setMe(usernameObj);

			saveMeState();

			/** Add friends whose accepted me */
			addAcceptedFriendRequests();

			/* Notify all friends! I'm online */
			notifyAllFriends(true);
			getMessagesReceivedWhileOffline(usernameObj);
			getPendentFriendRequests();
			updateOnlineFriends();

			logger.info(username + " logged in");
		} else {
			throw new ConfChatException(ExceptionEnum.wrongPassword);
		}
	}

	private void getMessagesReceivedWhileOffline(Username usernameObj) {
		ArrayList<String> listMsg = usernameObj.getMessagesReceivedWhileOffline();
		// Print all the messages
		if (listMsg.size() != 0) {
			logger.info("\nMessages received while Offline\n");
			for (String m : listMsg) {
				confchat.addPendentNotifications(m);
			}
		}
		usernameObj.resetMessagesReceivedWhileOffline();
		// Update the object in Pastry
		saveMeState();
	}

	/**
	 * Logout user
	 * 
	 * @return "User logged out"
	 * @throws NotLoggedInException
	 */
	public void logout() throws NotLoggedInException {
		this.interrupt();

		/** Cancelar o refesh periodico */
		sheduleRefreshCancel();
		Username myUsername = getMe();
		myUsername.setNodeHandle(null);

		boolean inserted = false;
		while (!inserted) {
			try {
				confchat.getStorageAPI().insertObject(myUsername.getUsername(), myUsername);
				inserted = true;
			} catch (ConfChatException e) {
			}
		}

		//Unsubscribe from ChatRooms
		try {
			confchat.getChatRoomManager().unsubscribeFromAll();
		} catch (ConfChatException e) {
			logger.info("Could not unsubscribe from chatrooms");
		}

		// Notify All Friends, I'm going sleep
		notifyAllFriends(false);


		// Logout program
		setMe(null);

		logger.info("User logged out");
	}

	/**
	 * Change user password
	 */
	public void changePassword(String username, String oldPassword, String newPassword) throws ConfChatException {
		Username usernameObj;
		if ((usernameObj = getUsernameObject(username)) == null) {
			throw new ConfChatException(ExceptionEnum.invalidUsername);
		}

		String hashOldPasswd = hashString(oldPassword);

		if (usernameObj.getPassword().equals(hashOldPasswd)) {
			String hashNewPassword = hashString(newPassword);

			// Update password and save
			usernameObj.setPassword(hashNewPassword);
			confchat.getStorageAPI().insertObject(username, usernameObj);

			// Also change local password
			try {
				this.getMe().setPassword(hashNewPassword);
			} catch (NotLoggedInException e) {
				throw new ConfChatException(ExceptionEnum.userNotOnline);
			}

			logger.info("Password Changed");

		} else {
			throw new ConfChatException(ExceptionEnum.wrongPassword);
		}

	}


	/**
	 * -------------------------------------------------------------------------------
	 * Online Status Management
	 * -------------------------------------------------------------------------------
	 */

	public void sheduleRefreshCancel() {
		if (messageToSelfTask != null) {
			messageToSelfTask.cancel();
		}
	}

	public void refreshHandle() {
		try {
			NodeHandle myHandle = confchat.getPastryNode().getLocalHandle();
			getMe().setNodeHandle(myHandle);
			logger.debug("Handler Refreshed");
		} catch (NotLoggedInException e) {
			logger.error("Refresh handle failed, please login again");
			sheduleRefreshCancel();
		}
	}

	/**
	 * -------------------------------------------------------------------------------
	 * Friends management
	 * -------------------------------------------------------------------------------
	 */
	private void addAcceptedFriendRequests() throws NotLoggedInException {
		ArrayList<Friend> acceptedFriends = getMe().getAcceptedFriendsList();
		for (Friend friend : acceptedFriends) {
			getMe().addFriend(friend);
			String s = friend.getUsername() + "accepted your friend request";
			confchat.addPendentNotifications(s);
		}
		updateOnlineFriends();
		getMe().cleanAcceptedFriendsList();
		saveMeState();
	}

	private void getPendentFriendRequests() throws NotLoggedInException {
		for (Friend friend : getMe().getRequestFriendsList()) {
			String s = friend.getUsername() + ":" + friend.getFullName() + "  Wants to be your friend";
			confchat.addPendentNotifications(s);
		}
	}

	/**
	 * Notify that I'm are online. For each friend, get the current handle and try notify
	 * that I'm online now @
	 * 
	 * @throws NotLoggedInException
	 */
	private void notifyAllFriends(boolean online) throws NotLoggedInException {
		Friend me = new Friend(getMe().getUsername(), getMe().getFullName(), online);
		for (Friend friend : getMe().getFriends()) {
			Username friendUsername;
			try {
				friendUsername = getUsernameObject(friend.getUsername());
			} catch (ConfChatException e) {
				continue;
			}
			NodeHandle friendHandle;
			try {
				friendHandle = friendUsername.getUserNode();
				routeMsgDirect(friendHandle, new FriendMsg(me, MsgType.OnlineNotification));
			} catch (UserOfflineException e1) {
				continue;
			} catch (ConfChatException e) {
				continue;
			}
		}
	}

	/**
	 * Search username retriving the fullname. Only username whose full name has all name
	 * parts of searched fullname. For each partname, get all usernames and check if them
	 * fullname contains all names which are being searched
	 * 
	 * @param Full Name to search
	 * @return String username set
	 */
	public ArrayList<Friend> searchByPartOfName(String pastOfName) {
		logger.debug("Search by Part of Full Name " + pastOfName);
		ArrayList<Friend> usernames = new ArrayList<Friend>();
		for (String partName : pastOfName.split(" ")) {
			ArrayList<Username> list = getUsernamesOfPartName(partName);
			for (Username user : list) {
				// Fullname compare is case-insensitve
				if (user.getFullName().toLowerCase().contains(pastOfName.toLowerCase())) {
					boolean online = true;
					try {
						user.getUserNode();
					} catch (UserOfflineException e) {
						online = false;
					}
					usernames.add(new Friend(user.getUsername(), user.getFullName(), online));
				}
			}
		}

		return usernames;
	}

	public ArrayList<Username> getUsernamesOfPartName(String partName) {
		ArrayList<Username> out = new ArrayList<Username>();

		PastObject obj = confchat.getStorageAPI().getObject(partName, PastObjectTypes.PARTOFNAME);

		if (obj != null && obj.getType().equals(PastObjectTypes.PARTOFNAME)) {

			PartOfFullNames partObj = (PartOfFullNames) obj;

			for (String username : partObj.getUsernames()) {

				Username usernameObj;
				try {
					usernameObj = getUsernameObject(username);
				} catch (ConfChatException e) {
					continue;
				}
				out.add(usernameObj);
			}
		}
		return out;

	}




	/**
	 * If friend is offline, save the request at his username object. Else try send
	 * request to him. If he doesn't ack, save at his username object
	 */
	public void sendFriendRequest(String username) throws ConfChatException, NotLoggedInException {
		// Create my friend instance to Send to Destination
		final Friend me = new Friend(getMe().getUsername(), getMe().getFullName());

		if (me.getUsername().equals(username)) {
			throw new ConfChatException(ExceptionEnum.selfFriend);
		}
		if (getMe().isMyFriend(username)) {
			throw new ConfChatException(ExceptionEnum.alreadyFriend);
		}

		// Get Friend location
		final Username friendUsername;
		if ((friendUsername = getUsernameObject(username)) == null) {
			throw new ConfChatException(ExceptionEnum.invalidUsername);
		}
		friendUsername.addFriendRequest(me);

		NodeHandle friendHandle;
		try {
			friendHandle = friendUsername.getUserNode();
		} catch (UserOfflineException e1) {
			// is offline, store in object again
			confchat.getStorageAPI().insertObject(friendUsername.getUsername(), friendUsername);
			logger.info("User is offline, friend request has been saved");
			return;
		}
		DeliveryNotification notification = new DeliveryNotification() {
			@Override
			public void sent(MessageReceipt arg0) {
				confchat.addPendentNotifications("Friend request delivered");
			}

			@Override
			public void sendFailed(MessageReceipt arg0, Exception arg1) {
				while (true) {
					try {
						confchat.addPendentNotifications("User " + friendUsername + " is offline");
						confchat.getStorageAPI().insertObject(friendUsername.getUsername(), friendUsername);
						confchat.getUserManager().setFriendOfflineHandler(friendUsername.getUsername());
						break;
					} catch (ConfChatException e) {
					}
				}

				confchat.addPendentNotifications("Friend request has NOT delivered, but has been saved");
			}
		};
		super.routeMsgDirect(friendHandle, new FriendMsg(me, MsgType.FriendRequest), notification);

		logger.info("Friend Request sent");
	}

	/**
	 * Extract the friend request, move it to my friends and notify destination
	 */
	public void acceptFriendRequest(String username) throws ConfChatException, NotLoggedInException {
		if (getMe().isMyFriend(username)) {
			throw new ConfChatException(ExceptionEnum.alreadyFriend);
		}
		Friend friend = getMe().extractFriendRequest(username);
		getMe().addFriend(friend);

		// Get Friend location
		final Username friendUsername;
		if ((friendUsername = getUsernameObject(username)) == null) {
			throw new ConfChatException(ExceptionEnum.invalidUsername);
		}

		Friend me = new Friend(getMe().getUsername(), getMe().getFullName());
		friendUsername.addAcceptedFriendRequest(me);


		NodeHandle friendHandle = null;
		try {
			friendHandle = friendUsername.getUserNode();
		} catch (UserOfflineException e1) {
			confchat.getStorageAPI().insertObject(friendUsername.getUsername(), friendUsername);
			logger.info("User is offline, friend accepted notification has been saved");
			return;
		}

		DeliveryNotification notification = new DeliveryNotification() {
			@Override
			public void sent(MessageReceipt arg0) {
				confchat.addPendentNotifications("Friend accepted notification delivered");
			}

			@Override
			public void sendFailed(MessageReceipt arg0, Exception arg1) {
				boolean inserted = false;
				while (!inserted) {
					try {
						confchat.getStorageAPI().insertObject(friendUsername.getUsername(), friendUsername);
						inserted = true;
					} catch (ConfChatException e) {
					}

				}
				confchat.getUserManager().setFriendOfflineHandler(friendUsername.getUsername());
				confchat.addPendentNotifications("Friend accepted notification has NOT delivered, but has been saved");
			}
		};
		super.routeMsgDirect(friendHandle, new FriendMsg(me, MsgType.FriendRequestAccepted), notification);

		updateOnlineFriends();
		logger.info(username + " is now your friend");
	}

	/**
	 * Accept all friends Requests
	 */
	public void acceptALLFriendRequest() throws NotLoggedInException, ConfChatException {
		ArrayList<Friend> requests = getMe().getFriendsRequests();
		for (Friend request : requests) {
			acceptFriendRequest(request.getUsername());
		}
	}


	/**
	 * Process all received message - Friend Requests: Add friend request to queue and
	 * notify user - Friend Request accepted: We can add our friend
	 */
	@Override
	public void deliver(Id id, Msg message) {

		try {
			getMe();
			MsgType type = message.getType();
			FriendMsg msg;
			Friend friend;
			switch (type) {
			case OnlineNotification:
				msg = (FriendMsg) message;
				friend = msg.getFriend();
				boolean online = friend.isOnline();

				getMe().getFriend(friend.getUsername()).setOnline(online);
				if (online) {
					confchat.addPendentNotifications(friend.getUsername() + " : " + friend.getFullName() + " - Is Online now");
				} else {
					confchat.addPendentNotifications(friend.getUsername() + " : " + friend.getFullName() + " - Is offline now");
				}
				break;
			case FriendRequest:
				msg = (FriendMsg) message;
				friend = msg.getFriend();
				getMe().addFriendRequest(friend);
				// saveMeState();
				confchat.addPendentNotifications("You have a new friend Request from: " + friend.getUsername() + " : " + friend.getFullName());
				break;
			case FriendRequestAccepted:
				msg = (FriendMsg) message;
				friend = msg.getFriend();
				getMe().addFriend(friend);
				// saveMeState();
				confchat.addPendentNotifications(friend.getUsername() + " : " + friend.getFullName() + " is now your friend");
				break;

			case InviteTopic:
				InviteTopic inviteTopic = (InviteTopic) message;
				String topicName = inviteTopic.getUniqueTopic().getTopicName();
				String uniqueTopicRoute = inviteTopic.getUniqueTopic().getUniqueTopicName();
				String friendName = inviteTopic.getFrom();

				confchat.getChatRoomManager().subscribeChatRoom(topicName, uniqueTopicRoute, friendName);
				confchat.addPendentNotifications(inviteTopic.getFrom() + " Invited you to participate " + "in ChatRoom with Topic: " + topicName);
				break;

			default:
				break;
			}

		} catch (NotLoggedInException e) {
			logger.info("User is not logged in");
			return;
		} catch (ConfChatException e) {
			logger.info(e.getMsg());
		}
	}


	/**
	 * Check if my friends are online or not, updates your friend list and store it
	 */
	public void updateOnlineFriends() throws NotLoggedInException {
		ArrayList<Friend> friendsList = getMe().getFriends();
		for (Friend friend : friendsList) {
			Username user;
			try {
				user = getUsernameObject(friend.username);
			} catch (ConfChatException e) {
				continue;
			}
			try {
				user.getUserNode();
				friend.setOnline(true);

			} catch (UserOfflineException e) {
				friend.setOnline(false);
			}
		}
		saveMeState();
	}


	/** Set our friend OFFLINE, we detected that he fails */
	public void setFriendOfflineHandler(String username) {
		ArrayList<Friend> friendsList;
		try {
			friendsList = getMe().getFriends();
		} catch (NotLoggedInException e1) {
			// If not logged in, ignore this
			return;
		}
		for (Friend friend : friendsList) {
			Username user;
			try {
				user = getUsernameObject(friend.username);
			} catch (ConfChatException e) {
				continue;
			}
			if (friend.username.equals(username)) {
				user.setUserOffline();
			}
		}
		saveMeState();
	}








	/**
	 * -------------------------------------------------------------------------------
	 * Helper, Get & Set
	 * -------------------------------------------------------------------------------
	 * 
	 * @throws ConfChatException
	 */

	public Username getUsernameObject(String username) throws ConfChatException {

		PastObject obj = confchat.getStorageAPI().getObject(username, PastObjectTypes.USERNAME);

		if (obj != null && obj.getType().equals(PastObjectTypes.USERNAME)) {
			Username usernameObj = (Username) obj;
			return usernameObj;
		} else {
			throw new ConfChatException(ExceptionEnum.invalidUsername);
		}
	}






	private String hashString(String str) {
		MessageDigest md;
		byte byteData[];
		try {
			md = MessageDigest.getInstance("SHA-256");
			md.update(str.getBytes());
			byteData = md.digest();

			// convert the byte to hex format
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < byteData.length; i++) {
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
			}
			// Slogger.debug("Hex format : " + sb.toString());

			return sb.toString();

		} catch (NoSuchAlgorithmException e) {
			// this exception is never thrown
			logger.error("his exception is never thrown");
		}
		return "";
	}


	// Getters and Setters
	public Username getMe() throws NotLoggedInException {
		if (me == null) {
			throw new NotLoggedInException();
		}
		synchronized (me) {
			return me;
		}
	}

	public void setMe(Username me) {
		this.me = me;
	}




	/**
	 * List Current Friends
	 * 
	 * @return Friends Array List
	 * @throws NotLoggedInException
	 */
	public ArrayList<Friend> listFriends() throws NotLoggedInException {
		return getMe().getFriends();
	}



	/**
	 * List users whose request your friendship
	 * 
	 * @return Array of friends
	 * @throws NotLoggedInException
	 */
	public ArrayList<Friend> listFriendRequest() throws NotLoggedInException {
		return getMe().getRequestFriendsList();
	}


	public void saveMeState() {
		Username myUsername;
		try {
			myUsername = getMe();
			NodeHandle myHandle = confchat.getPastryNode().getLocalHandle();
			myUsername.setNodeHandle(myHandle);

			// Force to save it
			while (true) {
				try {
					confchat.getStorageAPI().insertObject(myUsername.getUsername(), myUsername);
					break;
				} catch (ConfChatException e) {
				}
			}
		} catch (NotLoggedInException e) {
			logger.debug("Not logged in");
		}

	}


}
