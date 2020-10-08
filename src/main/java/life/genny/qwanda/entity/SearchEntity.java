package life.genny.qwanda.entity;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import life.genny.qwanda.attribute.AttributeBoolean;
import life.genny.qwanda.attribute.AttributeDateTime;
import life.genny.qwanda.attribute.AttributeDouble;
import life.genny.qwanda.attribute.AttributeInteger;
import life.genny.qwanda.attribute.AttributeLong;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.exception.BadDataException;

import life.genny.qwanda.qwandautils.QwandaJsonUtils;
import org.apache.logging.log4j.Logger;

/* SearchEntity class implements the search of base entities applying different filters/search to the
 * baseEntity and its attributes
 */
public class SearchEntity extends BaseEntity {
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    private static final long serialVersionUID = 1L;

    Double colIndex = 1.0;
    Double sortIndex = 1.0;
    Double actionIndex = 1.0;
    Double groupIndex = 1.0;

    /*
     * This Sort Enum is used to sort the search results in either Ascending and
     * descending order
     */
    public enum Sort {
        ASC {
            public String toString() {
                return "ASC";
            }
        },
        DESC {
            public String toString() {
                return "DESC";
            }
        }
    }

    /*
     * This StringFilter Enum is used to put the filter to the search entity. It
     * filters the string values in the attributes
     */
    public enum StringFilter {
        EQUAL {
            public String toString() {
                return SearchEntity.convertToSaveable("=");
            }
        },
        LIKE {
            public String toString() {
                return "LIKE";
            }
        }
    }

    /*
     * This Filter Enum is used to put the filter to the search entity. It filtesr
     * the numeric and bit masked values of the attributes
     */
    public enum Filter {
        EQUALS {
            public String toString() {
                return SearchEntity.convertToSaveable("=");
            }
        },
        GREATER_THAN {
            public String toString() {
                return SearchEntity.convertToSaveable(">");
            }
        },
        GREATER_THAN_AND_EQUAL {
            public String toString() {
                return SearchEntity.convertToSaveable(">=");
            }
        },
        LESS_THAN_AND_EQUAL {
            public String toString() {
                return SearchEntity.convertToSaveable("<=");
            }
        },
        LESS_THAN {
            public String toString() {
                return SearchEntity.convertToSaveable("<");
            }
        },
        BIT_MASK_POSITIVE {
            public String toString() {
                return SearchEntity.convertToSaveable("&+");
            }
        },
        BIT_MASK_ZERO {
            public String toString() {
                return SearchEntity.convertToSaveable("&0");
            }
        }
    }

    public void convertBEToSaveable() {
        for (EntityAttribute ea : this.getBaseEntityAttributes()) {
            if (!((ea.getAttributeCode().startsWith("COL_")) || (ea.getAttributeCode().startsWith("CAL_"))
                    || (ea.getAttributeCode().startsWith("SCH_")))) {
                String name = ea.getAttributeName();
                name = name.replaceAll(">", "_GT_");
                name = name.replaceAll("<", "_LT_");
                name = name.replaceAll(">=", "_GTE_");
                name = name.replaceAll("<=", "_LTE_");
                name = name.replaceAll("=", "_EQ_");
                name = name.replaceAll("&", "_AMP_");
                name = name.replaceAll("+", "_PLUS_");
                name = name.replaceAll("-", "_MINUS_");
                name = name.replaceAll("&+", "_BPLUS_");
                name = name.replaceAll("&0", "_BZERO_");
                ea.setAttributeName(name);
            }
        }
    }

    public void convertBEFromSaveable() {
        for (EntityAttribute ea : this.getBaseEntityAttributes()) {
            if (!((ea.getAttributeCode().startsWith("COL_")) || (ea.getAttributeCode().startsWith("CAL_"))
                    || (ea.getAttributeCode().startsWith("SCH_")))) {
                String name = ea.getAttributeName();
                name = name.replaceAll("_GT_", ">");
                name = name.replaceAll("_GTE_", ">=");
                name = name.replaceAll("_LT_", "<");
                name = name.replaceAll("_LTE_", "<=");
                name = name.replaceAll("_EQ_", "=");
                name = name.replaceAll("_AMP_", "&");
                name = name.replaceAll("_PLUS_", "+");
                name = name.replaceAll("_MINUS_", "-");
                name = name.replaceAll("_BPLUS_", "&+");
                name = name.replaceAll("_BZERO_", "&0");
                ea.setAttributeName(name);
            }
        }
    }

