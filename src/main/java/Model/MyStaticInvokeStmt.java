package Model;

import java.util.List;

public class MyStaticInvokeStmt extends MyInvokeStmt{
    private String type = MyConstants.STATIC_INVOKE;
    private boolean ifAssign = false;
    private String returnName;
    private String returnClass;
    private int argLen;
    private List<String> argNames;
    private List<String> argTypes;
    private String methodName;
    private String MethodSignature;
    private String MethodBaseClassType;

    public MyStaticInvokeStmt(String returnName, String MethodBaseClassType, String returnClass,
                              int argLen, List<String> argNames, List<String> argTypes,
                              String methodName, String methodSignature) {
        this.returnName = returnName;
        if (!returnName.equals(""))
            this.ifAssign = true;
        this.MethodBaseClassType = MethodBaseClassType;
        this.returnClass = returnClass;
        this.argLen = argLen;
        this.argNames = argNames;
        this.argTypes = argTypes;
        this.methodName = methodName;
        this.MethodSignature = methodSignature;
    }

    public String getMethodBaseClassType() {
        return MethodBaseClassType;
    }

    public void setMethodBaseClassType(String methodBaseClassType) {
        MethodBaseClassType = methodBaseClassType;
    }

    public boolean isIfAssign() {
        return ifAssign;
    }

    public void setIfAssign(boolean ifAssign) {
        this.ifAssign = ifAssign;
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

    public String getReturnClass() {
        return returnClass;
    }

    public void setReturnClass(String returnClass) {
        this.returnClass = returnClass;
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

    public List<String> getArgTypes() {
        return argTypes;
    }

    public void setArgTypes(List<String> argTypes) {
        this.argTypes = argTypes;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodSignature() {
        return MethodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.MethodSignature = methodSignature;
    }
}
