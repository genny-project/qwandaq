package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;

public class QCmdViewMessageAction {

    @Expose
    private String code;

    @Expose
    private String title;

    @Expose
    private String icon;

    @Expose
    private String color;

    public QCmdViewMessageAction(String code) {
        super();
        this.code = code;
    }

    public QCmdViewMessageAction(String code, String title) {
        super();
        this.code = code;
        this.title = title;
    }

    public QCmdViewMessageAction(String code, String title, String icon) {
        super();
        this.code = code;
        this.title = title;
        this.icon = icon;
    }

    public QCmdViewMessageAction(String code, String title, String icon, String color) {
        super();
        this.code = code;
        this.title = title;
        this.icon = icon;
        this.color = color;
    }


    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "QCmdViewMessageAction [code=" + code + ", title=" + title + ", icon=" + icon + ", color=" + color + "]";
    }


}
