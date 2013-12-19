package project.MsgSenders.msgTypes;

import project.MsgSenders.Msg;
import project.MsgSenders.MsgType;

public class UpdateMsg extends
        Msg {


    private static final long serialVersionUID = 1L;

    public UpdateMsg() {
        super(MsgType.UpdateMsg);
    }
}
