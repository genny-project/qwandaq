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
 * Contributors:
 * 	Adam Crow 
 *	Byron Aguirre
 *	Jasper Robison
 */

package life.genny.qwandaq.datatype;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.CurrencyUnit;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlTransient;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import life.genny.qwandaq.converter.ValidationListConverter;
import life.genny.qwandaq.validation.Validation;
import life.genny.qwandaq.validation.ValidationList;

import org.javamoney.moneta.Money;



/**
 * DataType represents a distinct abstract Data Representation in the Qwanda
 * library. The data types express the format and the validations required for
 * values collected. In addition to the extended CoreEntity this information
 * includes:
 * <ul>
 * <li>The code type of the base data e.g. Text, Integer, etc.
 * <li>The List of default Validation items
 * <li>The default mask used for data entry
 * </ul>
 * <p>
 * 
 * <p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @author Jasper Robison
 * @version %I%, %G%
 * @since 1.0
 */

@Embeddable
@RegisterForReflection
public class DataType implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final Logger log = Logger.getLogger(DataType.class);

	public static final String DTT_LINK = "LNK_ATTRIBUTE"; // This datatype classname indicates the datatype belongs to
															// the BaseEntity set with parent
	@NotNull
	@Size(max = 120)
	private String dttCode; // e.g. java.util.String

	@NotNull
	@Size(max = 120)
	private String className; // e.g. java.util.String

	@NotNull
	@Size(max = 120)
	// @JsonIgnore
	private String typeName; // e.g. TEXT

	private String inputmask;

	
	/** 
	 * @return String
	 */
	public String getComponent() {
		return component;
	}

	
	/** 
	 * @param component
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	private String component;


	/**
	 * A fieldlist that stores the validations for this object.
	 * <p>
	 * Note that this is stored into a single object
	 */

	@Column(name = "validation_list", length = 512)
	@Convert(converter = ValidationListConverter.class)
	private List<Validation> validationList = new CopyOnWriteArrayList<Validation>();

	/**
	 * Constructor.
	 * 
	 * @param none
	 */
	@SuppressWarnings("unused")
	public DataType() {
		super();
		// dummy for hibernate
	}

	public DataType(final Class clazz) {
		this(clazz, new ValidationList());
	}

	public DataType(final String className) {
		this(className, new ValidationList());
	}

	public DataType(final String className, final ValidationList aValidationList, final String name,
			final String inputmask) {
        setDttCodeFromClassName(className);
		setClassName(className);
		setValidationList(aValidationList.getValidationList());
		setTypeName(name);
		setInputmask(inputmask);
	}

	public DataType(final String className, final ValidationList aValidationList, final String name,
					final String inputmask, final  String component) {
		setDttCodeFromClassName(className);
		setClassName(className);
		setValidationList(aValidationList.getValidationList());
		setTypeName(name);
		setInputmask(inputmask);
		setComponent(component);
	}

	public DataType(final String className, final ValidationList aValidationList, final String name) {
		this(className, aValidationList, name, "");
	}

    
	/** 
	 * @param str
	 */
	public void setDttCodeFromClassName(String str){
		String[] strs = str.split("\\.");
		String type;

		if (strs.length > 1){
			type = strs[strs.length-1];
		} else {
			type = strs[0];
		}
		if (str.contains("DTT")) {
			setDttCode(str);
		}else {
			setDttCode("DTT_" + type.toUpperCase());
		}
	}

	public DataType(final String className, final ValidationList aValidationList) {
		this(className, aValidationList, className);
	}

	public DataType(final Class clazz, final ValidationList aValidationList) {
		this(clazz.getCanonicalName(), aValidationList);
	}

	/**
	 * @return the validationList
	 */
	public List<Validation> getValidationList() {
		return validationList;
	}

	/**
	 * @param validationList
	 *            the validationList to set
	 */
	public void setValidationList(final List<Validation> validationList) {
		this.validationList = validationList;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @param className
	 *            the className to set
	 */
	public void setClassName(final String className) {
		this.className = className;
	}

	/**
	 * @return the name
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setTypeName(String name) {
		this.typeName = name;
	}

	/**
	 * @return the name
	 */
	public String getDttCode() {
		return this.dttCode;
	}

	/**
	 * @param code
	 *            the name to set
	 */
	public void setDttCode(String code) {
		this.dttCode = code;
	}
	/**
	 * @return the inputmask
	 */
	public String getInputmask() {
		return inputmask;
	}

	/**
	 * @param inputmask
	 *            the inputmask to set
	 */
	public void setInputmask(String inputmask) {
		this.inputmask = inputmask;
	}

	
	/** 
	 * @param clazz
	 */
	@JsonIgnore
	@Transient
	@XmlTransient
	public void setClass(final Class clazz) {
		final String simpleClassName = clazz.getCanonicalName();
		setClassName(simpleClassName);
	}

	
	/** 
	 * @return String
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DataType(" + className + " component: "+this.component+", inputmask:"+this.inputmask+")";
	}

	
	/** 
	 * @param className
	 * @return DataType
	 */
	static public DataType getInstance(final String className) {
		final List<Validation> validationList = new CopyOnWriteArrayList<Validation>();
		ValidationList vlist = new ValidationList(validationList);
		DataType dataTypeInstance = new DataType(className, vlist);
		return dataTypeInstance;
	}

	
	/** 
	 * @param dtype
	 * @return boolean
	 */
	// Is DataType summable?

	static public boolean summable(DataType dtype) {
		switch (dtype.getClassName()) {
		case "java.lang.Integer":
		case "Integer":
		case "java.lang.Long":
		case "Long":
		case "java.lang.Double":
		case "Double":
		case "org.javamoney.moneta.Money":
		case "Money":
			return true;
		default:
			return false;
		}
	}

	
	/** 
	 * @param dtype
	 * @return Object
	 */
	static public Object Zero(DataType dtype) {
		switch (dtype.getClassName()) {
		case "java.lang.Integer":
		case "Integer":
			return new Integer(0);
		case "java.lang.Long":
		case "Long":
			return new Long(0);
		case "java.lang.Double":
		case "Double":
			return new Double(0.0);
		case "javax.money.CurrencyUnit":
		case "org.javamoney.moneta.Money":
		case "Money":
			return Money.zero(Monetary.getCurrency("AUD"));
		default:
			return null;
		}
	}

	
	/** 
	 * @param dtype
	 * @param v1
	 * @param v2
	 * @return Object
	 */
	static public Object add(DataType dtype, Object v1, Object v2) {
		switch (dtype.getClassName()) {
		case "java.lang.Integer":
		case "Integer":
			return ((Integer)v1) + ((Integer)v2);
		case "java.lang.Long":
		case "Long":
			return ((Long)v1) + ((Long)v2);
		case "java.lang.Double":
		case "Double":
			return ((Double)v1) + ((Double)v2);
		case "org.javamoney.moneta.Money":
		case "Money":
			Money m1 = (Money)v1;
			Money m2 = (Money)v2;
			Money sum = m1.add(m2);
			return sum;
		default:
			return null;
		}
	}

}
