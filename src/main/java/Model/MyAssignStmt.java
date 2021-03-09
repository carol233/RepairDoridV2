package Model;

public class MyAssignStmt extends MyInputObject {
    private String type = MyConstants.ASSIGNMENT_Str;
    private String leftVariableName = "";
    private String rightVariableName = "";
    private String rightVariableName2 = "";
    private String fieldType;
    private boolean ifHasVarOnTheRight = false;
    private String valueStr;
    private String fieldCustomName = "";
    private String attributeBaseName = "";
    private String attributeBaseType = "";
    private String varcastType = "";
    private String varcastSourceName = "";
    private String InnediateBox = "";
    private String symbol = "";

    public MyAssignStmt(String type, String leftVariableName, String varcastType, String varcastSourceName, int ifcast) {
        assert ifcast == 1;
        this.type = type;
        this.leftVariableName = leftVariableName;
        this.varcastType = varcastType;
        this.varcastSourceName = varcastSourceName;
    }

    public MyAssignStmt(String type, String leftVariableName, String right1, String symbol, String right2) {
        this.type = type;
        this.leftVariableName = leftVariableName;
        this.rightVariableName = right1;
        this.rightVariableName2 = right2;
        this.symbol = symbol;
    }


    public MyAssignStmt(String type, String leftVariableName, String fieldType, String valueStr) {
        // for New object and sdk_int
        this.type = type;
        this.leftVariableName = leftVariableName;
        this.fieldType = fieldType;
        this.valueStr = valueStr;
    }

    public MyAssignStmt(String type, String leftVariableName, String right) {
        // for length array and constant
        this.type = type;
        this.leftVariableName = leftVariableName;
        this.rightVariableName = right;
    }

    public MyAssignStmt(String type, String leftVariableName, String LocalBox, String InnediateBox, boolean a) {
        // for array
        this.type = type;
        this.leftVariableName = leftVariableName;
        this.rightVariableName = LocalBox;
        this.InnediateBox = InnediateBox;
    }

    // (2) $r4 = $r3.<android.su...mpat: android.sup...onCompat$b a>;
    // (3) $r2 = <android.su...mpat: android.sup...onCompat$b a>;
    public MyAssignStmt(String type, String VariableName, String fieldType, String fieldName,
                        String valueStr, String attributeBaseName, String attributeBaseType, boolean ifHasVarOnTheRight) {
        this.type = type;
        this.ifHasVarOnTheRight = ifHasVarOnTheRight;
        if (ifHasVarOnTheRight)
            this.rightVariableName = VariableName;
        else
            this.leftVariableName = VariableName;
        this.fieldType = fieldType;
        this.fieldCustomName = fieldName;
        this.valueStr = valueStr;
        this.attributeBaseName = attributeBaseName;
        this.attributeBaseType = attributeBaseType;
    }

    public MyAssignStmt(String type, String leftVariableName, String fieldType, String fieldName,
                        String valueStr, String attributeBaseType) {
        this.type = type;
        this.leftVariableName = leftVariableName;
        this.fieldType = fieldType;
        this.valueStr = valueStr;
        this.fieldCustomName = fieldName;
        this.attributeBaseType = attributeBaseType;
    }

    public String getRightVariableName() {
        return rightVariableName;
    }

    public void setRightVariableName(String rightVariableName) {
        this.rightVariableName = rightVariableName;
    }

    public String getAttributeBaseName() {
        return attributeBaseName;
    }

    public void setAttributeBaseName(String attributeBaseName) {
        this.attributeBaseName = attributeBaseName;
    }

    public String getAttributeBaseType() {
        return attributeBaseType;
    }

    public void setAttributeBaseType(String attributeBaseType) {
        this.attributeBaseType = attributeBaseType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVarcastType() {
        return varcastType;
    }

    public void setVarcastType(String varcastType) {
        this.varcastType = varcastType;
    }

    public String getVarcastSourceName() {
        return varcastSourceName;
    }

    public void setVarcastSourceName(String varcastSourceName) {
        this.varcastSourceName = varcastSourceName;
    }

    public String getLeftVariableName() {
        return leftVariableName;
    }

    public void setLeftVariableName(String leftVariableName) {
        this.leftVariableName = leftVariableName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public boolean isIfHasVarOnTheRight() {
        return ifHasVarOnTheRight;
    }

    public void setIfHasVarOnTheRight(boolean ifHasVarOnTheRight) {
        this.ifHasVarOnTheRight = ifHasVarOnTheRight;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getValueStr() {
        return valueStr;
    }

    public void setValueStr(String valueStr) {
        this.valueStr = valueStr;
    }

    public String getFieldCustomName() {
        return fieldCustomName;
    }

    public void setFieldCustomName(String fieldCustomName) {
        this.fieldCustomName = fieldCustomName;
    }

    public String getInnediateBox() {
        return InnediateBox;
    }

    public void setInnediateBox(String innediateBox) {
        InnediateBox = innediateBox;
    }

    public String getRightVariableName2() {
        return rightVariableName2;
    }

    public void setRightVariableName2(String rightVariableName2) {
        this.rightVariableName2 = rightVariableName2;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
