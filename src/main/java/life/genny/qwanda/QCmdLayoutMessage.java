package life.genny.qwanda;

import com.google.gson.annotations.Expose;

public class QCmdLayoutMessage extends QCmdMessage {
    private static final String CMD_TYPE = "CMD_LAYOUT";
    @Expose
    private String data;
    @Expose
    private Boolean visible;


    public QCmdLayoutMessage(final String layoutCode, final String layout) {
        super(CMD_TYPE, layoutCode);
        setData(layout);
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    private void setData(final String data) {
        this.data = data;
    }

    /**
     * @return the visible
     */
    public Boolean getVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(final Boolean visible) {
        this.visible = visible;
    }


}
