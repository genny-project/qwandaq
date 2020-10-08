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

package life.genny.qwanda;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.gson.annotations.Expose;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import life.genny.qwanda.exception.BadDataException;
import org.apache.commons.lang3.builder.CompareToBuilder;

import javax.persistence.*;
import javax.validation.Valid;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ask represents the presentation of a Question to a source entity. A Question
 * object is refered to as a means of requesting information from a source about
 * a target attribute. This ask information includes:
 * <ul>
 * <li>The source of the answer (Who is being asked the question?)
 * <li>The target of the answer (To whom does the answer refer to?)
 * <li>The text that presents the question to the source
 * <li>The context entities that relate to the question
 * <li>The associated Question object
 * <li>The expiry duration that should be required to answer.
 * <li>The media used to ask this question.
 * <li>The associated answers List
 * </ul>
 * <p>
 * Asks represent the major way of retrieving facts (answers) about a target
 * from sources. Each ask is associated with an question which represents one or
 * more distinct fact about a target.
 * <p>
 *
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */

@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
@Table(name = "ask", indexes = {@Index(columnList = "id", name = "code_idx"),
        @Index(columnList = "realm", name = "code_idx")}, uniqueConstraints = @UniqueConstraint(columnNames = {"id",
        "realm"}))

@Entity
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Inheritance(strategy = InheritanceType.JOINED)

