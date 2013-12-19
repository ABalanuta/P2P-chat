package project.storage.util;


import org.apache.log4j.Logger;

import project.storage.StorageAPI;
import project.storage.Objects.Username;
import rice.Continuation;


@SuppressWarnings("rawtypes")
public class ObjectContinuation implements Continuation {

	static Logger logger = Logger.getLogger(StorageAPI.class);

	private boolean ready = false;
	private Object result = null;
	
	public ObjectContinuation() {
	}


	@Override
	public void receiveException(Exception arg0) {

		logger.debug("Not Recevied Local identified Object");
		ready = true;
		
	}

	@Override
	public void receiveResult(Object arg0) {
		if(arg0 instanceof Username){
			logger.debug("Retrived Username: "+((Username)arg0).getFullName());
			result = arg0;
		}
		ready = true;
	}


	public boolean isReady() {
		return this.ready;
	}


	public Object getResult() {
		return result;
	}
	
}
