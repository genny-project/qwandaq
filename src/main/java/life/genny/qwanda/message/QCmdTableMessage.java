package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class QCmdTableMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Expose
    private String title;

    @Expose
    private String icon;

    @Expose
    private String code;

    @Expose
    private String subCode;

    @Expose
    private String color;

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
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

    /**
     * @return the subCode
     */
    public String getSubCode() {
        return subCode;
    }

    /**
     * @param subCode the subCode to set
     */
    public void setSubCode(String subCode) {
        this.subCode = subCode;
    }

    /**
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(String color) {
        this.color = color;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "QCmdTableMessage [title=" + title + ", icon=" + icon + ", code=" + code + ", subCode=" + subCode
                + ", color=" + color + "]";
    }

    public QCmdTableMessage(String code) {
        super();
        this.code = code;
    }

    public QCmdTableMessage(String code, String title) {
        super();
        this.code = code;
        this.title = title;
    }

    public QCmdTableMessage(String code, String title, String icon) {
        super();
        this.code = code;
        this.title = title;
        this.icon = icon;
    }

    public QCmdTableMessage(String code, String title, String icon, String color) {
        super();
        this.code = code;
        this.title = title;
        this.icon = icon;
        this.color = color;
    }

    public QCmdTableMessage(String code, String title, String icon, String color, String subCode) {
        super();
        this.code = code;
        this.title = title;
        this.icon = icon;
        this.color = color;
        this.subCode = subCode;
    }


}
