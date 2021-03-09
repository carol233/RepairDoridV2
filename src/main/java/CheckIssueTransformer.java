import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckIssueTransformer extends BodyTransformer
{

    private String Sha256;
    private long fileSize;
    private String pkgName;
    private String dbPath;
    private int mode = -1;
    private  List<String> OldList = new ArrayList<>();

    CheckIssueTransformer(String appPath, String dbPath, String CDA_path) {
        this.pkgName = getPackageName(appPath);
        this.fileSize = getFileSize(appPath);
        List<String> pathElements = new ArrayList<>();
        Paths.get(appPath).forEach(p -> pathElements.add(p.toString()));
        String s = pathElements.get(pathElements.size() - 1);
        this.Sha256 = s.substring(0, s.length() - 4);
        this.dbPath = dbPath;

        try {
            loadfromCDAFile(CDA_path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long getFileSize(String filename) {
        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            System.out.println("file not exists");
            return -1;
        }
        return file.length();
    }

    protected void internalTransform(final Body body, String phaseName, @SuppressWarnings("rawtypes") Map options) {

        SootMethod sm = body.getMethod();
        SootClass sc = sm.getDeclaringClass();

        final PatchingChain<Unit> units = body.getUnits();

        for (Iterator<Unit> unitIter = units.snapshotIterator(); unitIter.hasNext(); ) {
            Stmt stmt = (Stmt) unitIter.next();

            if(stmt.toString().contains("android.os.Build$VERSION: int SDK_INT")){
                return;
            }

            if (stmt.containsInvokeExpr()) {
                SootMethod callee = stmt.getInvokeExpr().getMethod();
                String calleeSig = callee.getSignature();
                if (this.OldList.contains(calleeSig)) { // unprotected
                    // save
                    try {
                        SaveResults(calleeSig, sm.getSignature());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }


    public void SaveResults(String API, String methodSig) throws IOException {
        FileOutputStream fos = new FileOutputStream(this.dbPath, true);
        OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        CSVFormat csvFormat = CSVFormat.DEFAULT;
        CSVPrinter csvPrinter = new CSVPrinter(osw, csvFormat);
        System.out.println("Sha256 = " + this.Sha256 + ", API = " + API + ", MethodSig = " + methodSig);
        csvPrinter.printRecord(this.Sha256, this.fileSize, API, methodSig);
        csvPrinter.flush();
        csvPrinter.close();
    }


    private void SaveDic(HashMap<String, List<String>> Dict) throws Exception{
        FileOutputStream fos = new FileOutputStream(this.dbPath);
        OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader("MethodSig", "FirstInvoke");
        CSVPrinter csvPrinter = new CSVPrinter(osw, csvFormat);

        for (Map.Entry<String, List<String>> entry : Dict.entrySet()) {
            System.out.println("MethodSig = " + entry.getKey() + ", FirstInvoke = " + entry.getValue());
            csvPrinter.printRecord(entry.getKey(), entry.getValue());
        }

        csvPrinter.flush();
        csvPrinter.close();
    }

    private boolean isSelfClass(SootClass sootClass)
    {
        if (sootClass.isPhantom())
        {
            return false;
        }

        String packageName = sootClass.getPackageName();

        return packageName.startsWith(this.pkgName);
    }

    public static String getPackageName(String apkPath) {
        String packageName = "";
        try {
            ProcessManifest manifest = new ProcessManifest(apkPath);
            packageName = manifest.getPackageName();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        return packageName;
    }

    private void loadfromCDAFile(String CDApath) throws IOException {
        File filename = new File(CDApath); // 要读取以上路径的input.txt文件
        InputStreamReader reader = new InputStreamReader(
                new FileInputStream(filename)); // 建立一个输入流对象reader
        BufferedReader br = new BufferedReader(reader); // 建立一个对象，它把文件内容转成计算机能读懂的语言
        String line = "";
        line = br.readLine();
        while (line != null) {
            // --->    <android.widget.RemoteViews: void setRemoteAdapter(int,android.content.Intent)>[normal]
            Pattern pattern = Pattern.compile("<\\S+:\\s\\S+\\s[\\w<>]+\\(.*\\)>");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String old_sig = matcher.group(0);
                this.OldList.add(old_sig);
            }
            line = br.readLine(); // 一次读入一行数据
        }
        System.out.println(this.OldList.size());
    }

    private boolean CheckandMkdir(String path){
        File folder = new File(path);
        if (!folder.exists() && !folder.isDirectory()) {
            if (!folder.mkdir()) {
                System.out.println("Dir create error!");
                return false;
            }
        }
        return true;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
