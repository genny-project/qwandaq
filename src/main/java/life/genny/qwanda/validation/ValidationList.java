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

package life.genny.qwanda.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jboss.logging.Logger;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * ValidationList represents a set of Validations the Qwanda library. The validations in the list
 * can be applied to a passed value.
 * <ul>
 * <li>List of Validation
 * </ul>
 * <p>
 * 
 * <p>
 * 
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */

@Embeddable
public class ValidationList implements Serializable {

	private static final Logger log = Logger.getLogger(ValidationList.class);

 
  /**
   * A fieldlist that stores the validations for this object.
   * <p>
   */

  @JsonIgnore
  @XmlTransient
  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "dtype_id", referencedColumnName = "id")
  public List<Validation> validationList = new CopyOnWriteArrayList<Validation>();

  public ValidationList() {

  }

  /**
   * Constructor.
   * 
   * @param none
   */

  public ValidationList(final List<Validation> validations) {
    this.validationList = validations;
  }



  /**
   * @return the validationList
   */
  public List<Validation> getValidationList() {
    return validationList;
  }



  /**
   * @param validationList the validationList to set
   */
  public void setValidationList(final List<Validation> validationList) {
    this.validationList = validationList;
  }



}
