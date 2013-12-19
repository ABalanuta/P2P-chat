package project.storage.util;

import org.apache.log4j.Logger;

import project.storage.StorageAPI;
import rice.Continuation;

@SuppressWarnings("rawtypes")
public class PutObjectContinuation
        implements Continuation {

    static Logger logger = Logger.getLogger(StorageAPI.class);

    private int stores = 0;
    private boolean ready = false;
    private boolean failed = false;

    public PutObjectContinuation() {
    }

    @Override
    public void receiveResult(Object result) {


        Boolean[] results = ((Boolean[]) result);
        int numSuccessfulStores = 0;

        for (int ctr = 0; ctr < results.length; ctr++) {
            if (results[ctr].booleanValue())
                numSuccessfulStores++;
        }
        stores = numSuccessfulStores;
        
        if(stores == 0){
        	failed = true;
        }
        
        ready = true;
    }

    @Override
    public void receiveException(Exception e) {
    	failed = true;
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }
    
    public boolean hasFailed() {
        return failed;
    }

    public int getStores() {
        return stores;
    }

}
