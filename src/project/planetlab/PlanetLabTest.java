package project.planetlab;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.Logger;

import project.ConfChatAPI;
import project.exception.ConfChatException;
import project.exception.NotLoggedInException;
import project.management.Friend;
import project.multiPersonChat.UniqueTopic;

public class PlanetLabTest {

	static Logger logger = Logger.getLogger(StartPlanetLabTest.class);
	String servername;
	private final ConfChatAPI iface;
	private boolean running = true;
	private long timeout;
	private ArrayList<Friend> frindendRequests = new ArrayList<Friend>();
	private boolean privateChatroomCreated = false;

	private ArrayList<String> otherServers = new ArrayList<String>();
	private ArrayList<String> serverDomains = new ArrayList<String>();
	private boolean bootServer;

	public PlanetLabTest(ConfChatAPI iface, String serverName, String serverListFileName, long finalTimer, boolean bootServ) {
		this.iface = iface;
		this.servername = serverName;
		this.timeout = finalTimer;
		this.bootServer = bootServ;



		// Loads the File with Servers
		try {
			load(serverListFileName);
		} catch (IOException e) {
			logger.info("Could no load File " + serverListFileName);
			return;
		}


		// Gets The Domains of The Servers
		for (String server : otherServers) {
			String[] splited = server.split("\\.");
			logger.debug(splited[splited.length - 1]);
			if (!serverDomains.contains(splited[splited.length - 1])) {
				serverDomains.add(splited[splited.length - 1]);
			}
		}
	}






	public void run() {

		MessageListener mListener = new MessageListener();
		mListener.start();
		try {
			registerMyself();
			Thread.sleep(3000);
			logIn();


			while (running) {
				
				long currentTime = System.currentTimeMillis();
				
				if (currentTime > timeout) {
					running = false;
					continue;
				}


				inviteFriends();
				Thread.sleep(1000);

				acceptFriendships();
				Thread.sleep(1000);

				sendMessagesToFriends();
				Thread.sleep(1000);

				chatRoomSimulation();
				Thread.sleep(1000);

				unsubscribefromRandomChatRoom();
				Thread.sleep(1000);

				logger.info(iface.show_status().toString());
				logger.info("I have "+iface.listFriends().size()+" friends");
				Thread.sleep(1000);
			}


			// Catch Exeptions
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.info("Invalid Command");
		} catch (StringIndexOutOfBoundsException e) {
			logger.info("Invalid Command");
		} catch (IllegalArgumentException e) {
			logger.info("Invalid Command");
		}

		catch (InterruptedException e) {
			logger.error("Run: Interruption Exeption Happen");
		}catch (ConfChatException e) {
			logger.error("Run: "+e.getMsg());
		} catch (NotLoggedInException e) {
			logger.error("Run: "+e.getMsg());
		}

		// Iterupts Auxialiry Threads
		mListener.interrupt();
		// Deleate Thread
		logger.info("End of Thread*");
		Thread.currentThread().interrupt();
	}



	@SuppressWarnings("unchecked")
	private void acceptFriendships() {
		// AcceptFriendships With Probability of 70%
		Random dice = new Random();
		ArrayList<Friend> friendsReq = null;

		try {

			friendsReq = (ArrayList<Friend>) iface.listFriendRequest().clone();
			Thread.sleep(1000);
			if(!friendsReq.isEmpty()){
				for (Friend fr : friendsReq) {
					if (dice.nextFloat() < 0.7){
						iface.acceptFriendRequest(fr.getUsername());
						Thread.sleep(1000);
						logger.info("Acepted " + fr.getUsername() + " Friendship Request");
					}
				}
			}


		}catch (ConfChatException e) {
			logger.info(e.getMsg());
		}catch (NotLoggedInException e1) {
			logger.info(e1.getMsg());
		} catch (InterruptedException e) {
			logger.info("FATAL  InterruptedException not Happen");
		}
	}

	@SuppressWarnings({ "deprecation"})
	private void sendMessagesToFriends() {
		// Send Message with Probability of 70%
		Random dice = new Random();
		ArrayList<Friend> friends = null;

		try {
			friends = iface.listFriends();
			Thread.sleep(1000);

			if (friends != null && friends.size() > 0) {
				
				for (Friend fr : friends) {

					if (dice.nextFloat() < 0.7) {
						iface.sendMessage(fr.getUsername(), "Hello my name is " + this.servername + " and time here is " + (new Date()).toGMTString());
						Thread.sleep(500);
						iface.sendMessage(fr.getUsername(), "What is you real Name? ;)");
						Thread.sleep(500);
					}
				}
			}

		} catch (ConfChatException e) {
			logger.info(e.getMsg());
		} catch (NotLoggedInException e) {
			logger.info(e.getMsg());
		} catch (InterruptedException e1) {
			logger.info("FATAL  InterruptedException cannot Happen");
		}
	}

	private void logIn() {
		try {
			/* With 70%, login now. Otherwise, wait 1s to 4s */
			if (bootServer) {
				iface.login(servername, "12345");
				Thread.sleep(1000);
			} else {
				long time = (long) (Math.random() * 4000) + 1000;
				logger.info("Waiting " + time / 1000 + " seconds");
				Thread.sleep(time);
				iface.login(servername, "12345");
				Thread.sleep(500);
			}
			logger.info("USER: "+servername+" Registered sucsecefully !!!!!");
		} catch (InterruptedException e) {
			logger.info("FATAL  InterruptedException not Happen");
		} catch (ConfChatException e) {
			logger.info("At LOGIN: "+e.getMsg());
		} catch (NotLoggedInException e) {
			logger.info("At LOGIN: "+e.getMsg());
		}

	}




