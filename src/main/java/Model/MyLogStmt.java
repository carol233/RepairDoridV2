package Model;

public class MyLogStmt extends MyInputObject {
    private String type = MyConstants.LOGSTMT;
    private String logInfo;

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    public MyLogStmt(String logInfo) {
        this.logInfo = logInfo;
    }

    public String getLogInfo() {
        return logInfo;
    }

    public void setLogInfo(String logInfo) {
        this.logInfo = logInfo;
    }
}
