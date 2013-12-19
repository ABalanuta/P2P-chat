package project;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.log4j.Logger;

import project.exception.ConfChatException;
import project.exception.NotLoggedInException;
import project.management.Friend;
import project.multiPersonChat.UniqueTopic;

public class SimpleUI {

	static Logger logger = Logger.getLogger(ConfChatMain.class);

	ConfChatAPI iface = null;
	private boolean loading = false;
	BufferedReader buffer;
	boolean running = true;
	ArrayList<MessageTimer> mTimer;
	ArrayList<TopicTimer> tTimer;

	public SimpleUI(ConfChatAPI iface) {
		this.iface = iface;
	}


	public void start() {
		logger.info("\n Write help to show the list of Commands \n");

		// Prints Incomming Mesages
		MessageListener listener = new MessageListener();
		listener.start();

		tTimer = new ArrayList<TopicTimer>();
		mTimer = new ArrayList<MessageTimer>();

		// Get Commands Loop
		while (running) {

			// loads commands from a file(if any)
			String input = loadingFile();
			// reads from user input if no commands loaded
			if (input == null) {
				input = getUserInput();
			}
			selectMenu(input);
		}

		// Kills Message Threads
		for (MessageTimer m : mTimer) {
			m.interrupt();
		}

		// Kills Topic Threads
		for (TopicTimer t : tTimer) {
			logger.debug("Lets kill timer");
			t.interrupt();
		}

		logger.debug("Lets kill Listener");

		// Kills Listener
		listener.interrupt();
		logger.debug("DONE");
		return;
	}

	/**
	 * Reads a line of commands to execute
	 */
	private String loadingFile() {

		if (!loading)
			return null;

		String line = null;
		try {
			line = buffer.readLine();
		} catch (IOException e) {
			logger.error("Could not read from buffer");
		}

		if (line != null) {
			return line;
		} else {
			loading = false;
			return null;
		}
	}

	/**
	 * Selects the comand selected by the User
	 */
	private void selectMenu(String input) {

		String[] parsed = input.split(" ");

		try {
			if (parsed.length > 0) {
				if (parsed[0].length() == 0) {
					return;
				}

				MenuOption opt = MenuOption.valueOf(parsed[0]);
				switch (opt) {

				case exit:
					running = false;
					break;

				case show_status:
					logger.info(iface.show_status());
					break;

				case help:
					showCommandList();
					break;

				case login:
					iface.login(parsed[1], parsed[2]);
					break;

				case register:
					String fullname = input.substring(parsed[0].length() + parsed[1].length() + parsed[2].length() + 3);
					iface.register(parsed[1], parsed[2], fullname);
					break;

				case passwd:
					iface.changePassword(parsed[1], parsed[2], parsed[3]);
					break;

				case search_friends:
					String partnames = input.substring(parsed[0].length() + 1);
					ArrayList<Friend> users = iface.searchByPartOfName(partnames);
					for (Friend f : users) {
						logger.info("USername: " + f.getUsername() + "  FullName: " + f.getFullName());
					}
					break;

				case list_friends:
					ArrayList<Friend> users1 = iface.listFriends();
					logger.info(users1);
					break;

				case logout:
					iface.logout();
					break;

				case invite_friend:
					iface.sendFriendRequest(parsed[1]);
					break;

				case accept_request:
					iface.acceptFriendRequest(parsed[1]);
					break;

				case friend_requests:
					ArrayList<Friend> users2 = iface.listFriendRequest();
					logger.info(users2);
					break;

				case chat:
					String message = input.substring(parsed[0].length() + parsed[1].length() + 2);
					iface.sendMessage(parsed[1], message);
					break;

				case create:
					iface.createChatRoom(parsed[1]);
					break;

				case chat_room:
					String message2 = input.substring(parsed[0].length() + parsed[1].length() + 2);
					iface.sendMessageChatRoom(parsed[1], message2);
					break;

				case list_rooms:
					ArrayList<UniqueTopic> list = iface.listChatRoomTopics();
					for (UniqueTopic uniqueTopic : list) {
						String roomName = "Room[" + uniqueTopic.getTopicName() + "] UniqueID[" + uniqueTopic.getUniqueTopicName() + "]";
						logger.info(roomName + "\n \tUsers: " + uniqueTopic.getUsers() + "\n");
					}

					break;

				case invite_room:
					iface.inviteToChatRoom(parsed[1], parsed[2]);
					break;

				case unsubscribe:
					iface.unsubscribeRoom(parsed[1]);
					break;

				case load:
					this.load(parsed[1]);
					break;

				case sleep:
					try {
						Thread.sleep(Integer.parseInt(parsed[1]) * 1000);
					} catch (InterruptedException e) {
						logger.error("Sleep Error");
					}
					break;

				case msgtimer:
					MessageTimer m = new MessageTimer(parsed[1], parsed[2], Long.parseLong(parsed[3]), Long.parseLong(parsed[4]));
					m.start();
					mTimer.add(m);
					break;

				case topictimer:
					TopicTimer t = new TopicTimer(parsed[1], parsed[2], Long.parseLong(parsed[3]), Long.parseLong(parsed[4]));
					t.start();
					tTimer.add(t);
					break;

				default:
					logger.info("Command do not exists");
					break;
				}
			}

			// Catch Exeptions
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.info("Invalid Command");
		} catch (StringIndexOutOfBoundsException e) {
			logger.info("Invalid Command");
		} catch (IllegalArgumentException e) {
			logger.info("Invalid Command");
		} catch (ConfChatException e1) {
			logger.info(e1.getMsg());
		} catch (NotLoggedInException e1) {
			logger.info(e1.getMsg());
		}

	}


