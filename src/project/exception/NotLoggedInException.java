package project.exception;

public class NotLoggedInException extends
        Exception {

    private static final long serialVersionUID = 1L;
    String msg = "User not logged in";

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public NotLoggedInException() {
        super();
    }




}
