/*
 * (C) Copyright 2017 GADA Technology (http://www.outcome-hub.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Adam Crow
 *     Byron Aguirre
 */

package life.genny.qwanda.attribute;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwanda.adapter.LocalDateTimeAdapter;
import life.genny.qwanda.datatype.DataType;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jboss.logging.Logger;

import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Attribute represents a distinct abstract Fact about a target entity
 * managed in the Qwanda library.
 * An attribute may be used directly in processing meaning for a target
 * entity. Such processing may be in relation to a comparison score against
 * another target entity, or to generate more attribute information via
 * inference and induction  This
 * attribute information includes:
 * <ul>
 * <li>The Human Readable name for this attibute (used for summary lists)
 * <li>The unique code for the attribute
 * <li>The description of the attribute
 * <li>The answerType that represents the format of the attribute
 * </ul>
 * <p>
 * Attributes represent facts about a target.
 * <p>
 *
 * @author Adam Crow
 * @author Byron Aguirre
 * @version %I%, %G%
 * @since 1.0
 */


@Entity
@Table(name = "qattribute",
        indexes = {
                @Index(columnList = "code", name = "code_idx"),
                @Index(columnList = "realm", name = "code_idx")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"code", "realm"}))
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@RegisterForReflection
public class Attribute extends PanacheEntity {

    private static final Logger log = Logger.getLogger(Attribute.class);

    private static final String DEFAULT_CODE_PREFIX = "PRI_";
    private static final String REGEX_CODE = "[A-Z]{3}\\_[A-Z0-9\\.\\-\\@\\_]*";

    private static final String REGEX_NAME = "[\\pL0-9/\\:\\ \\_\\.\\,\\?\\>\\<\\%\\$\\&\\!\\*";
    private static final String REGEX_REALM = "[a-zA-Z0-9]+";
    private static final String DEFAULT_REALM = "genny";

    /**
     *
     */

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

    @NotNull
    @Size(max = 128)
    @Pattern(regexp = REGEX_NAME, message = "Must contain valid characters for name")
    @Column(name = "name", updatable = true, nullable = true)
    public String name;


    @JsonbTypeAdapter(LocalDateTimeAdapter.class)
    public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));

    @JsonbTypeAdapter(LocalDateTimeAdapter.class)
    public LocalDateTime updated;


    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @Embedded
    @NotNull
    public DataType dataType;

    public Boolean defaultPrivacyFlag = false;

    public String description;

    public String help;

    public String placeholder;

    public String defaultValue;


    /**
     * Constructor.
     */
    @SuppressWarnings("unused")
    protected Attribute() {
    }


    public Attribute(String code, String name, DataType dataType) {
        this.code = code;
        this.name = name;
        this.dataType = dataType;
        this.placeholder = name;
    }


    /**
     * getDefaultCodePrefix This method is overrides the Base class
     *
     * @return the default Code prefix for this class.
     */
    static public String getDefaultCodePrefix() {
        return DEFAULT_CODE_PREFIX;
    }


    @Override
    public String toString() {
        return code + ",dataType=" + dataType;
    }


}