    public static String convertToSaveable(String value) {
        String name = value.replaceAll("\\>", "_GT_");
        name = name.replaceAll("\\<", "_LT_");
        name = name.replaceAll("\\=", "_EQ_");
        name = name.replaceAll("\\&", "_AMP_");
        name = name.replaceAll("\\+", "_PLUS_");
        name = name.replaceAll("\\-", "_MINUS_");
        return name;
    }

    public static String convertFromSaveable(String value) {
        if (value != null) {
            String name = value.replaceAll("_GT_", ">");
            name = name.replaceAll("_LT_", "<");
            name = name.replaceAll("_EQ_", "=");
            name = name.replaceAll("_AMP_", "&");
            name = name.replaceAll("_PLUS_", "+");
            name = name.replaceAll("_MINUS_", "-");
            return name;
        } else {
            return null;
        }
    }

    /* Constructor to create SearchEntity with code and name */
    public SearchEntity() {
        this("SBE_" + UUID.randomUUID().toString().substring(0, 15), "Or Search");
    }

    /* Constructor to create SearchEntity with code and name */
    public SearchEntity(final String code, final String name) {
        super(code, name);
        setPageStart(0);
        setPageSize(20);
        setTitle(name);
    }

    /* Constructor to create SearchEntity passing BaseEntity */
    public SearchEntity(final BaseEntity be) {
        super(be.getCode(), be.getName());
        this.setCreated(be.getCreated());
        this.setUpdated(be.getUpdated());
        this.setBaseEntityAttributes(be.getBaseEntityAttributes());
    }

    /*
     * This method allows to add the attributes to the SearchEntity that is required
     * in the result BaseEntities
     */
    public SearchEntity addColumn(final String attributeCode, final String columnName) {
        AttributeText attributeColumn = new AttributeText("COL_" + attributeCode, columnName);
        try {
            EntityAttribute ea = addAttribute(attributeColumn, colIndex);
            ea.setIndex(colIndex.intValue());
            colIndex += 1.0;
        } catch (BadDataException e) {
            log.error("Bad Column Initialisation");
        }
        return this;
    }

    /*
     * This method allows to add the action attributes to the SearchEntity that is required
     * in the result BaseEntities
     */
    public SearchEntity addAction(final String attributeCode, final String columnName) {
        AttributeText attributeColumn = new AttributeText("ACT_" + attributeCode, columnName);
        try {
            EntityAttribute ea = addAttribute(attributeColumn, actionIndex);
            ea.setIndex(actionIndex.intValue());
            actionIndex += 1.0;
        } catch (BadDataException e) {
            log.error("Bad Column Initialisation");
        }
        return this;
    }

    /*
     * This method allows to add the action attributes to the SearchEntity that is required
     * in the result BaseEntities
     */
    public SearchEntity addDefaultAction(final String attributeCode, final String columnName) {
        AttributeText attributeColumn = new AttributeText("ACT_" + attributeCode, columnName);
        try {
            EntityAttribute ea = addAttribute(attributeColumn, 0.0);
            ea.setIndex(0);
        } catch (BadDataException e) {
            log.error("Bad Column Initialisation");
        }
        return this;
    }

    /*
     * This method allows to add the associated attributes to the SearchEntity that
     * is required in the result BaseEntities
     */
    public SearchEntity addAssociatedColumn(final String attributeCode, final String columnName) {
        AttributeText attributeColumn = new AttributeText("CAL_" + attributeCode, columnName);
        try {
            EntityAttribute ea = addAttribute(attributeColumn, colIndex);
            ea.setIndex(colIndex.intValue());
            colIndex += 1.0;
        } catch (BadDataException e) {
            log.error("Bad Associated Column Initialisation");
        }
        return this;
    }

