package Model;

public class MyMemberVariable extends MyInputObject{
    private String type = MyConstants.MEMBERVARIABLE;
    private boolean ifFinal;
    private boolean ifPublic;
    private boolean ifPrivate;
    private boolean ifStatic;
    private boolean ifProtected;
    private String VariableName;
    private String VariableTypeName;

    public MyMemberVariable(boolean ifFinal, boolean ifPublic,
                            boolean ifPrivate, boolean ifStatic, boolean ifProtected,
                            String variableName, String variableTypeName) {
        this.ifFinal = ifFinal;
        this.ifPublic = ifPublic;
        this.ifPrivate = ifPrivate;
        this.ifStatic = ifStatic;
        this.ifProtected = ifProtected;
        this.VariableName = variableName;
        this.VariableTypeName = variableTypeName;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    public boolean isIfFinal() {
        return ifFinal;
    }

    public void setIfFinal(boolean ifFinal) {
        this.ifFinal = ifFinal;
    }

    public boolean isIfPublic() {
        return ifPublic;
    }

    public void setIfPublic(boolean ifPublic) {
        this.ifPublic = ifPublic;
    }

    public boolean isIfPrivate() {
        return ifPrivate;
    }

    public void setIfPrivate(boolean ifPrivate) {
        this.ifPrivate = ifPrivate;
    }

    public boolean isIfStatic() {
        return ifStatic;
    }

    public void setIfStatic(boolean ifStatic) {
        this.ifStatic = ifStatic;
    }

    public boolean isIfProtected() {
        return ifProtected;
    }

    public void setIfProtected(boolean ifProtected) {
        this.ifProtected = ifProtected;
    }

    public String getVariableName() {
        return VariableName;
    }

    public void setVariableName(String variableName) {
        VariableName = variableName;
    }

    public String getVariableTypeName() {
        return VariableTypeName;
    }

    public void setVariableTypeName(String variableTypeName) {
        VariableTypeName = variableTypeName;
    }
}