public class Ask extends PanacheEntity implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XmlTransient
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id", nullable = false)
    @Expose
    private Question question;

    @Expose
    private String sourceCode;
    @Expose
    private String targetCode;
    @Expose
    private String questionCode;
    @Expose
    private String attributeCode;

    @Expose
    private Boolean mandatory = false;
    @Expose
    private Boolean oneshot = false;

    @Expose
    private Boolean disabled = false;
    @Expose
    private Boolean hidden = false;

    @Expose
    private Boolean readonly = false;
    @Expose
    private Double weight = 0.0;

    @Expose
    private Long parentId = 0L;

    @Expose
    private Boolean formTrigger = false;

    @Expose
    private Boolean createOnTrigger = false;

    @Transient
    @Expose
    private Ask[] childAsks;

    // @Embedded
    // @Valid
    // @JsonInclude(Include.NON_NULL)
    // private AnswerList answerList;

    @Embedded
    @Valid
    @JsonInclude(Include.NON_NULL)
    @Expose
    private ContextList contextList;

    /**
     * Constructor.
     */
    @SuppressWarnings("unused")
    public Ask() {
        // dummy for hibernate
    }

    /**
     * Constructor.
     *
     * @param aQuestion The associated Question
     */
    public Ask(final Question aQuestion) {
        setQuestion(aQuestion);
        contextList = new ContextList(new CopyOnWriteArrayList<Context>());
        this.disabled = false;
        this.hidden = false;
    }

    /**
     * Constructor.
     *
     * @param aAttributeCode The associated Attribute
     * @param aSourceCode    The person answering the question
     * @param aTargetCode    The BaseEntity that the question is about
     */
    public Ask(final String aAttributeCode, final String aSourceCode, final String aTargetCode, final String name) {
        this.sourceCode = aSourceCode;
        this.targetCode = aTargetCode;
        this.attributeCode = aAttributeCode;
        contextList = new ContextList(new CopyOnWriteArrayList<Context>());
        this.disabled = false;
        this.hidden = false;
        this.readonly = false;
    }

    /**
     * Constructor.
     *
     * @param aQuestion   The associated Question
     * @param aSourceCode The person answering the question
     * @param aTargetCode The BaseEntity that the question is about
     */
    public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode) {
        this(aQuestion, aSourceCode, aTargetCode, false);
    }

    /**
     * Constructor.
     *
     * @param aQuestion   The associated Question
     * @param aSourceCode The person answering the question
     * @param aTargetCode The BaseEntity that the question is about
     * @param aMandatory  Is this ask mandatory?
     */
    public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode, final Boolean aMandatory) {
        this(aQuestion, aSourceCode, aTargetCode, aMandatory, 0.0);
    }

    /**
     * Constructor.
     *
     * @param aQuestion   The associated Question
     * @param aSourceCode The person answering the question
     * @param aTargetCode The BaseEntity that the question is about
     * @param aMandatory  Is this ask mandatory?
     * @param weight
     */
    public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode, final Boolean aMandatory,
               final Double weight) {
        this(aQuestion, aSourceCode, aTargetCode, aMandatory, weight, false, false);
    }

    /**
     * Constructor.
     *
     * @param aQuestion   The associated Question
     * @param aSourceCode The person answering the question
     * @param aTargetCode The BaseEntity that the question is about
     * @param aMandatory  Is this ask mandatory?
     * @param weight
     */
    public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode, final Boolean aMandatory,
               final Double weight, final Boolean disabled, final Boolean hidden) {

        this(aQuestion, aSourceCode, aTargetCode, aMandatory, weight, disabled, hidden, false);
    }

    /**
     * Constructor.
     *
     * @param aQuestion   The associated Question
     * @param aSourceCode The person answering the question
     * @param aTargetCode The BaseEntity that the question is about
     * @param aMandatory  Is this ask mandatory?
     * @param weight
     * @param readonly
     */
    public Ask(final Question aQuestion, final String aSourceCode, final String aTargetCode, final Boolean aMandatory,
               final Double weight, final Boolean disabled, final Boolean hidden, final Boolean readonly) {
        setQuestion(aQuestion);
        this.sourceCode = aSourceCode;
        this.targetCode = aTargetCode;
        this.attributeCode = aQuestion.getAttributeCode();
        contextList = new ContextList(new CopyOnWriteArrayList<Context>());
        this.mandatory = aMandatory;
        this.weight = weight;
        this.disabled = disabled;
        this.hidden = hidden;
        this.readonly = readonly;
    }

    /**
     * @return the question
     */
    public Question getQuestion() {
        return question;
    }

    /**
     * @param question the question to set
     */
    public void setQuestion(final Question question) {
        this.question = question;
        this.questionCode = question.getCode();
        this.attributeCode = question.getAttributeCode(); // .getAttribute().getCode();
    }

    /**
     * @return the contextList
     */
    public ContextList getContextList() {
        return contextList;
    }

    /**
     * @param contextList the contextList to set
     */
    public void setContextList(final ContextList contextList) {
        this.contextList = contextList;
    }

    /**
     * @return the mandatory
     */
    public Boolean getMandatory() {
        return mandatory;
    }

    /**
     * @param mandatory the mandatory to set
     */
    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * @return the weight
     */
    public Double getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(Double weight) {
        this.weight = weight;
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
    private void setSourceCode(final String sourceCode) {
        this.sourceCode = sourceCode;
    }

    /**
     * @return the targetCode
     */
    public String getTargetCode() {
        return targetCode;
    }

    /**
     * @return the questionCode
     */
    public String getQuestionCode() {
        return questionCode;
    }

    /**
     * @param questionCode the questionCode to set
     */
    public void setQuestionCode(final String questionCode) {
        this.questionCode = questionCode;
    }

    /**
     * @return the disabled
     */
    public Boolean getDisabled() {
        return disabled;
    }

    /**
     * @param disabled the disabled to set
     */
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * @return the hidden
     */
    public Boolean getHidden() {
        return hidden;
    }

    /**
     * @param hidden the hidden to set
     */
    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * @return the parentId
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * @return the attributeCode
     */
    public String getAttributeCode() {
        return attributeCode;
    }

    /**
     * @param attributeCode the attributeCode to set
     */
    public void setAttributeCode(final String attributeCode) {
        this.attributeCode = attributeCode;
    }

    /**
     * @param targetCode the targetCode to set
     */
    public void setTargetCode(final String targetCode) {
        this.targetCode = targetCode;
    }

    public void add(final Answer answer) throws BadDataException {
        if ((answer.getSourceCode().equals(sourceCode)) && (answer.getTargetCode().equals(targetCode))
                && (answer.getAttributeCode().equals(attributeCode))) {
            // getAnswerList().getAnswerList().add(new AnswerLink(source, target, answer));
        } else {
            throw new BadDataException("Source / Target ids do not match Ask");
        }

    }

    public int compareTo(Object o) {
        Ask myClass = (Ask) o;
        return new CompareToBuilder().append(questionCode, myClass.getQuestionCode())
                .append(targetCode, myClass.getTargetCode()).toComparison();
    }

    /**
     * @return the childAsks
     */
    @Transient
    public Ask[] getChildAsks() {
        return childAsks;
    }

    /**
     * @param childAsks the childAsks to set
     */
    public void setChildAsks(Ask[] childAsks) {
        this.childAsks = childAsks;
    }

    /**
     * @return the oneshot
     */
    public Boolean getOneshot() {
        return oneshot;
    }

    /**
     * @param oneshot the oneshot to set
     */
    public void setOneshot(Boolean oneshot) {
        this.oneshot = oneshot;
    }

    /**
     * @return the readonly
     */
    public Boolean getReadonly() {
        return readonly;
    }

    /**
     * @param readonly the readonly to set
     */
    public void setReadonly(Boolean readonly) {
        this.readonly = readonly;
    }

    /**
     * @return the formTrigger
     */
    public Boolean getFormTrigger() {
        return formTrigger;
    }

    /**
     * @param formTrigger the formTrigger to set
     */
    public void setFormTrigger(Boolean formTrigger) {
        this.formTrigger = formTrigger;
    }


    /**
     * @return the createOnTrigger
     */
    public Boolean getCreateOnTrigger() {
        return createOnTrigger;
    }

    /**
     * @param createOnTrigger the createOnTrigger to set
     */
    public void setCreateOnTrigger(Boolean createOnTrigger) {
        this.createOnTrigger = createOnTrigger;
    }

    @XmlTransient
    @Transient
    public Boolean hasTriggerQuestion() {
        // recurse through the childAsks
        // this is used to tell if intermediate BaseEntity is to be created and then
        // copied in upon a trigger question
        if (this.formTrigger) {
            return true;
        } else {
            if ((this.childAsks != null) && (this.childAsks.length > 0)) {
                for (Ask childAsk : this.childAsks) {
                    return childAsk.hasTriggerQuestion();
                }
            }
        }
        return false;
    }

    public static Ask clone(Ask ask) {
        Ask newAsk = new Ask();
        newAsk.sourceCode = ask.getSourceCode();
        newAsk.targetCode = ask.getTargetCode();
        newAsk.questionCode = ask.getQuestionCode();
        newAsk.question = ask.getQuestion();
        newAsk.attributeCode = ask.getAttributeCode();
        newAsk.mandatory = ask.getMandatory();
        newAsk.oneshot = ask.getOneshot();
        newAsk.disabled = ask.getDisabled();
        newAsk.readonly = ask.getReadonly();
        newAsk.weight = ask.getWeight();
        newAsk.parentId = ask.getParentId();
        newAsk.formTrigger = ask.getFormTrigger();
        newAsk.createOnTrigger = ask.getCreateOnTrigger();
        if (ask.getChildAsks() != null && ask.getChildAsks().length > 0) {
            newAsk.childAsks = ask.getChildAsks();
        }
        if (ask.getContextList() != null) {
            newAsk.contextList = ask.getContextList();
        }
        return newAsk;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
