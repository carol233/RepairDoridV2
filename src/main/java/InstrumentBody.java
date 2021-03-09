import Model.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.util.Chain;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class InstrumentBody {

    static SootMethod createNewMethod(MyMethodObject methodObject, SootClass sc, HashMap<String, String> super2Class) {

        String methodName = methodObject.getMethodName();
        List<Type> paramTypes = createTypesList(methodObject.getParameterTypeList(), sc, super2Class);
        Type rtnType = getRightType(super2Class, methodObject.getReturnType());

        int modifiers = 0;
        if (methodObject.isIfPublic())
            modifiers |= Modifier.PUBLIC;
        if (methodObject.isIfFinal())
            modifiers |= Modifier.FINAL;
        if (methodObject.isIfPrivate())
            modifiers |= Modifier.PRIVATE;
        if(methodObject.isIfProtected())
            modifiers |= Modifier.PROTECTED;
        if (methodObject.isIfStatic())
            modifiers |= Modifier.STATIC;

        SootMethod sm;
        sm = new SootMethod(methodName, paramTypes, rtnType, modifiers);

        addMethodToClass(sc, sm);

        JimpleBody body = Jimple.v().newBody(sm);
        sm.setActiveBody(body);

        return sm;
    }

    static SootClass createNewClass(MyClassObject classObject) {

        Scene.v().loadClassAndSupport("java.lang.Object");
        Scene.v().loadClassAndSupport("java.lang.System");

        String className = classObject.getDeclaringClassName();

        int modifiers = 0;
        if (classObject.isIfPublic())
            modifiers |= Modifier.PUBLIC;
        if (classObject.isIfFinal())
            modifiers |= Modifier.FINAL;
        if (classObject.isIfPrivate())
            modifiers |= Modifier.PRIVATE;
        if(classObject.isIfProtected())
            modifiers |= Modifier.PROTECTED;
        if (classObject.isIfStatic())
            modifiers |= Modifier.STATIC;
        if (classObject.isIfAbstract())
            modifiers |= Modifier.ABSTRACT;

        SootClass sc;
        // declaration
        sc = new SootClass(className, modifiers);

        //extends Object
        if (classObject.isHasSuperClass()) {
            sc.setSuperclass(Scene.v().getSootClass(classObject.getSuperClassName()));
        } else { // default
            sc.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        }

        Scene.v().addClass(sc);
        return sc;
    }

    static void addNewFieldToClass(MyMemberVariable myMemberVariable, SootClass sc, HashMap<String, String> old_new_Fields,
                    HashMap<String, String> super2Class) {
        int modifiers = 0;
        if (myMemberVariable.isIfPublic())
            modifiers |= Modifier.PUBLIC;
        if (myMemberVariable.isIfFinal())
            modifiers |= Modifier.FINAL;
        if (myMemberVariable.isIfPrivate())
            modifiers |= Modifier.PRIVATE;
        if(myMemberVariable.isIfProtected())
            modifiers |= Modifier.PROTECTED;
        if (myMemberVariable.isIfStatic())
            modifiers |= Modifier.STATIC;

        String customFeild = myMemberVariable.getVariableName();
        if (old_new_Fields.containsKey(customFeild)){
            customFeild = customFeild.replace(customFeild, old_new_Fields.get(customFeild));
        }

        // create a new field, while others just refer to existing fields
        SootField sootField = new SootField(customFeild, getRightType(super2Class, myMemberVariable.getVariableTypeName()));
        sc.addField(sootField);
    }

    static List<Type> createTypesList(List<String> argTypes, SootClass sc, HashMap<String, String> super2Class) {
        List<Type> parameterTypes = new ArrayList<>();
        for (String argType : argTypes) {
            Type argNewType = getRightType(super2Class, argType);
            parameterTypes.add(argNewType);
        }
        return parameterTypes;
    }

    static AtomicBoolean searchVariables(Stack<Stmt> VisitedStmt, Body b, HashMap<String, Local> old_new_VarArgs,
                                         String varName, String varType, ArrayList<Stmt> newStmtsForSearchFields) {
        AtomicBoolean flag = new AtomicBoolean(false);

        // 1. search in the current method
        Stack<Stmt> Visited_clone = (Stack<Stmt>) VisitedStmt.clone();
        while(!Visited_clone.empty() && !flag.get()){
            Stmt tmpSt = Visited_clone.pop();
            if (tmpSt instanceof AssignStmt) {
                AssignStmt tmpassignStmt = (AssignStmt)tmpSt;
                if (tmpassignStmt.getLeftOp() instanceof Local){
                    Local tmpLeft = (Local) tmpassignStmt.getLeftOp();
                    Type tmpLeftType = tmpLeft.getType();
                    if (tmpLeftType.toString().equals(varType)) {
                        old_new_VarArgs.put(varName, tmpLeft);
                        flag.set(true);
                        return flag;
                    }
                }
            }
        }

        //2. search in the class
        SootClass sootClass = b.getMethod().getDeclaringClass();
        for (SootField currentField: sootClass.getFields()) {
            if (currentField.getType().toString().equals(varType)){
                Local newLocal = InstrumentUtil.generateNewLocal(b, currentField.getType());
                StaticFieldRef newFieldRef = Jimple.v().newStaticFieldRef(currentField.makeRef());
                Stmt newStmt = Jimple.v().newAssignStmt(newLocal, newFieldRef);
                newStmtsForSearchFields.add(newStmt);
                flag.set(true);
                return flag;
            }
        }

        return flag;
    }

    static void addStmtToMethodInorder (SootMethod sm, Stmt stmt) {
        Body body = sm.retrieveActiveBody();
        Chain<Unit> units2 = body.getUnits();
        units2.add(stmt);
    }

    static void addMethodToClass (SootClass sc, SootMethod sm) {
        try{
            sc.addMethod(sm);
        } catch (RuntimeException e){
            e.printStackTrace();
        }
    }

    static boolean ifHasSuperClass (SootClass sc) {
        return sc.hasSuperclass() && !sc.getSuperclassUnsafe().getName().equals("java.lang.Object");
    }

    public static List<SootClass> getAllSuperClasses (SootClass sc) {
        List<SootClass> rtn = new ArrayList<>();
        SootClass sc1 = sc;
        while (sc1 != null) {
            sc1 = sc1.getSuperclassUnsafe();
            if (sc1 != null && ! sc1.getName().equals("java.lang.Object"))
                rtn.add(sc1);
            else
                break;
        }
        return rtn;
    }

    static SootClass getSootClassOfLocal (Local v1) {
        RefType refTypeV1 = RefType.v(v1.getType().toString());
        return refTypeV1.getSootClass();
    }

    static Type getRightType(HashMap <String, String> super2Class, String s) {
        Type theType = null;
        switch (s) {
            case "void":
                theType = VoidType.v();
                break;
            case "int":
                theType = IntType.v();
                break;
            case "boolean":
                theType = BooleanType.v();
                break;
            case "long":
                theType = LongType.v();
                break;
            case "short":
                theType = ShortType.v();
                break;
            case "float":
                theType = FloatType.v();
                break;
            case "double":
                theType = DoubleType.v();
                break;
            case "long[]":
                theType = ArrayType.v(LongType.v(), 1);
                break;
            case "int[]":
                theType = ArrayType.v(IntType.v(), 1);
                break;
            case "short[]":
                theType = ArrayType.v(ShortType.v(), 1);
                break;
            case "float[]":
                theType = ArrayType.v(FloatType.v(), 1);
                break;
            case "boolean[]":
                theType = ArrayType.v(BooleanType.v(), 1);
                break;
            default: {
                if (s.contains("[]")) {
                    String baseType = s.replace("[]", "");
                    baseType = replaceSuperClass(super2Class, baseType);
                    theType = ArrayType.v(RefType.v(baseType), 1);
                } else {
                    try {
                        theType = RefType.v(replaceSuperClass(super2Class, s));
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        return theType;
    }

    static Local ifHasExtendRelation(Local v1, Local v2) {
        SootClass sc1 = getSootClassOfLocal(v1);
        SootClass sc2 = getSootClassOfLocal(v2);

        List<SootClass> tmp1 = getAllSuperClasses(sc1);
        List<SootClass> tmp2 = getAllSuperClasses(sc2);

        if (tmp1.contains(sc2)) {
            return v2;
        } else if (tmp2.contains(sc1)) {
            return v1;
        } else {
            return null;
        }
    }

    static Body getBodySafely(SootMethod method) {
        try {
            return method.getActiveBody();
        } catch (Exception exception) {
            return method.retrieveActiveBody();
        }
    }

    private static Local getLocalForPasteOperation(Body b, Local old_l, HashMap<Local, Local> oldLocalsToNew) {
        Local new_l;
        if (oldLocalsToNew.containsKey(old_l))
            new_l = oldLocalsToNew.get(old_l);
        else {
            new_l = (Local) old_l.clone();
            b.getLocals().add(new_l);
            oldLocalsToNew.put(old_l, new_l);
        }
        return new_l;
    }


    private static InvokeStmt getFirstSpecialInvoke(Body b) {
        for (Unit u : b.getUnits()) {
            Stmt s = (Stmt) u;
            if (!(s instanceof InvokeStmt)) {
                continue;
            }

            InvokeExpr invokeExpr = ((InvokeStmt) s).getInvokeExpr();
            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            return (InvokeStmt) s;
        }
        // but there will always be either a call to this() or to super()
        // from the constructor
        return null;
    }


    private static void TransferOldStmtsToNew (Body b,
                                              List<Stmt> insertLocationList,
                                              HashMap <Local, String> SaveVarOfCurrentMethod,
                                              HashMap <Local, String> SaveVarOfNewMethod,
                                              List <Stmt> VisitedStmtForOperation,
                                              HashMap<Stmt, Stmt> oldStmtsToNew
    ) {

        // InvokeStmt firstSpecialInvoke = getFirstSpecialInvoke(b);
        // Chain<Unit> containerUnits = b.getUnits();

        HashMap <Local, Local> oldLocalsToNew = new HashMap<>();
        HashMap <Stmt, NopStmt> OldStmtToLabel = new HashMap<>();

        // utilize the relationships of @this and @parameter order:
        // Must be one-to-one correspondence
        // oldLocalsToNew initialized
        List<Local> addedLocal2 = new ArrayList<>();
        for (Local local1: SaveVarOfCurrentMethod.keySet())
            for (Local local2: SaveVarOfNewMethod.keySet()) {
                if (!addedLocal2.contains(local2) &&
                        SaveVarOfCurrentMethod.get(local1).equals(SaveVarOfNewMethod.get(local2))) {
                    oldLocalsToNew.put(local1, local2);
                    addedLocal2.add(local2);
                    break;
                }
            }

        // for the vars that have not been searched
        for(Local local1: SaveVarOfCurrentMethod.keySet()){
            if (!oldLocalsToNew.containsKey(local1)){
                for (Local local2: SaveVarOfNewMethod.keySet()) {
                    if (!addedLocal2.contains(local2)) {
                        SootClass clocal1 = ((RefType)local1.getType()).getSootClass();
                        SootClass clocal2 = ((RefType)local2.getType()).getSootClass();
                        if (ifHasSuperClass(clocal2)){
                            List<SootClass> allclasses = getAllSuperClasses(clocal2);
                            if (allclasses.contains(clocal1)) {
                                oldLocalsToNew.put(local1, local2);
                                addedLocal2.add(local2);
                                break;
                            }
                        }
                    }
                }
            }
        }


        for (Stmt stmt: VisitedStmtForOperation) {
            // handle different stmts
            Stmt newStmt = new JNopStmt();
            if (stmt instanceof ThrowStmt) {
                ThrowStmt throwStmt = (ThrowStmt) stmt;
                Value target = throwStmt.getOp();
                Local newTarget = getLocalForPasteOperation(b, (Local) target, oldLocalsToNew);
                newStmt = new JThrowStmt(newTarget);
            }
            else if (stmt instanceof IdentityStmt) {
                IdentityStmt idStmt = (IdentityStmt) stmt;
                if (idStmt.getRightOp() instanceof CaughtExceptionRef) {
                    newStmt = (Stmt) stmt.clone();
                    for (ValueBox next : newStmt.getUseAndDefBoxes()) {
                        if (next.getValue() instanceof Local) {
                            next.setValue(oldLocalsToNew.get(next.getValue()));
                        }
                    }
                }
            }
            else if (stmt instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) stmt;
                ConditionExpr expr = (ConditionExpr) ifStmt.getCondition();
                ConditionExpr newExpr = null;
                if (expr.getOp1() instanceof Local && expr.getOp2() instanceof Local) {
                    Local newOp1 = getLocalForPasteOperation(b, (Local) expr.getOp1(), oldLocalsToNew);
                    Local newOp2 = getLocalForPasteOperation(b, (Local) expr.getOp2(), oldLocalsToNew);
                    if ((expr instanceof EqExpr)){
                        newExpr = new JEqExpr(newOp1, newOp2);
                    } else if (expr instanceof NeExpr){
                        newExpr = new JNeExpr(newOp1, newOp2);
                    } else if (expr instanceof LeExpr) {
                        newExpr = new JLeExpr(newOp1, newOp2);
                    } else if (expr instanceof GeExpr) {
                        newExpr = new JGeExpr(newOp1, newOp2);
                    } else if (expr instanceof LtExpr) {
                        newExpr = new JLtExpr(newOp1, newOp2);
                    } else if (expr instanceof GtExpr) {
                        newExpr = new JGtExpr(newOp1, newOp2);
                    }
                } else if (expr.getOp1() instanceof Local){
                    Local newOp1 = getLocalForPasteOperation(b, (Local) expr.getOp1(), oldLocalsToNew);
                    if ((expr instanceof EqExpr)){
                        newExpr = new JEqExpr(newOp1, expr.getOp2());
                    } else if (expr instanceof NeExpr){
                        newExpr = new JNeExpr(newOp1, expr.getOp2());
                    } else if (expr instanceof LeExpr) {
                        newExpr = new JLeExpr(newOp1, expr.getOp2());
                    } else if (expr instanceof GeExpr) {
                        newExpr = new JGeExpr(newOp1, expr.getOp2());
                    } else if (expr instanceof LtExpr) {
                        newExpr = new JLtExpr(newOp1, expr.getOp2());
                    } else if (expr instanceof GtExpr) {
                        newExpr = new JGtExpr(newOp1, expr.getOp2());
                    }
                } else {
                    Local newOp2 = getLocalForPasteOperation(b, (Local) expr.getOp2(), oldLocalsToNew);
                    if ((expr instanceof EqExpr)){
                        newExpr = new JEqExpr(expr.getOp1(), newOp2);
                    } else if (expr instanceof NeExpr){
                        newExpr = new JNeExpr(expr.getOp1(), newOp2);
                    } else if (expr instanceof LeExpr) {
                        newExpr = new JLeExpr(expr.getOp1(), newOp2);
                    } else if (expr instanceof GeExpr) {
                        newExpr = new JGeExpr(expr.getOp1(), newOp2);
                    } else if (expr instanceof LtExpr) {
                        newExpr = new JLtExpr(expr.getOp1(), newOp2);
                    } else if (expr instanceof GtExpr) {
                        newExpr = new JGtExpr(expr.getOp1(), newOp2);
                    }
                }
                Stmt oldTarget = ifStmt.getTarget();
                NopStmt nopStmt = Jimple.v().newNopStmt();
                OldStmtToLabel.put(oldTarget, nopStmt);
                newStmt = new JIfStmt(newExpr, nopStmt);
            }
            else if (stmt instanceof GotoStmt) {
                GotoStmt gotoStmt = (GotoStmt) stmt;
                Unit oldTarget = gotoStmt.getTarget();
                NopStmt nopStmt = Jimple.v().newNopStmt();
                OldStmtToLabel.put((Stmt)oldTarget, nopStmt);
                // when inserting, then replace the target stmt
                newStmt = new JGotoStmt(nopStmt);
            }
            else if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
                if (stmt instanceof ReturnStmt) {
                    ReturnStmt returnStmt = (ReturnStmt) stmt;
                    Value returnValue = returnStmt.getOp();
                    if (returnValue instanceof Local) {
                        Local newReturnValue = getLocalForPasteOperation(b, (Local) returnValue, oldLocalsToNew);
                        newStmt = new JReturnStmt(newReturnValue);
                    } else {
                        newStmt = new JReturnStmt(returnValue);
                    }
                } else {
                    newStmt = new JReturnVoidStmt();
                }
            }
            else if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                if (assignStmt.containsInvokeExpr()) {
                    if (assignStmt.getRightOp() instanceof InvokeExpr) {
                        // TODO: 16/1/21  other types
                        InvokeExpr invokeExpr = (InvokeExpr) assignStmt.getRightOp();
                        Local tmpLeft = (Local) assignStmt.getLeftOp();
                        Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                        List <Value> newArgsList = new ArrayList<>();
                        for (Value arg : invokeExpr.getArgs()) {
                            if (arg instanceof Constant) {
                                newArgsList.add(arg);
                            } else if (arg instanceof Local) {
                                Local rightArg = (Local) arg;
                                Local newRightArg = getLocalForPasteOperation(b, rightArg, oldLocalsToNew);
                                newArgsList.add(newRightArg);
                            } else {
                                System.out.println("Arg type error.");
                            }
                        }

                        InvokeExpr newInvokeExpr = null;
                        if (invokeExpr instanceof SpecialInvokeExpr) {
                            Local baseLocal = (Local) ((SpecialInvokeExpr) invokeExpr).getBase();
                            Local newBase = getLocalForPasteOperation(b, baseLocal, oldLocalsToNew);
                            newInvokeExpr = new JSpecialInvokeExpr(newBase,
                                    invokeExpr.getMethodRef(), newArgsList);
                        } else if (invokeExpr instanceof StaticInvokeExpr) {
                            newInvokeExpr = new JStaticInvokeExpr(invokeExpr.getMethodRef(), newArgsList);
                        } else if (invokeExpr instanceof VirtualInvokeExpr) {
                            Local baseLocal = (Local) ((VirtualInvokeExpr) invokeExpr).getBase();
                            Local newBase = getLocalForPasteOperation(b, baseLocal, oldLocalsToNew);
                            newInvokeExpr = new JVirtualInvokeExpr(newBase,
                                    invokeExpr.getMethodRef(), newArgsList);
                        } else if (invokeExpr instanceof InterfaceInvokeExpr) {
                            Local baseLocal = (Local) ((InterfaceInvokeExpr) invokeExpr).getBase();
                            Local newBase = getLocalForPasteOperation(b, baseLocal, oldLocalsToNew);
                            newInvokeExpr = new JInterfaceInvokeExpr(newBase,
                                    invokeExpr.getMethodRef(), newArgsList);
                        } else {
                            // todo: other, like instanceInvoke
                        }

                        newStmt = new JAssignStmt(newLeft, newInvokeExpr);
                    }
                }
                else if (assignStmt.containsFieldRef()) {
                    FieldRef newFieldRef = null;
                    if (assignStmt.getRightOp() instanceof FieldRef) {
                        Local tmpLeft = (Local) assignStmt.getLeftOp();
                        Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                        FieldRef fieldRef = (FieldRef) assignStmt.getRightOp();
                        if (fieldRef instanceof StaticFieldRef) {
                            newFieldRef = fieldRef;
                        } else if (fieldRef instanceof InstanceFieldRef) {
                            Local tmpOldBase = (Local) ((InstanceFieldRef) fieldRef).getBase();
                            Local newBase = getLocalForPasteOperation(b, tmpOldBase, oldLocalsToNew);
                            newFieldRef = new JInstanceFieldRef(newBase, fieldRef.getFieldRef());
                        }
                        newStmt = new JAssignStmt(newLeft, newFieldRef);
                    } else { // assignStmt.getLeftOp() instanceof FieldRef
                        FieldRef fieldRef = (FieldRef) assignStmt.getLeftOp();
                        if (fieldRef instanceof StaticFieldRef) {
                            newFieldRef = fieldRef;
                        } else if (fieldRef instanceof InstanceFieldRef) {
                            Local tmpOldBase = (Local) ((InstanceFieldRef) fieldRef).getBase();
                            Local newBase = getLocalForPasteOperation(b, tmpOldBase, oldLocalsToNew);
                            newFieldRef = new JInstanceFieldRef(newBase, fieldRef.getFieldRef());
                        }

                        Value tmpRight = assignStmt.getRightOp();
                        Value newRight = null;
                        if (tmpRight instanceof Constant) {
                            newRight = tmpRight;
                        } else if (tmpRight instanceof Local) { // Local
                            newRight = getLocalForPasteOperation(b, (Local)tmpRight, oldLocalsToNew);
                        } else {
                            System.out.println("Arg error.");
                        }
                        newStmt = new JAssignStmt(newFieldRef, newRight);

                    }
                }
                else if (assignStmt.containsArrayRef()) {
                    ArrayRef aRef = assignStmt.getArrayRef();
                    Local aBase = (Local) aRef.getBase();
                    Local newAbase = getLocalForPasteOperation(b, aBase, oldLocalsToNew);

                    if (assignStmt.getRightOpBox().getValue() instanceof ArrayRef) {
                        Local tmpLeft = (Local) assignStmt.getLeftOp();
                        Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                        ArrayRef newArrayRef = Jimple.v().newArrayRef(newAbase, aRef.getIndex());
                        newStmt = Jimple.v().newAssignStmt(newLeft, newArrayRef);
                    } else {
                        Local tmpRight = (Local) assignStmt.getRightOp();
                        Local newRight = getLocalForPasteOperation(b, tmpRight, oldLocalsToNew);
                        ArrayRef newArrayRef = Jimple.v().newArrayRef(newAbase, aRef.getIndex());
                        newStmt = Jimple.v().newAssignStmt(newArrayRef, newRight);
                    }
                }
                else if (assignStmt.getRightOp() instanceof  LengthExpr) {
                    LengthExpr rightRef = (LengthExpr) assignStmt.getRightOp();
                    Local tmpLeft = (Local) assignStmt.getLeftOp();
                    Local tmpRight = (Local) rightRef.getOp();

                    Local newRight = getLocalForPasteOperation(b, tmpRight, oldLocalsToNew);
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);

                    newStmt = new JAssignStmt(newLeft, new JLengthExpr(newRight));
                }
                else if (assignStmt.getRightOp() instanceof CastExpr) {
                    Local tmpRight = (Local) ((CastExpr) assignStmt.getRightOp()).getOp();
                    Local tmpLeft = (Local) assignStmt.getLeftOp();

                    Local newRight = getLocalForPasteOperation(b, tmpRight, oldLocalsToNew);
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);

                    if (!newRight.getType().equals(newLeft.getType())) {
                        CastExpr cast = Jimple.v().newCastExpr(newRight, newLeft.getType());
                        newStmt = new JAssignStmt(newLeft, cast);
                    } else {
                        newStmt = new JAssignStmt(newLeft, newRight);
                    }

                }
                else if (assignStmt.getRightOp() instanceof NewExpr) {
                    Local tmpLeft = (Local) assignStmt.getLeftOp();
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);

                    NewExpr tmpRight = (NewExpr) assignStmt.getRightOp();
                    newStmt = new JAssignStmt(newLeft, tmpRight);
                }
                else if (assignStmt.getRightOp() instanceof Constant) {
                    Local tmpLeft = (Local) assignStmt.getLeftOp();
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                    newStmt = new JAssignStmt(newLeft, assignStmt.getRightOp());
                }
                else if (assignStmt.getRightOp() instanceof AddExpr) {
                    AddExpr addExpr = (AddExpr) assignStmt.getRightOp();
                    Local tmpLeft = (Local) assignStmt.getLeftOp();
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                    Value right1 = addExpr.getOp1();
                    Value right2 = addExpr.getOp1();
                    if (right1 instanceof Local) {
                        right1 = getLocalForPasteOperation(b, (Local)right1, oldLocalsToNew);
                    }
                    if (right2 instanceof Local) {
                        right2 = getLocalForPasteOperation(b, (Local)right2, oldLocalsToNew);
                    }
                    newStmt = new JAssignStmt(newLeft, new JAddExpr(right1, right2));
                }
                else if (assignStmt.getRightOp() instanceof SubExpr) {
                    SubExpr subExpr = (SubExpr) assignStmt.getRightOp();
                    Local tmpLeft = (Local) assignStmt.getLeftOp();
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                    Value right1 = subExpr.getOp1();
                    Value right2 = subExpr.getOp1();
                    if (right1 instanceof Local) {
                        right1 = getLocalForPasteOperation(b, (Local)right1, oldLocalsToNew);
                    }
                    if (right2 instanceof Local) {
                        right2 = getLocalForPasteOperation(b, (Local)right2, oldLocalsToNew);
                    }
                    newStmt = new JAssignStmt(newLeft, new JSubExpr(right1, right2));
                }
                else if (assignStmt.getRightOp() instanceof MulExpr) {
                    MulExpr mulExpr = (MulExpr) assignStmt.getRightOp();
                    Local tmpLeft = (Local) assignStmt.getLeftOp();
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                    Value right1 = mulExpr.getOp1();
                    Value right2 = mulExpr.getOp1();
                    if (right1 instanceof Local) {
                        right1 = getLocalForPasteOperation(b, (Local)right1, oldLocalsToNew);
                    }
                    if (right2 instanceof Local) {
                        right2 = getLocalForPasteOperation(b, (Local)right2, oldLocalsToNew);
                    }
                    newStmt = new JAssignStmt(newLeft, new JMulExpr(right1, right2));
                }
                else if (assignStmt.getRightOp() instanceof DivExpr) {
                    DivExpr divExpr = (DivExpr) assignStmt.getRightOp();
                    Local tmpLeft = (Local) assignStmt.getLeftOp();
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                    Value right1 = divExpr.getOp1();
                    Value right2 = divExpr.getOp1();
                    if (right1 instanceof Local) {
                        right1 = getLocalForPasteOperation(b, (Local)right1, oldLocalsToNew);
                    }
                    if (right2 instanceof Local) {
                        right2 = getLocalForPasteOperation(b, (Local)right2, oldLocalsToNew);
                    }
                    newStmt = new JAssignStmt(newLeft, new JDivExpr(right1, right2));
                }
                else {
                    Local tmpLeft = (Local) assignStmt.getLeftOp();
                    Local newLeft = getLocalForPasteOperation(b, tmpLeft, oldLocalsToNew);
                    newStmt = new JAssignStmt(newLeft, assignStmt.getRightOp());
                }
            }
            else if (stmt instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) stmt;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                List <Value> newArgsList = new ArrayList<>();
                for (Value arg : invokeStmt.getInvokeExpr().getArgs()) {
                    if (arg instanceof Constant) {
                        newArgsList.add(arg);
                    } else if (arg instanceof Local) {
                        Local rightArg = (Local) arg;
                        Local newRightArg = getLocalForPasteOperation(b, rightArg, oldLocalsToNew);
                        newArgsList.add(newRightArg);
                    } else {
                        System.out.println("Other variables.");
                    }
                }

                InvokeExpr newInvokeExpr = null;
                if (invokeStmt.getInvokeExpr() instanceof SpecialInvokeExpr) {
                    Local tmpbase = (Local) ((SpecialInvokeExpr) invokeExpr).getBase();
                    Local newBase = getLocalForPasteOperation(b, tmpbase, oldLocalsToNew);
                    newInvokeExpr = new JSpecialInvokeExpr(newBase,
                            invokeExpr.getMethodRef(), newArgsList);
                } else if (invokeStmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                    Local tmpbase = (Local) ((VirtualInvokeExpr) invokeExpr).getBase();
                    Local newBase = getLocalForPasteOperation(b, tmpbase, oldLocalsToNew);
                    newInvokeExpr = new JVirtualInvokeExpr(newBase,
                            invokeExpr.getMethodRef(), newArgsList);
                } else if (invokeStmt.getInvokeExpr() instanceof InterfaceInvokeExpr) {
                    Local tmpbase = (Local) ((InterfaceInvokeExpr) invokeExpr).getBase();
                    Local newBase = getLocalForPasteOperation(b, tmpbase, oldLocalsToNew);
                    newInvokeExpr = new JInterfaceInvokeExpr(newBase,
                            invokeExpr.getMethodRef(), newArgsList);
                } else if (invokeStmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                    newInvokeExpr = new JStaticInvokeExpr(invokeExpr.getMethodRef(), newArgsList);
                }

                newStmt = new JInvokeStmt(newInvokeExpr);

            }
            else {
                // TODO: 17/1/21  other cases
            }

            if (OldStmtToLabel.containsKey(stmt)){
                insertLocationList.add(OldStmtToLabel.get(stmt));
            }
            insertLocationList.add(newStmt);
            oldStmtsToNew.put(stmt, newStmt);
        }
    }

    public static String replaceSuperClass(HashMap<String, String> super2Class, String original) {
        for (String key: super2Class.keySet()){
            if (original.contains(key)){
                return original.replace(key, super2Class.get(key))
                        .replace("[", "").replace("]", "");
            }
        }
        return original;
    }

    static void createSingleStmt(MyInputObject addStatement,
                              Body b,
                              HashMap<String, Local> old_new_VarArgs,
                              HashMap<String, Constant> old_new_Constants,
                              HashMap<String, String> old_new_Fields,
                              Stack<Stmt> VisitedStmt,
                              HashMap<MyForSearchStmt, Stmt> SearchedStmt,
                              PatchParser dataObject,
                              Map<MyLabelStmt, NopStmt> nopMap,
                              String DECLARINGCLASS,
                              List<Stmt> insertLocationList,
                              HashMap <Local, String> SaveVarOfCurrentMethod,
                              HashMap <Local, String> SaveVarOfNewMethod,
                              List <Stmt> VisitedStmtForOperation,
                              HashMap<Stmt, Stmt> oldStmtsToNew,
                              HashMap<String, String> super2Class
    ){
        if (addStatement.getType().equals(MyConstants.FORSEARCH)){
            // TODO: 14/1/21 to be completed, to be included inner classes
            MyForSearchStmt myForSearchStmt = (MyForSearchStmt) addStatement;
            String varName = myForSearchStmt.getVarName();
            String varType = myForSearchStmt.getVarType();
            Local LeftVar;
            if (old_new_VarArgs.containsKey(varName)) {
                LeftVar = old_new_VarArgs.get(varName);
            } else {
                LeftVar = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, varType));
                old_new_VarArgs.put(varName, LeftVar);
            }
            Stack<Stmt> Visited_clone = (Stack<Stmt>) VisitedStmt.clone();
            AtomicBoolean flag = new AtomicBoolean(false);

            while(!Visited_clone.empty() && !flag.get()){
                Stmt tmpSt = Visited_clone.pop();
                if (tmpSt instanceof AssignStmt) {
                    AssignStmt tmpassignStmt = (AssignStmt)tmpSt;
                    if (tmpassignStmt.getLeftOp() instanceof Local){
                        if (tmpassignStmt.getRightOp() instanceof InstanceFieldRef){
                            InstanceFieldRef tmpField = (InstanceFieldRef)tmpassignStmt.getRightOp();
                            if (tmpField.getField().getType().toString().equals(varType)) {
                                flag.set(true);
                                SearchedStmt.put(myForSearchStmt, tmpSt);
                                // locating and searching
                                AssignStmt assignStmt = Jimple.v().newAssignStmt(LeftVar, tmpassignStmt.getRightOp());
                                insertLocationList.add(assignStmt);
                            }
                        } else if (tmpassignStmt.getRightOp() instanceof InvokeExpr) {
                            InvokeStmt tmpinvokeSt = (InvokeStmt) tmpassignStmt.getRightOp();
                            if (tmpinvokeSt.getInvokeExpr().getMethod().getReturnType().toString().equals(varType)) {
                                flag.set(true);
                                SearchedStmt.put(myForSearchStmt, tmpSt);
                                AssignStmt assignStmt = Jimple.v().newAssignStmt(LeftVar, tmpassignStmt.getLeftOp());
                                insertLocationList.add(assignStmt);
                            }
                        } else {
                            // System.out.println("Unresolved case!");
                        }
                    }
                }
            }
        }

        else if (addStatement.getType().equals(MyConstants.CUT_OPERATION) || addStatement.getType().equals(MyConstants.COPY_OPERATION)) {
            // TODO: 19/1/21 set a range for cut/copy
        }

        else if (addStatement.getType().equals(MyConstants.PASTE_OPERATION)) {
            TransferOldStmtsToNew(b, insertLocationList, SaveVarOfCurrentMethod, SaveVarOfNewMethod, VisitedStmtForOperation, oldStmtsToNew);
        }

        else if (addStatement.getType().equals(MyConstants.RETURNSTMT)) {
            MyReturnStmt myReturnStmt = (MyReturnStmt) addStatement;
            if (myReturnStmt.isIfRtnVoid()) {
                ReturnVoidStmt returnVoidStmt = new JReturnVoidStmt();
                insertLocationList.add(returnVoidStmt);
            } else {
                ReturnStmt returnStmt;
                if (myReturnStmt.getReturnVariableName().contains("$")){ // return $r1
                    Value returnValue = old_new_VarArgs.get(myReturnStmt.getReturnVariableName());
                    returnStmt = new JReturnStmt(returnValue);
                } else if (isNumeric(myReturnStmt.getReturnVariableName())){ // return 1 or 0
                    Value returnValue = IntConstant.v(Integer.parseInt(myReturnStmt.getReturnVariableName()));
                    returnStmt = new JReturnStmt(returnValue);
                } else {
                    returnStmt = new JReturnStmt(NullConstant.v());
                }

                insertLocationList.add(returnStmt);
            }
        }

        else if (addStatement.getType().equals(MyConstants.IDENTITYSTMT)) {
            MyIdentityStmt myIdentityStmt = (MyIdentityStmt) addStatement;
            String varType = myIdentityStmt.getVariableType();
            Type tmpType = getRightType(super2Class, varType);
            Local tmpRef = InstrumentUtil.generateNewLocal(b, tmpType);
            old_new_VarArgs.put(myIdentityStmt.getLeftVariableName(), tmpRef);

            IdentityRef identityRef = null;
            if (myIdentityStmt.getSign().equals("@this")) {
                identityRef = new ThisRef(RefType.v(replaceSuperClass(super2Class, varType)));
                SaveVarOfNewMethod.put(tmpRef, tmpRef.getType().toString());

            } else if (myIdentityStmt.getSign().equals("@parameter")) {
                identityRef = new ParameterRef(tmpType, myIdentityStmt.getParamaterOrder_n());
                SaveVarOfNewMethod.put(tmpRef, tmpRef.getType().toString());

            } else {
                // todo: soot has three cases: @this: , @parameter: , @caughtexception
            }
            IdentityStmt identityStmt = new JIdentityStmt(tmpRef, identityRef);
            insertLocationList.add(identityStmt);
        }

        else if (addStatement.getType().equals(MyConstants.SDK_VERSION_ASSIGNMENT)){
            Local tmpRef = InstrumentUtil.generateNewLocal(b, IntType.v());
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
            AssignStmt assignStmt = new JAssignStmt(
                    tmpRef, Jimple.v().newStaticFieldRef(Scene.v().
                    getField("<android.os.Build$VERSION: int SDK_INT>").makeRef()));
            insertLocationList.add(assignStmt);
        }

        else if (addStatement.getType().equals(MyConstants.LOGSTMT)){
            // <Log>("new stmt")
            String TAG = "<API_Migration>";
            MyLogStmt myLogStmt = (MyLogStmt) addStatement;
            SootMethod logsm = Scene.v().getMethod(
                    "<android.util.Log: int i(java.lang.String,java.lang.String)>");
            StaticInvokeExpr invokeExpr = Jimple.v().newStaticInvokeExpr(logsm.makeRef(),
                    StringConstant.v(TAG),
                    StringConstant.v(myLogStmt.getLogInfo()));
            InvokeStmt invokeStmt = new JInvokeStmt(invokeExpr);
            insertLocationList.add(invokeStmt);
        }

        else if (addStatement.getType().equals(MyConstants.NEW_OBJECT_ASSIGNMENT)){
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            Local tmpRef = InstrumentUtil.generateNewLocal(b,
                    getRightType(super2Class, myAssignStmt.getFieldType()));
            AssignStmt assignStmt = new JAssignStmt(
                    tmpRef, Jimple.v().newNewExpr(RefType.v(replaceSuperClass(super2Class, myAssignStmt.getFieldType()))));
            insertLocationList.add(assignStmt);
            old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
        }

        else if (addStatement.getType().equals(MyConstants.VARIABLE_CAST_ASSIGNMENT)) {
            assert addStatement instanceof MyAssignStmt;
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            Local tmpRef;
            if (old_new_VarArgs.containsKey(myAssignStmt.getLeftVariableName())){
                tmpRef = old_new_VarArgs.get(myAssignStmt.getLeftVariableName());
            } else {
                tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, myAssignStmt.getVarcastType()));
                old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
            }
            Local tmpRef_source = old_new_VarArgs.get(myAssignStmt.getVarcastSourceName());
            CastExpr cast = Jimple.v().newCastExpr(tmpRef_source, getRightType(super2Class, myAssignStmt.getVarcastType()));
            AssignStmt assignStmt = new JAssignStmt(tmpRef, cast);
            insertLocationList.add(assignStmt);
        }

        else if (addStatement.getType().equals(MyConstants.INSTANCE_FIELD_ASSIGNMENT)){
            // (2) $r4 = $r3.<android.su...mpat: android.sup...onCompat$b a>;
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            Local tmpRef;
            if (old_new_VarArgs.containsKey(myAssignStmt.getLeftVariableName())){
                tmpRef = old_new_VarArgs.get(myAssignStmt.getLeftVariableName());
            } else {
                tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, myAssignStmt.getFieldType()));
                old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
            }
            assert old_new_VarArgs.containsKey(myAssignStmt.getAttributeBaseName());
            Local baseRef = old_new_VarArgs.get(myAssignStmt.getAttributeBaseName());
            String feildStr = replaceSuperClass(super2Class, myAssignStmt.getValueStr());
            String customFeild = myAssignStmt.getFieldCustomName();
            if (old_new_Fields.containsKey(customFeild)){
                feildStr = feildStr.replace(customFeild, old_new_Fields.get(customFeild));
            }
            AssignStmt assignStmt = new JAssignStmt(
                    tmpRef, new JInstanceFieldRef(baseRef, Scene.v().getField(feildStr).makeRef()));
            insertLocationList.add(assignStmt);
        }

        else if (addStatement.getType().equals(MyConstants.LEFT_INSTANCE_FIELD_ASSIGNMENT)){
            // $r3.<android.su...mpat: android.sup...onCompat$b a> = $r4;  or
            // $r3.<android.su...mpat: android.sup...onCompat$b a> = 0;
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;

            assert old_new_VarArgs.containsKey(myAssignStmt.getAttributeBaseName());
            Local baseRef = old_new_VarArgs.get(myAssignStmt.getAttributeBaseName());
            String feildStr = replaceSuperClass(super2Class, myAssignStmt.getValueStr());
            String customFeild = myAssignStmt.getFieldCustomName();
            if (old_new_Fields.containsKey(customFeild)){
                feildStr = feildStr.replace(customFeild, old_new_Fields.get(customFeild));
            }

            AssignStmt assignStmt;
            if (myAssignStmt.getRightVariableName().contains("$")) {
                Local tmpRef;
                if (old_new_VarArgs.containsKey(myAssignStmt.getRightVariableName())){
                    tmpRef = old_new_VarArgs.get(myAssignStmt.getRightVariableName());
                } else {
                    tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, myAssignStmt.getFieldType()));
                    old_new_VarArgs.put(myAssignStmt.getRightVariableName(), tmpRef);
                }
                assignStmt = new JAssignStmt(
                        new JInstanceFieldRef(baseRef, Scene.v().getField(feildStr).makeRef()), tmpRef);
            } else {
                String typeOfRightVar = myAssignStmt.getFieldType();
                Value tmpValue;
                switch (typeOfRightVar) {
                    case "int":
                    case "boolean":
                        tmpValue = IntConstant.v(Integer.parseInt(typeOfRightVar));
                        break;
                    case "long":
                        tmpValue = LongConstant.v(Long.parseLong(typeOfRightVar));
                        break;
                    case "float":
                        tmpValue = FloatConstant.v(Float.parseFloat(typeOfRightVar));
                        break;
                    case "double":
                        tmpValue = DoubleConstant.v(Double.parseDouble(typeOfRightVar));
                        break;
                    case "java.lang.String":
                        tmpValue = StringConstant.v(typeOfRightVar.replaceAll("[\"']+", ""));
                        break;
                    case "null":
                        tmpValue = NullConstant.v();
                        break;
                    default:
                        return;
                }
                assignStmt = new JAssignStmt(
                        new JInstanceFieldRef(baseRef, Scene.v().getField(feildStr).makeRef()), tmpValue);
            }

            insertLocationList.add(assignStmt);
        }

        else if (addStatement.getType().equals(MyConstants.LengthExpr_ASSIGNMENT)) {
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            Local tmpRef, tmpRight = null;
            if (old_new_VarArgs.containsKey(myAssignStmt.getLeftVariableName())){
                tmpRef = old_new_VarArgs.get(myAssignStmt.getLeftVariableName());
            } else {
                tmpRef = InstrumentUtil.generateNewLocal(b, IntType.v());
                old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
            }

            if (old_new_VarArgs.containsKey(myAssignStmt.getRightVariableName())){
                tmpRight = old_new_VarArgs.get(myAssignStmt.getRightVariableName());
            } else {
                System.out.println("[-] JLengthExpr Error");
            }
            AssignStmt assignStmt = new JAssignStmt(tmpRef, Jimple.v().newLengthExpr(tmpRight));
            insertLocationList.add(assignStmt);
        }

        else if (addStatement.getType().equals(MyConstants.Compute_ASSIGNMENT)) {
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            Local tmpRef;
            Value tmpRight1 = null, tmpRight2 = null;
            if (old_new_VarArgs.containsKey(myAssignStmt.getLeftVariableName())){
                tmpRef = old_new_VarArgs.get(myAssignStmt.getLeftVariableName());
            } else {
                tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, dataObject.getVirables().
                        get(myAssignStmt.getLeftVariableName())));
                old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
            }

            if (myAssignStmt.getRightVariableName().contains("$")){
                if (old_new_VarArgs.containsKey(myAssignStmt.getRightVariableName())){
                    tmpRight1 = old_new_VarArgs.get(myAssignStmt.getRightVariableName());
                } else {
                    System.out.println("[-] Compute Error 1");
                }
            } else {
                tmpRight1 = IntConstant.v(Integer.parseInt(myAssignStmt.getRightVariableName()));
            }

            if (myAssignStmt.getRightVariableName2().contains("$")) {
                if (old_new_VarArgs.containsKey(myAssignStmt.getRightVariableName2())){
                    tmpRight2 = old_new_VarArgs.get(myAssignStmt.getRightVariableName2());
                } else {
                    System.out.println("[-] Compute Error 2");
                }
            } else {
                tmpRight2 = IntConstant.v(Integer.parseInt(myAssignStmt.getRightVariableName2()));
            }


            AssignStmt assignStmt;
            switch (myAssignStmt.getSymbol().trim()){
                case "-":
                    assignStmt = new JAssignStmt(tmpRef, new JSubExpr(tmpRight1, tmpRight2));
                    break;
                case "*":
                    assignStmt = new JAssignStmt(tmpRef, new JMulExpr(tmpRight1, tmpRight2));
                    break;
                case "/":
                    assignStmt = new JAssignStmt(tmpRef, new JDivExpr(tmpRight1, tmpRight2));
                    break;
                default:
                    assignStmt = new JAssignStmt(tmpRef, new JAddExpr(tmpRight1, tmpRight2));
                    break;
            }
            insertLocationList.add(assignStmt);

        }

        else if (addStatement.getType().equals(MyConstants.Constant_ASSIGNMENT)) {
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            Local tmpRef;
            if (old_new_VarArgs.containsKey(myAssignStmt.getLeftVariableName())){
                tmpRef = old_new_VarArgs.get(myAssignStmt.getLeftVariableName());
            } else {
                tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, dataObject.getVirables().
                        get(myAssignStmt.getLeftVariableName())));
                old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
            }
            // todo: only IntConstant now
            AssignStmt assignStmt = new JAssignStmt(tmpRef, IntConstant.v(Integer.parseInt(myAssignStmt.getRightVariableName())));
            insertLocationList.add(assignStmt);

        }

        else if (addStatement.getType().equals(MyConstants.ArrayRef_ASSIGNMENT)) {
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            Local tmpRef, tmpRight = null, Innediate = null;
            if (old_new_VarArgs.containsKey(myAssignStmt.getLeftVariableName())){
                tmpRef = old_new_VarArgs.get(myAssignStmt.getLeftVariableName());
            } else {
                tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, dataObject.getVirables().
                        get(myAssignStmt.getLeftVariableName())));
                old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
            }

            if (old_new_VarArgs.containsKey(myAssignStmt.getRightVariableName())){
                tmpRight = old_new_VarArgs.get(myAssignStmt.getRightVariableName());
            } else {
                System.out.println("[-] JArrayRef Error 1");
            }

            if (old_new_VarArgs.containsKey(myAssignStmt.getInnediateBox())){
                Innediate = old_new_VarArgs.get(myAssignStmt.getInnediateBox());
            } else {
                System.out.println("[-] JArrayRef Error 2");
            }

            AssignStmt assignStmt = new JAssignStmt(tmpRef, Jimple.v().newArrayRef(tmpRight, Innediate));
            insertLocationList.add(assignStmt);
        }

        else if (addStatement.getType().equals(MyConstants.STATIC_FIELD_ASSIGNMENT)){
            MyAssignStmt myAssignStmt = (MyAssignStmt) addStatement;
            Local tmpRef;
            if (old_new_VarArgs.containsKey(myAssignStmt.getLeftVariableName())){
                tmpRef = old_new_VarArgs.get(myAssignStmt.getLeftVariableName());
            } else {
                tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, myAssignStmt.getFieldType()));
                old_new_VarArgs.put(myAssignStmt.getLeftVariableName(), tmpRef);
            }

            String feildStr = replaceSuperClass(super2Class, myAssignStmt.getValueStr());
            String customFeild = myAssignStmt.getFieldCustomName();
            if (old_new_Fields.containsKey(customFeild)){
                feildStr = feildStr.replace(customFeild, old_new_Fields.get(customFeild));
            }
            AssignStmt assignStmt = new JAssignStmt(
                    tmpRef, Jimple.v().newStaticFieldRef(Scene.v().getField(feildStr).makeRef()));
            insertLocationList.add(assignStmt);
        }

        else if (addStatement.getType().equals(MyConstants.LABELSTMT)){
            assert addStatement instanceof MyLabelStmt;
            insertLocationList.add(nopMap.get(addStatement));
        }

        else if (addStatement.getType().equals(MyConstants.IFSTMT)){
            assert addStatement instanceof MyIfStmt;
            MyIfStmt myIfStmt = (MyIfStmt) addStatement;
            assert old_new_VarArgs.containsKey(myIfStmt.getOp1Var());
            Local tmpRef = old_new_VarArgs.get(myIfStmt.getOp1Var());
            // TODO: 12/1/21  other cases, only int, null, Local now
            Value Op2Var = null;
            if (myIfStmt.getOp2Var().equals("null")) {
                Op2Var = NullConstant.v();
            } else if (isNumeric(myIfStmt.getOp2Var())){
                int migration = Integer.parseInt(myIfStmt.getOp2Var());
                Op2Var = IntConstant.v(migration);
            } else if (myIfStmt.getOp2Var().contains("$")){
                // variable
                Op2Var = old_new_VarArgs.get(myIfStmt.getOp2Var().trim());
            }
            ConditionExpr condition = null;
            switch (myIfStmt.getSymbol()) {
                case "<":
                    condition = new JLtExpr(tmpRef, Op2Var);
                    break;
                case ">":
                    condition = new JGtExpr(tmpRef, Op2Var);
                    break;
                case "<=":
                    condition = new JLeExpr(tmpRef, Op2Var);
                    break;
                case ">=":
                    condition = new JGeExpr(tmpRef, Op2Var);
                    break;
                case "==":
                    condition = new JEqExpr(tmpRef, Op2Var);
                    break;
                case "!=":
                    condition = new JNeExpr(tmpRef, Op2Var);
                    break;
            }
            MyLabelStmt myLabelStmt = dataObject.getLabelStmts().get(myIfStmt.getTarget());
            IfStmt ifStmt = new JIfStmt(condition, nopMap.get(myLabelStmt));
            insertLocationList.add(ifStmt);
        }

        else if (addStatement.getType().equals(MyConstants.GOTOSTMT)){
            assert addStatement instanceof MyGotoStmt;
            MyGotoStmt myGotoStmt = (MyGotoStmt) addStatement;
            assert dataObject.getLabelStmts().containsKey(myGotoStmt.getTarget());

            GotoStmt newgotoStmt = new JGotoStmt(nopMap.get(
                    dataObject.getLabelStmts().get(myGotoStmt.getTarget())));
            insertLocationList.add(newgotoStmt);
        }

        else if (addStatement.getType().contains(MyConstants.INVOKE_Str)){
            assert addStatement instanceof MyInvokeStmt;
            MyInvokeStmt newInvokeStmt = (MyInvokeStmt)addStatement;
            List<Type> parameterTypes = new ArrayList<>();
            List<Value> parameterValues = new ArrayList<>();

            List<String> argTypes = newInvokeStmt.getArgTypes();
            for (int i = 0; i < newInvokeStmt.getArgLen(); i++) {
                String tmpargname = newInvokeStmt.getArgNames().get(i);
                String singleTypeName = argTypes.get(i).replaceAll("[\"']+", "");
                Type argNewType = getRightType(super2Class, singleTypeName);
                // TODO: 8/9/20 restrict the "$" to distinguish var and new constant
                if (!tmpargname.startsWith("$")) {
                    String tmptype = argTypes.get(i);
                    if (tmptype.equals("int")) {
                        parameterTypes.add(IntType.v());
                        parameterValues.add(IntConstant.v(Integer.parseInt(tmpargname)));
                    } else if (tmptype.equals("boolean")) {
                        parameterTypes.add(BooleanType.v());
                        parameterValues.add(IntConstant.v(Integer.parseInt(tmpargname)));
                    } else if (tmptype.equals("long")) {
                        parameterTypes.add(LongType.v());
                        parameterValues.add(LongConstant.v(Long.parseLong(tmpargname)));
                    } else if (tmptype.equals("double")) {
                        parameterTypes.add(DoubleType.v());
                        parameterValues.add(DoubleConstant.v(Double.parseDouble(tmpargname)));
                    } else if (tmptype.equals("float")) {
                        parameterTypes.add(FloatType.v());
                        parameterValues.add(FloatConstant.v(Float.parseFloat(tmpargname)));
                    } else if (tmptype.equals("java.lang.String")) {
                        parameterTypes.add(argNewType);
                        parameterValues.add(StringConstant.v(tmpargname.replaceAll("[\"']+", "")));
                    } else if (tmpargname.equals("null")) {
                        parameterTypes.add(argNewType);
                        parameterValues.add(NullConstant.v());
                    } else if (tmptype.equals("java.lang.Object")) {
                        parameterTypes.add(argNewType);
                        parameterValues.add(StringConstant.v(tmpargname.replaceAll("[\"']+", "")));
                    } else {
                        // TODO: 2/11/20 other constants
                        System.out.println(tmptype + " " + tmpargname + ": arg type and constant error");
                        return;
                    }
                }
                else { // variables start with $
                    if (old_new_VarArgs.containsKey(tmpargname)) {

                        parameterTypes.add(argNewType);

                        parameterValues.add(old_new_VarArgs.get(tmpargname));

                    } else if (old_new_Constants.containsKey(tmpargname)) {

                        parameterTypes.add(argNewType);

                        parameterValues.add(old_new_Constants.get(tmpargname));

                    } else {
                        // TODO: 21/12/20
                        System.out.println("Parameter error.");
                    }
                }
            }

            SootClass classtmp = Scene.v().getSootClass(replaceSuperClass(super2Class, newInvokeStmt.getMethodBaseClassType()));
            String mtdname = replaceSuperClass(super2Class, newInvokeStmt.getMethodName());
            Type returnType = getRightType(super2Class, newInvokeStmt.getReturnClass());

            Stmt newinvokeStmt;
            boolean ifhasRtn = true;

            if (((MyInvokeStmt)addStatement).getReturnName().equals("")){
                ifhasRtn = false;
            }

            switch (newInvokeStmt.getType()) {
                case MyConstants.VIRTUAL_INVOKE: {
                    SootMethodRefImpl newmethodref = new SootMethodRefImpl(classtmp, mtdname,
                            parameterTypes, returnType, false);

                    Local BaseVar;
                    if (old_new_VarArgs.containsKey(newInvokeStmt.getBaseName())) {
                        BaseVar = old_new_VarArgs.get(newInvokeStmt.getBaseName());
                    } else {
                        BaseVar = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, newInvokeStmt.
                                getMethodBaseClassType()));
                        old_new_VarArgs.put(newInvokeStmt.getBaseName(), BaseVar);
                    }

                    InvokeExpr virtualInvokeExpr = new JVirtualInvokeExpr(BaseVar, newmethodref, parameterValues);

                    if (!ifhasRtn) {
                        newinvokeStmt = new JInvokeStmt(virtualInvokeExpr);
                    } else if (old_new_VarArgs.containsKey(newInvokeStmt.getReturnName())) {
                        Value tmpRef = old_new_VarArgs.get(newInvokeStmt.getReturnName());
                        newinvokeStmt = Jimple.v().newAssignStmt(tmpRef, virtualInvokeExpr);
                    } else {
                        MyInvokeStmt myInvokeStmt = (MyInvokeStmt) addStatement;
                        Local tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, myInvokeStmt.getReturnClass()));
                        newinvokeStmt = Jimple.v().newAssignStmt(tmpRef, virtualInvokeExpr);
                        old_new_VarArgs.put(myInvokeStmt.getReturnName(), tmpRef);
                    }

                    break;
                }
                case MyConstants.STATIC_INVOKE: {
                    SootMethodRefImpl newmethodref = new SootMethodRefImpl(classtmp, mtdname,
                            parameterTypes, returnType, true);
                    InvokeExpr staticInvokeExpr = new JStaticInvokeExpr(newmethodref, parameterValues);

                    if (!ifhasRtn) {
                        newinvokeStmt = new JInvokeStmt(staticInvokeExpr);
                    } else if (old_new_VarArgs.containsKey(newInvokeStmt.getReturnName())) {
                        Value tmpRef = old_new_VarArgs.get(newInvokeStmt.getReturnName());
                        newinvokeStmt = Jimple.v().newAssignStmt(tmpRef, staticInvokeExpr);
                    } else {
                        MyInvokeStmt myInvokeStmt = (MyInvokeStmt) addStatement;
                        Local tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, myInvokeStmt.getReturnClass()));
                        newinvokeStmt = Jimple.v().newAssignStmt(tmpRef, staticInvokeExpr);
                        old_new_VarArgs.put(myInvokeStmt.getReturnName(), tmpRef);
                    }

                    break;
                }
                case MyConstants.SPECIAL_INVOKE: {
                    SootMethodRefImpl newmethodref = new SootMethodRefImpl(classtmp, mtdname,
                            parameterTypes, returnType, false);
                    Local BaseVar;
                    if (old_new_VarArgs.containsKey(newInvokeStmt.getBaseName())) {
                        BaseVar = old_new_VarArgs.get(newInvokeStmt.getBaseName());
                    } else {
                        BaseVar = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, newInvokeStmt.getMethodBaseClassType()));
                        old_new_VarArgs.put(newInvokeStmt.getBaseName(), BaseVar);
                    }

                    InvokeExpr specialInvokeExpr = new JSpecialInvokeExpr(BaseVar, newmethodref, parameterValues);

                    if (!ifhasRtn) {
                        newinvokeStmt = new JInvokeStmt(specialInvokeExpr);
                    } else if (old_new_VarArgs.containsKey(newInvokeStmt.getReturnName())) {
                        Value tmpRef = old_new_VarArgs.get(newInvokeStmt.getReturnName());
                        newinvokeStmt = Jimple.v().newAssignStmt(tmpRef, specialInvokeExpr);
                    } else {
                        MyInvokeStmt myInvokeStmt = (MyInvokeStmt) addStatement;
                        Local tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, myInvokeStmt.getReturnClass()));
                        newinvokeStmt = Jimple.v().newAssignStmt(tmpRef, specialInvokeExpr);
                        old_new_VarArgs.put(myInvokeStmt.getReturnName(), tmpRef);
                    }

                    break;
                }
                case MyConstants.INTERFACE_INVOKE: {
                    SootMethodRefImpl newmethodref = new SootMethodRefImpl(classtmp, mtdname,
                            parameterTypes, returnType, false);
                    Local BaseVar;
                    if (old_new_VarArgs.containsKey(newInvokeStmt.getBaseName())) {
                        BaseVar = old_new_VarArgs.get(newInvokeStmt.getBaseName());
                    } else {
                        BaseVar = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, newInvokeStmt.getMethodBaseClassType()));
                        old_new_VarArgs.put(newInvokeStmt.getBaseName(), BaseVar);
                    }

                    InvokeExpr interfaceInvokeExpr = new JInterfaceInvokeExpr(BaseVar, newmethodref, parameterValues);
                    if (!ifhasRtn) {
                        newinvokeStmt = new JInvokeStmt(interfaceInvokeExpr);
                    } else if (old_new_VarArgs.containsKey(newInvokeStmt.getReturnName())) {
                        Value tmpRef = old_new_VarArgs.get(newInvokeStmt.getReturnName());
                        newinvokeStmt = Jimple.v().newAssignStmt(tmpRef, interfaceInvokeExpr);
                    } else {
                        MyInvokeStmt myInvokeStmt = (MyInvokeStmt) addStatement;
                        Local tmpRef = InstrumentUtil.generateNewLocal(b, getRightType(super2Class, myInvokeStmt.getReturnClass()));
                        newinvokeStmt = Jimple.v().newAssignStmt(tmpRef, interfaceInvokeExpr);
                        old_new_VarArgs.put(myInvokeStmt.getReturnName(), tmpRef);
                    }
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected value: " + newInvokeStmt.getType());
            }
            insertLocationList.add(newinvokeStmt);
        }

    }

    public static boolean isNumeric(String str){
        for (int i = 0; i < str.length(); i++){
            if (!Character.isDigit(str.charAt(i)) && str.charAt(i) != '-'){
                return false;
            }
        }
        return true;
    }
}
