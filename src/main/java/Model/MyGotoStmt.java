package Model;

public class MyGotoStmt extends MyInputObject {
    private String type = MyConstants.GOTOSTMT;
    private String target;

    public MyGotoStmt(String target) {
        this.target = target;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
