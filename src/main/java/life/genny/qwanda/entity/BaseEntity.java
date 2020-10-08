/*
 * (C) Copyright 2017 GADA Technology (http://www.outcome-hub.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributors: Adam Crow Byron Aguirre
 */

package life.genny.qwanda.entity;

import com.google.gson.annotations.Expose;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwanda.Answer;
import life.genny.qwanda.AnswerLink;
import life.genny.qwanda.adapter.LocalDateTimeAdapter;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.exception.BadDataException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jboss.logging.Logger;

import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlTransient;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BaseEntity represents a base entity that contains many attributes. It is the
 * base parent for many Qwanda classes and serves to establish Hibernate
 * compatibility and datetime stamping. BaseEntity objects may be scored against
 * each other. BaseEntity objects may not have a deterministic code Examples of
 * derivative entities may be Person, Company, Event, Product, TradeService.
 * This attribute information includes:
 * <ul>
 * <li>The List of attributes
 * </ul>
 *
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */

@Entity
@Table(name = "qbaseentity",
        indexes = {
                @Index(columnList = "active", name = "code_idx"),
                @Index(columnList = "code", name = "code_idx"),
                @Index(columnList = "realm", name = "code_idx")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"code", "realm"}))
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@RegisterForReflection
public class BaseEntity extends PanacheEntity {


    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(BaseEntity.class);

    private static final String DEFAULT_CODE_PREFIX = "BAS_";
    private static final String REGEX_CODE = "[A-Z]{3}\\_[A-Z0-9\\.\\-\\@\\_]*";

    private static final String REGEX_NAME = "[\\pL0-9/\\:\\ \\_\\.\\,\\?\\>\\<\\%\\$\\&\\!\\*";
    private static final String REGEX_REALM = "[a-zA-Z0-9]+";
    private static final String DEFAULT_REALM = "genny";

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Column(name = "active")
    public Boolean active = true;


    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @NotEmpty
    @JsonbTransient
    @Pattern(regexp = REGEX_REALM, message = "Must be valid Realm Format!")
    public String realm = DEFAULT_REALM;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @NotNull
    @Size(max = 64)
    @Pattern(regexp = REGEX_CODE, message = "Must be valid Code!")
    @Column(name = "code", updatable = false, nullable = false, unique = true)
    public String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    @Size(max = 128)
    @Pattern(regexp = REGEX_NAME, message = "Must contain valid characters for name")
    @Column(name = "name", updatable = true, nullable = true)
    public String name;


    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    @JsonbTypeAdapter(LocalDateTimeAdapter.class)
    public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    @JsonbTypeAdapter(LocalDateTimeAdapter.class)
    public LocalDateTime updated;

    public Set<EntityAttribute> getBaseEntityAttributes() {
        return baseEntityAttributes;
    }

    public void setBaseEntityAttributes(Set<EntityAttribute> baseEntityAttributes) {
        this.baseEntityAttributes = baseEntityAttributes;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    public Set<EntityAttribute> baseEntityAttributes = new HashSet<>();

    public Set<EntityEntity> getLinks() {
        return links;
    }

    public void setLinks(Set<EntityEntity> links) {
        this.links = links;
    }

    //	@OneToMany(fetch = FetchType.EAGER, mappedBy = "pk.source")
//	@JsonBackReference(value = "entityEntity")
//	@Cascade({ CascadeType.MERGE, CascadeType.DELETE })
//	@Expose
//	/* Stores the links of BaseEntity to another BaseEntity */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    public Set<EntityEntity> links = new HashSet<>();


    public Set<EntityQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<EntityQuestion> questions) {
        this.questions = questions;
    }

    @Transient
    public Set<EntityQuestion> questions = new HashSet<EntityQuestion>(0);

//	@JsonbTransient
//	@OneToMany(fetch = FetchType.LAZY, mappedBy = "pk.source")
//	@Cascade({ CascadeType.MERGE, CascadeType.DELETE })
//	private Set<AnswerLink> answers = new HashSet<AnswerLink>(0);

    /**
     * Constructor.
     *
     * @param none
     */
    @SuppressWarnings("unused")
    public BaseEntity() {

    }

    /**
     * Constructor.
     *
     * @param Name the summary name of the core entity
     */
    public BaseEntity(final String aName) {
        this(BaseEntity.DEFAULT_CODE_PREFIX + UUID.randomUUID().toString(), aName);
    }

