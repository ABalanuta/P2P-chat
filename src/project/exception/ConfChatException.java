package project.exception;

public class ConfChatException extends
        Exception {

    private static final long serialVersionUID = 1L;
    ExceptionEnum msg;

    public ConfChatException(ExceptionEnum msg) {
        super();
        this.msg = msg;
    }

    public ExceptionEnum getMsg() {
        return msg;
    }

    public void setMsg(ExceptionEnum msg) {
        this.msg = msg;
    }


}
