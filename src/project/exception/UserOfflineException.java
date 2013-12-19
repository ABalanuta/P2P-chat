package project.exception;

public class UserOfflineException extends
        Exception {

    private static final long serialVersionUID = 1L;
    String msg;
    String username;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String username) {
        this.msg = "User " + username + " is offline";
    }


    public String getUsername() {
        return username;
    }

    public UserOfflineException(String username) {
        super();
        this.msg = "User " + username + " is offline";
        this.username = username;
    }



}
