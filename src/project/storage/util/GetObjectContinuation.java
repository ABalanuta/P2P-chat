package project.storage.util;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import project.storage.StorageAPI;
import project.storage.Objects.PastObject;
import project.storage.Objects.PastObjectHandle;
import rice.Continuation;
import rice.p2p.past.Past;
import rice.p2p.past.PastContentHandle;


@SuppressWarnings("rawtypes")
public class GetObjectContinuation
implements Continuation {

	static Logger logger = Logger.getLogger(StorageAPI.class);

	static final Integer PARALEL_QUERIES = 3; // number of queries in parallel
	static final Integer TIMEOUT_TIME = 25; // time after wich we desist from the query

	private final Past storage;
	private final ArrayList<PastObjectHandle> array;
	private ArrayList<FetchObjectContinuation> fetchArray = null;
	private ArrayList<PastObjectHandle> fetchPohArray = null;

	private boolean ready = false;
	private boolean failed = false;
	private boolean isEmpty = false;
	private PastObject result = null;

	
	public GetObjectContinuation(Past st) {
		this.storage = st;
		array = new ArrayList<PastObjectHandle>();
		fetchArray = new ArrayList<FetchObjectContinuation>();
		fetchPohArray = new ArrayList<PastObjectHandle>();
	}

	@Override
	public void receiveResult(Object o) {

		logger.debug("Responses size:"+((PastContentHandle[]) o).length);

		// add objects to array
		for (PastContentHandle pch : (PastContentHandle[]) o) {
			array.add((PastObjectHandle) pch);
		}

		// --Find the object with the highest Object version --//
		long maxVer = -1;
		Integer nullResponses = 0;
		
		for (PastObjectHandle poh : array) {

			// saves the lastest Object
			if (poh != null && poh.getVersion() > maxVer) {
				maxVer = poh.getVersion();
			}
			
			if (poh == null){
				logger.debug("NULL Response");
				nullResponses++;
			}
		}

		// if all null report as inexistent Object
		if(nullResponses == array.size()){
			this.isEmpty = true;
			this.ready = true;
			return;
		}


		// saves all the handles that have the max version
		ArrayList<PastObjectHandle> toReturn = new ArrayList<PastObjectHandle>();
		
		for(PastObjectHandle poh : array) {
			if (poh != null && poh.getVersion() == maxVer) {
				toReturn.add(poh);
			}
		}

		//For Debug
		for(PastObjectHandle poh : toReturn){
			logger.debug("Max Version found was "+maxVer+" at "+poh.getNodeHandle()+" "+poh.toString());
		}

		
		// -- Fetch from at least $PARALEL_QUERIES diferent nodes that have the max object


		for(PastObjectHandle poh : toReturn){

			// break if already have $PARALEL_QUERIES hosts
			if(fetchArray.size() == PARALEL_QUERIES)
				break;

			if(fetchPohArray.contains(poh)){
				logger.debug("Repeated Host ignoring");
				
			}else{
				FetchObjectContinuation fetchCont = new FetchObjectContinuation();
				storage.fetch(poh, fetchCont);
				fetchArray.add(fetchCont);
				fetchPohArray.add(poh);
				logger.debug("Added "+poh.getNodeHandle()+"to inquery List");
			}
		}
		ready = true;
		return;
	}


	@Override
	public void receiveException(Exception e) {

		logger.debug("Oops! An error occurred: getting the object Handlers "+e.toString());
		failed = true;
		ready = true;
	}

	public boolean isReady() {
		return ready;
	}

	public boolean hasFailed(){			
		return failed;
	}

	public boolean isEmpty(){
		return isEmpty;
	}


	public PastObject getResult() {


		if(isEmpty){
			return null;
		}

		if(fetchArray == null){
			failed = true;
			return null;
		}

		boolean fetchdOne = false;
		FetchObjectContinuation returned = null;
		long time = System.currentTimeMillis();

		while(!fetchdOne){

			//if not responded in $TIMEOUT_TIME then node Failed
			if((time+TIMEOUT_TIME*1000) < System.currentTimeMillis()){
				logger.debug("Waited to much for the response. Quiting...");
				failed = true;
				ready = true;
				return null;
			}

			// verifies if receved any response			
			for(FetchObjectContinuation f : fetchArray){
				if(f.isReady()){
					fetchdOne = true;
					returned = f;
					continue;
				}
			}
			try {
				Thread.sleep(200);
				logger.debug(".");
			} catch (InterruptedException e) {
				logger.debug("InterruptedException Failure");
			}
		}

		result = returned.getResult();
		logger.debug("Fetched: "+result.getType()+" version"+result.getVersion());
		return result;
	}

}