    /**
     * Constructor.
     *
     * @param Code the unique code of the core entity
     * @param Name the summary name of the core entity
     */
    public BaseEntity(final String aCode, final String aName) {
        this.code = aCode;
        this.name = aName;

    }
//
//	/**
//	 * @return the answers
//	 */
//	public Set<AnswerLink> getAnswers() {
//		return answers;
//	}
//
//	/**
//	 * @param answers the answers to set
//	 */
//	public void setAnswers(final Set<AnswerLink> answers) {
//		this.answers = answers;
//	}
//
//	/**
//	 * @return the links
//	 */
//	@JsonInclude(JsonInclude.Include.NON_NULL)
//	public Set<EntityEntity> getLinks() {
//		return links;
//	}
//
//	/**
//	 * Sets the Links of the BaseEntity with another BaseEntity
//	 * 
//	 * @param links the links to set
//	 */
//	public void setLinks(final Set<EntityEntity> links) {
//		this.links = links;
//	}
//

    /**
     * containsEntityAttribute This checks if an attribute exists in the baseEntity.
     *
     * @param attributeCode
     * @returns boolean
     */
    public boolean containsEntityAttribute(final String attributeCode) {
        boolean ret = false;

        // Check if this code exists in the baseEntityAttributes
        if (baseEntityAttributes.parallelStream().anyMatch(ti -> ti.attribute.code.equals(attributeCode))) {
            ret = true;
        }
        return ret;
    }
//
//	/**
//	 * containsLink This checks if an attribute link code is linked to the
//	 * baseEntity.
//	 * 
//	 * @param attributeCode
//	 * @returns boolean
//	 */
//	public boolean containsLink(final String linkAttributeCode) {
//		boolean ret = false;
//
//		// Check if this code exists in the baseEntityAttributes
//		if (getLinks().parallelStream().anyMatch(ti -> ti.getPk().getAttribute().getCode().equals(linkAttributeCode))) {
//			ret = true;
//		}
//		return ret;
//	}
//
//	/**
//	 * containsTarget This checks if another baseEntity is linked to the baseEntity.
//	 * 
//	 * @param targetCode
//	 * @param linkAttributeCode
//	 * @returns boolean
//	 */
//	public boolean containsTarget(final String targetCode, final String linkAttributeCode) {
//		boolean ret = false;
//
//		// Check if this code exists in the baseEntityAttributes
//		if (getLinks().parallelStream().anyMatch(ti -> (ti.getLink().getAttributeCode().equals(linkAttributeCode)
//				&& (ti.getLink().getTargetCode().equals(targetCode))))) {
//			ret = true;
//		}
//		return ret;
//	}
//

    /**
     * findEntityAttribute This returns an attributeEntity if it exists in the
     * baseEntity.
     *
     * @param attributeCode
     * @returns Optional<EntityAttribute>
     */
    public Optional<EntityAttribute> findEntityAttribute(final String attributeCode) {

        Optional<EntityAttribute> foundEntity = null;

        try {
            foundEntity = baseEntityAttributes.stream().filter(x -> (x.attribute.code.equals(attributeCode)))
                    .findFirst();
        } catch (Exception e) {
            log.error("Error in fetching attribute value");
        }

//    Optional.of(getBaseEntityAttributes().stream()
//            .filter(x -> (x.getAttribute().getCode().equals(attributeCode))).findFirst().get());

        return foundEntity;
    }

    /**
     * findEntityAttribute This returns an attributeEntity if it exists in the
     * baseEntity. Could be more efficient in retrival (ACC: test)
     *
     * @param attribute
     * @returns EntityAttribute
     */
    public List<EntityAttribute> findPrefixEntityAttributes(final String attributePrefix) {
        List<EntityAttribute> foundEntitys = baseEntityAttributes.stream()
                .filter(x -> (x.attribute.code.startsWith(attributePrefix))).collect(Collectors.toList());

        return foundEntitys;
    }

    /**
     * findEntityAttributes This returns attributeEntitys if it exists in the
     * baseEntity. Could be more efficient in retrival (ACC: test)
     *
     * @param attribute
     * @returns EntityAttribute
     */
    public EntityAttribute findEntityAttribute(final Attribute attribute) {
        final EntityAttribute foundEntity = baseEntityAttributes.stream()
                .filter(x -> (x.attribute.code.equals(attribute.code))).findFirst().get();

        return foundEntity;
    }

