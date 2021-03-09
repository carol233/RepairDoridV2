package Model;

import java.util.ArrayList;
import java.util.List;

public class MyClassObject extends MyInputObject{
    private String type = MyConstants.CLASS;
    private String declaringClassName;
    private String superClassName;
    private boolean hasSuperClass;
    private boolean ifPublic;
    private boolean ifPrivate;
    private boolean ifStatic;
    private boolean ifProtected;
    private boolean ifFinal;
    private boolean ifAbstract;

    private List<MyMethodObject> MethodInBody = new ArrayList<>();
    private List<MyMemberVariable> MemberVariables = new ArrayList<>();


    public MyClassObject(String declaringClassName, String superClassName,
                         boolean hasSuperClass, boolean ifPublic, boolean ifPrivate,
                         boolean ifStatic, boolean ifProtected, boolean ifFinal, boolean ifAbstract) {
        this.declaringClassName = declaringClassName;
        this.superClassName = superClassName;
        this.hasSuperClass = hasSuperClass;
        this.ifPublic = ifPublic;
        this.ifPrivate = ifPrivate;
        this.ifStatic = ifStatic;
        this.ifProtected = ifProtected;
        this.ifFinal = ifFinal;
        this.ifAbstract = ifAbstract;
    }

    public boolean isIfAbstract() {
        return ifAbstract;
    }

    public void setIfAbstract(boolean ifAbstract) {
        this.ifAbstract = ifAbstract;
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

    public void addNewMethodToClass(MyMethodObject e) {
        MethodInBody.add(e);
    }

    public void addMemberVariable(MyMemberVariable e) {
        MemberVariables.add(e);
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public boolean isHasSuperClass() {
        return hasSuperClass;
    }

    public void setHasSuperClass(boolean hasSuperClass) {
        this.hasSuperClass = hasSuperClass;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public void setDeclaringClassName(String declaringClassName) {
        this.declaringClassName = declaringClassName;
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

    public List<MyMethodObject> getMethodInBody() {
        return MethodInBody;
    }

    public void setMethodInBody(List<MyMethodObject> methodInBody) {
        MethodInBody = methodInBody;
    }

    public List<MyMemberVariable> getMemberVariables() {
        return MemberVariables;
    }

    public void setMemberVariables(List<MyMemberVariable> memberVariables) {
        MemberVariables = memberVariables;
    }
}