    /*
     * This method allows to add sorting to the attributes of the search results It
     * can either sort in ascending or descending order
     */
    public SearchEntity addSort(final String attributeCode, final String sortHelpText, final Sort sortType) {
        AttributeText attributeSort = new AttributeText("SRT_" + attributeCode, sortHelpText);
        try {
            addAttribute(attributeSort, sortIndex, sortType.toString());
            sortIndex += 1.0;

        } catch (BadDataException e) {
            log.error("Bad Sort Initialisation");
        }

        return this;
    }

    public SearchEntity addSortAttribute(final String attributeCode, final String aName) {
        AttributeText attributeSort = new AttributeText("ATTRSRT_" + attributeCode, aName);
        try {
            addAttribute(attributeSort, 1.0);
        } catch (BadDataException e) {
            log.error("Bad Sort Initialisation");
        }
        return this;
    }

    /*
     * This method allows to add grouping by a specific attribute.
     * all BEs who's value corresponding to this attribute will be grouped together.
     */
    public SearchEntity addGroupBy(final String groupBy) {
        AttributeText attribute = new AttributeText("GPB_" + groupBy, "GroupBy");
        try {
            addAttribute(attribute, groupIndex, groupBy);
            groupIndex += 1.0;

        } catch (BadDataException e) {
            log.error("Bad Group By Initialisation");
        }

        return this;
    }

    /*
     * This method allows to set the filter for the integer value in the search
     *
     * @param attributeCode - the attributeCode which holds integer value where we
     * apply the filter
     *
     * @param filterType - type of the filter
     *
     * @param value - filter against (search for) this value
     */
    public SearchEntity addFilter(final String attributeCode, final Filter filterType, final Integer value) {
        AttributeInteger attribute = new AttributeInteger(attributeCode, filterType.toString());
        try {
            addAttribute(attribute, 1.0, value);
        } catch (BadDataException e) {
            log.error("Bad Integer Filter Initialisation");
        }

        return this;
    }

    /*
     * This method allows to set the filter for the Long value in the search
     *
     * @param attributeCode - the attributeCode which holds long value where we
     * apply the filter
     *
     * @param filterType - type of the filter
     *
     * @param value - filter against (search for) this value
     */
    public SearchEntity addFilter(final String attributeCode, final Filter filterType, final Long value) {
        AttributeLong attribute = new AttributeLong(attributeCode, filterType.toString());
        try {
            addAttribute(attribute, 1.0, value);
        } catch (BadDataException e) {
            log.error("Bad Long Filter Initialisation");
        }

        return this;
    }

    /*
     * This method allows to set the filter for the LocalDateTime value in the
     * search
     *
     * @param attributeCode - the attributeCode which holds LocalDateTime value
     * where we apply the filter
     *
     * @param filterType - type of the filter
     *
     * @param value - filter against (search for) this value
     */
    public SearchEntity addFilter(final String attributeCode, final Filter filterType, final LocalDateTime value) {
        AttributeDateTime attribute = new AttributeDateTime(attributeCode, filterType.toString());
        try {
            addAttribute(attribute, 1.0, value);
        } catch (BadDataException e) {
            log.error("Bad Double Filter Initialisation");
        }

        return this;
    }

    /*
     * This method allows to set the filter for the Boolean value in the search
     *
     * @param attributeCode - the attributeCode which holds Boolean value where we
     * apply the filter
     *
     * @param value - filter against (search for) this value
     */
    public SearchEntity addFilter(final String attributeCode, final Boolean value) {
        AttributeBoolean attribute = new AttributeBoolean(attributeCode, "=");
        try {
            addAttribute(attribute, 1.0, value);
        } catch (BadDataException e) {
            log.error("Bad Double Filter Initialisation");
        }

        return this;
    }

    /*
     * This method allows to set the filter for the String value in the search
     *
     * @param attributeCode - the attributeCode which holds String value where we
     * apply the filter
     *
     * @param filterType - type of the string filter
     *
     * @param value - filter against (search for) this value
     */
    public SearchEntity addFilter(final String attributeCode, final StringFilter filterType, final String value) {
        AttributeText attribute = new AttributeText(attributeCode, filterType.toString());
        try {
            addAttribute(attribute, 1.0, value);
        } catch (BadDataException e) {
            log.error("Bad String Filter Initialisation");
        }

        return this;
    }

