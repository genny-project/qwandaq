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

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * A Link object
 * is used as a means of storing information from a source to a target attribute. This link
 * information includes:
 * <ul>
 * <li>The SourceCode
 * <li>The TargetCode
 * <li> The LinkCode
 * </ul>
 * <p>
 * Link class represents a simple pojo about baseentity links are stored. 
 * <p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */


@Embeddable
@RegisterForReflection
public class Link implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final Logger log = Logger.getLogger(Link.class);
	
	private static final String RULE_PARENT_OVERRIDE = "PO";
	private static final String RULE_CHILD_OVERRIDE = "CO";	
	private static final String RULE_NO_OVERRIDE = null;
	
  /**
   * 
   */
 
  /**
   * A field that stores the human readable attributecode associated with this link.
   * <p>
   */
  @Column(name = "LINK_CODE", updatable = false, nullable = false, unique = false)
  public String attributeCode;

 
  /**
   * A field that stores the human readable targetcode associated with this link.
   * <p>
   */
  @Column(name = "TARGET_CODE", updatable = false, nullable = false, unique = false)
  public String targetCode;

  /**
   * A field that stores the human readable sourcecode associated with this link.
   * <p>
   */
  @Column(name = "SOURCE_CODE", updatable = false, nullable = false, unique = false)
  public String sourceCode;


  /**
   * A field that stores the human readable link Value associated with this link.
   * <p>
   */
  public String linkValue;
  
  @Column(name = "LINK_WEIGHT", updatable = true, nullable = true, unique = false)
  public Double weight;
  
  @Column(name = "CHILD_COL", updatable = true, nullable = true, unique = false)
  public String childColor;
  @Column(name = "PARENT_COL", updatable = true, nullable = true, unique = false)
  public String parentColor;
  @Column(name = "RULE", updatable = true, nullable = true, unique = false)
  public String rule;

  /**
   * Constructor.
   * 
   * @param none
   */
  @SuppressWarnings("unused")
  public Link() {
    // dummy for hibernate
  }



  /**
   * Constructor.
   * 
   * @param source The source associated with this Answer
   * @param target The target associated with this Answer
   * @param attribute The attribute associated with this Answer
   */
  public Link(final String source, final String target, final String linkCode) {
   this(source, target, linkCode, "LINK");  
}

 
  /**
   * Constructor.
   * 
   * @param source The source associated with this Answer
   * @param target The target associated with this Answer
   * @param attribute The attribute associated with this Answer
   * @param value The associated String value
   */
  public Link(final String source, final String target, final String linkCode, final String linkValue) {
   this(source,target,linkCode,linkValue,0.0);
  }

  /**
   * Constructor.
   * 
   * @param source The source associated with this Answer
   * @param target The target associated with this Answer
   * @param attribute The attribute associated with this Answer
   * @param value The associated String value
   */
  public Link(final String source, final String target, final String linkCode, final String linkValue, final Double weight) {
    this.sourceCode = source;
    this.targetCode = target;
    this.attributeCode = linkCode;
    this.linkValue = linkValue;
    this.weight = weight;
    this.rule = Link.RULE_NO_OVERRIDE;
  }


}