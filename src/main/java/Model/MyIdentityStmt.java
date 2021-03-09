package Model;

public class MyIdentityStmt extends MyInputObject {
    private String type = MyConstants.IDENTITYSTMT;
    private String sign; // @parameter or @this
    private String leftVariableName;
    private int paramaterOrder_n = 0; // default 0
    private String variableType;

    public MyIdentityStmt(String sign, String leftVariableName, int paramaterOrder_n, String variableType) {
        this.sign = sign;
        this.leftVariableName = leftVariableName;
        this.paramaterOrder_n = paramaterOrder_n;
        this.variableType = variableType;
    }

    public MyIdentityStmt(String sign, String leftVariableName, String variableType) {
        this.sign = sign;
        this.leftVariableName = leftVariableName;
        this.variableType = variableType;
    }

    public String getLeftVariableName() {
        return leftVariableName;
    }

    public void setLeftVariableName(String leftVariableName) {
        this.leftVariableName = leftVariableName;
    }

    public int getParamaterOrder_n() {
        return paramaterOrder_n;
    }

    public void setParamaterOrder_n(int paramaterOrder_n) {
        this.paramaterOrder_n = paramaterOrder_n;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getVariableType() {
        return variableType;
    }

    public void setVariableType(String variableType) {
        this.variableType = variableType;
    }
}
