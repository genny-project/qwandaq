package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class MessageData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return " MessageData [code=" + code + "   " + id + "]";
    }

    @Expose
    private String code;

    @Expose
    private String parentCode;

    @Expose
    private String rootCode;

    @Expose
    private String targetCode;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the parentCode
     */
    public String getParentCode() {
        return parentCode;
    }

    /**
     * @param parentCode the parentCode to set
     */
    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    /**
     * @return the rootCode
     */
    public String getRootCode() {
        return rootCode;
    }

    /**
     * @param rootCode the rootCode to set
     */
    public void setRootCode(String rootCode) {
        this.rootCode = rootCode;
    }

    /**
     * @return the targetCode
     */
    public String getTargetCode() {
        return targetCode;
    }

    /**
     * @param targetCode the targetCode to set
     */
    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }


    @Expose
    private Long id;
    @Expose
    private String value;

    public MessageData(String code) {
        this.code = code;
    }
}