package project.storage.Objects;

import rice.p2p.commonapi.Id;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.PastException;

public class PastObject
        implements PastContent {

    private static final long serialVersionUID = 1L;
    protected Id id;
    private long version = 0;
    private PastObjectTypes type = PastObjectTypes.NOTDEFINED;


    public PastObject(PastObjectTypes type) {
        super();
        this.type = type;
    }

    public PastObjectTypes getType() {
        return type;
    }

    @Override
    public PastContent checkInsert(Id id, PastContent existing) throws PastException {
        // this implement overwriting behavior
        // logger.debug("REplacing"+existing.toString()+" with "+this.toString());
        return this;
    }


    @Override
    public Id getId() {
        return id;
    }

    public void setId(Id i) {
        this.id = i;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long ver) {
        this.version = ver;
    }


    // this is the method which is call when a handle is
    // requested. This is called on your object
    // *on the replica holder*. Here, you should build and return
    // an appropriate handle.
    @Override
    public PastContentHandle getHandle(Past local) {
        return new PastObjectHandle(getId(), local.getLocalNodeHandle(), this.version);
    }


    // disable caching of the objects
    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public String toString() {
        return this.getId().toString() + " at ver_" + this.getVersion();
    }
}
