package project.MsgSenders.msgTypes;

import project.MsgSenders.Msg;
import project.MsgSenders.MsgType;
import project.management.Friend;

public class FriendMsg extends
        Msg {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    Friend friend;

    public FriendMsg(Friend friend, MsgType type) {
        super(type);
        this.friend = friend;
    }

    public Friend getFriend() {
        return friend;
    }

    public void setFriend(Friend friend) {
        this.friend = friend;
    }

}
