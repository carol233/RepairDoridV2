import Model.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JNopStmt;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeviceInstrumenter{

    private String logPath;
    private String pkg;
    private PatchParser dataObject;
    private String SemanticPatchPath;

    DeviceInstrumenter(String pkg, String SemanticPatchPath, String logFilePath, PatchParser dataObject) {
        this.logPath = logFilePath;
        this.pkg = pkg;
        this.dataObject = dataObject;
        this.SemanticPatchPath = SemanticPatchPath;
    }

    public void DeviceInternalTransform(Body b) {

        // todo superclass
        HashMap <String, String> super2Class = new HashMap<>();

        String methodSig = b.getMethod().getSignature();
        String CURRENT_DECLARING_CLASS = b.getMethod().getDeclaringClass().getName();

        // First we filter out Android framework methods
//        if (!methodSig.contains(this.pkg))
//            return;

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
                            (stmt.toString().contains(valueStr)
                                    || (stmt.toString().contains(apiOldVersion.getFieldType()) && (valueStr.contains(CURRENT_DECLARING_CLASS)))) ){

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
                        break;
                    }
                } else if (type.contains(MyConstants.INVOKE_Str)) {
                    assert targetStmt instanceof MyInvokeStmt;
                    assert stmt instanceof InvokeStmt || stmt instanceof AssignStmt;
                    if (stmt.toString().contains("if") && stmt.toString().contains("goto")){
                        continue;
                    }
                    MyInvokeStmt apiOldInvoke = (MyInvokeStmt) targetStmt;
                    String valueStr = apiOldInvoke.getMethodSignature().replace(MyConstants.DECLARING_CLASS_SIGN, CURRENT_DECLARING_CLASS);
                    if (stmt.toString().contains(valueStr)){
                        count_matched_old_stmt += 1;
                        if_found_this_stmt.set(true);
                        try {
                            List<Value> OldAPIargs = stmt.getInvokeExpr().getArgs();
                            FoundOldStmtTmpList.put(apiOldInvoke, stmt);
                            for (int i = 0; i < OldAPIargs.size(); i++) {
                                if(OldAPIargs.get(i) instanceof Constant) {
                                    old_new_Constants.put(apiOldInvoke.getArgNames().get(i), (Constant)OldAPIargs.get(i));
                                    continue;
                                }
                                old_new_VarArgs.put(apiOldInvoke.getArgNames().get(i), (Local)OldAPIargs.get(i));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (stmt instanceof AssignStmt) {
                            Local rtnLocal = (Local) ((AssignStmt) stmt).getLeftOp();
                            old_new_VarArgs.put(apiOldInvoke.getReturnName(), rtnLocal);
                        }
                        if (!apiOldInvoke.getBaseName().equals("")){
                            VirtualInvokeExpr tmpVirtualValue = (VirtualInvokeExpr) stmt.getInvokeExprBox().getValue();
                            Local BaseVar = (Local) tmpVirtualValue.getBase();
                            old_new_VarArgs.put(apiOldInvoke.getBaseName(), BaseVar);
                        }
                        break;
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
            } else if (count_matched_old_stmt < sizeofOldStmts) {
                continue;
            } else if (!stmt.containsInvokeExpr()) { // default setting: the last old stmt must contain invokeExpr
                continue;
            }
            // SimpleLocalDefs.getDefsOfAt()
            SootMethod InvokedMethod = stmt.getInvokeExpr().getMethod();
            String invokedMethodSignature = InvokedMethod.getSignature();

            // todo: now only allowing the last input is an invoke stmt
            MyInputObject apiOld = this.dataObject.getTargetStmts().get(sizeofOldStmts - 1);
            String type = apiOld.getType();
            MyInvokeStmt apiOldVersion;
            if (type.contains("invoke")) {
                apiOldVersion = (MyInvokeStmt) apiOld;
            } else {
                continue;
            }
            // localized the last old stmt
            if (!invokedMethodSignature.equals(apiOldVersion.getMethodSignature())) {
                continue;
            }

            try {
                InstrumentUtil.Log2File("Potential", this.logPath, invokedMethodSignature,
                        this.dataObject.getIssueType(), methodSig, "null", "null", this.SemanticPatchPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //check condition

            if (!Helper.checkIfIssueForDevice(b, this.dataObject.getIssueMethod()))
                return;

            try {
                InstrumentUtil.Log2File("Localization", this.logPath, invokedMethodSignature,
                        this.dataObject.getIssueType(), methodSig, "null", "null", this.SemanticPatchPath);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // search variables
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
                insertAfterStmts.addAll(newStmtsForSearchFields);
            }

            // insert the labels
            for (String myLabelStr: this.dataObject.getLabelStmts().keySet()) {
                NopStmt nopStmt = new JNopStmt();
                nopMap.put(this.dataObject.getLabelStmts().get(myLabelStr), nopStmt);
            }

            // insert other new stmts
            for (MyInputObject addStatement: this.dataObject.getAddStmts()) {
                InstrumentBody.createSingleStmt(addStatement, b,
                        old_new_VarArgs, old_new_Constants, old_new_Fields,
                        VisitedStmtStack, SearchedStmt, dataObject,
                        nopMap, CURRENT_DECLARING_CLASS, insertAfterStmts,
                        SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew, super2Class);
            }

            for (Stmt tmpStmt: insertAfterStmts) {
                // change the unit !
                units.insertAfter(tmpStmt, unit);
                unit = tmpStmt;
            }

            flag_if_insert.set(true);
            break;

        }

        if (! flag_if_insert.get())
            return;

        try {
            b.validate();
            System.out.println("[+] Device validation success!");
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