    /*
     * This method allows to set a filter without checking if the the attribute already exists. Use Carefully.
     *
     * @param attributeCode - the attributeCode which holds integer value where we
     * apply the filter
     *
     * @param filterType - type of the filter
     *
     * @param value - filter against (search for) this value
     */
    public SearchEntity addFilterOmitCheck(final String attributeCode, final Filter filterType, final Integer value) {
        AttributeInteger attribute = new AttributeInteger(attributeCode, filterType.toString());
        try {
            addAttributeOmitCheck(attribute, 1.0, value);
        } catch (BadDataException e) {
            log.error("Bad Integer Filter Initialisation in Omit Check");
        }

        return this;
    }

    /*
     * This method allows to set the LinkWeight to the resulted BaseEntities to its
     * parent
     *
     * @param value - value/linkWeight to be set
     */
    public SearchEntity setLinkWeight(final Double value) {
        AttributeDouble attribute = new AttributeDouble("SCH_LINK_WEIGHT", "LinkWeight");
        try {
            addAttribute(attribute, 1.0, value);
        } catch (BadDataException e) {
            log.error("Bad String Filter Initialisation");
        }

        return this;
    }

    /*
     * This method allows to set the filter based on the linkweight value of
     * BaseEntities to its parent
     *
     * @param filterType - type of the filter set to the linkWeight
     */
    public SearchEntity addFilterToLinkWeight(final Filter filterType) {
        AttributeText attribute = new AttributeText("SCH_LINK_FILTER", "LinkFilterByWeight");
        try {
            addAttribute(attribute, 1.0, filterType.toString());
        } catch (BadDataException e) {
            log.error("Bad String Filter Initialisation");
        }

        return this;
    }

    /*
     * This method allows to set the title of the results data to be sent
     *
     * @param title - The page Title
     */
    public SearchEntity setTitle(final String title) {
        AttributeText attributeTitle = new AttributeText("SCH_TITLE", "Title");
        try {
            addAttribute(attributeTitle, 5.0, title);
        } catch (BadDataException e) {
            log.error("Bad Title ");
        }

        return this;
    }

    /*
     * This method allows to set the start/begining number of the range(page) of the
     * results data to be sent
     *
     * @param pageStart - start of the page number
     */
    public SearchEntity setPageStart(final Integer pageStart) {
        AttributeInteger attributePageStart = new AttributeInteger("SCH_PAGE_START", "PageStart");
        try {
            addAttribute(attributePageStart, 3.0, pageStart);
        } catch (BadDataException e) {
            log.error("Bad Page Start ");
        }

        return this;
    }

    /*
     * This method allows to set size of the selection allowed for a searchEntity
     *
     * @param pageStart - size of selection
     */
    public SearchEntity setSelectSize(final Integer selectSize) {
        AttributeInteger attributeSelectSize = new AttributeInteger("SCH_SELECT_SIZE", "SelectSize");
        try {
            addAttribute(attributeSelectSize, 1.0, selectSize);
        } catch (BadDataException e) {
            log.error("Bad Page Start ");
        }

        return this;
    }

    /*
     * This method allows to set the total number of the results (BaseEntites) to be
     * sent
     *
     * @param pageSize - total number of data's to be sent
     */
    public SearchEntity setPageSize(final Integer pageSize) {
        AttributeInteger attributePageSize = new AttributeInteger("SCH_PAGE_SIZE", "PageSize");
        try {
            addAttribute(attributePageSize, 3.0, pageSize);
        } catch (BadDataException e) {
            log.error("Bad Page Size");
        }

        return this;
    }

