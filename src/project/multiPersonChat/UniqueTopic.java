package project.multiPersonChat;

import java.io.Serializable;
import java.util.ArrayList;

public class UniqueTopic
implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String topicName;
    private final String uniqueTopicName;
    private ArrayList<String> listOfUsers;
    private long currentGossipID;

	public UniqueTopic(String name, String uniqueName) {
		this.topicName = name;
		this.uniqueTopicName = uniqueName;
		listOfUsers = new ArrayList<String>();
		currentGossipID = 0;
	}




    public long getCurrentGossipID() {
        return currentGossipID;
    }


    public void setCurrentGossipID(long currentGossipID) {
        this.currentGossipID = currentGossipID;
    }


	public String getTopicName() {
		synchronized (topicName) {
			return topicName;
		}
	}


	public String getUniqueTopicName() {
		synchronized(uniqueTopicName){
			return uniqueTopicName;
		}
	}

	public void addUser(String username) {
		synchronized (listOfUsers) {
			if (!listOfUsers.contains(username)) {
				listOfUsers.add(username);
			}
		}
	}

	public void delUser(String username) {
		synchronized (listOfUsers) {
			if (listOfUsers.contains(username)) {
				listOfUsers.remove(username);
			}
		}
	}
	
	public void delUserAll(){
		synchronized (listOfUsers) {
			listOfUsers = new ArrayList<String>();
		}
	}

	public String printUsers() {
		String users = "";
		for (String u : this.listOfUsers) {
			users = users + u + ", ";
		}
		return users;
	}

	public ArrayList<String> getUsers() {
		synchronized(listOfUsers){
			return listOfUsers;
		}
	}



}
