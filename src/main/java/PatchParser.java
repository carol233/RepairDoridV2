import Model.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatchParser {
    private String patchPath;
    private int SDKVersion = -1;
    private String DeviceName = "";
    private String IssueType = "";
    private String IssueMethod = "";
    private MyMethodObject locationTarget;
    private Map<String, String> virables = new HashMap<>();
    private Map<String, String> virables_for_search = new HashMap<>();
    private List<String> class_for_search = new ArrayList<>();
    private String BASECLASS = "";
    private List<MyInputObject> inputStmts = new ArrayList<>();
    private List<MyInputObject> targetStmts = new ArrayList<>();
    private List<MyInputObject> addStmts = new ArrayList<>();
    private List<MyInputObject> deleteStmts = new ArrayList<>();
    private List<MyInputObject> nextStmts = new ArrayList<>();
    private Map<String, Model.MyLabelStmt> labelStmts = new HashMap<>();

    private int mode = 0; // mode for global
    private AtomicBoolean ifNext = new AtomicBoolean(false); // ifNext for global
    private AtomicBoolean ifReadingNewMethod = new AtomicBoolean(false);
    private AtomicBoolean ifReadingNewClass = new AtomicBoolean(false);
    private MyMethodObject currentMethod = null;
    private MyClassObject currentClass = null;

    PatchParser(String path) throws IOException {
        this.patchPath = path;
        this.readData();
    }

    private void addToCategoryStmt(int modeForLine, MyInputObject myNewInputStmt) {
        this.inputStmts.add(myNewInputStmt);
        switch (modeForLine) {
            case 1: case 10: case 11: case 12:
                if (ifReadingNewMethod.get()) {
                    currentMethod.addStmtToBody(myNewInputStmt);
                } else {
                    // TODO: 12/1/21 only OS issue use the nextStmts List
                    if (ifNext.get() && IssueType.equals(MyConstants.ISSUE_TYPE_OS))
                        this.nextStmts.add(myNewInputStmt);
                    else
                        this.addStmts.add(myNewInputStmt);
                }
                break;
            case 13:
                if (ifReadingNewClass.get()) {
                    currentClass.addMemberVariable((MyMemberVariable) myNewInputStmt);
                }
                break;
            case 2: // new method
                assert myNewInputStmt instanceof  MyMethodObject;
                currentMethod = (MyMethodObject) myNewInputStmt;
                break;
            case 3: // new class
                assert myNewInputStmt instanceof  MyClassObject;
                currentClass = (MyClassObject) myNewInputStmt;
                break;
            case 6:
            case 7:
                this.targetStmts.add(myNewInputStmt);
                break;
            case 8:
                this.deleteStmts.add(myNewInputStmt);
                break;
            case 9:
                this.targetStmts.add(myNewInputStmt);
                this.deleteStmts.add(myNewInputStmt);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + modeForLine);
        }
    }

    private void readData() throws IOException {
        FileInputStream inputStream = new FileInputStream(this.patchPath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String str;
        while((str = bufferedReader.readLine()) != null) {

            str = str.replaceAll("[:;,.]?$", "").trim();
            if (str.equals("")) continue;
            if (str.startsWith("//")) continue;

            if (str.toLowerCase().equals("@@ Variable Declaration".toLowerCase())) {
                mode = 1;
                continue;
            }
            if (str.toLowerCase().equals("@@ Issue Location".toLowerCase())) {
                mode = 2;
                continue;
            }
            if (str.toLowerCase().equals("@@ Patch Denotation".toLowerCase())) {
                mode = 3;
                continue;
            }

            if (mode == 1){
                // read virables; IdentityStmt;
                // $r0 := @this: [DECLARING_CLASS]
                // + $a2 := @parameter0: [DECLARING_CLASS]
                AtomicBoolean flag = new AtomicBoolean(false);
                AtomicBoolean flag_searchclass = new AtomicBoolean(false);
                if (str.startsWith("[SEARCH]")) {
                    flag.set(true);
                    str = str.replace("[SEARCH]", "");
                }
                if (str.startsWith("[SEARCH CLASS]")) {
                    flag_searchclass.set(true);
                    str = str.replace("[SEARCH CLASS]", "");
                    Pattern pattern0 = Pattern.compile("(\\[\\S+])");
                    Matcher matcher0 = pattern0.matcher(str);
                    if (matcher0.find()) {
                        String searchclass = matcher0.group(1);
                        class_for_search.add(searchclass);
                    }
                    continue;
                }
                String[] tmp = str.split(":=");
                Pattern pattern = Pattern.compile("[^\\n\\r\\f]+");
                Matcher matcher = pattern.matcher(tmp[1]);
                if (matcher.find()){
                    String var = matcher.group().trim();
                    this.virables.put(tmp[0].trim(), var);
                    if (flag.get())
                        this.virables_for_search.put(tmp[0].trim(), var);
                    continue;
                } else {
                    System.out.println("Patch input error (1)." + patchPath);
                    return;
                }
            }

            if (mode == 2) {
                // [CALLBACK | OS] <API> Build.VERSION.SDK_INT 23
                // [DEVICE] <Camera.Parameters: void setRecordingHint(boolean)> Build.MODEL "Nexus 4"
                Pattern pattern = Pattern.compile("\\[([\\s\\S]+)]\\s+(<(\\S+):\\s(\\S+)\\s(\\S+)\\((.*)\\)>)\\s+(\\S+)\\s+([^\\n\\r\\f]+)");
                Matcher matcher = pattern.matcher(str);
                if (matcher.find()) {
                    IssueType = matcher.group(1).trim();
                    IssueMethod = matcher.group(2).trim();
                    switch (IssueType) {
                        case MyConstants.ISSUE_TYPE_DEVICE:
                            DeviceName = matcher.group(8).replace("\"", "").trim();
                            break;
                        case MyConstants.ISSUE_TYPE_OS:
                        case MyConstants.ISSUE_TYPE_CALLBACK:
                            SDKVersion = Integer.parseInt(matcher.group(8).trim());
                            BASECLASS = matcher.group(3).trim();
                            break;
                        default:
                            System.out.println("Patch input error (2)." + patchPath);
                            return;
                    }

                    List<String> argTypes = new ArrayList<>();
                    String tmps = matcher.group(6).trim();
                    if (!tmps.trim().equals("")){
                        if (tmps.contains(",")) {
                            for (String argStr : tmps.split(","))
                                argTypes.add(argStr.trim());
                        } else{
                            argTypes.add(tmps.trim());
                        }
                    }
                    locationTarget = new MyMethodObject(matcher.group(3).trim(), matcher.group(4).trim(),
                            matcher.group(5).trim(), argTypes);
                }
                continue;
            }

            if (mode == 3) {
                int modeForLine = 0; // mode for Local
                if (str.startsWith("+")){
                    str = str.replaceFirst("^\\+", "").trim();
                    modeForLine = 1;
                    if (str.startsWith("[NEW Method]")) {
                        str = str.replaceFirst("^\\[NEW Method]", "").trim();
                        modeForLine = 2;
                        ifReadingNewMethod.set(true);
                    } else if (str.startsWith("[NEW Class]")) {
                        str = str.replaceFirst("^\\[NEW Class]", "").trim();
                        modeForLine = 3;
                        ifReadingNewClass.set(true);
                    } else if (str.startsWith("[END of Method]")) {
                        modeForLine = 4;
                        ifReadingNewMethod.set(false);
                    } else if (str.startsWith("[END of Class]")) {
                        modeForLine = 5;
                        ifReadingNewClass.set(false);
                    } else if (str.startsWith("[NEW Member Variable]")) {
                        str = str.replaceFirst("^\\[NEW Member Variable]", "").trim();
                        modeForLine = 13;
                    }
                } else if (str.startsWith("-")) {
                    str = str.replaceFirst("^-", "").trim();
                    modeForLine = 8;
                    if (str.startsWith("[Stmt]")) {
                        str = str.replaceFirst("^\\[Stmt]", "").trim();
                        modeForLine = 9;
                    }
                } else if (str.startsWith("[Stmt]")) {
                    str = str.replaceFirst("^\\[Stmt]", "").trim(); // original target stmt
                    modeForLine = 6;
                } else if (str.startsWith("[Method]")) { // original target method
                    str = str.replaceFirst("^\\[Method]", "").trim();
                    modeForLine = 7;
                } else if (str.startsWith("[CUT]")) {
                    // TODO: 12/1/21 add other statement as ending, default: cut until the end of a method
                    modeForLine = 10;
                } else if (str.startsWith("[COPY]")) {
                    modeForLine = 11;
                } else if (str.startsWith("[PASTE]")) {
                    modeForLine = 12;
                }

                switch (modeForLine) {
                    case 0:
                        System.out.println("Mode of Line error." + patchPath);
                        return;
                    case 10:
                        // TODO: 12/1/21 add other statement as ending, default: cut until the end of a method
                        MyOperationStmt myOperationStmt1 = new MyOperationStmt(MyConstants.CUT_OPERATION, new MyReturnStmt(true));
                        addToCategoryStmt(modeForLine, myOperationStmt1);
                        continue;
                    case 11:
                        MyOperationStmt myOperationStmt2 = new MyOperationStmt(MyConstants.COPY_OPERATION, new MyReturnStmt(true));
                        addToCategoryStmt(modeForLine, myOperationStmt2);
                        continue;
                    case 12:
                        MyOperationStmt myOperationStmt3 = new MyOperationStmt(MyConstants.PASTE_OPERATION);
                        addToCategoryStmt(modeForLine, myOperationStmt3);
                        continue;
                    case 2: case 7:
                        // [public] <[DECLARING_CLASS]: void onAttach(android.app.Activity)>
                        Pattern pattern = Pattern.compile("<(\\S+):\\s(\\S+)\\s(\\S+)\\((.*)\\)>");
                        Matcher matcher = pattern.matcher(str);
                        if (matcher.find()) {
                            List<String> argTypes = new ArrayList<>();
                            String tmps = matcher.group(4).trim();
                            if (!tmps.trim().equals("")){
                                if (tmps.contains(",")) {
                                    for (String argStr : tmps.split(","))
                                        argTypes.add(argStr.trim());
                                } else {
                                    argTypes.add(tmps.trim());
                                }
                            }
                            MyMethodObject myMethodObject = new MyMethodObject(matcher.group(1).trim(), str.toLowerCase().contains("[public]"),
                                    str.toLowerCase().contains("[private]"), str.toLowerCase().contains("[static]"), str.toLowerCase().contains("[protected]"),
                                    str.toLowerCase().contains("[final]"), matcher.group(2).trim(), matcher.group(3).trim(), argTypes);
                            addToCategoryStmt(modeForLine, myMethodObject);
                        }
                        continue;
                    case 3:
                        // [final] <android.support.v4.speech.tts.TextToSpeechICSMR1$1> extends <android.speech.tts.UtteranceProgressListener>
                        Pattern pattern2 = Pattern.compile("<(.*)>\\sextends\\s<(.*)>");
                        Matcher matcher2 = pattern2.matcher(str);
                        if (matcher2.find()) {
                            MyClassObject myClassObject = new MyClassObject(matcher2.group(1).trim(), matcher2.group(2).trim(), true,
                                    str.toLowerCase().contains("[public]"), str.toLowerCase().contains("[private]"),
                                    str.toLowerCase().contains("[static]"), str.toLowerCase().contains("[protected]"),
                                    str.toLowerCase().contains("[final]"), str.toLowerCase().contains("[abstract]"));
                            addToCategoryStmt(modeForLine, myClassObject);
                        }
                        continue;
                    case 13:
                        //+ [NEW Member Variable][final] <[CURRENT_CLASS]$UtteranceProgressListenerICSMR1 [field_0]>;
                        Pattern pattern3 = Pattern.compile("<(\\S+)\\s+(\\S+)>");
                        Matcher matcher3 = pattern3.matcher(str);
                        if (matcher3.find()) {
                            MyMemberVariable myMemberVariable = new MyMemberVariable(
                                    str.toLowerCase().contains("[public]"), str.toLowerCase().contains("[private]"),
                                    str.toLowerCase().contains("[static]"), str.toLowerCase().contains("[protected]"),
                                    str.toLowerCase().contains("[final]"), matcher3.group(2).trim(), matcher3.group(1).trim());
                            addToCategoryStmt(modeForLine, myMemberVariable);
                        }
                        continue;
                    case 4: // end of method: target method don'y have body, only one line
                        if (ifReadingNewClass.get()) {
                            currentClass.addNewMethodToClass(currentMethod);
                        } else {
                            this.addStmts.add(currentMethod);
                        }
                        currentMethod = null;
                        continue;
                    case 5: // end of class
                        this.addStmts.add(currentClass);
                        currentClass = null;
                        continue;
                }

                if (str.startsWith("return")) {
                    // TODO: 13/1/21 not return void
                    if (str.trim().equals("return")) {
                        MyReturnStmt myReturnStmt = new MyReturnStmt(true);
                        addToCategoryStmt(modeForLine, myReturnStmt);
                    } else {
                        Pattern pattern = Pattern.compile("return\\s(\\S+)");
                        Matcher matcher = pattern.matcher(str);
                        if (matcher.find()) {
                            String var = matcher.group(1).trim();
                            MyReturnStmt myReturnStmt = new MyReturnStmt(false, var);
                            addToCategoryStmt(modeForLine, myReturnStmt);
                        }
                    }
                    continue;
                }
                // for modeForLine == 1, 6, 8, 9
                if (str.startsWith("<label")) {
                    Pattern pattern = Pattern.compile("<label\\S+>");
                    Matcher matcher = pattern.matcher(str);
                    if (matcher.find()) {
                        String var = matcher.group().trim();
                        MyLabelStmt myLabelStmt = new MyLabelStmt(var);
                        this.labelStmts.put(var, myLabelStmt);
                        if (var.equals("<label_next>"))
                            ifNext.set(true);
                        addToCategoryStmt(modeForLine, myLabelStmt);
                        continue;
                    } else {
                        System.out.println("Patch input error (8)." + patchPath);
                        return;
                    }
                }

                if (str.contains(":=")){
                    // $r0 := @this: [DECLARING_CLASS]
                    // + $a2 := @parameter0: [DECLARING_CLASS]
                    String[] tmp = str.split(":=");
                    Pattern pattern = Pattern.compile("[^\\n\\r]+");
                    Matcher matcher = pattern.matcher(tmp[1]);
                    if (matcher.find()){
                        String var = matcher.group().trim();
                        if (var.contains("@this")){
                            var = var.replace("@this: ", "");
                            MyIdentityStmt myIdentityStmt = new
                                    MyIdentityStmt("@this", tmp[0].trim(), var.trim());
                            this.virables.put(tmp[0].trim(), var.trim());
                            addToCategoryStmt(modeForLine, myIdentityStmt);
                        } else if (var.contains("@parameter")) {
                            Pattern pattern2 = Pattern.compile("@parameter(\\d+):\\s+(\\S+)");
                            Matcher matcher2 = pattern2.matcher(var);
                            if (matcher2.find()){
                                MyIdentityStmt myIdentityStmt = new
                                        MyIdentityStmt("@parameter", tmp[0].trim(), Integer.parseInt(matcher2.group(1).trim()),
                                        matcher2.group(2).trim());
                                this.virables.put(tmp[0].trim(), matcher2.group(2).trim());
                                addToCategoryStmt(modeForLine, myIdentityStmt);
                            } else {
                                System.out.println("Patch input error (9)." + patchPath);
                                return;
                            }
                        } else {
                            this.virables.put(tmp[0].trim(), var.trim());
                        }
                        continue;
                    } else {
                        System.out.println("Patch input error (3)." + patchPath);
                        return;
                    }
                }

                if (str.contains("<Log>")) {
                    Pattern pattern = Pattern.compile("<Log>\\(([\\s\\S]+)\\)");
                    Matcher matcher = pattern.matcher(str);
                    if (matcher.find()) {
                        String var = matcher.group(1).trim();
                        var = var.replaceAll("[\"']?$", "");
                        var = var.replaceAll("^[\"']?", "");
                        MyLogStmt myLogStmt = new MyLogStmt(var);
                        addToCategoryStmt(modeForLine, myLogStmt);
                        continue;
                    }
                }

                if (str.startsWith("if ")) {
                    // solve if statement
                    String newstr = str.replaceFirst("^if\\s+", "");

                    String[] tmp = newstr.split("goto");

                    String target = tmp[1].trim();
                    String condition = tmp[0].trim();
                    String symbol;
                    if (condition.contains(">=")){
                        symbol = ">=";
                    }else if (condition.contains("<=")){
                        symbol = "<=";
                    }else if (condition.contains("==")) {
                        symbol = "==";
                    }else if (condition.contains("!=")) {
                        symbol = "!=";
                    }else if (condition.contains("<")){
                        symbol = "<";
                    }else if (condition.contains(">")){
                        symbol = ">";
                    }else{
                        System.out.println("Patch input error (7)." + patchPath);
                        return;
                    }

                    String[] conditioned = condition.split(symbol);
                    Pattern pattern = Pattern.compile("^[-\\d]+$"); // number
                    Matcher matcher = pattern.matcher(conditioned[1].trim());
                    if (matcher.find()) {
                        MyIfStmt myIfStmt = new MyIfStmt(symbol, conditioned[0].trim(), matcher.group().trim(), target);
                        addToCategoryStmt(modeForLine, myIfStmt);
                        continue;
                    } else {
                        Pattern pattern2 = Pattern.compile("\\S+"); // null or $r2
                        Matcher matcher2 = pattern2.matcher(conditioned[1]);
                        if (matcher2.find()) {
                            MyIfStmt myIfStmt = new MyIfStmt(symbol, conditioned[0].trim(), matcher2.group().trim(), target);
                            addToCategoryStmt(modeForLine, myIfStmt);
                            continue;
                        } else {
                            System.out.println("Patch input error (5)." + patchPath);
                            return;
                        }
                    }
                }

                if (str.startsWith("goto")){
                    String newstr = str.substring("goto".length());
                    MyGotoStmt myGotoStmt = new MyGotoStmt(newstr.trim());
                    addToCategoryStmt(modeForLine, myGotoStmt);
                    continue;
                }

                // assignment
                if (str.contains("=") && !str.contains(":=") && !str.contains("if ") && !str.contains("invoke")){
                    String[] tmp = str.split("=");
                    // (1) $r1 = new android.media.AudioAttributes$Builder;
                    // (2) $r4 = $r3.<android.su...mpat: android.sup...onCompat$b a>;
                    // (3) $r2 = <android.os.Build$VERSION: int SDK_INT>;
                    // (4) $r2 = <android.view.accessibility.AccessibilityNodeInfo$AccessibilityAction: android.view.accessibility.AccessibilityNodeInfo$AccessibilityAction ACTION_SCROLL_BACKWARD>
                    Pattern pattern = Pattern.compile("new\\s(.*)");
                    Matcher matcher = pattern.matcher(tmp[1]);
                    if (matcher.find()){ // (1)
                        String type = MyConstants.NEW_OBJECT_ASSIGNMENT;
                        // $r2 = new android.support.v4.speech.tts.TextToSpeechICSMR1$1
                        MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[0].trim(),
                                matcher.group(1), matcher.group(0));
                        addToCategoryStmt(modeForLine, myAssignStmt);
                        continue;
                    } else { // (2) (3)
                        if (tmp[1].contains("<android.os.Build$VERSION: int SDK_INT>")){
                            String type = MyConstants.SDK_VERSION_ASSIGNMENT;
                            MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[0].trim(),
                                    "int", "<android.os.Build$VERSION: int SDK_INT>");
                            addToCategoryStmt(modeForLine, myAssignStmt);
                            continue;
                        } else {
                            Pattern pattern2 = Pattern.compile("(\\S+).(<(\\S+):\\s(\\S+)\\s(\\S+)>)");
                            Matcher matcher2 = pattern2.matcher(tmp[1].trim());
                            if (matcher2.find()){
                                String type = MyConstants.INSTANCE_FIELD_ASSIGNMENT;
                                MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[0].trim(),
                                        matcher2.group(4), matcher2.group(5), matcher2.group(2), matcher2.group(1), matcher2.group(3), false);
                                addToCategoryStmt(modeForLine, myAssignStmt);
                                continue;
                            } else {
                                Pattern pattern3 = Pattern.compile("<(\\S+):\\s(\\S+)\\s(\\S+)>");
                                Matcher matcher3 = pattern3.matcher(tmp[1].trim());
                                if (matcher3.find()){
                                    String type = MyConstants.STATIC_FIELD_ASSIGNMENT;
                                    MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[0].trim(),
                                            matcher3.group(2), matcher3.group(3), matcher3.group(0), matcher3.group(1));
                                    addToCategoryStmt(modeForLine, myAssignStmt);
                                    continue;
                                } else {
                                    // $r6 = (android.location.OnNmeaMessageListener) $r1
                                    Pattern pattern4 = Pattern.compile("\\((\\S+)\\)\\s(\\S+)");
                                    Matcher matcher4 = pattern4.matcher(tmp[1].trim());
                                    if (matcher4.find()) {
                                        String type = MyConstants.VARIABLE_CAST_ASSIGNMENT;
                                        MyAssignStmt myAssignStmt = new MyAssignStmt(type,
                                                tmp[0].trim(), matcher4.group(1), matcher4.group(2), 1);
                                        addToCategoryStmt(modeForLine, myAssignStmt);
                                        continue;
                                    } else {
                                        // $i0 = lengthof $r6
                                        Pattern pattern5 = Pattern.compile("lengthof\\s(\\S+)");
                                        Matcher matcher5 = pattern5.matcher(tmp[1].trim());
                                        if (matcher5.find()) {
                                            String type = MyConstants.LengthExpr_ASSIGNMENT;
                                            MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[0].trim(), matcher5.group(1));
                                            addToCategoryStmt(modeForLine, myAssignStmt);
                                            continue;
                                        } else {
                                            // $r2 = $r6[$i1]
                                            Pattern pattern6 = Pattern.compile("(\\S+)\\[(\\S+)]");
                                            Matcher matcher6 = pattern6.matcher(tmp[1].trim());
                                            if (matcher6.find()) {
                                                String type = MyConstants.ArrayRef_ASSIGNMENT;
                                                MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[0].trim(),
                                                        matcher6.group(1), matcher6.group(2), true);
                                                addToCategoryStmt(modeForLine, myAssignStmt);
                                                continue;
                                            } else {
                                                // $r0.<com.ms.engage.widget.videoview.MAVideoView: int g> = 0;
                                                Pattern pattern7 = Pattern.compile("(\\S+).(<(\\S+):\\s(\\S+)\\s(\\S+)>)");
                                                Matcher matcher7 = pattern7.matcher(tmp[0].trim());
                                                if (matcher7.find()) {
                                                    String type = MyConstants.LEFT_INSTANCE_FIELD_ASSIGNMENT;
                                                    MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[1].trim(),
                                                            matcher7.group(4), matcher7.group(5), matcher7.group(2),
                                                            matcher7.group(1), matcher7.group(3), true);
                                                    addToCategoryStmt(modeForLine, myAssignStmt);
                                                    continue;
                                                } else {
                                                    // $i1 = $i1 + 1
                                                    Pattern pattern9 = Pattern.compile("(\\S+)\\s?([+\\-*/])\\s?(\\S+)");
                                                    Matcher matcher9 = pattern9.matcher(tmp[1].trim());
                                                    if (matcher9.find()) {
                                                        String type = MyConstants.Compute_ASSIGNMENT;
                                                        MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[0].trim(), matcher9.group(1), matcher9.group(2), matcher9.group(3));
                                                        addToCategoryStmt(modeForLine, myAssignStmt);
                                                        continue;
                                                    } else {
                                                        // $i1 = 0;
                                                        Pattern pattern8 = Pattern.compile("^[-\\d]+$");
                                                        Matcher matcher8 = pattern8.matcher(tmp[1].trim());
                                                        if (matcher8.find()) {
                                                            String type = MyConstants.Constant_ASSIGNMENT;
                                                            MyAssignStmt myAssignStmt = new MyAssignStmt(type, tmp[0].trim(), matcher8.group(0));
                                                            addToCategoryStmt(modeForLine, myAssignStmt);
                                                            continue;
                                                        } else {
                                                            System.out.println("Other Assignment cases.");
                                                            continue;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (str.contains("invoke")){
                    // read diffs;
                    String newstr = str.trim();

                    String type;
                    String MethodSignature = "";

                    String returnClass = "";
                    String returnName = "";
                    int argLen = 0;
                    List<String> argTypes = new ArrayList<>();
                    List<String> argNames = new ArrayList<>();
                    String baseName;
                    String methodName = "";
                    String MethodBaseClassType = "";
                    if (newstr.contains("=")){
                        String[] tmp = newstr.split("=");
                        returnName = tmp[0].trim();
                    }

                    if (newstr.contains("staticinvoke")) {
                        type = MyConstants.STATIC_INVOKE;
                    }else if (newstr.contains("virtualinvoke")) {
                        type = MyConstants.VIRTUAL_INVOKE;
                    }else if (newstr.contains("specialinvoke")){
                        //specialinvoke $r8.<android.media.AudioAttributes$Builder: void <init>()>()
                        type = MyConstants.SPECIAL_INVOKE; // <init> or super
                    } else if (newstr.contains("interfaceinvoke")){
                        //$a3 = interfaceinvoke $a2.<android.webkit.WebResourceRequest: android.net.Uri getUrl()>()
                        type = MyConstants.INTERFACE_INVOKE;
                    } else {
                        System.out.println("Patch input error (10)." + patchPath);
                        return;
                    }

                    if (type.equals(MyConstants.VIRTUAL_INVOKE) ||
                            type.equals(MyConstants.SPECIAL_INVOKE) ||
                            type.equals(MyConstants.INTERFACE_INVOKE)){
                        Pattern pattern = Pattern.compile("\\s(\\S+).(<(\\S+):\\s(\\S+)\\s(\\S+)\\((.*)\\)>)\\((.*)\\)");
                        Matcher matcher = pattern.matcher(str);
                        if (matcher.find()){
                            baseName = matcher.group(1);
                            MethodSignature = matcher.group(2);
                            MethodBaseClassType = matcher.group(3);
                            returnClass = matcher.group(4);
                            methodName = matcher.group(5);
                            String tmps = matcher.group(6);
                            if (!tmps.trim().equals("")){
                                if (tmps.contains(",")) {
                                    String[] tmpargs = tmps.split(",");
                                    argLen = tmpargs.length;
                                    for (String argStr : tmpargs)
                                        argTypes.add(argStr.trim());
                                }else{
                                    argLen = 1;
                                    argTypes.add(tmps.trim());
                                }
                            }
                            String tmp_args = matcher.group(7);
                            if (!tmp_args.trim().equals("")) {
                                if (tmp_args.contains(",")) {
                                    String[] tmpargs = tmp_args.split(",");
                                    for (String argStr : tmpargs)
                                        argNames.add(argStr.trim());
                                }else{
                                    argNames.add(tmp_args.trim());
                                }
                            }
                        } else {
                            System.out.println("Patch input error (6)." + patchPath);
                            System.out.println("Error line: " + str);
                            return;
                        }

                        MyInvokeStmt myStmtObject;
                        if (type.equals(MyConstants.VIRTUAL_INVOKE)){
                            myStmtObject = new MyVirtualInvokeStmt(returnName, returnClass,
                                    argLen, argNames, argTypes, baseName, MethodBaseClassType, methodName, MethodSignature);
                        } else if (type.equals(MyConstants.SPECIAL_INVOKE)){
                            myStmtObject = new MySpecialInvokeStmt(returnName, returnClass,
                                    argLen, argNames, argTypes, baseName, MethodBaseClassType, methodName, MethodSignature);
                        } else {
                            myStmtObject = new MyInterfaceInvokeStmt(returnName, returnClass,
                                    argLen, argNames, argTypes, baseName, MethodBaseClassType, methodName, MethodSignature);
                        }
                        addToCategoryStmt(modeForLine, myStmtObject);

                    } else { // static invoke
                        Pattern pattern = Pattern.compile("\\s(<(\\S+):\\s(\\S+)\\s(\\w+)\\((.*)\\)>)\\((.*)\\)");
                        Matcher matcher = pattern.matcher(str);
                        if (matcher.find()){
                            MethodSignature = matcher.group(1);
                            MethodBaseClassType = matcher.group(2);
                            returnClass = matcher.group(3);
                            methodName = matcher.group(4);
                            String tmps = matcher.group(5);
                            if (!tmps.trim().equals("")){
                                if (tmps.contains(",")) {
                                    String[] tmpargs = tmps.split(",");
                                    argLen = tmpargs.length;
                                    for (String argStr : tmpargs)
                                        argTypes.add(argStr.trim());
                                }else{
                                    argLen = 1;
                                    argTypes.add(tmps.trim());
                                }
                            }
                            String tmp_args = matcher.group(6);
                            if (!tmp_args.trim().equals("")) {
                                if (tmp_args.contains(",")) {
                                    String[] tmpargs = tmp_args.split(",");
                                    for (String argStr : tmpargs)
                                        argNames.add(argStr.trim());
                                }else{
                                    argNames.add(tmp_args.trim());
                                }
                            }
                        }
                        MyStaticInvokeStmt myStmtObject = new MyStaticInvokeStmt(returnName, MethodBaseClassType, returnClass,
                                argLen, argNames, argTypes, methodName, MethodSignature);
                        addToCategoryStmt(modeForLine, myStmtObject);
                    }
                }
            }
        }

        //close
        inputStream.close();
        bufferedReader.close();
    }

    public String getPatchPath() {
        return patchPath;
    }

    public void setPatchPath(String patchPath) {
        this.patchPath = patchPath;
    }

    public int getSDKVersion() {
        return SDKVersion;
    }

    public void setSDKVersion(int SDKVersion) {
        this.SDKVersion = SDKVersion;
    }

    public String getDeviceName() {
        return DeviceName;
    }

    public void setDeviceName(String deviceName) {
        DeviceName = deviceName;
    }

    public List<MyInputObject> getDeleteStmts() {
        return deleteStmts;
    }

    public void setDeleteStmts(List<MyInputObject> deleteStmts) {
        this.deleteStmts = deleteStmts;
    }

    public int getMode() {
        return mode;
    }

    public String getIssueType() {
        return IssueType;
    }

    public void setIssueType(String issueType) {
        IssueType = issueType;
    }

    public MyMethodObject getLocationTarget() {
        return locationTarget;
    }

    public void setLocationTarget(MyMethodObject locationTarget) {
        this.locationTarget = locationTarget;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public AtomicBoolean getIfNext() {
        return ifNext;
    }

    public void setIfNext(AtomicBoolean ifNext) {
        this.ifNext = ifNext;
    }

    public AtomicBoolean getIfReadingNewMethod() {
        return ifReadingNewMethod;
    }

    public void setIfReadingNewMethod(AtomicBoolean ifReadingNewMethod) {
        this.ifReadingNewMethod = ifReadingNewMethod;
    }

    public AtomicBoolean getIfReadingNewClass() {
        return ifReadingNewClass;
    }

    public void setIfReadingNewClass(AtomicBoolean ifReadingNewClass) {
        this.ifReadingNewClass = ifReadingNewClass;
    }

    public MyMethodObject getCurrentMethod() {
        return currentMethod;
    }

    public void setCurrentMethod(MyMethodObject currentMethod) {
        this.currentMethod = currentMethod;
    }

    public MyClassObject getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(MyClassObject currentClass) {
        this.currentClass = currentClass;
    }

    public Map<String, String> getVirables() {
        return virables;
    }

    public void setVirables(Map<String, String> virables) {
        this.virables = virables;
    }

    public List<MyInputObject> getInputStmts() {
        return inputStmts;
    }

    public void setInputStmts(List<MyInputObject> inputStmts) {
        this.inputStmts = inputStmts;
    }

    List<MyInputObject> getTargetStmts() {
        return targetStmts;
    }

    public void setTargetStmts(List<MyInputObject> targetStmts) {
        this.targetStmts = targetStmts;
    }

    List<MyInputObject> getAddStmts() {
        return addStmts;
    }

    public void setAddStmts(List<MyInputObject> addStmts) {
        this.addStmts = addStmts;
    }

    List<MyInputObject> getNextStmts() {
        return nextStmts;
    }

    public void setNextStmts(List<MyInputObject> nextStmts) {
        this.nextStmts = nextStmts;
    }

    public void setLabelStmts(Map<String, MyLabelStmt> labelStmts) {
        this.labelStmts = labelStmts;
    }

    Map<String, MyLabelStmt> getLabelStmts() {
        return labelStmts;
    }

    public Map<String, String> getVirables_for_search() {
        return virables_for_search;
    }

    public void setVirables_for_search(Map<String, String> virables_for_search) {
        this.virables_for_search = virables_for_search;
    }

    public String getIssueMethod() {
        return IssueMethod;
    }

    public void setIssueMethod(String issueMethod) {
        IssueMethod = issueMethod;
    }

    public List<String> getClass_for_search() {
        return class_for_search;
    }

    public void setClass_for_search(List<String> class_for_search) {
        this.class_for_search = class_for_search;
    }

    public String getBASECLASS() {
        return BASECLASS;
    }

    public void setBASECLASS(String BASECLASS) {
        this.BASECLASS = BASECLASS;
    }
}
