import Model.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JNopStmt;
import soot.util.Chain;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class OSAPIInstrumenter {

    private String logPath;
    private String pkg;
    private PatchParser dataObject;
    private String SemanticPatchPath;

    OSAPIInstrumenter(String pkg, String SemanticPatchPath, String logFilePath, PatchParser dataObject) {
        this.logPath = logFilePath;
        this.pkg = pkg;
        this.dataObject = dataObject;
        this.SemanticPatchPath = SemanticPatchPath;
    }

    public void OSInternalTransform(Body b) {
        // todo superclass
        HashMap <String, String> super2Class = new HashMap<>();
        SootMethod smm = b.getMethod();
        String methodSig = smm.getSignature();

        String CURRENT_DECLARING_CLASS = b.getMethod().getDeclaringClass().getName();

        PatchingChain<Unit> units = b.getUnits();

        Stack <Stmt> VisitedStmtStack = new Stack<>();
        List <Stmt> VisitedStmtForOperation = new ArrayList<>();
        HashMap <Local, String> SaveVarOfCurrentMethod = new HashMap<>(); // {($r0, "@this")}
        HashMap <Local, String> SaveVarOfNewMethod = new HashMap<>();

        HashMap<MyForSearchStmt, Stmt> SearchedStmt = new HashMap<>();
        HashMap<MyInputObject, Stmt> FoundOldStmtTmpList = new HashMap<>();
        HashMap <Stmt, Stmt> oldStmtsToNew = new HashMap<>();
        HashMap<String, Local> old_new_VarArgs = new HashMap<>();
        HashMap<String, String> old_new_Fields = new HashMap<>();
        HashMap<String, Constant> old_new_Constants = new HashMap<>();
        List<Stmt> insertBeforeStmts = new ArrayList<>();
        List<Stmt> insertAfterStmts = new ArrayList<>();
        Map<MyLabelStmt, NopStmt> nopMap = new HashMap<>();
        int sizeofOldStmts = this.dataObject.getTargetStmts().size();
        int count_matched_old_stmt = 0;
        AtomicBoolean flag_if_insert = new AtomicBoolean(false);

        //获取Body里所有的units, 一个Body对应Java里一个方法的方法体，Unit代表里面的语句
        Iterator<Unit> unitsIterator = units.snapshotIterator();
        while (unitsIterator.hasNext()) {
            Unit unit = unitsIterator.next();
            //将Unit强制转换为Stmt, Stmt为Jimple里每条语句的表示
            Stmt stmt = (Stmt) unit;
            VisitedStmtStack.push(stmt);
            AtomicBoolean if_found_this_stmt = new AtomicBoolean(false);

            // for each stmt, check if it is in OldStmts,
            // if yes, then record the variables
            // localization
            for (MyInputObject targetStmt: this.dataObject.getTargetStmts()) {
                String type = targetStmt.getType();
                if (type.contains(MyConstants.ASSIGNMENT_Str)) {
                    MyAssignStmt apiOldVersion = (MyAssignStmt) targetStmt;
                    String valueStr = apiOldVersion.getValueStr();
                    valueStr = valueStr.replace(MyConstants.DECLARING_CLASS_SIGN, CURRENT_DECLARING_CLASS);
                    // only instancefeild and staticfeild
                    if ( !(stmt instanceof IfStmt) &&
                            !(stmt instanceof InvokeStmt) &&
                            !stmt.toString().contains("goto") &&
                            (stmt.toString().contains(valueStr) ||
                                    (stmt.toString().contains(apiOldVersion.getFieldType()) && (valueStr.contains(CURRENT_DECLARING_CLASS)))) ){

                        if (stmt.toString().contains(apiOldVersion.getFieldType()) && (valueStr.contains(CURRENT_DECLARING_CLASS))){
                            assert stmt instanceof AssignStmt;
                            Value right = ((AssignStmt) stmt).getRightOpBox().getValue();
                            String fieldType = "";
                            if (right instanceof InstanceFieldRef) {
                                fieldType = ((InstanceFieldRef) right).getField().getType().toString();
                            } else if (right instanceof StaticFieldRef){
                                fieldType = ((StaticFieldRef) right).getField().getType().toString();
                            }
                            if (!fieldType.equals(apiOldVersion.getFieldType())){
                                continue; // find the second original stmt
                            }
                        }

                        count_matched_old_stmt += 1;
                        if_found_this_stmt.set(true);
                        FoundOldStmtTmpList.put(apiOldVersion, stmt);
                        if (!apiOldVersion.getLeftVariableName().equals("")){
                            assert stmt instanceof AssignStmt;
                            if (! (((AssignStmt) stmt).getLeftOp() instanceof Local)){
                                // if fail, remember to minus 1
                                count_matched_old_stmt -= 1;
                                if_found_this_stmt.set(false);
                                break;
                            }
                            // todo: $r0.<$OnAudioFocusChangeListener afChangeListener> = $r7
                            // default: only solve the Local Var on the left
                            Local rtnLocal = (Local) ((AssignStmt) stmt).getLeftOp();
                            old_new_VarArgs.put(apiOldVersion.getLeftVariableName(), rtnLocal);
                        }
                        if (!apiOldVersion.getVarcastSourceName().equals("")){
                            assert stmt instanceof AssignStmt;
                            Value right = ((AssignStmt) stmt).getRightOpBox().getValue();
                            if (right instanceof CastExpr) {
                                Local source = (Local) ((CastExpr) right).getOp();
                                old_new_VarArgs.put(apiOldVersion.getLeftVariableName(), source);
                            }
                        }
                        if (!apiOldVersion.getAttributeBaseName().equals("")){
                            assert stmt instanceof AssignStmt;
                            Value right = ((AssignStmt) stmt).getRightOpBox().getValue();
                            if (right instanceof InstanceFieldRef) {
                                Local baseLocal = (Local) ((InstanceFieldRef) right).getBase();
                                old_new_VarArgs.put(apiOldVersion.getAttributeBaseName(), baseLocal);
                            }
                        }
                        if (!apiOldVersion.getFieldCustomName().equals("")){
                            // $r2 = $r0.<[DECLARING_CLASS]: ...Manager$OnAudioFocusChangeListener [FIELD_0]>
                            if (apiOldVersion.getFieldCustomName().matches("\\[FIELD_\\d+]")){
                                assert stmt instanceof AssignStmt;
                                Value right = ((AssignStmt) stmt).getRightOpBox().getValue();
                                if (right instanceof InstanceFieldRef) {
                                    String fieldCustomName = ((InstanceFieldRef) right).getField().getName();
                                    old_new_Fields.put(apiOldVersion.getFieldCustomName(), fieldCustomName);
                                } else if (right instanceof StaticFieldRef){
                                    String fieldCustomName = ((StaticFieldRef) right).getField().getName();
                                    old_new_Fields.put(apiOldVersion.getFieldCustomName(), fieldCustomName);
                                }
                            }
                        }
                    }
                }
                else if (type.contains(MyConstants.INVOKE_Str)) {
                    assert targetStmt instanceof MyInvokeStmt;
                    assert stmt instanceof InvokeStmt || stmt instanceof AssignStmt;
                    assert stmt.containsInvokeExpr();
                    if (stmt.toString().contains("if") && stmt.toString().contains("goto")){
                        continue;
                    }
                    MyInvokeStmt apiOldInvoke = (MyInvokeStmt) targetStmt;
                    String valueStr = InstrumentBody.replaceSuperClass(super2Class, apiOldInvoke.getMethodSignature());
                    if (stmt.toString().contains(valueStr)){

                        if (!stmt.containsInvokeExpr())
                            continue;

                        List<Value> OldAPIargs = stmt.getInvokeExpr().getArgs();
                        count_matched_old_stmt += 1;
                        if_found_this_stmt.set(true);
                        FoundOldStmtTmpList.put(apiOldInvoke, stmt);
                        for (int i = 0; i < OldAPIargs.size(); i++) {
                            if(OldAPIargs.get(i) instanceof Constant) {
                                old_new_Constants.put(apiOldInvoke.getArgNames().get(i), (Constant)OldAPIargs.get(i));
                                continue;
                            }
                            old_new_VarArgs.put(apiOldInvoke.getArgNames().get(i), (Local)OldAPIargs.get(i));
                        }
                        if (stmt instanceof AssignStmt) {
                            Local rtnLocal = (Local) ((AssignStmt) stmt).getLeftOp();
                            old_new_VarArgs.put(apiOldInvoke.getReturnName(), rtnLocal);
                        }
                        if (!apiOldInvoke.getBaseName().equals("")){
                            Local BaseVar = null;
                            if (stmt.getInvokeExprBox().getValue() instanceof VirtualInvokeExpr){
                                VirtualInvokeExpr tmpVirtualValue = (VirtualInvokeExpr) stmt.getInvokeExprBox().getValue();
                                BaseVar = (Local) tmpVirtualValue.getBase();
                            } else if (stmt.getInvokeExprBox().getValue() instanceof SpecialInvokeExpr){
                                SpecialInvokeExpr tmpVirtualValue = (SpecialInvokeExpr) stmt.getInvokeExprBox().getValue();
                                BaseVar = (Local) tmpVirtualValue.getBase();
                            } else if (stmt.getInvokeExprBox().getValue() instanceof InterfaceInvokeExpr){
                                InterfaceInvokeExpr tmpVirtualValue = (InterfaceInvokeExpr) stmt.getInvokeExprBox().getValue();
                                BaseVar = (Local) tmpVirtualValue.getBase();
                            } else {
                                System.out.println("[-] other type of invoke");
                            }
                            old_new_VarArgs.put(apiOldInvoke.getBaseName(), BaseVar);
                        }
                    }
                }
            }

            if (!if_found_this_stmt.get()){
                count_matched_old_stmt = 0;
                old_new_VarArgs.clear();
                old_new_Constants.clear();
                old_new_Fields.clear();
                FoundOldStmtTmpList.clear();
                continue;
            } else if (count_matched_old_stmt < sizeofOldStmts){
                continue;
            } else if (!stmt.containsInvokeExpr()){ // default setting: the last old stmt must contain invokeExpr
                continue;
            }

            // SimpleLocalDefs.getDefsOfAt()
            SootMethod InvokedMethod = stmt.getInvokeExpr().getMethod();
            String invokedMethodSignature = InvokedMethod.getSignature();

            try {
                InstrumentUtil.Log2File("Potential", this.logPath, invokedMethodSignature,
                        this.dataObject.getIssueType(), methodSig, "null", "null", this.SemanticPatchPath);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!Helper.checkIfHaveFixed(b, invokedMethodSignature))
                return;

            // todo: now only allowing the last input is an invoke stmt
            MyInputObject apiOld = this.dataObject.getTargetStmts().get(sizeofOldStmts - 1);
            String type = apiOld.getType();
            MyInvokeStmt apiOldVersion;
            if (type.contains(MyConstants.INVOKE_Str)) {
                apiOldVersion = (MyInvokeStmt) apiOld;
            } else {
                continue;
            }
            // localized the last old stmt
            if (!invokedMethodSignature.equals(apiOldVersion.getMethodSignature()))
                continue;

            try {
                InstrumentUtil.Log2File("Localization", this.logPath, invokedMethodSignature,
                        this.dataObject.getIssueType(), methodSig, "null", "null", this.SemanticPatchPath);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // search variables and fields
            ArrayList<Stmt> newStmtsForSearchFields = new ArrayList<>();
            for (Map.Entry<String, String> entry : this.dataObject.getVirables_for_search().entrySet()) {
                String varName = entry.getKey();
                String varType = entry.getValue();

                AtomicBoolean flag_if_found_var = InstrumentBody.searchVariables(VisitedStmtStack, b,
                        old_new_VarArgs, varName, varType, newStmtsForSearchFields);
                if (!flag_if_found_var.get()) {
                    System.out.println("Error (11): cannot search the variable, " + varName + " " + varType);
                    return;
                }
            }
            if (newStmtsForSearchFields.size() > 0) {
                insertBeforeStmts.addAll(newStmtsForSearchFields);
            }

            // insert the labels
            for (String myLabelStr: this.dataObject.getLabelStmts().keySet()){
                NopStmt nopStmt = new JNopStmt();
                nopMap.put(this.dataObject.getLabelStmts().get(myLabelStr), nopStmt);
            }

            // insert other new stmts
            for (MyInputObject addStatement: this.dataObject.getAddStmts()) {

                if (addStatement.getType().equals(MyConstants.CLASS)) {
                    MyClassObject myClassObject = (MyClassObject) addStatement;
                    SootClass sc = InstrumentBody.createNewClass(myClassObject);
                    // add methods belonging to the new class
                    for (MyMethodObject myMethodObject: myClassObject.getMethodInBody()) {
                        SootMethod sm = InstrumentBody.createNewMethod(myMethodObject, sc, super2Class);
                        Body body2 = InstrumentBody.getBodySafely(sm);
                        List<Stmt> StmtInNewMethod = new ArrayList<>();
                        for (MyInputObject myInputObject: myMethodObject.getStmtInBody()) {
                            // Todo: VisitedStmt, remember to visit the inner classes
                            InstrumentBody.createSingleStmt(myInputObject, body2,
                                    old_new_VarArgs, old_new_Constants, old_new_Fields,
                                    VisitedStmtStack, SearchedStmt, dataObject,
                                    nopMap, CURRENT_DECLARING_CLASS, StmtInNewMethod,
                                    SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew, super2Class);
                        }
                        for (Stmt stmt2: StmtInNewMethod) {
                            Chain<Unit> units2 = body2.getUnits();
                            units2.add(stmt2);
                        }
                    }
                    // add fields belonging to the new class
                    for (MyMemberVariable myMemberVariable: myClassObject.getMemberVariables()) {
                        InstrumentBody.addNewFieldToClass(myMemberVariable, sc, old_new_Fields, super2Class);
                    }

                }
                else if (addStatement.getType().equals(MyConstants.METHOD)) {
                    MyMethodObject myMethodObject = (MyMethodObject) addStatement;
                    // now default insert method to current class
                    SootMethod sm = InstrumentBody.createNewMethod(myMethodObject, b.getMethod().getDeclaringClass(), super2Class);
                    List<Stmt> StmtInNewMethod = new ArrayList<>();
                    Body body3 = InstrumentBody.getBodySafely(sm);
                    for (MyInputObject myInputObject: myMethodObject.getStmtInBody()) {
                        // Todo: VisitedStmt, remember to visit the inner classes
                        InstrumentBody.createSingleStmt(myInputObject, body3,
                                old_new_VarArgs, old_new_Constants, old_new_Fields,
                                VisitedStmtStack, SearchedStmt, dataObject,
                                nopMap, CURRENT_DECLARING_CLASS, StmtInNewMethod,
                                SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew, super2Class);
                    }
                    for (Stmt stmt3: StmtInNewMethod) {
                        Chain<Unit> units3 = body3.getUnits();
                        units3.add(stmt3);
                    }
                }
                else {
                    // default insert to the current method
                    InstrumentBody.createSingleStmt(addStatement, b,
                            old_new_VarArgs, old_new_Constants, old_new_Fields,
                            VisitedStmtStack, SearchedStmt, dataObject,
                            nopMap, CURRENT_DECLARING_CLASS, insertBeforeStmts,
                            SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew, super2Class);
                }
            }

            for (MyInputObject nextStatement: this.dataObject.getNextStmts()) {
                InstrumentBody.createSingleStmt(nextStatement, b,
                        old_new_VarArgs, old_new_Constants, old_new_Fields,
                        VisitedStmtStack, SearchedStmt, dataObject,
                        nopMap, CURRENT_DECLARING_CLASS, insertAfterStmts,
                        SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew, super2Class);
            }

            // Insert start
            for (Stmt tmpStmt: insertAfterStmts){
                units.insertAfter(tmpStmt, unit);
            }

            // Find the first and insert
            MyInputObject theFirstOldStmt = this.dataObject.getTargetStmts().get(0);
            Stmt tmpFirstStmt = FoundOldStmtTmpList.get(theFirstOldStmt);
            b.getMethod().getActiveBody().getUnits().stream().anyMatch(item -> {
                if (item.equals(tmpFirstStmt)){
                    for (Stmt tmpStmt: insertBeforeStmts) {
                        units.insertBefore(tmpStmt, item);
                    }
                    return true;
                }
                return false;
            });

            flag_if_insert.set(true);

            // Init: except for Visited
            count_matched_old_stmt = 0;
            FoundOldStmtTmpList.clear();
            SearchedStmt.clear();
            old_new_VarArgs.clear();
            old_new_Fields.clear();
            old_new_Constants.clear();
            insertBeforeStmts.clear();
            insertAfterStmts.clear();
            nopMap.clear();

        }

        if (!flag_if_insert.get())
            return;

        try {
            b.validate();
            System.out.println("[+] OS validation success!");
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

