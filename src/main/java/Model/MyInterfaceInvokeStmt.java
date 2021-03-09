package Model;

import java.util.List;

public class MyInterfaceInvokeStmt extends MyInvokeStmt {
    private String type = MyConstants.SPECIAL_INVOKE;
    private boolean ifAssign = false;
    private String returnName;
    private String returnClass;
    private int argLen;
    private List<String> argNames;
    private List<String> argTypes;
    private String baseName;
    private String MethodBaseClassName;
    private String methodName;
    private String MethodSignature;

    public MyInterfaceInvokeStmt(String returnName, String returnClass,
                               int argLen, List<String> argNames, List<String> argTypes,
                               String baseName, String MethodBaseClassName,
                               String methodName, String MethodSignature) {
        this.returnName = returnName;
        if (!returnName.equals(""))
            this.ifAssign = true;
        this.returnClass = returnClass;
        this.argLen = argLen;
        this.argNames = argNames;
        this.argTypes = argTypes;
        this.baseName = baseName;
        this.MethodBaseClassName = MethodBaseClassName;
        this.methodName = methodName;
        this.MethodSignature = MethodSignature;
    }

    public boolean isIfAssign() {
        return ifAssign;
    }

    public void setIfAssign(boolean ifAssign) {
        this.ifAssign = ifAssign;
    }

    public String getReturnClass() {
        return returnClass;
    }

    public void setReturnClass(String returnClass) {
        this.returnClass = returnClass;
    }

    public List<String> getArgTypes() {
        return argTypes;
    }

    public void setArgTypes(List<String> argTypes) {
        this.argTypes = argTypes;
    }

    public String getMethodBaseClassType() {
        return MethodBaseClassName;
    }

    public void setMethodBaseClassType(String methodBaseClassType) {
        MethodBaseClassName = methodBaseClassType;
    }

    public String getMethodSignature() {
        return MethodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        MethodSignature = methodSignature;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReturnName() {
        return returnName;
    }

    public void setReturnName(String returnName) {
        this.returnName = returnName;
    }

    public int getArgLen() {
        return argLen;
    }

    public void setArgLen(int argLen) {
        this.argLen = argLen;
    }

    public List<String> getArgNames() {
        return argNames;
    }

    public void setArgNames(List<String> argNames) {
        this.argNames = argNames;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
