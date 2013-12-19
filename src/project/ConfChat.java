package project;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import project.admin.AdminStatistics;
import project.admin.StatisticsSummary;
import project.exception.ConfChatException;
import project.exception.NotLoggedInException;
import project.management.UserManager;
import project.multiPersonChat.ChatRoomManager;
import project.storage.StorageAPI;
import project.twoPersonChat.ChatOneToOne;
import rice.environment.Environment;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;


public class ConfChat {
	static Logger logger = Logger.getLogger(ConfChat.class);

	// Loads pastry settings
	private final Environment env = new Environment();
	private PastryNode pastryNode;
	private StorageAPI storageAPI;
	private ChatRoomManager chatRoomManager;
	private UserManager userManager;
	private ChatOneToOne chatOneToOne;
	private AdminStatistics admin;

	private ArrayList<String> pendentNotifications;
	private boolean master;


	public ConfChat(String bindPort, String bootstrapIP, String bootstrapPort) throws IOException, InterruptedException {
		PastrybootStrap(bindPort, bootstrapIP, bootstrapPort);
		restartPendentNotifications();
	}

	private void PastrybootStrap(String bindPortS, String bootstrapIP, String bootstrapPort) throws IOException, InterruptedException {
		// disable the UPnP setting (in case you are testing this on a NATted LAN)
		env.getParameters().setString("nat_search_policy", "never");

		// the port to use locally
		int bindport = Integer.parseInt(bindPortS);

		if (bindPortS.equals(bootstrapPort)) {
			this.master = true;
		} else {
			this.master = false;
		}


		// build the bootaddress from the command line args
		Inet4Address bootaddr = (Inet4Address) Inet4Address.getByName(bootstrapIP);
		int bootport = Integer.parseInt(bootstrapPort);
		InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

		// Generate the Node Id Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);
		// construct a node, but this does not cause it to boot
		pastryNode = factory.newNode();

		// Start the Apps
		startModulus();

		pastryNode.boot(bootaddress);

		// the node may require sending several messages to fully boot into the ring
		synchronized (pastryNode) {
			while (!pastryNode.isReady() && !pastryNode.joinFailed()) {
				// delay so we don't busy-wait
				pastryNode.wait(200);

				// abort if can't join
				if (pastryNode.joinFailed()) {
					logger.warn("Could not join the Chat ring: ConfChat.java");
					throw new IOException("Could not join the Chat ring.");
				}
			}
		}
		logger.debug("Finished creating new node " + pastryNode);
		// Start admin gossip
		if (master) {
			// Only master node launch new increment
			admin.start();
			userManager.start();
		}
	}

	private void startModulus() throws IOException {


		// disable the UPnP setting (in case you are testing this on a NATted LAN)
		pastryNode.getEnvironment().getParameters().setString("nat_search_policy", "never");
		
//		// make replication maintenece faster for tests
//		pastryNode.getEnvironment().getParameters().setInt("p2p_replication_maintenance_interval",200);
//		pastryNode.getEnvironment().getParameters().setInt("p2p_replication_max_keys_in_message",1000);

//		// # the time before it will retry a route that was already found dead
//		pastryNode.getEnvironment().getParameters().setInt("pastry_socket_srm_check_dead_throttle", 300000);
		
		// # how many pings until we call the node faulty
		pastryNode.getEnvironment().getParameters().setInt("pastry_socket_scm_num_ping_tries", 1);
//		pastryNode.getEnvironment().getParameters().setInt("p2p_past_messageTimeout", 30000);
//		pastryNode.getEnvironment().getParameters().setFloat("p2p_past_successfulInsertThreshold", (float) 0.5);

		// #time for a subscribe fail to be thrown (in millis)
//		pastryNode.getEnvironment().getParameters().setInt("p2p_scribe_message_timeout", 30000);


		



		storageAPI = new StorageAPI(pastryNode);
		userManager = new UserManager(this);
		setChatOneToOne(new ChatOneToOne(this));
		chatRoomManager = new ChatRoomManager(this);
		admin = new AdminStatistics(this, master);
	}




	/**
	 * Logout user
	 */
	public void logout() throws ConfChatException, NotLoggedInException {
		chatRoomManager.unsubscribeFromAll();
		userManager.logout();
	}



	public void finishConfChat() {

		logger.info("Exiting.....");

		// Finish all chatrooms
		try {
			chatRoomManager.unsubscribeFromAll();
			logout();
		} catch (ConfChatException e1) {
			logger.debug(e1.getMessage());
		} catch (NotLoggedInException e2) {
			logger.debug("user wasn't logged in");
		}


		admin.finishApp();


		logger.debug("Destry env");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}

		logger.debug("Destry env");
		this.getEnv().destroy();

		while (admin.isAlive()) {
			logger.debug("Admin not alive. Wait");
		}
		logger.info("Goodbye ;) ");
	}


	public StatisticsSummary showStatistics() throws ConfChatException {
		return admin.showStatistics();
	}






	/******************** Set & Get Methods ********************************/
	public Environment getEnv() {
		return env;
	}

	public AdminStatistics getAdmin() {
		return admin;
	}

	public PastryNode getPastryNode() {
		return pastryNode;
	}

	public StorageAPI getStorageAPI() {
		return storageAPI;
	}


	public ChatRoomManager getMultiPersonChat() {

		synchronized (chatRoomManager) {
			return chatRoomManager;
		}
	}

	public UserManager getUserManager() {
		synchronized (userManager) {
			return userManager;
		}
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getPendentNotifications() {
		return (ArrayList<String>) this.pendentNotifications.clone();
	}

	public void setPendentNotifications(ArrayList<String> pendentNotifications) {
		synchronized (this.pendentNotifications) {
			this.pendentNotifications = pendentNotifications;
		}
	}

	public void addPendentNotifications(String notification) {
		synchronized (this.pendentNotifications) {
			this.pendentNotifications.add(notification);
		}
	}


	public void restartPendentNotifications() {
		pendentNotifications = new ArrayList<String>();
	}


	public ChatRoomManager getChatRoomManager() {
		return this.chatRoomManager;
	}

	/**
	 * @return the chatOneToOne
	 */
	 public ChatOneToOne getChatOneToOne() {
		 return chatOneToOne;
	 }

	 /**
	  * @param chatOneToOne the chatOneToOne to set
	  */
	 public void setChatOneToOne(ChatOneToOne chatOneToOne) {
		 this.chatOneToOne = chatOneToOne;
	 }

}
