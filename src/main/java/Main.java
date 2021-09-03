import soot.*;
import soot.options.Options;
import java.util.Collections;

public class Main {


    public static void main(String[] args) throws Exception {

//        String SemanticPatchPath_os = args[0];
//        String SemanticPatchPath_device = args[1];
//        String SemanticPatchPath_callback = args[2];
//        String androidJar = args[3];
//        String APK = args[4];
//        String logPath = args[5];

        String SemanticPatchPath_os = "OSPatches";
        String SemanticPatchPath_device = "DevicePatches";
        String SemanticPatchPath_callback = "CallBackPatches";
        String androidJar = "/Users/yzha0544/Library/Android/sdk/platforms/";
        String APK = "app_old/053E33F48962A81653C26E022AA2F3F40B791C417C740EB51308B624CA3813CE.apk";
        String logPath = "ManualTestCSV/053E33F48962A81653C26E022AA2F3F40B791C417C740EB51308B624CA3813CE.apk.csv";

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
        Options.v().set_output_format(Options.output_format_dex);
        Options.v().set_output_dir("ManualTest");
        //-force-overwrite
        Options.v().set_force_overwrite(true);
        Options.v().set_validate(true); // Validate Jimple bodies in each transformation pack
        // Resolve required classes
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.net.NetworkCapabilities", SootClass.HIERARCHY);
        Scene.v().addBasicClass("android.webkit.WebResourceRequest", SootClass.HIERARCHY);
        Scene.v().addBasicClass("android.webkit.WebResourceRequest$Builder", SootClass.HIERARCHY);
        Scene.v().addBasicClass("android.media.AudioAttributes", SootClass.HIERARCHY);
        Scene.v().addBasicClass("android.media.AudioAttributes$Builder", SootClass.HIERARCHY);
        Scene.v().addBasicClass("android.media.AudioFocusRequest$Builder", SootClass.HIERARCHY);
        Scene.v().addBasicClass("android.app.Webview", SootClass.HIERARCHY);
        Scene.v().addBasicClass("java.String.string", SootClass.HIERARCHY);


        Scene.v().loadNecessaryClasses();

        long startTime = System.currentTimeMillis();

        RepairDroidInstrumenter transformer = new RepairDroidInstrumenter(APK, SemanticPatchPath_os,
                SemanticPatchPath_device, SemanticPatchPath_callback, logPath);
        PackManager.v().getPack("jtp").add(new Transform("jtp.RepairDroidInstrumenter", transformer));

        PackManager.v().runPacks();
        PackManager.v().writeOutput();
        long endTime = System.currentTimeMillis();

        System.out.println("[+] Running time: " + (endTime - startTime) + "ms");
        String timeToString = Long.toString(endTime - startTime);
        long size = Helper.getFileSize(APK);
        InstrumentUtil.Log2File("TimeRecord", logPath, "null", "null", "null",
                timeToString, Long.toString(size), "null");

    }
}
