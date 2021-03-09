package Model;

public class MyReturnStmt extends MyInputObject{
    private String type = MyConstants.RETURNSTMT;
    private boolean ifRtnVoid = true;
    private String returnVariableName = "";

    public MyReturnStmt(boolean ifRtnVoid) {
        this.ifRtnVoid = ifRtnVoid;
    }

    public MyReturnStmt(boolean ifRtnVoid, String returnVariableName) {
        this.ifRtnVoid = ifRtnVoid;
        this.returnVariableName = returnVariableName;
    }

    public String getReturnVariableName() {
        return returnVariableName;
    }

    public void setReturnVariableName(String returnVariableName) {
        this.returnVariableName = returnVariableName;
    }

    public boolean isIfRtnVoid() {
        return ifRtnVoid;
    }

    public void setIfRtnVoid(boolean ifRtnVoid) {
        this.ifRtnVoid = ifRtnVoid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