    /**
     * addAttribute This adds an attribute with default weight of 0.0 to the
     * baseEntity. It auto creates the EntityAttribute object. For efficiency we
     * assume the attribute does not already exist
     *
     * @param ea
     * @throws BadDataException
     */
    public EntityAttribute addAttribute(final EntityAttribute ea) throws BadDataException {
        if (ea == null)
            throw new BadDataException("missing Attribute");

        return addAttribute(ea.attribute, ea.getWeight(), ea.getValue());
    }

    /**
     * addAttribute This adds an attribute and associated weight to the baseEntity.
     * It auto creates the EntityAttribute object. For efficiency we assume the
     * attribute does not already exist
     *
     * @param attribute
     * @throws BadDataException
     */
    public EntityAttribute addAttribute(final Attribute attribute) throws BadDataException {
        return addAttribute(attribute, 1.0);
    }

    /**
     * addAttribute This adds an attribute and associated weight to the baseEntity.
     * It auto creates the EntityAttribute object. For efficiency we assume the
     * attribute does not already exist
     *
     * @param attribute
     * @param weight
     * @throws BadDataException
     */
    public EntityAttribute addAttribute(final Attribute attribute, final Double weight) throws BadDataException {
        return addAttribute(attribute, weight, null);
    }

    /**
     * addAttribute This adds an attribute and associated weight to the baseEntity.
     * It auto creates the EntityAttribute object. For efficiency we assume the
     * attribute does not already exist
     *
     * @param attribute
     * @param weight
     * @param value     (of type String, LocalDateTime, Long, Integer, Boolean
     * @throws BadDataException
     */
    public EntityAttribute addAttribute(final Attribute attribute, final Double weight, final Object value)
            throws BadDataException {
        if (attribute == null)
            throw new BadDataException("missing Attribute");
        if (weight == null)
            throw new BadDataException("missing weight");

        final EntityAttribute entityAttribute = new EntityAttribute(this, attribute, weight, value);
        Optional<EntityAttribute> existing = findEntityAttribute(attribute.code);
        if (existing.isPresent()) {
            existing.get().setValue(value);
            existing.get().setWeight(weight);
            // removeAttribute(existing.get().getAttributeCode());
        } else {
            baseEntityAttributes.add(entityAttribute);
        }
        return entityAttribute;
    }

    /**
     * addAttributeOmitCheck This adds an attribute and associated weight to the baseEntity.
     * This method will NOT check and update any existing attributes. Use with Caution.
     *
     * @param attribute
     * @param weight
     * @param value     (of type String, LocalDateTime, Long, Integer, Boolean
     * @throws BadDataException
     */
    public EntityAttribute addAttributeOmitCheck(final Attribute attribute, final Double weight, final Object value)
            throws BadDataException {
        if (attribute == null)
            throw new BadDataException("missing Attribute");
        if (weight == null)
            throw new BadDataException("missing weight");

        final EntityAttribute entityAttribute = new EntityAttribute(this, attribute, weight, value);
        baseEntityAttributes.add(entityAttribute);

        return entityAttribute;
    }

    /**
     * removeAttribute This removes an attribute and associated weight from the
     * baseEntity. For efficiency we assume the attribute exists
     *
     * @param attributeCode
     * @param weight
     */
    public Boolean removeAttribute(final String attributeCode) {
        Boolean removed = false;

        Iterator<EntityAttribute> i = this.baseEntityAttributes.iterator();
        while (i.hasNext()) {
            EntityAttribute ea = i.next();
            if (ea.attribute.code.equals(attributeCode)) {
                i.remove();
                removed = true;
                break;
            }
        }

        return removed;
    }

