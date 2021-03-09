package Model;

public class MyForSearchStmt extends MyInputObject {
    private String type = MyConstants.FORSEARCH;
    private String varName;
    private String varType;

    public MyForSearchStmt(String varName, String varType) {
        this.varName = varName;
        this.varType = varType;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String getVarType() {
        return varType;
    }

    public void setVarType(String varType) {
        this.varType = varType;
    }
}
