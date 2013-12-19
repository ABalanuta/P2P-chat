package project.storage.util;

import org.apache.log4j.Logger;

import project.storage.StorageAPI;
import project.storage.Objects.PastObject;
import rice.Continuation;


public class FetchObjectContinuation implements Continuation{

	static Logger logger = Logger.getLogger(StorageAPI.class);
	private PastObject result = null;
	private boolean ready = false;
	private boolean failed = false;
	private boolean emptySpace = false;

	@Override
	public void receiveResult(Object o) {
		result = ((PastObject) o);
		logger.debug("Returning: "+result.getId()+" "+result.getType());
		ready = true;
	}

	@Override
	public void receiveException(Exception e) {
		if(e instanceof NullPointerException){
			emptySpace = true;
		}
		failed = true;
		ready = true;
		
	}


	public boolean isReady() {
		return ready;
	}
	
	public boolean hasFailed(){
		return failed;
	}

	public PastObject getResult() {
		return result;
	}
	
	public boolean isEmpty(){
		return emptySpace;
	}

}