    /*
     * This method allows to set the stakeholder/user code to the search. It will
     * search for the BaseEntites that the given user is stakeholder of.
     *
     * @param stakeholderCode - userCode
     */
    public SearchEntity setStakeholder(final String stakeholderCode) {
        AttributeText attribute = new AttributeText("SCH_STAKEHOLDER_CODE", "Stakeholder");
        try {
            addAttribute(attribute, 1.0, stakeholderCode);
        } catch (BadDataException e) {
            log.error("Bad Stakeholder");
        }

        return this;
    }

    /*
     * This method allows to set the stakeholder/user code to the parent/source
     * Basentity involved in the search. It will search for the BaseEntites under
     * the give source BE that the given user is stakeholder of.
     *
     * @param sourceStakeholderCode - userCode
     */
    public SearchEntity setSourceStakeholder(final String sourceStakeholderCode) {
        AttributeText attribute = new AttributeText("SCH_SOURCE_STAKEHOLDER_CODE", "SourceStakeholder");
        try {
            addAttribute(attribute, 1.0, sourceStakeholderCode);
        } catch (BadDataException e) {
            log.error("Bad Source Stakeholder");
        }

        return this;
    }

    /*
     * This method allows to set the stakeholder/user code to the parent/source
     * Basentity involved in the search. It will search for the BaseEntites under
     * the give source BE that the given user is stakeholder of.
     *
     * @param sourceStakeholderCode - userCode
     */
    public SearchEntity setLinkCode(final String linkCode) {
        AttributeText attribute = new AttributeText("SCH_LINK_CODE", "LinkCode");
        try {
            addAttribute(attribute, 1.0, linkCode);
        } catch (BadDataException e) {
            log.error("Bad Stakeholder");
        }

        return this;
    }

    /*
     * This method allows to set the link value the result of the search.
     *
     * @param linkValue - linkValue of the sourceCode to the results (BaseEntities)
     * of the search
     */
    public SearchEntity setLinkValue(final String linkValue) {
        AttributeText attribute = new AttributeText("SCH_LINK_VALUE", "LinkValue");
        try {
            addAttribute(attribute, 1.0, linkValue);
        } catch (BadDataException e) {
            log.error("Bad Link Value");
        }

        return this;
    }

    public SearchEntity setSourceCode(final String sourceCode) {
        AttributeText attribute = new AttributeText("SCH_SOURCE_CODE", "SourceCode");
        try {
            addAttribute(attribute, 1.0, sourceCode);
        } catch (BadDataException e) {
            log.error("Bad SourceCode");
        }
        return this;
    }

    public SearchEntity setTargetCode(final String targetCode) {
        AttributeText attribute = new AttributeText("SCH_TARGET_CODE", "TargetCode");
        try {
            addAttribute(attribute, 1.0, targetCode);
        } catch (BadDataException e) {
            log.error("Bad Target Code");
        }

        return this;
    }

    public SearchEntity setDisplayMode(final String displayMode) {
        AttributeText attribute = new AttributeText("SCH_DISPLAY_MODE", "DisplayMode");
        try {
            addAttribute(attribute, 1.0, displayMode);
        } catch (BadDataException e) {
            log.error("Bad Display Mode");
        }

        return this;
    }

    /*
     * This method allows to set thewildcard of the results data to be sent
     *
     * @param wildcard - the widlcard
     */
    public SearchEntity setWildcard(String wildcard) {
        // only allow characters
        wildcard = wildcard.replaceAll(("[^A-Za-z0-9 ]"), "");

        AttributeText attributeWildcard = new AttributeText("SCH_WILDCARD", "Wildcard");
        try {
            addAttribute(attributeWildcard, 1.0, wildcard);
        } catch (BadDataException e) {
            log.error("Bad Wildcard ");
        }

        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return QwandaJsonUtils.toJson(this);
    }

    public Integer getPageStart(Integer defaultPageSize) {
        return getValue("SCH_PAGE_START", defaultPageSize);
    }

    public Integer getPageSize(Integer defaultPageSize) {
        return getValue("SCH_PAGE_SIZE", defaultPageSize);
    }

    public boolean hasCode() {
        return code != null && !"".equals(code.trim());
    }


    /**
     * @return the baseEntityAttributes
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<EntityAttribute> getBaseEntityAttributes() {
        return baseEntityAttributes;
    }
}
