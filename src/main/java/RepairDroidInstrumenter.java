import soot.Body;
import soot.BodyTransformer;
import soot.SootMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class RepairDroidInstrumenter extends BodyTransformer {

    private String logPath;
    private String pkg;
    private String SemanticPatchPath_os;
    private String SemanticPatchPath_device;
    private String SemanticPatchPath_callback;
    private HashMap <String, PatchParser> dataObjects = new HashMap<>();
    private ArrayList<String> OS_Patches;
    private ArrayList<String> DEVICE_Patches;
    private ArrayList<String> CALLBACK_Patches;

    RepairDroidInstrumenter(String apk, String SemanticPatchPath_os,
                            String SemanticPatchPath_device,
                            String SemanticPatchPath_callback, String logFilePath) throws IOException {
        this.logPath = logFilePath;
        this.pkg = InstrumentUtil.getPackageName(apk);
        this.SemanticPatchPath_os = SemanticPatchPath_os;
        this.SemanticPatchPath_device = SemanticPatchPath_device;
        this.SemanticPatchPath_callback = SemanticPatchPath_callback;
        this.OS_Patches = Helper.getFiles(SemanticPatchPath_os, ".patch");
        this.DEVICE_Patches = Helper.getFiles(SemanticPatchPath_device, ".patch");
        this.CALLBACK_Patches = Helper.getFiles(SemanticPatchPath_callback, ".patch");

        for (String patchFile: OS_Patches){
            PatchParser dataObject = new PatchParser(patchFile);
            this.dataObjects.put(patchFile, dataObject);
        }

        for (String patchFile: DEVICE_Patches){
            PatchParser dataObject = new PatchParser(patchFile);
            this.dataObjects.put(patchFile, dataObject);
        }

        for (String patchFile: CALLBACK_Patches){
            PatchParser dataObject = new PatchParser(patchFile);
            this.dataObjects.put(patchFile, dataObject);
        }

    }

    @Override
    protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {

        // filter the Android official method
        SootMethod sm = b.getMethod();
        if (InstrumentUtil.isAndroidMethod(sm))
            return;

//        if (!methodSig.contains(this.pkg))
//            return;

        for (String patchFile: OS_Patches){
            PatchParser dataObject = this.dataObjects.get(patchFile);
            try {
                OSAPIInstrumenter osapiInstrumenter = new OSAPIInstrumenter(this.pkg, patchFile, this.logPath, dataObject);
                osapiInstrumenter.OSInternalTransform(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String patchFile: DEVICE_Patches){
            PatchParser dataObject = this.dataObjects.get(patchFile);
            try {
                DeviceInstrumenter deviceInstrumenter = new DeviceInstrumenter(this.pkg, patchFile, this.logPath, dataObject);
                deviceInstrumenter.DeviceInternalTransform(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String patchFile: CALLBACK_Patches){
            PatchParser dataObject = this.dataObjects.get(patchFile);
            try {
                CallbackInstrumenter callbackInstrumenter = new CallbackInstrumenter(this.pkg, patchFile, this.logPath, dataObject);
                callbackInstrumenter.CallBackInternalTransform(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}