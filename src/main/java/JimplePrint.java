import soot.*;
import soot.jimple.Stmt;
import soot.options.Options;
import soot.util.Chain;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

class JimplePrint {
    private final static String androidJar = "/Users/yzha0544/Library/Android/sdk/platforms/";
    //要处理的apk文件的路径
//    private final static String APK = "apps/0869A1BB1DAC1EA7AF3D3734021861588444C56066EDA2138E03195646D9A611.apk";
//    private final static String searchAPI = "<android.speech.tts.TextToSpeech: int setOnUtteranceCompletedListener(android.speech.tts.TextToSpeech$OnUtteranceCompletedListener)>";
//    private final static String searchMethod = "<android.support.v4.speech.tts.TextToSpeechICSMR1: void setUtteranceProgressListener(android.speech.tts.TextToSpeech,android.support.v4.speech.tts.TextToSpeechICSMR1$UtteranceProgressListenerICSMR1)>";

    //private final static String APK = "apps/wpandroid-8.9.apk";


    private final static String APK = "/Users/yzha0544/AndroidStudioProjects/ForJimple/app/build/outputs/apk/debug/app-debug.apk";
    private final static String searchMethod = "";

//    private final static String APK = "CallbackTestCases/login-sample-debug.apk";
//    private final static String searchMethod = "com.uber.sdk.android.core.auth.LoginActivity";

    //"/Users/yzha0544/AndroidStudioProjects/ForJimple/app/build/outputs/apk/debug/app-debug.apk";
//            "apps/wpandroid-8.9.apk";
//    private final static String searchAPI = "<android.accessibilityservice.AccessibilityServiceInfo: java.lang.String getDescription()>";
//    private final static String searchMethod = "<android.support.v4.accessibilityservice.AccessibilityServiceInfoCompat: java.lang.String loadDescription(android.accessibilityservice.AccessibilityServiceInfo,android.content.pm.PackageManager)>";
    private final static String searchAPI = "<android.media.MediaPlayer: void setAudioStreamType(int)>";

    //private final static String APK = "apps/wpandroid-16.3-universal.apk";
    //private final static String searchMethod = "org.wordpress.android.ui.themes.ThemeBrowserActivity";
    //private final static String searchMethod = "org.wordpress.android.ui.themes.ThemeBrowserFragment";

    public static void main(String[] args) throws IOException {
        G.reset();
        //set options
        //设置允许伪类（Phantom class），指的是soot为那些在其classpath找不到的类建立的模型
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true); //prepend the VM's classpath to Soot's own classpath
        Options.v().set_android_jars(androidJar);
        //input options
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(APK));
        Options.v().set_process_multiple_dex(true);  // Inform Dexpler that the APK may have more than one .dex files
        Options.v().set_include_all(true);
        //output options
        Options.v().set_output_format(Options.output_format_jimple);
        // Options.v().set_output_format(Options.output_format_dex);
        //-force-overwrite
        Options.v().set_force_overwrite(true);
        Options.v().set_validate(true); // Validate Jimple bodies in each transofrmation pack
        // Resolve required classes
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
        Scene.v().loadNecessaryClasses();

        String logPath = "tmp_output.csv";
        String SemanticPatchPath = "tmp.txt";

        PrintTransformer transformer = new PrintTransformer(APK, SemanticPatchPath, searchAPI, searchMethod, logPath);
        PackManager.v().getPack("jtp").add(new Transform("jtp.instrumenter", transformer));

        PackManager.v().runPacks();
        PackManager.v().writeOutput();
    }
}


class PrintTransformer extends BodyTransformer {

    private String logPath;
    private String pkg;
    private String SemanticPatchPath;
    private String searchAPI;
    private String searchMethod;

    PrintTransformer(String apk, String SemanticPatchPath, String searchAPI, String searchMethod, String logFilePath) throws IOException {
        this.logPath = logFilePath;
        this.pkg = InstrumentUtil.getPackageName(apk);
        this.SemanticPatchPath = SemanticPatchPath;
        this.searchMethod = searchMethod;
        this.searchAPI = searchAPI;
    }

    @Override
    protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
        // TODO Auto-generated method stub

        String methodSig = b.getMethod().getSignature();

        // First we filter out Android framework methods
        if (!methodSig.contains(this.pkg)){
            return;
        }

//        if (!methodSig.contains("shouldOverride")) {
//            return;
//        }

        PatchingChain<Unit> units = b.getUnits();

        Stack<Stmt> AssignStmtTmpList = new Stack<>();
        Stack<Stmt> IdentityStmtTmpList = new Stack<>();

        //获取Body里所有的units, 一个Body对应Java里一个方法的方法体，Unit代表里面的语句
        Iterator<Unit> unitsIterator = units.snapshotIterator();
        while (unitsIterator.hasNext()) {
            Unit unit = unitsIterator.next();
            //将Unit强制转换为Stmt,Stmt为Jimple里每条语句的表示
            Stmt stmt = (Stmt) unit;

            SootClass currentClass = b.getMethod().getDeclaringClass();
            List<SootMethod> methods = currentClass.getMethods();
            for (SootMethod method: methods) {
                String ss = method.getName();
                Body body = method.retrieveActiveBody();
                Chain units2 = body.getUnits();
            }

            if (stmt.containsInvokeExpr()) {
                SootMethod InvokedMethod = stmt.getInvokeExpr().getMethod();
                String invokedMethodSignature = InvokedMethod.getSignature();
                SootClass InvokedMethodClass = InvokedMethod.getDeclaringClass();

                if (invokedMethodSignature.contains(this.searchAPI)){

                    SootClass currentClass1 = b.getMethod().getDeclaringClass();
                    List<SootMethod> methods2 = currentClass1.getMethods();
                    for (SootMethod method1: methods2) {
                        String ss = method1.getName();
                        Body body = method1.retrieveActiveBody();
                        Chain units2 = body.getUnits();
                    }
                    System.out.println(111);
                    File f = new File(this.SemanticPatchPath);
                    try (PrintWriter writer = new PrintWriter(new FileWriter(f, true))) {
                        writer.print(stmt.toString());
                    } catch (IOException e) {
                        // ... handle IO exception
                    }
                }
            }
        }
    }
}