	/**
	 * Method used to get User Input
	 */
	private String getUserInput() {

		@SuppressWarnings("resource")
		Scanner scan = new Scanner(System.in);
		String userInput = scan.nextLine();
		return userInput;
	}

	/**
	 * Method Used to load a file with Comands
	 */
	private void load(String file_name) {
		loading = true;
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream((new File(file_name)));
		} catch (FileNotFoundException e) {
			logger.error("File Not Found !!!");
			loading = false;
			buffer = null;
		}
		DataInputStream in = new DataInputStream(fstream);
		buffer = new BufferedReader(new InputStreamReader(in));
	}


	/**
	 * Shows the Help Menu to the User
	 */
	private void showCommandList() {
		logger.info("\n System Menu:");

		logger.info("\n---User---");
		logger.info("login <username> <password>");
		logger.info("register <username> <password> <fullname>");
		logger.info("passwd <username> <old password> <new password>");
		logger.info("logout");
		logger.info("exit");

		logger.info("\n---Friends---");
		logger.info("search_friends <part_of_name>");
		logger.info("list_friends");
		logger.info("invite_friend <username>");
		logger.info("friend_requests");
		logger.info("accept_request <username>");
		logger.info("chat <username> <message>");

		logger.info("\n---ChatRoom---");
		logger.info("chat_room <topic> <message>");
		logger.info("create <chat_room_topic>");
		logger.info("unsubscribe <topic>");
		logger.info("invite_room <username> <topic>");
		logger.info("list_rooms");

		logger.info("\n---Statistics---");
		logger.info("show_status  (show admin statistics)");


		logger.info("\n---Misc---");
		logger.info("load <filename>");
		logger.info("msgtimer <username> <message> <interval in miliseconds>  <number of messages>");
		logger.info("topictimer <topic> <message> <interval in miliseconds> <number of messages>");
	}


	/**
	 * e Private Class that is used to send messages to a specific Person
	 */
	private class MessageTimer extends
	Thread {

		private final String destination;
		private final String message;
		private final long timer;
		private long count = 0;
		private final long times;

		public MessageTimer(String username, String message, long time, long times) {
			this.destination = username;
			this.message = message;
			this.timer = time;
			this.times = times;
		}

		@Override
		public void run() {
			while (count < times) {
				try {
					iface.sendMessage(destination, message);
					count++;
					Thread.sleep(timer);
				} catch (NotLoggedInException e1) {
					logger.info(e1.getMsg());
					break;
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt(); // very important
					break;
				} catch (Exception e) {
					break;
				}
			}
		}
	}




	/**
	 * Private Class that is used to send messages to a specific ChatRoom
	 */
	class TopicTimer extends
	MessageTimer {

		public TopicTimer(String topic, String message, long time, long times) {
			super(topic, message, time, times);
		}

		@Override
		public void run() {
			while (super.count < super.times) {
				try {
					iface.sendMessageChatRoom(super.destination, super.count + " " + super.message);
					super.count++;
					Thread.sleep(super.timer);
				} catch (NotLoggedInException e1) {
					logger.info(e1.getMsg());
					break;
				} catch (ConfChatException e1) {
					logger.info(e1.getMsg());
					break;
				} catch (InterruptedException e) {
					logger.debug("TopicTimer: KillingThread");
					Thread.currentThread().interrupt(); // very important
					logger.debug("TIMER DEAD");
					break;
				} catch (Exception e) {
					break;
				}
			}
		}
	}


	/**
	 * Private Class that is used to Listen for new Messages and Prints Them
	 */
	class MessageListener extends
	Thread {

		@Override
		public void run() {

			while (true) {

				for (String notification : iface.getPendentNotifications()) {
					logger.info(notification);
				}
				iface.restartPendentNotifications();
				try {
					Thread.sleep(1000);

				} catch (InterruptedException e) {
					logger.debug("MessageListener: KillingThread");
					Thread.currentThread().interrupt(); // very important
					break;
				}

			}
		}
	}
}
