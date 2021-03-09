package Model;

public class MyLabelStmt extends MyInputObject {
    private String Type = MyConstants.LABELSTMT;
    private boolean if_label_original = false;
    private boolean if_label_next = false;
    private boolean if_label_new = false;
    private boolean if_label_other = false;
    private String labelName;

    public MyLabelStmt(String labelName) {
        this.labelName = labelName;
        switch (labelName) {
            case "<label_original>":
                if_label_original = true;
                break;
            case "<label_next>":
                if_label_next = true;
                break;
            case "<label_new>":
                if_label_new = true;
                break;
            default:
                if_label_other = true;
                break;
        }
    }

    @Override
    public String getType() {
        return Type;
    }

    @Override
    public void setType(String type) {
        Type = type;
    }

    public boolean isIf_label_original() {
        return if_label_original;
    }

    public void setIf_label_original(boolean if_label_original) {
        this.if_label_original = if_label_original;
    }

    public boolean isIf_label_next() {
        return if_label_next;
    }

    public void setIf_label_next(boolean if_label_next) {
        this.if_label_next = if_label_next;
    }

    public boolean isIf_label_new() {
        return if_label_new;
    }

    public void setIf_label_new(boolean if_label_new) {
        this.if_label_new = if_label_new;
    }

    public boolean isIf_label_other() {
        return if_label_other;
    }

    public void setIf_label_other(boolean if_label_other) {
        this.if_label_other = if_label_other;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }
}
