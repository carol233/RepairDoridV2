import Model.MyMethodObject;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

class Helper {
    static boolean checkIfHaveFixed(Body body, String targetAPISig) {

        AtomicBoolean ifFoundSDKCheck = new AtomicBoolean(false);
        AtomicBoolean ifCurrentMethod = new AtomicBoolean(false);
        AtomicBoolean ifCaller = new AtomicBoolean(true);
        PatchingChain<Unit> units = body.getUnits();
        SootMethod sm = body.getMethod();
        String thisMethodSig = sm.getSignature();
        for (Iterator<Unit> unitIter = units.snapshotIterator(); unitIter.hasNext(); ) {
            Stmt stmt = (Stmt) unitIter.next();

            if(stmt.toString().contains("android.os.Build$VERSION: int SDK_INT")){
                ifFoundSDKCheck.set(true);
                continue;
            }

            if (stmt.containsInvokeExpr()) {
                SootMethod callee = stmt.getInvokeExpr().getMethod();
                String calleeSig = callee.getSignature();
                if (!ifFoundSDKCheck.get() && targetAPISig.equals(calleeSig)) { // unprotected
                    ifCurrentMethod.set(true);
                    break;
                }
            }
        }

        // 3. check if has been protected in the outer caller method
        CallGraph cg = Scene.v().getCallGraph();
        Iterator<Edge> ite = cg.iterator();
        while (ite.hasNext()) {
            Edge edge = ite.next();
            SootMethod src = edge.src();
            SootMethod tgt = edge.tgt();
            if (tgt.getSignature().equals(thisMethodSig)){
                Body srcBody = src.getActiveBody();
                PatchingChain<Unit> srcUnits = srcBody.getUnits();
                AtomicBoolean ifFoundSDKCheck1 = new AtomicBoolean(false);
                for (Iterator<Unit> unitIter1 = srcUnits.snapshotIterator(); unitIter1.hasNext(); ) {
                    try {
                        Stmt stmt1 = (Stmt) unitIter1.next();

                        if(stmt1.toString().contains("android.os.Build$VERSION: int SDK_INT")){
                            ifFoundSDKCheck1.set(true);
                            continue;
                        }

                        if (stmt1.containsInvokeExpr()) {
                            String calleeSig = stmt1.getInvokeExpr().getMethod().getSignature();
                            if (ifFoundSDKCheck1.get() && thisMethodSig.equals(calleeSig)) { // unprotected
                                ifCaller.set(false);
                                break;
                            }
                        }

                    } catch (ClassCastException e) {
                        // System.out.println(e.getMessage());
                    }
                }
                break; // only check one src until now
            }
        }

        return ifCurrentMethod.get() && ifCaller.get();
    }


    public static long getFileSize(String filename) {
        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            System.out.println("file not exists");
            return -1;
        }
        return file.length();
    }

    static boolean checkIfPotentialCallBack(Body body, MyMethodObject targetAPISig){
        SootMethod sm = body.getMethod();
        if (sm.getName().equals(targetAPISig.getMethodName())){
            List<String> s1 = targetAPISig.getParameterTypeList();
            List<Type> s2 = sm.getParameterTypes();
            for (int i = 0; i < s1.size(); i++){
                if (!s2.get(i).toString().equals(s1.get(i))){
                    return false;
                }
            }
            // todo: compare
            if (sm.getReturnType().toString().equals(targetAPISig.getReturnType())){
                // todo: check if exits methods
                return true;
            }
        }
        return false;
    }


    static boolean checkIfTrueCallBack(Body body, MyMethodObject targetAPISig){
        SootMethod sm = body.getMethod();
        SootClass sc = sm.getDeclaringClass();
        List<SootMethod> methods = sc.getMethods();
        for (SootMethod sootMethod: methods) {
            if (sootMethod.getName().equals(targetAPISig.getMethodName()) &&
                    sootMethod.getReturnType().toString().equals(targetAPISig.getReturnType())) {
                List<String> s1 = targetAPISig.getParameterTypeList();
                List<Type> s2 = sootMethod.getParameterTypes();
                if (s1.size() == s2.size()) {
                    int all = 0;
                    for (int i = 0; i < s1.size(); i++){
                        if (s2.get(i).toString().equals(s1.get(i))){
                            all += 1;
                        }
                    }
                    if (all == s1.size()){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static boolean checkIfIssueForDevice(Body body, String targetAPISig) {
        AtomicBoolean ifModelCheck = new AtomicBoolean(false);
        AtomicBoolean ifCurrentMethod = new AtomicBoolean(false);
        PatchingChain<Unit> units = body.getUnits();
        SootMethod sm = body.getMethod();
        for (Iterator<Unit> unitIter = units.snapshotIterator(); unitIter.hasNext(); ) {
            Stmt stmt = (Stmt) unitIter.next();

            if (stmt.toString().contains("android.os.Build: java.lang.String MANUFACTURER") ||
                    stmt.toString().contains("android.os.Build: java.lang.String MODEL")) {
                ifModelCheck.set(true);
                continue;
            }

            if (stmt.containsInvokeExpr()) {
                SootMethod callee = stmt.getInvokeExpr().getMethod();
                String calleeSig = callee.getSignature();
                if (!ifModelCheck.get() && targetAPISig.equals(calleeSig)) { // unprotected
                    ifCurrentMethod.set(true);
                    break;
                }
            }
        }
        return ifCurrentMethod.get();
    }

    public static boolean ifHasSuperclass(SootClass sc) {
        return sc.hasSuperclass()
                && !sc.getSuperclassUnsafe().getName().equals("java.lang.Object");
    }

    public static ArrayList<String> getFiles(String path, String endStr) throws IOException {
        ArrayList<String> files = new ArrayList<>();
        try (Stream<Path> filePathStream=Files.walk(Paths.get(path))) {
            filePathStream.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (filePath.toString().endsWith(endStr))
                        files.add(filePath.toString());
                }
            });
        }
        return files;
    }

}
