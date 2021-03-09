package Model;

public class MyOperationStmt extends MyInputObject{
    private String type;
    private MyInputObject ending;

    public MyOperationStmt(String type) {
        this.type = type;
    }

    public MyOperationStmt(String type, MyInputObject ending) {
        this.type = type;
        this.ending = ending;
    }

    public MyInputObject getEnding() {
        return ending;
    }

    public void setEnding(MyInputObject ending) {
        this.ending = ending;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }
}
