package life.genny.qwanda;

import com.google.gson.annotations.Expose;
import life.genny.qwanda.message.QTabView;

public class QCmdTabViewMessage extends QCmdViewMessage {

    private static final String CMD_TYPE = "CMD_VIEW";
    private static final String VIEW_TYPE = "TAB_VIEW";
    private static final long serialVersionUID = 1L;

    /*
     * @Expose private QTabView[] views;
     */

    @Expose
    private QTabView[] tabs;


    public QCmdTabViewMessage(final QTabView[] tabs) {
        super(CMD_TYPE, VIEW_TYPE);
        // this.views = views;
        this.tabs = tabs;
    }

    public QCmdTabViewMessage(final Object root, final QTabView[] tabs) {
        super(VIEW_TYPE, root);
        this.tabs = tabs;
    }

    /*
     * public void setViews(QTabView[] views) { this.views = views; }
     *
     * public QTabView[] getViews() { return this.views; }
     */

    /**
     * @return the tabs
     */
    public QTabView[] getTabs() {
        return tabs;
    }

    /**
     * @param tabs the tabs to set
     */
    public void setTabs(QTabView[] tabs) {
        this.tabs = tabs;
    }


}
