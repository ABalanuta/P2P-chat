package project.MsgSenders.msgTypes;

import project.MsgSenders.Msg;
import project.MsgSenders.MsgType;
import rice.p2p.commonapi.Id;

public class IDRoutedMsg extends
        Msg {

    private static final long serialVersionUID = -3522917750191143661L;

    private final String message;
    private final String username;
    private final Id from;
    private final Id to;


    public IDRoutedMsg(Id from, Id to, String msg, String username) {
        super(MsgType.IDRouterMsg);
        this.from = from;
        this.to = to;
        this.message = msg;
        this.username = username;
    }

    @Override
    public String toString() {
        return "Simple Message from: " + username + " MSG: " + message;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }


}
