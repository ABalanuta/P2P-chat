package project.MsgSenders.msgTypes;

import project.MsgSenders.Msg;
import project.MsgSenders.MsgType;


public class UsernameMsg extends
        Msg {

    private static final long serialVersionUID = 1L;
    /**
     * Username of requester
     */
    private final String username;

    public UsernameMsg(String username, MsgType type) {
        super(type);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }




}