    //
//	/**
//	 * addTarget This links this baseEntity to a target BaseEntity and associated
//	 * weight,value to the baseEntity. It auto creates the EntityEntity object and
//	 * sets itself to be the source. For efficiency we assume the link does not
//	 * already exist
//	 * 
//	 * @param target        Entity
//	 * @param linkAttribute
//	 * @param weight
//	 * @throws BadDataException
//	 */
//	public EntityEntity addTarget(final BaseEntity target, final Attribute linkAttribute, final Double weight)
//			throws BadDataException {
//		return addTarget(target, linkAttribute, weight, null);
//	}
//
//	/**
//	 * addTarget This links this baseEntity to a target BaseEntity and associated
//	 * weight,value to the baseEntity. It auto creates the EntityEntity object and
//	 * sets itself to be the source. For efficiency we assume the link does not
//	 * already exist
//	 * 
//	 * @param target
//	 * @param linkAttribute
//	 * @param weight
//	 * @param value         (of type String, LocalDateTime, Long, Integer, Boolean
//	 * @throws BadDataException
//	 */
//	public EntityEntity addTarget(final BaseEntity target, final Attribute linkAttribute, final Double weight,
//			final Object value) throws BadDataException {
//		if (target == null)
//			throw new BadDataException("missing Target Entity");
//		if (linkAttribute == null)
//			throw new BadDataException("missing Link Attribute");
//		if (weight == null)
//			throw new BadDataException("missing weight");
//
//		final EntityEntity entityEntity = new EntityEntity(this, target, linkAttribute, value, weight);
//		getLinks().add(entityEntity);
//		return entityEntity;
//	}
//
//	/**
//	 * addAnswer This links this baseEntity to a target BaseEntity and associated
//	 * Answer. It auto creates the AnswerLink object and sets itself to be the
//	 * source and assumes itself to be the target. For efficiency we assume the link
//	 * does not already exist and weight = 0
//	 * 
//	 * @param answer
//	 * @throws BadDataException
//	 */
//	public AnswerLink addAnswer(final Answer answer) throws BadDataException {
//		return addAnswer(this, answer, 0.0);
//	}
//
//	/**
//	 * addAnswer This links this baseEntity to a target BaseEntity and associated
//	 * Answer. It auto creates the AnswerLink object and sets itself to be the
//	 * source and assumes itself to be the target. For efficiency we assume the link
//	 * does not already exist
//	 * 
//	 * @param answer
//	 * @param weight
//	 * @throws BadDataException
//	 */
//	public AnswerLink addAnswer(final Answer answer, final Double weight) throws BadDataException {
//		return addAnswer(this, answer, weight);
//	}
//
//	/**
//	 * addAnswer This links this baseEntity to a target BaseEntity and associated
//	 * Answer. It auto creates the AnswerLink object and sets itself to be the
//	 * source. For efficiency we assume the link does not already exist
//	 * 
//	 * @param target
//	 * @param answer
//	 * @param weight
//	 * @throws BadDataException
//	 */
//	public AnswerLink addAnswer(final BaseEntity source, final Answer answer, final Double weight)
//			throws BadDataException {
//		if (source == null)
//			throw new BadDataException("missing Target Entity");
//		if (answer == null)
//			throw new BadDataException("missing Answer");
//		if (weight == null)
//			throw new BadDataException("missing weight");
//
//		final AnswerLink answerLink = new AnswerLink(source, this, answer, weight);
//		if (answer.getAskId() != null) {
//			answerLink.setAskId(answer.getAskId());
//		}
//		// Set<AnswerLink> answerLinkSet = new HashSet<AnswerLink>();
//		// answerLinkSet.addAll(answer.getAsk().getAnswerList().getAnswerList());
//		// getAnswers().add(answerLink);
//
//		// Update the EntityAttribute
//		Optional<EntityAttribute> ea = findEntityAttribute(answer.getAttributeCode());
//		if (ea.isPresent()) {
//			// modify
//			ea.get().setValue(answerLink.getValue());
//			ea.get().setInferred(answer.getInferred());
//			ea.get().setWeight(answer.getWeight());
//		} else {
//			EntityAttribute newEA = new EntityAttribute(this, answerLink.getAttribute(), weight, answerLink.getValue());
//			newEA.setInferred(answerLink.getInferred());
//			this.baseEntityAttributes.add(newEA);
//		}
//
//		return answerLink;
//		// update attributes!
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see java.lang.Object#toString()
//	 */
////  @Override
////  public String toString() {
////    return "BE:" + ":" + super.toString() + " EAs:"
////        + baseEntityAttributes;
////  }
//
    @Transient
    @JsonbTransient
    public Set<EntityAttribute> merge(final BaseEntity entity) {
        final Set<EntityAttribute> changes = new HashSet<EntityAttribute>();

        // go through the attributes in the entity and check if already existing , if so
        // then check the
        // value and override, else add new attribute

        for (final EntityAttribute ea : entity.baseEntityAttributes) {
            final Attribute attribute = ea.attribute;
            if (this.containsEntityAttribute(attribute.code)) {
                // check for update value
                final Object oldValue = this.getValue(attribute);
                final Object newValue = this.getValue(ea);
                if (newValue != null) {
                    if (!newValue.equals(oldValue)) {
                        // override the old value // TODO allow versioning
                        try {
                            this.setValue(attribute, this.getValue(ea), ea.getValueDouble());
                        } catch (BadDataException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                // add this new entityAttribute
                try {
                    addAttribute(ea);
                    changes.add(ea);
                } catch (final BadDataException e) {
                    // TODO - log error and continue
                }
            }
        }

        return changes;
    }

    @JsonbTransient
    @Transient
    private <T> T getValue(final Attribute attribute) {
        // TODO Dumb find for attribute. needs a hashMap

        for (final EntityAttribute ea : this.baseEntityAttributes) {
            if (ea.attribute.code.equalsIgnoreCase(attribute.code)) {
                return getValue(ea);
            }
        }
        return null;
    }

    @JsonbTransient
    @Transient
    private <T> T getValue(final EntityAttribute ea) {
        return ea.getValue();

    }

    @JsonbTransient
    @Transient
    public <T> Optional<T> getValue(final String attributeCode) {
        Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);

        Optional<T> result = Optional.empty();
        if (ea.isPresent()) {
            if (ea.get() != null) {
                if (ea.get().getValue() != null) {
                    result = Optional.of(ea.get().getValue());
                }
            }
        }
        return result;

    }

    @JsonbTransient
    @Transient
    public <T> Optional<T> getLoopValue(final String attributeCode) {
        Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);

        Optional<T> result = Optional.empty();
        if (ea.isPresent()) {
            result = Optional.of(ea.get().getLoopValue());
        }
        return result;

    }

    @JsonbTransient
    @Transient
    public String getValueAsString(final String attributeCode) {
        Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);
        String result = null;
        if (ea.isPresent()) {
            if (ea.get() != null) {
                if (ea.get().getValue() != null) {
                    result = ea.get().getAsString();
                }
            }
        }
        return result;

    }

