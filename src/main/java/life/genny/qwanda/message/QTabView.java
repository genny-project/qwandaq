package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;
import life.genny.qwanda.QCmdViewMessage;

public class QTabView {

    @Expose
    private String name;

    @Expose
    private String title;

    @Expose
    private String icon;

    @Expose
    private QCmdViewMessage layout;

    public void setTitle(String title) {
        this.title = title;
    }


    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }


    public String getTitle() {
        return this.title;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return this.icon;
    }


    /**
     * @return the layout
     */
    public QCmdViewMessage getLayout() {
        return layout;
    }


    /**
     * @param layout the layout to set
     */
    public void setLayout(QCmdViewMessage layout) {
        this.layout = layout;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "QTabView [name=" + name + ", title=" + title + ", icon=" + icon + ", layout=" + layout + "]";
    }


    public QTabView(String name, String icon, QCmdViewMessage layout) {
        super();
        this.name = name;
        this.icon = icon;
        this.layout = layout;
    }

}
