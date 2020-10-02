package life.genny.qwanda.attribute;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwanda.adapter.LocalDateTimeAdapter;
import life.genny.qwanda.Value;
import life.genny.qwanda.entity.BaseEntity;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jboss.logging.Logger;

import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

@Entity

@Table(name = "qbaseentity_attribute",
        indexes = {
                @Index(columnList = "baseEntityCode", name = "ba_idx"),
                @Index(columnList = "attributeCode", name = "ba_idx"),
                @Index(columnList = "valueString", name = "ba_idx"),
                @Index(columnList = "valueBoolean", name = "ba_idx")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"attributeCode", "baseEntityCode", "realm"})
)

@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@RegisterForReflection
public class EntityAttribute extends PanacheEntity {
    //
    private static final Logger log = Logger.getLogger(EntityAttribute.class);

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
    //
//	@JsonbTypeAdapter(AttributeAdapter.class)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    public Attribute attribute;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BASEENTITY_ID", nullable = false)
    public BaseEntity baseentity;

    // For compatibility initially
    public String baseEntityCode;
    public String attributeCode;
    public String attributeName;

    @Embedded
    @NotNull
    public Value value = new Value();

    public Boolean readonly = false;

    @Transient
    public Integer index = 0; // used to assist with ordering

    /**
     * Store the relative importance of the attribute for the baseEntity
     */
    public Boolean inferred = false;

    /**
     * Store the privacy of this attribute , i.e. Don't display
     */
    public Boolean privacyFlag = false;

    public EntityAttribute() {
    }

    /**
     * Constructor.
     *
     * @param baseEntity the entity that needs to contain attributes
     * @param attribute  the associated Attribute
     * @param weight     the weighted importance of this attribute (relative to the
     *                   other attributes)
     */
    public EntityAttribute(final BaseEntity baseEntity, final Attribute attribute, Double weight) {
        this(baseEntity, attribute, weight, null);
    }

    /**
     * Constructor.
     *
     * @param baseEntity the entity that needs to contain attributes
     * @param attribute  the associated Attribute
     * @param weight     the weighted importance of this attribute (relative to the
     *                   other attributes)
     * @param value      the value associated with this attribute
     */
    public EntityAttribute(final BaseEntity baseEntity, final Attribute attribute, Double weight, final Object value) {
        autocreateCreated();
        this.baseentity = baseEntity;
        this.attribute = attribute;
        setWeight(weight);
        privacyFlag = attribute.defaultPrivacyFlag;
        setValue(value);
    }

    @PreUpdate
    public void autocreateUpdate() {
        updated = LocalDateTime.now(ZoneId.of("UTC"));
    }

    @PrePersist
    public void autocreateCreated() {
        if (created == null)
            created = LocalDateTime.now(ZoneId.of("UTC"));
    }

    @Transient
    @JsonbTransient
    public Date getCreatedDate() {
        final Date out = Date.from(created.atZone(ZoneId.systemDefault()).toInstant());
        return out;
    }

    @Transient
    @JsonbTransient
    public Date getUpdatedDate() {
        if (updated == null)
            return null;
        final Date out = Date.from(updated.atZone(ZoneId.systemDefault()).toInstant());
        return out;
    }

    @SuppressWarnings("unchecked")
    @JsonbTransient
    @Transient
    public <T> T getValue() {
        return value.getValue();
    }

    @JsonbTransient
    @Transient
    public <T> void setValue(final Object value) {
        if (this.readonly) {
            log.error("Trying to set the value of a readonly EntityAttribute! " + attribute.code);
            return;
        }
        setValue(value, true);
    }

    @JsonbTransient
    @Transient
    public <T> void setValue(final Object value, final Boolean lock) {
        if (this.readonly) {
            log.error("Trying to set the value of a readonly EntityAttribute! " + attribute.code);
            return;
        }

        if (value == null) {
            this.value.setValue(this.attribute.defaultValue);
        } else {
            this.value.setValue(value);
        }
        // if the lock is set then 'Lock it in Eddie!'.
        if (lock) {
            this.readonly = true;
        }
    }

    @JsonbTransient
    @Transient
    public <T> void setLoopValue(final Object value) {
        setValue(value, false);
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

    public int compareTo(EntityAttribute obj) {
        if (this == obj)
            return 0;
        return value.compareTo(obj.value);
    }

    @Override
    public String toString() {
        return "attributeCode=" + attribute.code + ", value=" + value + ", weight=" + value.weight + ", inferred="
                + inferred + "] be=" + baseentity.code;
    }

    @SuppressWarnings("unchecked")
    @JsonbTransient
    @Transient
    public <T> T getObject() {
        return getValue();
    }

    @JsonbTransient
    @Transient
    public String getObjectAsString() {
        return value.toString();
    }

    @JsonbTransient
    @Transient
    public Boolean getValueBoolean() {
        return value.valueBoolean;
    }

    @JsonbTransient
    @Transient
    public String getValueString() {
        return value.valueString;
    }

    @JsonbTransient
    @Transient
    public Double getValueDouble() {
        return value.valueDouble;
    }

    @JsonbTransient
    @Transient
    public Integer getValueInteger() {
        return value.valueInteger;
    }

    @JsonbTransient
    @Transient
    public Long getValueLong() {
        return value.valueLong;
    }

    @JsonbTransient
    @Transient
    public LocalDateTime getValueDateTime() {
        return value.valueDateTime;
    }

    @JsonbTransient
    @Transient
    public LocalDate getValueDate() {
        return value.valueDate;
    }

    @JsonbTransient
    @Transient
    public LocalTime getValueTime() {
        return value.valueTime;
    }

    /**
     * @return the index
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    @Transient
    public Double getWeight() {
        return value.weight;
    }

    @Transient
    public void setWeight(Double weight) {
        if (weight == null) {
            weight = 0.0; // This permits ease of adding attributes and hides
            // attribute from scoring.
        }
        value.weight = weight;
    }
}