    @JsonbTransient
    @Transient
    public <T> T getValue(final String attributeCode, T defaultValue) {
        Optional<T> result = getValue(attributeCode);
        if (result.isPresent()) {
            if (!result.equals(Optional.empty())) {
                return result.get();
            }
        }
        return defaultValue;
    }

    @JsonbTransient
    @Transient
    public <T> T getLoopValue(final String attributeCode, T defaultValue) {
        Optional<T> result = getLoopValue(attributeCode);
        if (result.isPresent()) {
            if (!result.equals(Optional.empty())) {
                return result.get();
            }
        }
        return defaultValue;
    }

    @JsonbTransient
    @Transient
    public Boolean is(final String attributeCode) {
        Optional<EntityAttribute> ea = this.findEntityAttribute(attributeCode);
        Boolean result = false;

        if (ea.isPresent()) {
            result = ea.get().getValueBoolean();
            if (result == null) {
                return false;
            }
        }
        return result;

    }

    @JsonbTransient
    @Transient
    public <T> Optional<T> setValue(final Attribute attribute, T value, Double weight) throws BadDataException {
        Optional<EntityAttribute> oldValue = this.findEntityAttribute(attribute.code);

        Optional<T> result = Optional.empty();
        if (oldValue.isPresent()) {
            if (oldValue.get().getLoopValue() != null) {
                result = Optional.of(oldValue.get().getLoopValue());
            }
            EntityAttribute ea = oldValue.get();
            ea.setValue(value);
            ea.setWeight(weight);
        } else {
            this.addAttribute(attribute, weight, value);
        }
        return result;
    }

    @JsonbTransient
    @Transient
    public <T> Optional<T> setValue(final Attribute attribute, T value) throws BadDataException {
        return setValue(attribute, value, 0.0);
    }

    @JsonbTransient
    @Transient
    public <T> Optional<T> setValue(final String attributeCode, T value) throws BadDataException {
        return setValue(attributeCode, value, 0.0);
    }

    @JsonbTransient
    @Transient
    public <T> Optional<T> setValue(final String attributeCode, T value, Double weight) throws BadDataException {
        Optional<EntityAttribute> oldValue = this.findEntityAttribute(attributeCode);

        Optional<T> result = Optional.empty();
        if (oldValue.isPresent()) {
            if (oldValue.get().getLoopValue() != null) {
                result = Optional.of(oldValue.get().getLoopValue());
            }
            EntityAttribute ea = oldValue.get();
            ea.setValue(value);
            ea.setWeight(weight);
        }
        return result;
    }


