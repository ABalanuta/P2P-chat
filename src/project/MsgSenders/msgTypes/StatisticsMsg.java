package project.MsgSenders.msgTypes;

import project.MsgSenders.Msg;
import project.MsgSenders.MsgType;

public class StatisticsMsg extends
        Msg {


    private static final long serialVersionUID = 1L;
    public long gossipId;
    public long userRegistered;
    public long nodesRunning;
    public long msgWaitingToDelivery;
    public long chatRooms;
    public double msgSentAvgSum;
    public int friendsCount;
    public int friendsWaitingCount;
    public boolean isUpdated;

    public StatisticsMsg(long gossipId) {
        super(MsgType.StatisticsMsg);
        this.gossipId = gossipId;
        this.userRegistered = 0;
        this.nodesRunning = 0;
        this.msgWaitingToDelivery = 0;
        this.chatRooms = 0;
        this.msgSentAvgSum = 0;
        this.friendsCount = 0;
        this.friendsWaitingCount = 0;
        this.isUpdated = false;
    }






    public StatisticsMsg(long gossipId,
            long userRegistered,
            long nodesRunning,
            long msgWaitingToDelivery,
            long chatRooms,
            double msgSentAvgSum,
            int friendsCount,
            int friendsWaitingCount,
            boolean updated) {
        super(MsgType.StatisticsMsg);
        this.gossipId = gossipId;
        this.userRegistered = userRegistered;
        this.nodesRunning = nodesRunning;
        this.msgWaitingToDelivery = msgWaitingToDelivery;
        this.chatRooms = chatRooms;
        this.msgSentAvgSum = msgSentAvgSum;
        this.friendsCount = friendsCount;
        this.friendsWaitingCount = friendsWaitingCount;
        this.isUpdated = updated;
    }










    @Override
    public String toString() {
        return "StatisticsMsg [gossipId=" + gossipId + "\n userRegistered=" + userRegistered + "\n  nodesRunning=" + nodesRunning + "\n  msgWaitingToDelivery=" + msgWaitingToDelivery
                + "\n  chatRooms=" + chatRooms + "\n  msgSentAvgSum=" + msgSentAvgSum + "\n  friendsCount=" + friendsCount + "\n  friendsWaitingCount=" + friendsWaitingCount + "\n isUpdated: "
                + isUpdated;
    }






    @Override
    public StatisticsMsg clone() {
        return new StatisticsMsg(gossipId, userRegistered, nodesRunning, msgWaitingToDelivery, chatRooms, msgSentAvgSum, friendsCount, friendsWaitingCount, isUpdated);
    }
}
