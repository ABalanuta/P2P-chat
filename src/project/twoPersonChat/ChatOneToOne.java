package project.twoPersonChat;

import org.apache.log4j.Logger;

import project.ConfChat;
import project.MsgSenders.Msg;
import project.MsgSenders.MsgSender;
import project.MsgSenders.msgTypes.TextMsg;
import project.exception.ConfChatException;
import project.exception.NotLoggedInException;
import project.exception.UserOfflineException;
import project.statisticsForGraphs.LoggerForGraphs;
import project.storage.Objects.PastObjectTypes;
import project.storage.Objects.Username;
import rice.p2p.commonapi.DeliveryNotification;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.MessageReceipt;
import rice.p2p.commonapi.NodeHandle;

public class ChatOneToOne extends
        MsgSender {
    static Logger logger = Logger.getLogger(ChatOneToOne.class);


    public ChatOneToOne(ConfChat confchat) {
        super(confchat, "chatOneToOne");

    }

    /**
     * Send message to destination
     */
    public void sendMessage(final String username, String message) throws NotLoggedInException {
        // Get the Object of the Contact to Send the Message
        final Username userToSendMessage = (Username) getConfchat().getStorageAPI().getObject(username, PastObjectTypes.USERNAME);

        if (userToSendMessage == null) {
            logger.info("The user: " + username + " isn't registered on the network");
            return;
        }

        // Prepare Message
        final String textToSend = getConfchat().getUserManager().getMe().getUsername() + " said: " + message;

        // Prepare the message to send
        final TextMsg messageToSend = new TextMsg(textToSend, super.getConfchat().getUserManager().getMe().getUsername(), username, LoggerForGraphs.getNextGlobalMessageOneOnOneIDGenerator());

        // if Online sends Message Directly
        NodeHandle userHandle;
        try {
            userHandle = userToSendMessage.getUserNode();
        } catch (UserOfflineException e) {
            userWasOfflineStoreMessage(userToSendMessage, textToSend);
            return;
        }

        DeliveryNotification notification = new DeliveryNotification() {
            @Override
            public void sent(MessageReceipt arg0) {
                getConfchat().getAdmin().changeMsgSent(+1);
                // Log for Graphs
                try {
                    LoggerForGraphs.logMessageOneOnOne(getConfchat().getUserManager().getMe().getUsername(), username, messageToSend);
                } catch (NotLoggedInException e) {}
            }

            @Override
            public void sendFailed(MessageReceipt arg0, Exception arg1) {
                getConfchat().getUserManager().setFriendOfflineHandler(username);
                userWasOfflineStoreMessage(userToSendMessage, textToSend);
            }
        };
        routeMsgDirect(userHandle, messageToSend, notification);
    }

    // If offline, store the message in it's object
    public void userWasOfflineStoreMessage(Username userToSendMessage, String messageToSend) {
        logger.info("The user: " + userToSendMessage.getUsername() + " isn't online at the moment, the message will arquived");
        userToSendMessage.addMessageReceivedWhileOffline(messageToSend);
        boolean inserted = false;
        while (true) {
            try {
                getConfchat().getStorageAPI().insertObject(userToSendMessage.getUsername(), userToSendMessage);
                break;
            } catch (ConfChatException e1) {
            }
        }
    }




    @Override
    public void deliver(Id id, Msg message) {
        TextMsg textMsg = (TextMsg) message;
        // Log for Graphs
        LoggerForGraphs.logMessageOneOnOne(textMsg.getSourceUsername(), textMsg.getDestinationUsername(), textMsg);
        super.getConfchat().addPendentNotifications(textMsg.getMessage());
    }


}
