package project.storage;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import project.exception.ConfChatException;
import project.exception.ExceptionEnum;
import project.statisticsForGraphs.LoggerForGraphs;
import project.storage.Objects.PastObject;
import project.storage.Objects.PastObjectTypes;
import project.storage.Objects.Username;
import project.storage.util.GetObjectContinuation;
import project.storage.util.LocalStatisticsObject;
import project.storage.util.ObjectContinuation;
import project.storage.util.PutObjectContinuation;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdSet;
import rice.p2p.past.Past;
import rice.p2p.past.PastImpl;
import rice.pastry.PastryNode;
import rice.pastry.commonapi.PastryIdFactory;
import rice.persistence.LRUCache;
import rice.persistence.MemoryStorage;
import rice.persistence.Storage;
import rice.persistence.StorageManagerImpl;

public class StorageAPI<R> {
	static Logger logger = Logger.getLogger(StorageAPI.class);


	private final Past past;
	private final PastryIdFactory localFactory;
	private final Integer REPLICAS = 4;
	private final Integer READ_RETRIES_TIMES = 3;
	private final Integer WRITE_RETRIES_TIMES = 5;
	private final StorageManagerImpl storageManager;


	/**
	 * Constructor of the Class
	 */
	public StorageAPI(PastryNode node) {

		// used for generating PastContent object Ids.
		// this implements the "hash function" for our DHT
		localFactory = new rice.pastry.commonapi.PastryIdFactory(node.getEnvironment());

		// memory Storage
		Storage stor = new MemoryStorage(localFactory);
		// memory for cache
		Storage memStor = new MemoryStorage(localFactory);
		StorageManagerImpl smi = new StorageManagerImpl(localFactory, stor, new LRUCache(memStor, 512 * 1024, node.getEnvironment()));
		Past app = new PastImpl(node, smi, REPLICAS, "");
		this.storageManager = smi;
		this.past = app;
	}


	/**
	 * Method that retrive a object from the Past DHT
	 * 
	 * @param id The objects Id to retreve
	 * @return return the object if it exists , returns null otherwise
	 */
	public PastObject getObject(String id, PastObjectTypes type) {
		logger.debug("RETRIVAL-----------------------------------------INIT------------------------------------");
		
		Date startTime = new Date(); // For Graphs
		
		// generates a key based on the given string
		Id key = localFactory.buildId(id + type.toString());

		// Initialize the return vale
		PastObject retrived = null;

		Integer retries = 0;

		while (retries < READ_RETRIES_TIMES){

			logger.debug("Retriving try number "+retries);

			// Create a new Continuation Command witch will later fetch the latest result
			GetObjectContinuation command = new GetObjectContinuation(past);

			try{
				
				past.lookupHandles(key, REPLICAS, command);
				
			}catch(Exception e){
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {}
				retries++;
				continue;
			}
			
			// wait until results are in
			while (!command.isReady()) {
				try {
					logger.debug("-");
					Thread.sleep(200);
					//past.getEnvironment().getTimeSource().sleep(500);
				} catch (InterruptedException e) {}
			}

			if(command.isEmpty()){
				logger.debug("Object Does Not Exist");
				logger.debug("RETRIVAL-----------------------------------------END------------------------------------");
				return null;
			}

			retrived = command.getResult();

			if(command.hasFailed()){
				logger.debug("Retriving failed retrying...");
				retries++;
				continue;
			}

			if (retrived != null) {
				logger.debug("Retrived Object : " + retrived.getId().toString() + " at version: " + retrived.getVersion());
				break;
			}

		}
		
		//For Graphs
		Date finishTime = new Date();
		LoggerForGraphs.logStorageLookUp(startTime, finishTime);
		logger.debug("RETRIVAL-----------------------------------------END------------------------------------");
		return retrived;
	}



