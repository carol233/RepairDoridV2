package Model;

public class MyIfStmt extends MyInputObject {
    private String type = MyConstants.IFSTMT;
    private String Symbol;
    private String Op1Var;
    private String Op2Var;
    private String target;

    public MyIfStmt(String symbol, String op1Var, String op2Var, String target) {
        this.Symbol = symbol;
        this.Op1Var = op1Var;
        this.Op2Var = op2Var;
        this.target = target;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSymbol() {
        return Symbol;
    }

    public void setSymbol(String symbol) {
        Symbol = symbol;
    }

    public String getOp1Var() {
        return Op1Var;
    }

    public void setOp1Var(String op1Var) {
        Op1Var = op1Var;
    }

    public String getOp2Var() {
        return Op2Var;
    }

    public void setOp2Var(String op2Var) {
        Op2Var = op2Var;
    }
}
