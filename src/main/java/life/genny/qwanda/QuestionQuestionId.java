package life.genny.qwanda;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Embeddable
public class QuestionQuestionId implements java.io.Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonBackReference(value = "questionQuestion")
    @JsonIgnore
    private Question source;

    @Expose
    private String targetCode;
    @Expose
    private String sourceCode;


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

    //

    /**
     * @return the source
     */
    @JsonIgnore
    public Question getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(final Question source) {
        this.source = source;
        this.sourceCode = source.getCode();
    }


    /**
     * @return the sourceCode
     */
    public String getSourceCode() {
        return sourceCode;
    }

    /**
     * @param sourceCode the sourceCode to set
     */
    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    @Override
    public int hashCode() {
//        int result;
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(sourceCode);
        hcb.append(targetCode);
        return hcb.toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof QuestionQuestionId)) {
            return false;
        }
        QuestionQuestionId that = (QuestionQuestionId) obj;
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(sourceCode, that.sourceCode);
        eb.append(targetCode, that.targetCode);
        return eb.isEquals();
    }
}