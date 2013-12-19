package project.admin;

import org.apache.log4j.Logger;

import project.ConfChat;
import project.MsgSenders.Msg;
import project.MsgSenders.MsgSender;
import project.MsgSenders.MsgType;
import project.MsgSenders.msgTypes.StatisticsMsg;
import project.exception.ConfChatException;
import project.exception.NotLoggedInException;
import project.storage.util.LocalStatisticsObject;
import rice.p2p.commonapi.DeliveryNotification;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.MessageReceipt;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.standard.RandomNodeIdFactory;

/**
 * Gossip Administrator Panel Sends message to NUM_NODES_TO_NOTIFIE nodes with PERIODICITY
 * ms between them.
 */
public class AdminStatistics extends
        MsgSender {
    static Logger logger = Logger.getLogger(AdminStatistics.class);

    static StatisticsMsg globalState;

    /** periodically to send gossip message */
    int PERIODICITY = 10000;

    long firstRoundId;
    long secoundRoudId;

    double totalMsgNow;
    double totalMsgLastReport;
    long lastReportDate;
    double msgSentsumLastReport;

    boolean running = true;
    boolean correctUpdateSent = false;

    boolean previousFail = false;

    final boolean master;

    public AdminStatistics(ConfChat confchat, boolean master) {
        super(confchat, "adminStatistics");
        firstRoundId = 0;
        if (master) {
            secoundRoudId = 1;
        } else {
            secoundRoudId = 0;
        }
        totalMsgNow = 0;
        totalMsgLastReport = 0;
        lastReportDate = System.currentTimeMillis();
        msgSentsumLastReport = 0;
        correctUpdateSent = false;
        globalState = new StatisticsMsg(firstRoundId);
        this.master = master;
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (running) {
                    startNewRound();
                    correctUpdateSent = false;
                    int delay = PERIODICITY + (int) (Math.random() * 1000);
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private synchronized void startNewRound() {
        // Restart counter at master
        if (firstRoundId == Double.MAX_VALUE - 10) {
            firstRoundId = 0;
            secoundRoudId = 1;
        }

        StatisticsMsg newToken = new StatisticsMsg(++firstRoundId);
        updateGlobalState(newToken);
    }

    /**
     * Calculate current msg sum and send gossip status message to ONE Random Node
     */
    public synchronized void updateGlobalState(StatisticsMsg msg) {
        if (!running) {
            return;
        }

        // * Vamos actualizar na 1ª volta*/
        firstRoundId = msg.gossipId;

        double msgSentAvg = recalculateMsgAvg();

        // nodesRunning -> incrementar o valor recebido e propagar.
        msg.nodesRunning += 1;
        LocalStatisticsObject storageStatistics = getConfchat().getStorageAPI().getNumberOfUsersRegisteredInLocalDatabase();
        msg.userRegistered += storageStatistics.getNumbOfLocalStoredUsers();
        msg.msgWaitingToDelivery += storageStatistics.getNumbOfLocalMessagesToBeDelivered();
        try {
            msg.friendsCount = getConfchat().getUserManager().listFriends().size();
            msg.friendsWaitingCount = getConfchat().getUserManager().listFriendRequest().size();
            msg.chatRooms += getConfchat().getChatRoomManager().countRootRoom();
        } catch (NotLoggedInException e1) {
            msg.friendsCount += 0;
            msg.friendsWaitingCount += 0;
            msg.chatRooms += 0;
        }

        msg.msgSentAvgSum += msgSentAvg;

        notifyNodeWithNewToken(msg);
    }




    public synchronized void notifyNodeWithNewToken(StatisticsMsg msg) {
        if (!running) {
            return;
        }


        LeafSet leafSet = getConfchat().getPastryNode().getLeafSet();

        DeliveryNotification notification = new DeliveryNotification() {
            @Override
            public void sent(MessageReceipt arg0) {
                previousFail = false;
            }

            @Override
            public void sendFailed(MessageReceipt message, Exception arg1) {
                previousFail = true;
            }
        };
        NodeHandle nh = null;
        /** Route to random if previousFail or no neighbors */
        if (previousFail || leafSet.cwSize() == 0) {

            NodeIdFactory nidFactory = new RandomNodeIdFactory(getConfchat().getEnv());
            Id randId = nidFactory.generateNodeId();
            routeMsgId(randId, msg, notification);
            
        } else {
            nh = leafSet.get(1);
            // send to that key
            routeMsgDirect(nh, msg, notification);
        }
    }

    @Override
    public synchronized void deliver(Id id, Msg message) {
        if (!running) {
            return;
        }
        MsgType type = message.getType();
        switch (type) {
        case StatisticsMsg:
            StatisticsMsg statMsg = (StatisticsMsg) message;
            processReceivedMsg(statMsg);
            break;
        default:
            logger.error("Non-Gossip msg received in wrong thread");
            break;
        }
    }






    private synchronized double recalculateMsgAvg() {
        double deltaM = this.totalMsgNow - this.totalMsgLastReport;
        long msDeltaT = (System.currentTimeMillis() - this.lastReportDate);
        long secondsDelta = msDeltaT / 1000;
        /** Avg msg sent during the period between gossip msgs */
        double newAvgMsgSent = deltaM / secondsDelta;

        /***************** Update status ********************/
        lastReportDate = System.currentTimeMillis();
        totalMsgLastReport = totalMsgNow;
        return newAvgMsgSent;
    }


    private synchronized void processReceivedMsg(StatisticsMsg msg) {
        // If counter reset, restart counter
        if (msg.gossipId < firstRoundId - 10000) {
            firstRoundId = 0;
            secoundRoudId = 0;
        }


        // It's a new round here, add my values
        if (msg.gossipId > firstRoundId) {
            firstRoundId = msg.gossipId;
            // Update global State with our values and notify
            updateGlobalState(msg.clone());

        }
        
        // This message is 2nd round and Im master?
        if (master && msg.gossipId >= secoundRoudId) {
            msg.isUpdated = true;
        }
        if (msg.gossipId >= secoundRoudId && msg.isUpdated) {
            // Lets wait for the next round message
            // Global state is only updated at secound round
            secoundRoudId = msg.gossipId + 1;

            // SameID, update state to new global Values
            globalState = msg.clone();
            /** Send State */
            notifyNodeWithNewToken(globalState.clone());
        }
        // If smaller ID, drop

    }

    /**
     * Show processed statistics
     * 
     * @throws ConfChatException stillGossiping- Not Ready yet to send statistics
     */
    public synchronized StatisticsSummary showStatistics() throws ConfChatException {
        int numReplicationElements = getConfchat().getStorageAPI().getNumberOfReplicas();

        long id = globalState.gossipId;
        long nodesRunning = globalState.nodesRunning;
        long registeredUsers;

        // number of nodes inferior to Replica Size every one will have a copy of the
        // object

        if (nodesRunning == 0) {
            registeredUsers = 0;
        } else if (nodesRunning < numReplicationElements) {
            registeredUsers = globalState.userRegistered / nodesRunning;
        } else {
            registeredUsers = globalState.userRegistered / numReplicationElements;
        }


        long msgWaitingDelivery = globalState.msgWaitingToDelivery;
        long runningConferences = globalState.chatRooms;
        int friendsCount = globalState.friendsCount;
        int friendsWaitingCount = globalState.friendsWaitingCount;
        double avgMsgRate = globalState.msgSentAvgSum / globalState.nodesRunning;

        return new StatisticsSummary(id, nodesRunning, registeredUsers, msgWaitingDelivery, runningConferences, avgMsgRate, friendsCount, friendsWaitingCount, globalState.isUpdated);
    }

    /**
     * Finish gossip algorithm sending all data to other nodes.
     */
    public synchronized void finishApp() {
        // Parar a thread de envio
        running = false;
    }

    public void changeMsgSent(int i) {
        totalMsgNow += i;
    }

}
