package project.storage.Objects;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.PastContentHandle;

@SuppressWarnings("serial")
public class PastObjectHandle implements PastContentHandle {

    // these two encode the id and replica this handle is for
    protected Id id;
    protected NodeHandle nodeHandle;
    protected long version;

    public PastObjectHandle(Id id2, NodeHandle localNodeHandle, long ver) {
		this.id = id2;
		this.nodeHandle = localNodeHandle;
		this.version = ver;
	}
    
	public Id getId() { return id; }
	
    public NodeHandle getNodeHandle() { return nodeHandle; }
    
    public long getVersion(){
    	return this.version;
    }

    
 }