package project.admin;

public class StatisticsSummary {
    long id;
    long nodesRunning;
    long registeredUsers;
    long msgWaitingDelivery;
    long runningConferences;
    public int friendsCount;
    public int friendsWaitingCount;
    double avgMsgRate;
    boolean isUpdate;

    public StatisticsSummary(long id,
            long nodesRunning,
            long registeredUsers,
            long msgWaitingDelivery,
            long runningConferences,
            double avgMsgRate,
            int friendsCount,
            int friendsWaitingCount,
            boolean updated) {
        super();
        this.id = id;
        this.nodesRunning = nodesRunning;
        this.registeredUsers = registeredUsers;
        this.msgWaitingDelivery = msgWaitingDelivery;
        this.runningConferences = runningConferences;
        this.avgMsgRate = avgMsgRate;
        this.friendsCount = friendsCount;
        this.friendsWaitingCount = friendsWaitingCount;
        this.isUpdate = updated;
    }

    @Override
    public String toString() {
        String avgRate = String.format("%.4f", avgMsgRate);
        return "StatisticsSummary: \n id=" + id + "\n nodesRunning=" + nodesRunning + "\n registeredUsers=" + registeredUsers + "\n msgWaitingDelivery=" + msgWaitingDelivery
                + "\n runningConferences=" + runningConferences + "\n friendsCount=" + friendsCount + "\n friendsWaitingCount=" + friendsWaitingCount + "\n avgMsgRate=" + avgRate + "\n isUpdated"
                + isUpdate;
    }



}