	/**
	 * Inserts a Object in the DHT, if the object already exists, it updates the Object
	 * version and overwrites the old value.
	 * 
	 * @param id the id under witch the object will be stored
	 * @param pastObject the object to be stored
	 * @return returns true if it stored the object more than one time, returns false
	 *         otherwise
	 * @throws ConfChatException 
	 */
	@SuppressWarnings("unchecked")
	public boolean insertObject(String id, PastObject pastObject) throws ConfChatException {

		PastObject insertObject = pastObject;
		Date startTime = new Date(); //For Graphs
		logger.debug("INSERTION-----------------------------------------INIT------------------------------------");

		Id key = localFactory.buildId(id + insertObject.getType().toString());
		insertObject.setId(key);
		Integer retries = 0;
		long newVersion = 0;

		// if objects already Exists gets its Version Number
		PastObject last = getObject(id, insertObject.getType());
		if (last != null) {
			logger.debug("LastObject Found was "+last.getType()+" at version "+last.getVersion());
			// update the object Version
			newVersion = last.getVersion() + 1;
			insertObject.setVersion(newVersion);
		}

		while (retries < WRITE_RETRIES_TIMES){

			logger.debug("Inserting version: " + insertObject.getVersion() + " of the object: " + insertObject.getId());

			// Create a new Continuation Command witch will later fetch the lastest result
			PutObjectContinuation command = new PutObjectContinuation();

			// insert the data
			past.insert(insertObject, command);

			// Waits for all the responses
			while (!command.isReady()) {
				try {
					logger.debug("*");
					Thread.sleep(200);
					//past.getEnvironment().getTimeSource().sleep(500);
				} catch (InterruptedException e) {
				}
			}

			//if insertion filed retry
			if(command.hasFailed()){
				logger.info("Failed insertion.");
				continue;
			}


			logger.debug("--------------------Verification: object Inserted ?-------------");
			PastObject confirm = getObject(id, insertObject.getType());
			
			if (confirm != null) {
				
				//if object inserted Continue else retry
				if(confirm.getVersion() >= newVersion){
					logger.debug("---------Correctly Stored---- "+ confirm.getId()+" "+confirm.getType()+"------------------");
					logger.debug("INSERTION-----------------------------------------END------------------------------------");
					//For Graphs
					Date finishTime = new Date();
					LoggerForGraphs.logStoragePut(startTime, finishTime);
					return true;
				}				
			}
			
			//Object Wasnt Writen Try Again
			if(retries < WRITE_RETRIES_TIMES) {
				logger.debug("---------Retrying Insertion n:"+retries+"--object-- inserted "+command.getStores()+" times");
				retries++;
				continue;
			}
			
			
			logger.debug("Error Storing Object");
			throw new ConfChatException(ExceptionEnum.storageError);
		}
		

		
		logger.debug("INSERTION-----------------------------------------END------------------------------------");
		return false;
	}


	public LocalStatisticsObject getNumberOfUsersRegisteredInLocalDatabase() {


		IdSet ids = storageManager.scan();
		ObjectContinuation cont = null;
		Integer numberUsers = 0;
		Integer numberMessageToDeliver = 0;


		for (Id tempId : ids.asArray()) {

			storageManager.getObject(tempId, cont = new ObjectContinuation());

			while (!cont.isReady()) {
				// waits for the answer
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					logger.debug("Sleep Error Shoud not Happen");
				}
			}

			Object retrivedObject = cont.getResult();

			if (retrivedObject instanceof Username) {
				numberUsers++;
				numberMessageToDeliver += ((Username) retrivedObject).getMessagesReceivedWhileOffline().size();
			}
		}

		logger.debug("For Gossip: " + numberUsers + " Users storeed Localy " + numberMessageToDeliver + " stored to deliver");
		return new LocalStatisticsObject(numberUsers, numberMessageToDeliver);
	}

	public Integer getNumberOfReplicas() {
		return this.past.getReplicationFactor();
	}

}