    @JsonbTransient
    @Transient
    public void setPrivate(final Attribute attribute, final Boolean state) {
        Optional<EntityAttribute> optEa = this.findEntityAttribute(attribute.code);
        if (optEa.isPresent()) {
            EntityAttribute ea = optEa.get();
            ea.privacyFlag = state;
        }
    }


    @Transient
    @JsonbTransient
    public void setInferred(final Attribute attribute, final Boolean state) {
        Optional<EntityAttribute> optEa = this.findEntityAttribute(attribute.code);
        if (optEa.isPresent()) {
            EntityAttribute ea = optEa.get();
            ea.inferred = state;
        }
    }


    @Transient
    @JsonbTransient
    public void setReadonly(final Attribute attribute, final Boolean state) {
        Optional<EntityAttribute> optEa = this.findEntityAttribute(attribute.code);
        if (optEa.isPresent()) {
            EntityAttribute ea = optEa.get();
            ea.readonly = state;
        }
    }


    public AnswerLink addAnswer(final Answer answer) throws BadDataException {
        return addAnswer(this, answer, 0.0);
    }

    /**
     * addAnswer This links this baseEntity to a target BaseEntity and associated
     * Answer. It auto creates the AnswerLink object and sets itself to be the
     * source and assumes itself to be the target. For efficiency we assume the link
     * does not already exist
     *
     * @param answer
     * @param weight
     * @throws BadDataException
     */
    public AnswerLink addAnswer(final Answer answer, final Double weight) throws BadDataException {
        return addAnswer(this, answer, weight);
    }

    /**
     * addAnswer This links this baseEntity to a target BaseEntity and associated
     * Answer. It auto creates the AnswerLink object and sets itself to be the
     * source. For efficiency we assume the link does not already exist
     *
     * @param target
     * @param answer
     * @param weight
     * @throws BadDataException
     */
    public AnswerLink addAnswer(final BaseEntity source, final Answer answer, final Double weight)
            throws BadDataException {
        if (source == null)
            throw new BadDataException("missing Target Entity");
        if (answer == null)
            throw new BadDataException("missing Answer");
        if (weight == null)
            throw new BadDataException("missing weight");

        final AnswerLink answerLink = new AnswerLink(source, this, answer, weight);
        if (answer.getAskId() != null) {
            answerLink.setAskId(answer.getAskId());
        }
        // Set<AnswerLink> answerLinkSet = new HashSet<AnswerLink>();
        // answerLinkSet.addAll(answer.getAsk().getAnswerList().getAnswerList());
        // getAnswers().add(answerLink);

        // Update the EntityAttribute
        Optional<EntityAttribute> ea = findEntityAttribute(answer.getAttributeCode());
        if (ea.isPresent()) {
            // modify
            ea.get().setValue(answerLink.getValue());
            ea.get().setInferred(answer.getInferred());
            ea.get().setWeight(answer.getWeight());
        } else {
            EntityAttribute newEA = new EntityAttribute(this, answerLink.getAttribute(), weight, answerLink.getValue());
            newEA.setInferred(answerLink.getInferred());
            this.baseEntityAttributes.add(newEA);
        }

        return answerLink;
        // update attributes!
    }

    public EntityEntity addTarget(final BaseEntity target, final Attribute linkAttribute, final Double weight)
            throws BadDataException {
        return addTarget(target, linkAttribute, weight, null);
    }

    public EntityEntity addTarget(final BaseEntity target, final Attribute linkAttribute, final Double weight,
                                  final Object value) throws BadDataException {
        if (target == null)
            throw new BadDataException("missing Target Entity");
        if (linkAttribute == null)
            throw new BadDataException("missing Link Attribute");
        if (weight == null)
            throw new BadDataException("missing weight");

        final EntityEntity entityEntity = new EntityEntity(this, target, linkAttribute, value, weight);
        getLinks().add(entityEntity);
        return entityEntity;
    }

    @Transient
    @Expose
    private Integer index;

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

    public Boolean getFromCache() {
        return fromCache;
    }

    @XmlTransient
    @Transient
    private Boolean fromCache = false;

    /**
     * @param fromCache the fromCache to set
     */
    public void setFromCache(Boolean fromCache) {
        this.fromCache = fromCache;
    }

}
