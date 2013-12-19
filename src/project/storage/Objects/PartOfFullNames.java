package project.storage.Objects;

import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("serial")
public class PartOfFullNames extends
        PastObject {

    String partName;
    Set<String> usernames;


    public PartOfFullNames(String partName) {
        super(PastObjectTypes.PARTOFNAME);
        this.partName = partName;
        this.usernames = new TreeSet<String>();
    }


    public Set<String> getUsernames() {
        return usernames;
    }


    public void setUsernames(Set<String> usernames) {
        this.usernames = usernames;
    }

    public void addNewUsername(String username) {
        this.usernames.add(username);
    }
}
