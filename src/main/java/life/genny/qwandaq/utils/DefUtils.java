package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.exception.DebugException;
import life.genny.qwandaq.models.ANSIColour;

@RegisterForReflection
public class DefUtils {

	private static final Logger log = Logger.getLogger(DefUtils.class);

	public Map<String, Map<String, BaseEntity>> defs = new ConcurrentHashMap<>();

	public Jsonb jsonb = JsonbBuilder.create();

	private BaseEntityUtils beUtils;
	private QwandaUtils qwandaUtils;

	public DefUtils() {}

	public DefUtils(BaseEntityUtils beUtils, QwandaUtils qwandaUtils) {
		this.beUtils = beUtils;
		this.qwandaUtils = qwandaUtils;
	}

	public void initializeDefs() {

		GennyToken gennyToken = this.beUtils.getGennyToken();
		String realm = this.beUtils.getGennyToken().getRealm();

		SearchEntity searchBE = new SearchEntity("SBE_DEF", "DEF check")
			.addSort("PRI_NAME", "Created", SearchEntity.Sort.ASC)
			.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "DEF_%")
			.addColumn("PRI_CODE", "Name")
			.setPageStart(0)
			.setPageSize(1000);

		searchBE.setRealm(realm);

		List<BaseEntity> items = this.beUtils.getBaseEntitys(searchBE);

		this.defs.put(realm, new ConcurrentHashMap<String, BaseEntity>());

