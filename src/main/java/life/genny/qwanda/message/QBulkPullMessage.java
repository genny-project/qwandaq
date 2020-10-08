package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class QBulkPullMessage implements Serializable {


    private static final long serialVersionUID = 1L;

    @Expose
    private String token;

    @Expose
    private String data_type = QBulkPullMessage.class.getSimpleName();

    @Expose
    private String pullUrl;

    /**
     * @return the pullUrl
     */
    public String getPullUrl() {
        return pullUrl;
    }


    /**
     * @param pullUrl the pullUrl to set
     */
    public void setPullUrl(String pullUrl) {
        this.pullUrl = pullUrl;
    }


    public QBulkPullMessage(final String pullUrl) {
        this.pullUrl = pullUrl;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }


    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        this.token = token;
    }


    /**
     * @return the data_type
     */
    public String getData_type() {
        return data_type;
    }


    /**
     * @param data_type the data_type to set
     */
    public void setData_type(String data_type) {
        this.data_type = data_type;
    }

}
