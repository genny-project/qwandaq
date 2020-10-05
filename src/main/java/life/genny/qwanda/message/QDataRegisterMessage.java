package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;

public class QDataRegisterMessage {

    private static final long serialVersionUID = 1L;
    private static final String DATATYPE_REGISTER = "Register";
    @Expose
    private String keycloakUrl = null;
    @Expose
    private String realm = null;
    @Expose
    private String username = null;
    @Expose
    private String firstname = null;
    @Expose
    private String lastname = null;
    @Expose
    private String password = null;
    @Expose
    private String email = null;

    public QDataRegisterMessage() {
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    /**
     * @return the datatypeRegister
     */
    public static String getDatatypeRegister() {
        return DATATYPE_REGISTER;
    }

    /**
     * @return the keycloakUrl
     */
    public String getKeycloakUrl() {
        return keycloakUrl;
    }

    /**
     * @return the realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @return the lastname
     */
    public String getLastname() {
        return lastname;
    }


}
