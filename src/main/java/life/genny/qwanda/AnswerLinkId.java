package life.genny.qwanda;

import com.fasterxml.jackson.annotation.JsonIgnore;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.util.Objects;

@Embeddable
public class AnswerLinkId implements java.io.Serializable {

    @JsonIgnore
    @ManyToOne
    private BaseEntity source;

    @JsonIgnore
    @ManyToOne
    private BaseEntity target;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private Attribute attribute;

    // @JsonIgnore
    // @ManyToOne(optional = true, fetch = FetchType.LAZY)
    // private Ask ask;

    /**
     * @return the source
     */
    public BaseEntity getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(final BaseEntity source) {
        this.source = source;
    }

    /**
     * @return the target
     */
    public BaseEntity getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(final BaseEntity target) {
        this.target = target;
    }


    /**
     * @return the linkAttribute
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * @param attribute the linkAttribute to set
     */
    public void setAttribute(final Attribute attribute) {
        this.attribute = attribute;
    }


    // /**
    // * @return the ask
    // */
    // public Ask getAsk() {
    // return ask;
    // }
    //
    // /**
    // * @param ask the ask to set
    // */
    // public void setAsk(final Ask ask) {
    // this.ask = ask;
    // }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final AnswerLinkId that = (AnswerLinkId) o;

        if (!Objects.equals(source, that.source))
            return false;
        if (!Objects.equals(target, that.target))
            return false;
        if (!Objects.equals(attribute, that.attribute))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (source != null ? source.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 127 * result + (attribute != null ? attribute.hashCode() : 0);
        return result;
    }

}