	private void inviteFriends() {
		/** Try to Invite 3 friends from diferent domains list  with 60% success rate*/
		int counter = 0;
		Random dice = new Random();

		while (counter < 3) {
			try {	
				// random domain
				String domain = serverDomains.get(dice.nextInt(serverDomains.size()));
				logger.debug("Chosen Domain: " + domain);

				ArrayList<Friend> search = iface.searchByPartOfName(domain);
				logger.debug("Friend Search Results: " + search.toString());
				Thread.sleep(1000);

				if (!search.isEmpty() && dice.nextFloat()<0.60) {

					Friend user = search.get(dice.nextInt(search.size()));
					Thread.sleep(1000);

					// If he isn't my friend, if is not my self, if I didn't send request
					if (!(iface.listFriends().contains(user) || user.getUsername().equals(servername) || frindendRequests.contains(user))) {
						iface.sendFriendRequest(user.getUsername());
						Thread.sleep(1000);
						frindendRequests.add(user);
						logger.info("Invited " + user.getUsername() + " To be My Friend");
					}

				}
				counter++;
			} catch (ConfChatException e) {
				logger.info("Cannot " + e.getMsg());
			} catch (NotLoggedInException e) {
				logger.info("Friend not logged in" + e.getMsg());
			} catch (InterruptedException e) {
				logger.info("FATAL  InterruptedException not Happen");
			}
		}

	}



	/**
	 * Unsubscribe from 3 random ChatRooms with 40% Chance
	 */
	private void unsubscribefromRandomChatRoom() {

		try {
			Random dice = new Random();
			for(int x = 0 ; x < 2;x++){
				ArrayList<UniqueTopic> rooms = iface.listChatRoomTopics();
				Thread.sleep(1000);
				if(!rooms.isEmpty() && dice.nextFloat() < 0.40){
					UniqueTopic ut = rooms.get(dice.nextInt(rooms.size()));
					if(!ut.getTopicName().equals(servername)){
						iface.unsubscribeRoom(ut.getTopicName());
						Thread.sleep(1000);
					}
				}
			}
		} catch (NotLoggedInException e) {
			logger.info(e.getMsg());
		} catch (ConfChatException e) {
			logger.info(e.getMsg());
		} catch (InterruptedException e) {
			logger.info("FATAL  InterruptedException not Happen");
		}
	}

	/**
	 * Choose 3 random nodes to invite to chat 10 % cahnce
	 * Sends Six Messages to Random Chat Rooms
	 */
	private void chatRoomSimulation() {
		try {

			if (!privateChatroomCreated) {
				iface.createChatRoom(servername);
				privateChatroomCreated = true;
				Thread.sleep(1000);
			}

			/** Choose 3 random friends to invite to chat with 25% chance */
			ArrayList<Friend> friends = iface.listFriends();
			Thread.sleep(1000);
			Random dice = new Random();
			if(!friends.isEmpty()){
				for (int i = 0; i < 3; i++) {
					if (dice.nextFloat() > 0.10) {
						Friend friend = friends.get(dice.nextInt(friends.size()));
						ArrayList<UniqueTopic> unique = iface.listChatRoomTopics();
						Thread.sleep(1000);

						//adds only not already in the room
						for(UniqueTopic u : unique){
							if(u.getTopicName().equals(servername)){
								if(!u.getUsers().contains(friend.getUsername())){
									iface.inviteToChatRoom(friend.getUsername(), servername);
									Thread.sleep(1000);
								}
							}
						}

					}
				}
			}
			if(dice.nextFloat()>0.2){
				iface.sendMessageChatRoom(servername, "This Kingdom is Mine !!!");
				Thread.sleep(1000);
			}

			String[] msg = {"UFO Are Real !!!", "Skynet is Alive!!!", "Gangnam Style!!!", "EPIC WIN!!!", "WAAZZZAAAA", "Its LATE!"};

			//Send messages random Chat Rooms
			ArrayList<UniqueTopic> rooms = iface.listChatRoomTopics();
			if(!rooms.isEmpty()){
				for(int x = 0 ; x < 6;x++){
					UniqueTopic ut = rooms.get(dice.nextInt(rooms.size()));
					if(ut.getTopicName() != servername && dice.nextFloat()>0.25){
						iface.sendMessageChatRoom(ut.getTopicName(), msg[dice.nextInt(msg.length)]);
						Thread.sleep(1000);
					}
				}
			}


		} catch (NotLoggedInException e) {
			logger.info(e.getMsg());
		} catch (ConfChatException e) {
			logger.error(e.getMsg());
		} catch (InterruptedException e) {
			logger.info("FATAL  InterruptedException not Happen");
		}
	}



	/**
	 * Method Used to load a file with all machineNames
	 */
	private void load(String file_name) throws IOException {
		otherServers = new ArrayList<String>();

		FileInputStream fstream = new FileInputStream((new File(file_name)));
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
		String machine;
		while ((machine = buffer.readLine()) != null) {
			// add only if diferent from this machine
			if (!servername.equals(machine)) {
				otherServers.add(machine);
			}
		}
		buffer.close();

		logger.debug("OtherServers list");
		logger.debug(otherServers);

	}



	/**
	 * register this server into the Network
	 */
	private void registerMyself() {
		try {
			
			if(servername.contains(".")){
				logger.info("Registering with username: "+servername);
				iface.register(servername, "12345", servername.replace('.', ' '));
				Thread.sleep(1000);
			
			}else{
				iface.register(servername, "12345", servername);
				Thread.sleep(1000);
			}	
		} catch (ConfChatException e) {
			logger.debug("Error register");
		}catch (InterruptedException e) {
			logger.info("FATAL  InterruptedException not Happen");
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
