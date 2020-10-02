package life.genny.qwanda.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwanda.adapter.LocalDateTimeAdapter;
import life.genny.qwanda.Link;
import life.genny.qwanda.Value;
import life.genny.qwanda.attribute.Attribute;

@Entity
@Table(name = "qbaseentity_baseentity")
@RegisterForReflection
public class EntityEntity extends PanacheEntity implements java.io.Serializable, Comparable<Object> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(EntityEntity.class);

    private static final String REGEX_REALM = "[a-zA-Z0-9]+";
    private static final String DEFAULT_REALM = "genny";

    @NotEmpty
    @JsonbTransient
    @Pattern(regexp = REGEX_REALM, message = "Must be valid Realm Format!")
    public String realm = DEFAULT_REALM;

    @JsonbTypeAdapter(LocalDateTimeAdapter.class)
    public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));

    @JsonbTypeAdapter(LocalDateTimeAdapter.class)
    public LocalDateTime updated;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    public Attribute attribute;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOURCE_ID", nullable = true)
    public BaseEntity source;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TARGET_ID", nullable = true)
    public BaseEntity target;

    // For compatibility initially

    public String attributeCode;
    public String sourceCode;
    public String targetCode;

    @Embedded
    @NotNull
    public Value value = new Value();

    @Embedded
    public Link link = new Link();

    public EntityEntity() {
    }

    /**
     * Constructor.
     *
     * @param source    the source baseEntity
     * @param target    the target entity that is linked to
     * @param attribute the associated linkAttribute
     * @param weight    the weighted importance of this attribute (relative to the other attributes)
     */
    public EntityEntity(final BaseEntity source, final BaseEntity target,
                        final Attribute attribute, Double weight) {
        this(source, target, attribute, "DUMMY", weight);
        this.link.linkValue = null;
    }

    /**
     * Constructor.
     *
     * @param source    the source baseEntity
     * @param target    the target entity that is linked to
     * @param attribute the associated linkAttribute
     * @param value     the associated linkValue
     * @param weight    the weighted importance of this attribute (relative to the other attributes)
     */
    public EntityEntity(final BaseEntity source, final BaseEntity target,
                        final Attribute attribute, final Object value, Double weight) {
        this.source = source;
        this.attribute = attribute;
        this.realm = target.realm;
        this.target = target;
        link = new Link(source.code, target.code, attribute.code, null);

        if (value != null) {
            this.value.setValue(value);
        }
        if (weight == null) {
            weight = 0.0; // This permits ease of adding attributes and hides
            // attribute from scoring.
        }
        setWeight(weight);
    }
//

    /**
     * Constructor.
     *
     * @param BaseEntity    the entity that needs to contain attributes
     * @param Attribute     the associated Attribute
     * @param linkAttribute the associated linkAttribute
     * @param Weight        the weighted importance of this attribute (relative to
     *                      the other attributes)
     * @param Value         the value associated with this attribute
     */
    public EntityEntity(final BaseEntity source, final BaseEntity target, final Attribute attribute, Double weight,
                        final Object value) {
        this(source, target, attribute, value, weight);
    }

    /**
     * @return the weight
     */
    public Double getWeight() {
        return link.weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(final Double weight) {
        this.link.weight = weight;
    }


    /**
     * @param link the link to set
     */
    public void setLink(Link link) {
        this.link = link;
    }

    @JsonbTransient
    @Transient
    public <T> T getValue() {
        return value.getValue();

    }

    @JsonbTransient
    @Transient
    public <T> void setValue(final Object value) {

        if (value == null) {
            this.value.setValue(this.attribute.defaultValue);
        } else {
            this.value.setValue(value);
        }
    }

    @JsonbTransient
    @Transient
    public <T> void setLoopValue(final Object value) {
        setValue(value);
    }

    @JsonbTransient
    @Transient
    public String getAsString() {

        return value.toString();
    }

    @JsonbTransient
    @Transient
    public String getAsLoopString() {
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    @JsonbTransient
    @Transient
    public <T> T getLoopValue() {
        return getValue();

    }

    public int compareTo(EntityEntity obj) {
        if (this == obj)
            return 0;

        return value.compareTo(obj.value);
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
