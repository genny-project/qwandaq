package life.genny.qwanda.utils;

import life.genny.qwanda.GennyToken;

import java.io.Serializable;


public class SessionFacts implements Serializable {

    /**
     *
     */

    GennyToken serviceToken;
    GennyToken userToken;
    Object message;

    private SessionFacts() {
    }

    public SessionFacts(GennyToken serviceToken, GennyToken userToken, Object message) {
        this.serviceToken = serviceToken;
        this.userToken = userToken;
        this.message = message;
    }

    @Override
    public String toString() {
        return "SessionFacts [serviceToken=" + serviceToken + ", userToken=" + userToken + ", message=" + message + "]";
    }

    /**
     * @return the serviceToken
     */
    public GennyToken getServiceToken() {
        return serviceToken;
    }

    /**
     * @param serviceToken the serviceToken to set
     */
    public void setServiceToken(GennyToken serviceToken) {
        this.serviceToken = serviceToken;
    }

    /**
     * @return the userToken
     */
    public GennyToken getUserToken() {
        return userToken;
    }

    /**
     * @param userToken the userToken to set
     */
    public void setUserToken(GennyToken userToken) {
        this.userToken = userToken;
    }

    /**
     * @return the message
     */
    public Object getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(Object message) {
        this.message = message;
    }


}


