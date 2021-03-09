package Model;

import java.util.ArrayList;
import java.util.List;

public class MyMethodObject extends MyInputObject{
    private String type = MyConstants.METHOD;
    private String declaringClassName;
    private boolean ifPublic;
    private boolean ifPrivate;
    private boolean ifStatic;
    private boolean ifProtected;
    private boolean ifFinal;
    private String returnType;
    private String methodName;
    private List<String> parameterTypeList;
    private List<MyInputObject> StmtInBody = new ArrayList<>();

    public MyMethodObject(String declaringClassName, boolean ifPublic,
                          boolean ifPrivate, boolean ifStatic, boolean ifProtected,
                          boolean ifFinal, String returnType, String methodName, List<String> parameterTypeList) {
        this.declaringClassName = declaringClassName;
        this.ifPublic = ifPublic;
        this.ifPrivate = ifPrivate;
        this.ifStatic = ifStatic;
        this.ifProtected = ifProtected;
        this.ifFinal = ifFinal;
        this.returnType = returnType;
        this.methodName = methodName;
        this.parameterTypeList = parameterTypeList;
    }

    public MyMethodObject(String declaringClassName, String returnType, String methodName, List<String> parameterTypeList) {
        this.declaringClassName = declaringClassName;
        this.returnType = returnType;
        this.methodName = methodName;
        this.parameterTypeList = parameterTypeList;
    }

    public void addStmtToBody(MyInputObject e) {
        StmtInBody.add(e);
    }

    public List<MyInputObject> getStmtInBody() {
        return StmtInBody;
    }

    public void setStmtInBody(List<MyInputObject> stmtInBody) {
        StmtInBody = stmtInBody;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    public boolean isIfProtected() {
        return ifProtected;
    }

    public void setIfProtected(boolean ifProtected) {
        this.ifProtected = ifProtected;
    }

    public boolean isIfFinal() {
        return ifFinal;
    }

    public void setIfFinal(boolean ifFinal) {
        this.ifFinal = ifFinal;
    }

    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public void setDeclaringClassName(String declaringClassName) {
        this.declaringClassName = declaringClassName;
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

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<String> getParameterTypeList() {
        return parameterTypeList;
    }

    public void setParameterTypeList(List<String> parameterTypeList) {
        this.parameterTypeList = parameterTypeList;
    }
}
