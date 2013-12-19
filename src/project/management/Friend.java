package project.management;

import java.io.Serializable;


public class Friend
        implements Serializable, Comparable<Friend> {
    private static final long serialVersionUID = 1L;
    String username;
    String fullName;
    boolean online;


    public Friend(String username, String fullName, boolean online) {
        super();
        this.username = username;
        this.fullName = fullName;
        this.online = online;
    }


    public Friend(String username, String fullName) {
        super();
        this.username = username;
        this.fullName = fullName;
        this.online = true;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public Friend clone() {
        return new Friend(username, fullName);
    }


    @Override
    public String toString() {
        return "Friend [username=" + username + ", fullName=" + fullName + ", online=" + online + "]";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
        result = prime * result + (online ? 1231 : 1237);
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }





    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Friend other = (Friend) obj;


        if (fullName == null) {
            if (other.fullName != null)
                return false;
        } else if (!fullName.equals(other.fullName))
            return false;
        if (online != other.online)
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }



    @Override
    public int compareTo(Friend o) {
        return username.compareTo(o.username);
    }





}