		for (BaseEntity item : items) {

			// If the item is a def appointment, then add a default datetime for the start (Mandatory)
			if (item.getCode().equals("DEF_APPOINTMENT")) {

				Attribute attribute = new AttributeText("DFT_PRI_START_DATETIME", "Default Start Time");
				attribute.setRealm(realm);
				EntityAttribute newEA = new EntityAttribute(item, attribute, 1.0, "2021-07-28 00:00:00");

				try {
					item.addAttribute(newEA);
				} catch (BadDataException e) {
					e.printStackTrace();
				}

				Optional<EntityAttribute> ea = item.findEntityAttribute("ATT_PRI_START_DATETIME");
				if (ea.isPresent()) {
					ea.get().setValue(true);
				}
			}

			item.setFastAttributes(true);
			defs.get(realm).put(item.getCode(), item);
			log.info("Saving ("+realm+") DEF "+item.getCode());
		}
	}

	public Map<String, BaseEntity> getDefMap(final GennyToken userToken) {
		return getDefMap(userToken.getRealm());
	}
		
	public Map<String, BaseEntity> getDefMap(final String realm) {
		if ((defs == null) || (defs.isEmpty())) {
			initializeDefs();
			return defs.get(realm);
		}
		return defs.get(realm);
	}
	
	public SearchEntity mergeFilterValueVariables(SearchEntity searchBE, Map<String, Object> ctxMap) {

		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			// Iterate all Filters
			if (ea.getAttributeCode().startsWith("PRI_") || ea.getAttributeCode().startsWith("LNK_")) {

				// Grab the Attribute for this Code, using array in case this is an associated filter
				String[] attributeCodeArray = ea.getAttributeCode().split("\\.");
				String attributeCode = attributeCodeArray[attributeCodeArray.length-1];
				// Fetch the corresponding attribute
				Attribute att = this.qwandaUtils.getAttribute(attributeCode);
				DataType dataType = att.getDataType();

				Object attributeFilterValue = ea.getValue();
				if (attributeFilterValue != null) {
					// Ensure EntityAttribute Dataype is Correct for Filter
					Attribute searchAtt = new Attribute(ea.getAttributeCode(), ea.getAttributeName(), dataType);
					ea.setAttribute(searchAtt);
					String attrValStr = attributeFilterValue.toString();

					// First Check if Merge is required
					Boolean requiresMerging = MergeUtils.requiresMerging(attrValStr);

					if (requiresMerging != null && requiresMerging) {
						// update Map with latest baseentity
						ctxMap.keySet().forEach(key -> {
							Object value = ctxMap.get(key);
							if (value.getClass().equals(BaseEntity.class)) {
								BaseEntity baseEntity = (BaseEntity) value;
								ctxMap.put(key, this.beUtils.getBaseEntityByCode(baseEntity.getCode()));
							}
						});
						// Check if contexts are present
						if (MergeUtils.contextsArePresent(attrValStr, ctxMap)) {
							// NOTE: HACK, mergeUtils should be taking care of this bracket replacement - Jasper (6/08/2021)
							Object mergedObj = MergeUtils.wordMerge(attrValStr.replace("[[", "").replace("]]", ""), ctxMap);
							// Ensure Dataype is Correct, then set Value
							ea.setValue(mergedObj);
						} else {
							log.error(ANSIColour.RED + "Not all contexts are present for " + attrValStr + ANSIColour.RESET);
							return null;
						}
					} else {
						// This should filter out any values of incorrect datatype
						ea.setValue(attributeFilterValue);
					}
				} else {
					log.error(ANSIColour.RED + "Value is NULL for entity attribute " + attributeCode + ANSIColour.RESET);
					return null;
				}
			}
		}

		return searchBE;
	}
		
	public BaseEntity getDEF(final BaseEntity be) {

		GennyToken gennyToken = this.beUtils.getGennyToken();
		String realm = gennyToken.getRealm();

		if (be == null) {
			log.error("be param is NULL");
			try {
				throw new DebugException("BaseEntityUtils: getDEF: The passed BaseEntity is NULL, supplying trace");
			} catch (DebugException e) {
				e.printStackTrace();
			}
			return null;
		}

		if (be.getCode().startsWith("DEF_")) {
			return be;
		}
		// Some quick ones
		if (be.getCode().startsWith("PRJ_")) {
			BaseEntity defBe = getDefMap(realm).get("DEF_PROJECT");
			return defBe;
		}

		List<EntityAttribute> isAs = be.findPrefixEntityAttributes("PRI_IS_");

		// remove the non DEF ones
		/*
		 * PRI_IS_DELETED PRI_IS_EXPANDABLE PRI_IS_FULL PRI_IS_INHERITABLE PRI_IS_PHONE
		 * (?) PRI_IS_SKILLS
		 */
		Iterator<EntityAttribute> i = isAs.iterator();
		while (i.hasNext()) {
			EntityAttribute ea = i.next();

			if (ea.getAttributeCode().startsWith("PRI_IS_APPLIED_")) {

				i.remove();
			} else {
				switch (ea.getAttributeCode()) {
					case "PRI_IS_DELETED":
					case "PRI_IS_EXPANDABLE":
					case "PRI_IS_FULL":
					case "PRI_IS_INHERITABLE":
					case "PRI_IS_PHONE":
					case "PRI_IS_AGENT_PROFILE_GRP":
					case "PRI_IS_BUYER_PROFILE_GRP":
					case "PRI_IS_EDU_PROVIDER_STAFF_PROFILE_GRP":
					case "PRI_IS_REFERRER_PROFILE_GRP":
					case "PRI_IS_SELLER_PROFILE_GRP":
					case "PRI_IS SKILLS":
						log.warn("getDEF -> detected non DEFy attributeCode " + ea.getAttributeCode());
						i.remove();
						break;
					case "PRI_IS_DISABLED":
						log.warn("getDEF -> detected non DEFy attributeCode " + ea.getAttributeCode());
						// don't remove until we work it out...
						try {
							throw new DebugException("Bad DEF " + ea.getAttributeCode());
						} catch (DebugException e) {
							e.printStackTrace();
						}
						break;
					case "PRI_IS_LOGBOOK":
						log.debug("getDEF -> detected non DEFy attributeCode " + ea.getAttributeCode());
						i.remove();

					default:

				}
			}
		}

		if (isAs.size() == 1) {
			// Easy
			Map<String, BaseEntity> beMapping = getDefMap(realm);
			String attrCode = isAs.get(0).getAttributeCode();
			String trimedAttrCode = attrCode.substring("PRI_IS_".length());
			BaseEntity defBe = beMapping.get("DEF_" + trimedAttrCode);

			if (defBe == null) {
				log.error("No such DEF called " + "DEF_" + isAs.get(0).getAttributeCode().substring("PRI_IS_".length()));
			}
			return defBe;

		} else if (isAs.isEmpty()) {
			// THIS HANDLES CURRENT BAD BEs
			// loop through the defs looking for matching prefix
			for (BaseEntity defBe : getDefMap(realm).values()) {
				String prefix = defBe.getValue("PRI_PREFIX", null);
				if (prefix == null) {
					continue;
				}
				// LITTLE HACK FOR OHS DOCS, SORRY!
				if (prefix.equals("DOC") && be.getCode().startsWith("DOC_OHS_")) {
					continue;
				}
				if (be.getCode().startsWith(prefix + "_")) {
					return defBe;
				}
			}

			log.error("NO DEF ASSOCIATED WITH be " + be.getCode());
			return new BaseEntity("ERR_DEF", "No DEF");
		} else {
			// Create sorted merge code
			String mergedCode = "DEF_" + isAs.stream().sorted(Comparator.comparing(EntityAttribute::getAttributeCode))
				.map(ea -> ea.getAttributeCode()).collect(Collectors.joining("_"));

			mergedCode = mergedCode.replaceAll("_PRI_IS_DELETED", "");
			BaseEntity mergedBe = getDefMap(realm).get(mergedCode);

			if (mergedBe == null) {

				log.info("Detected NEW Combination DEF - " + mergedCode);
				// Get primary PRI_IS
				Optional<EntityAttribute> topDog = be.getHighestEA("PRI_IS_");
				if (topDog.isPresent()) {

					// So this combination DEF inherits top dogs name
					String topCode = topDog.get().getAttributeCode().substring("PRI_IS_".length());
					BaseEntity defTopDog = getDefMap(realm).get("DEF_" + topCode);
					mergedBe = new BaseEntity(mergedCode, mergedCode);

					// now copy all the combined DEF eas.
					for (EntityAttribute isea : isAs) {
						BaseEntity defEa = getDefMap(realm).get("DEF_" + isea.getAttributeCode().substring("PRI_IS_".length()));
						if (defEa != null) {
							for (EntityAttribute ea : defEa.getBaseEntityAttributes()) {
								try {
									mergedBe.addAttribute(ea);
								} catch (BadDataException e) {
									log.error("Bad data in getDEF ea merge " + mergedCode);
								}
							}
						} else {
							log.info("No DEF code -> " + "DEF_" + isea.getAttributeCode().substring("PRI_IS_".length()));
							return null;
						}
					}
					getDefMap(realm).put(mergedCode, mergedBe);
					return mergedBe;

				} else {
					log.error("NO DEF EXISTS FOR " + be.getCode());
					return null;
				}
			} else {
				// return 'merged' composite
				return mergedBe;
			}
		}

	}
}
