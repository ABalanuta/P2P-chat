package project.storage.util;

public class LocalStatisticsObject {

	private Integer numbOfLocalStoredUsers;
	private Integer numbOfLocalMessagesToBeDelivered;
	
	public LocalStatisticsObject(Integer numUsers, Integer numMessages){
		this.numbOfLocalStoredUsers = numUsers;
		this.numbOfLocalMessagesToBeDelivered = numMessages;
	}

	public Integer getNumbOfLocalStoredUsers() {
		return numbOfLocalStoredUsers;
	}

	public Integer getNumbOfLocalMessagesToBeDelivered() {
		return numbOfLocalMessagesToBeDelivered;
	}

}
