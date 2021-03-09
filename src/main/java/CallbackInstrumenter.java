import Model.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JNopStmt;
import soot.util.Chain;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class CallbackInstrumenter{

    private String logPath;
    private String pkg;
    private PatchParser dataObject;
    private String SemanticPatchPath;

    CallbackInstrumenter(String pkg, String SemanticPatchPath, String logFilePath, PatchParser dataObject) {
        this.logPath = logFilePath;
        this.pkg = pkg;
        this.dataObject = dataObject;
        this.SemanticPatchPath = SemanticPatchPath;
    }

    protected void CallBackInternalTransform(Body b) {

        HashMap <String, String> super2Class = new HashMap<>();

        String methodSig = b.getMethod().getSignature();

//        if (methodSig.contains("onAttach(android.app.Activity)"))
//            System.out.println(111);

        // First we filter out Android framework methods and third-party libraries, only app code
        if (!methodSig.contains(this.pkg))
            return;

        SootClass sc0 = b.getMethod().getDeclaringClass();
        String CURRENT_DECLARING_CLASS = sc0.getName();
        super2Class.put(MyConstants.DECLARING_CLASS_SIGN, CURRENT_DECLARING_CLASS);

        if (this.dataObject.getBASECLASS().contains("[")){ //super class
            if (sc0.hasSuperclass() && this.dataObject.getBASECLASS().equals("[" +
                            sc0.getSuperclassUnsafe().getName() + "]")){
                super2Class.put(this.dataObject.getBASECLASS(), sc0.getName());
                if (!Helper.checkIfPotentialCallBack(b, this.dataObject.getLocationTarget())){
                    return;
                }
            } else {
                return;
            }
        } else if (this.dataObject.getBASECLASS().equals(CURRENT_DECLARING_CLASS)) {
            if (!Helper.checkIfPotentialCallBack(b, this.dataObject.getLocationTarget())){
                return;
            }
        } else {
            return;
        }

        try {
            InstrumentUtil.Log2File("Potential", this.logPath, methodSig,
                    this.dataObject.getIssueType(), methodSig, "null", "null", this.SemanticPatchPath);
            System.out.println("[+] Potential callback issue.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // search other super class
        for (String clazz: this.dataObject.getClass_for_search()) {
            for (SootField sootField: sc0.getFields()) {
                Type tmpType = sootField.getType();
                if (!(tmpType instanceof RefType)){
                    continue;
                }
                SootClass class0 = ((RefType)tmpType).getSootClass();
                if (class0.hasSuperclass()){
                    List<SootClass> allSuperClass = InstrumentBody.getAllSuperClasses(class0);
                    for (SootClass supertmp: allSuperClass) {
                        if (clazz.equals("[" + supertmp.getName() + "]")) {
                            super2Class.put(clazz, class0.getName());
                        }
                    }
                } else {
                    if (clazz.equals("[" + class0.getName() + "]")) {
                        super2Class.put(clazz, class0.getName());
                    }
                }
            }

            if (!super2Class.containsKey(clazz)) {
                System.out.println("[-] Cannot find super class " + clazz);
                return;
            }
        }

        // only when make sure the target method doesn't exists, step in
        for (MyInputObject targetStmt: this.dataObject.getAddStmts()) {
            if (targetStmt.getType().equals(MyConstants.METHOD)) {
                MyMethodObject myMethodObject = (MyMethodObject) targetStmt;
                if (!Helper.checkIfTrueCallBack(b, myMethodObject)){
                    System.out.println("[-] Method exists! " + myMethodObject.getMethodName());
                    return;
                }
            }
        }

        PatchingChain<Unit> units = b.getUnits();

        HashMap <MyInputObject, Stmt> FoundOldStmtTmpList = new HashMap<>();
        HashMap <MyForSearchStmt, Stmt> SearchedStmt = new HashMap<>();
        HashMap <String, Local> old_new_VarArgs = new HashMap<>();
        HashMap <String, String> old_new_Fields = new HashMap<>();
        HashMap <String, Constant> old_new_Constants = new HashMap<>();
        List <Stmt> insertAftertheLast = new ArrayList<>();

        Map<MyLabelStmt, NopStmt> nopMap = new HashMap<>();
        AtomicBoolean if_insert_current_class = new AtomicBoolean(false);

        Stack <Stmt> VisitedStmtStack = new Stack<>();
        List <Stmt> VisitedStmtForOperation = new ArrayList<>();
        HashMap <Stmt, Stmt> oldStmtsToNew = new HashMap<>();
        HashMap <Local, String> SaveVarOfCurrentMethod = new HashMap<>(); // {($r0, "@this")}
        HashMap <Local, String> SaveVarOfNewMethod = new HashMap<>();
        AtomicBoolean if_contains_cut_operations = new AtomicBoolean(false);


        // check if has [CUT] in the current localized method, default is true
        for (MyInputObject addStatement: this.dataObject.getAddStmts()) {
            if (addStatement.getType().equals(MyConstants.CUT_OPERATION)) {
                if_contains_cut_operations.set(true);
                break;
            }
        }

        // 获取Body里所有的units, 一个Body对应Java里一个方法的方法体，Unit代表里面的语句
        // Traverse method, flag_if_insert indicates if there exists a bug
        Iterator<Unit> unitsIterator = units.snapshotIterator();
        Unit unit = null;
        while (unitsIterator.hasNext()) {
            unit = unitsIterator.next();
            Stmt stmt = (Stmt) unit;
            VisitedStmtStack.push(stmt);

            // filter the super call
            // $r0 := @this: [DECLARING_CLASS]
            // $a0 := @parameter0: android.content.Context
            // specialinvoke $r0.<android.support.v4.app.Fragment: void onAttach(android.content.Context)>($a0)
            // if the first invoke is a this() and not a super() inline the this()
            AtomicBoolean ifFoundSuper = new AtomicBoolean(false); // control that only one super stmt
            if (stmt instanceof IdentityStmt) {
                // TODO: 14/1/21 record the variables
                IdentityStmt identityStmt = (IdentityStmt) stmt;
                if (identityStmt.getRightOp() instanceof ThisRef) {
                    ThisRef thisRef = (ThisRef) identityStmt.getRightOp();
                    SaveVarOfCurrentMethod.put((Local)identityStmt.getLeftOp(), identityStmt.getLeftOp().getType().toString());

                    for (MyInputObject targetStmt: this.dataObject.getTargetStmts()) {
                        if (targetStmt.getType().equals(MyConstants.IDENTITYSTMT)) {
                            MyIdentityStmt myIdentityStmt = (MyIdentityStmt) targetStmt;
                            if (myIdentityStmt.getSign().equals("@this")) {
                                old_new_VarArgs.put(myIdentityStmt.getLeftVariableName(), (Local)identityStmt.getLeftOp());
                            }
                        }
                    }

                } else if (identityStmt.getRightOp() instanceof ParameterRef) {
                    ParameterRef parameterRef = (ParameterRef) identityStmt.getRightOp();
                    SaveVarOfCurrentMethod.put((Local)identityStmt.getLeftOp(), identityStmt.getLeftOp().getType().toString());
                    for (MyInputObject targetStmt: this.dataObject.getTargetStmts()) {
                        if (targetStmt.getType().equals(MyConstants.IDENTITYSTMT)) {
                            MyIdentityStmt myIdentityStmt = (MyIdentityStmt) targetStmt;
                            if (myIdentityStmt.getSign().equals("@parameter") &&
                            myIdentityStmt.getParamaterOrder_n() == parameterRef.getIndex()) {
                                old_new_VarArgs.put(myIdentityStmt.getLeftVariableName(), (Local)identityStmt.getLeftOp());
                            }
                        }
                    }
                }
                continue;
            } else if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof SpecialInvokeExpr && !ifFoundSuper.get()) {
                SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) stmt.getInvokeExpr();
                Local BaseVariable = (Local) specialInvokeExpr.getBase();
                assert SaveVarOfCurrentMethod.containsKey(BaseVariable); // @this
                String method_name = specialInvokeExpr.getMethod().getName();
                if (method_name.equals(b.getMethod().getName())) {
                    // super() method name is the same, while <init> different
                    ifFoundSuper.set(true);
                    continue;
                }
            }

            VisitedStmtForOperation.add(stmt);

        }

        // unit is the last stmt

        try {
            InstrumentUtil.Log2File("Localization", this.logPath, methodSig,
                    this.dataObject.getIssueType(), methodSig, "null", "null", this.SemanticPatchPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // insert the labels
        for (String myLabelStr: this.dataObject.getLabelStmts().keySet()) {
            NopStmt nopStmt = new JNopStmt();
            nopMap.put(this.dataObject.getLabelStmts().get(myLabelStr), nopStmt);
        }

        // insert other new stmts
        for (MyInputObject addStatement: this.dataObject.getAddStmts()) {

            switch (addStatement.getType()) {
                case MyConstants.CLASS:
                    MyClassObject myClassObject = (MyClassObject) addStatement;
                    SootClass sc = InstrumentBody.createNewClass(myClassObject);
                    // add fields belonging to the new class
                    for (MyMemberVariable myMemberVariable : myClassObject.getMemberVariables()) {
                        InstrumentBody.addNewFieldToClass(myMemberVariable, sc, old_new_Fields, super2Class);
                    }
                    // add methods belonging to the new class
                    for (MyMethodObject myMethodObject : myClassObject.getMethodInBody()) {
                        SootMethod sm = InstrumentBody.createNewMethod(myMethodObject, sc, super2Class);
                        List<Stmt> StmtInNewMethod = new ArrayList<>();

                        Body body2 = InstrumentBody.getBodySafely(sm);
                        for (MyInputObject myInputObject : myMethodObject.getStmtInBody()) {
                            // Todo: VisitedStmt, remember to visit the inner classes
                            InstrumentBody.createSingleStmt(myInputObject, body2,
                                    old_new_VarArgs, old_new_Constants, old_new_Fields,
                                    VisitedStmtStack, SearchedStmt, dataObject,
                                    nopMap, CURRENT_DECLARING_CLASS, StmtInNewMethod,
                                    SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew,super2Class);
                        }

                        for (Stmt stmt2 : StmtInNewMethod) {
                            if (stmt2 == null) continue;
                            Chain<Unit> units2 = body2.getUnits();
                            units2.add(stmt2);
                        }
                        SaveVarOfNewMethod.clear();
                        oldStmtsToNew.clear();

                        try {
                            body2.validate();
                            System.out.println("[+] CallBack One new method validation success!");

                            try {
                                InstrumentUtil.Log2File("OneMethodSuccess", this.logPath, "null", this.dataObject.getIssueType(),
                                        sm.getSignature(), "null", "null",  this.SemanticPatchPath);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        } catch(Exception e) {
                            e.printStackTrace();
                            try {
                                InstrumentUtil.Log2File("Exception", this.logPath, e.toString(), this.dataObject.getIssueType(),
                                        sm.getSignature(), "null", "null",  this.SemanticPatchPath);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    break;

                case MyConstants.METHOD:
                    MyMethodObject myMethodObject = (MyMethodObject) addStatement;
                    // now default insert method to current class
                    SootMethod sm = InstrumentBody.createNewMethod(myMethodObject, b.getMethod().getDeclaringClass(), super2Class);
                    List<Stmt> StmtInNewMethod = new ArrayList<>();

                    Body body3 = InstrumentBody.getBodySafely(sm);
                    for (MyInputObject myInputObject : myMethodObject.getStmtInBody()) {
                        // Todo: VisitedStmt, remember to visit the inner classes
                        InstrumentBody.createSingleStmt(myInputObject, body3,
                                old_new_VarArgs, old_new_Constants, old_new_Fields,
                                VisitedStmtStack, SearchedStmt, dataObject,
                                nopMap, CURRENT_DECLARING_CLASS, StmtInNewMethod,
                                SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew, super2Class);
                    }

                    for (Stmt stmt3 : StmtInNewMethod) {
                        if (stmt3 == null) continue;
                        Chain<Unit> units3 = body3.getUnits();
                        units3.add(stmt3);
                    }
                    SaveVarOfNewMethod.clear();
                    oldStmtsToNew.clear();

                    try {
                        body3.validate();
                        System.out.println("[+] CallBack One new method validation success!");

                        try {
                            InstrumentUtil.Log2File("OneMethodSuccess", this.logPath, "null", this.dataObject.getIssueType(),
                                    sm.getSignature(), "null", "null",  this.SemanticPatchPath);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } catch(Exception e) {
                        e.printStackTrace();
                        try {
                            InstrumentUtil.Log2File("Exception", this.logPath, e.toString(), this.dataObject.getIssueType(),
                                    sm.getSignature(), "null", "null",  this.SemanticPatchPath);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    break;

                default:
                    // default insert to the current method
                    InstrumentBody.createSingleStmt(addStatement, b, old_new_VarArgs, old_new_Constants,
                            old_new_Fields, VisitedStmtStack, SearchedStmt, dataObject,
                            nopMap, CURRENT_DECLARING_CLASS, insertAftertheLast,
                            SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew, super2Class);
            }
        }

        for (Stmt tmpStmt: insertAftertheLast){
            assert unit != null;
            if (tmpStmt == null) continue;
            units.insertAfter(tmpStmt, unit);
            unit = tmpStmt;
        }

        if (if_contains_cut_operations.get()) {
            for (Stmt stmt: VisitedStmtForOperation)
                units.remove(stmt);
        }


        try {
            b.validate();
            System.out.println("[+] CallBack validation success!");
            try {
                InstrumentUtil.Log2File("Success", this.logPath, "null", this.dataObject.getIssueType(),
                        methodSig, "null", "null",  this.SemanticPatchPath);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        } catch(Exception e) {
            e.printStackTrace();
            try {
                InstrumentUtil.Log2File("Exception", this.logPath, e.toString(), this.dataObject.getIssueType(),
                        methodSig, "null", "null",  this.SemanticPatchPath);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

    }
}

