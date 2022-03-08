package life.genny.qwandaq.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.handlers.RuleFlowGroupWorkItemHandler;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QSearchMessage;

/**
 * A static utility class used for performing table 
 * searches and search related operations.
 * 
 * @author Jasper Robison
 */
public class SearchUtils {

	static final Logger log = Logger.getLogger(SearchUtils.class);
	static Jsonb jsonb = JsonbBuilder.create();

	/**
	* Evaluate any conditional filters for a {@link SearchEntity}
	*
	* @param beUtils the utils to use
	* @param searchBE the SearchEntity to evaluate filters of
	* @return SearchEntity
	 */
	public static SearchEntity evaluateConditionalFilters(BaseEntityUtils beUtils, SearchEntity searchBE) {

		CapabilityUtils capabilityUtils = new CapabilityUtils(beUtils);

		List<String> shouldRemove = new ArrayList<>();

		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {

			if (!ea.getAttributeCode().startsWith("CND_")) {
				// find Conditional Filters
				EntityAttribute cnd = searchBE.findEntityAttribute("CND_"+ea.getAttributeCode()).orElse(null);

				if (cnd != null) {

					log.info("Condition found for " + ea.getAttributeCode() + " with value: " + cnd.getValue().toString());
					String[] condition = cnd.getValue().toString().split(":");

					String capability = condition[0];
					String mode = condition[1];

					// check for NOT operator
					Boolean not = capability.startsWith("!");
					capability = not ? capability.substring(1) : capability;

					// check for Capability
					Boolean hasCap = capabilityUtils.hasCapabilityThroughPriIs(capability, CapabilityMode.getMode(mode));

					// XNOR operator
					if (!(hasCap ^ not)) {
						shouldRemove.add(ea.getAttributeCode());
					}
				}
			}
		}

		// remove unwanted attrs
		shouldRemove.stream().forEach(item -> {searchBE.removeAttribute(item);});

		return searchBE;
	}

	/**
	* Perform a table like search in Genny using a {@link SearchEntity} code. 
	* The respective {@link SearchEntity} will be fetched from the cache befor processing.
	*
	* @param beUtils the utils to use
	* @param code the code of the SearchEntity to grab from cache and search
	 */
	public static void searchTable(BaseEntityUtils beUtils, String code) {

		String realm = beUtils.getRealm();

		String searchCode = code;
		if (searchCode.startsWith("CNS_")) {
			searchCode = searchCode.substring(4);
		} else if (!searchCode.startsWith("SBE_")) {
			searchCode = "SBE_" + searchCode;
		}

		log.info("SBE CODE   ::   " + searchCode);

		SearchEntity searchEntity = CacheUtils.getObject(realm, searchCode, SearchEntity.class);

		if (searchEntity == null) {
			log.info("Could not fetch " + searchCode + " from cache!!!");
		}

		if (code.startsWith("CNS_")) {
			searchEntity.setCode(code);
		}

		searchTable(beUtils, searchEntity);
	}

	/**
	* Perform a table like search in Genny using a {@link SearchEntity}.
	*
	* @param beUtils the utils to use
	* @param searchEntity the SearchEntity to search
	 */
	public static void searchTable(BaseEntityUtils beUtils, SearchEntity searchEntity) {

		String realm = beUtils.getRealm();
		String sessionCode = beUtils.getGennyToken().getSessionCode();

		if (searchEntity == null) {
			System.out.println("SearchBE is null");
		}

		// get session search code for SBE
		if (!searchEntity.getCode().contains(sessionCode.toUpperCase())) {
			searchEntity.setCode(searchEntity.getCode() + "_" + sessionCode.toUpperCase());
		}

		log.info("sessionSearchCode  ::  " + searchEntity.getCode());

		// update any nested search attribute codes
		searchEntity.getBaseEntityAttributes().stream()
			.filter(ea -> ea.getAttributeCode().startsWith("SBE_"))
			.forEach(ea -> {
				ea.setAttributeCode(ea.getAttributeCode() + "_" + sessionCode.toUpperCase());
			});

		// put session search in cache
		CacheUtils.putObject(realm, searchEntity.getCode(), searchEntity);

		// Add any necessary extra filters
		List<EntityAttribute> filters = getUserFilters(beUtils, searchEntity);

		if (!filters.isEmpty()) {
			log.info("Found " + filters.size() + " additional filters for " + searchEntity.getCode());

			for (EntityAttribute filter : filters) {
				searchEntity.getBaseEntityAttributes().add(filter);
			}
		}

		CacheUtils.putObject(realm, "LAST-SEARCH:"+sessionCode, searchEntity);

		// ensure column and action indexes are accurate
		searchEntity.updateColumnIndex();
		searchEntity.updateActionIndex();

		// package and send search message to fyodor
		QSearchMessage searchBeMsg = new QSearchMessage(searchEntity);
		searchBeMsg.setToken(beUtils.getGennyToken().getToken());
		searchBeMsg.setDestination("webcmds");
		KafkaUtils.writeMsg("search_events", searchBeMsg);
	}

	/**
	* A method to fetch any additional {@link EntityAttribute} filters for a given {@link SearchEntity} 
	* from the SearchFilters rulegroup.
	*
	* @param beUtils the utils to use
	* @param searchBE the SearchEntity to get additional filters for
	* @return List
	 */
	public static List<EntityAttribute> getUserFilters(BaseEntityUtils beUtils, SearchEntity searchBE) {

		List<EntityAttribute> filters = new ArrayList<>();

		Map<String, Object> facts = new ConcurrentHashMap<>();
		facts.put("serviceToken", beUtils.getServiceToken());
		facts.put("userToken", beUtils.getGennyToken());
		facts.put("searchBE", searchBE);

		Map<String, Object> results = new RuleFlowGroupWorkItemHandler()
			.executeRules(
				beUtils,
				facts, 
				"SearchFilters", 
				"SearchUtils:getUserFilters"
			);

		Object obj = results.get("payload");

		if (obj instanceof QBulkMessage) {
			QBulkMessage bulkMsg = (QBulkMessage) results.get("payload");

			// Check if bulkMsg not empty
			if (bulkMsg.getMessages().length > 0) {

				// Get the first QDataBaseEntityMessage from bulkMsg
				QDataBaseEntityMessage msg = bulkMsg.getMessages()[0];

				// Check if msg is not empty
				if (!msg.getItems().isEmpty()) {

					// Extract the baseEntityAttributes from the first BaseEntity
					Set<EntityAttribute> filtersSet = msg.getItems().get(0).getBaseEntityAttributes();
					filters.addAll(filtersSet);
				}
			}
		}
		return filters;
	}
	
	/** 
	 * @param beUtils the utils to use
	 * @param searchBE the SearchEntity to send filter questions for
	 */
	public static void sendFilterQuestions(BaseEntityUtils beUtils, SearchEntity searchBE) {
		log.error("Function not complete!");
	}
	
	/** 
	 * @param beUtils the utils to use
	 * @param baseBE the baseBE to get associated column for
	 * @param calEACode the calEACode to get
	 * @return Answer
	 */
	public static Answer getAssociatedColumnValue(BaseEntityUtils beUtils, BaseEntity baseBE, String calEACode) {
		log.error("Function not complete!");
		return null;
	}
	
	/** 
	 * @param searchCode the searchCode to get
	 * @return SearchEntity get session search of
	 */
	public static SearchEntity getSessionSearch(String searchCode) {
		log.error("Function not complete!");
		return null;
	}

	/** 
	 * @param beUtils the utils to use
	 * @param dropdownValue the dropdownValue to perform for
	 */
	public static void performQuickSearch(BaseEntityUtils beUtils, String dropdownValue) {

		Instant start = Instant.now();

		String realm = beUtils.getServiceToken().getRealm();
		String sToken = beUtils.getServiceToken().getToken();
		String gToken = beUtils.getGennyToken().getToken();
		String sessionCode = beUtils.getGennyToken().getUniqueId().toUpperCase();

		// convert to entity list
		log.info("dropdownValue = " + dropdownValue);
		String cleanCode = beUtils.cleanUpAttributeValue(dropdownValue);
		BaseEntity target = beUtils.getBaseEntityByCode(cleanCode);

		BaseEntity project = beUtils.getBaseEntityByCode("PRJ_" + realm.toUpperCase());

		if (project == null) {
			log.error("Null project Entity!!!");
			return;
		}

		String jsonStr = project.getValue("PRI_BUCKET_QUICK_SEARCH_JSON", null);

		if (jsonStr == null) {
			log.error("Null Bucket Json!!!");
			return;
		}

		// init merge contexts
		HashMap<String, Object> ctxMap = new HashMap<>();
		ctxMap.put("TARGET", target);

		JsonObject json = jsonb.fromJson(jsonStr, JsonObject.class);
		JsonArray bucketMapArray = json.getJsonArray("buckets");

		for (Object bm : bucketMapArray) {

			JsonObject bucketMap = (JsonObject) bm;

			String bucketMapCode = bucketMap.getString("code");

			SearchEntity baseSearch = CacheUtils.getObject(realm, bucketMapCode, SearchEntity.class);

			if (baseSearch == null) {
				log.error("SearchEntity " + bucketMapCode + " is NULL in cache!");
				continue;
			}

			// handle Pre Search Mutations
			JsonArray preSearchMutations = bucketMap.getJsonArray("mutations");

			for (Object m : preSearchMutations) {

				JsonObject mutation = (JsonObject) m;

				JsonArray conditions = mutation.getJsonArray("conditions");

				if (jsonConditionsMet(conditions, target)) {

					log.info("Pre Conditions met for : " + conditions.toString());

					String attributeCode = mutation.getString("attributeCode");
					String operator = mutation.getString("operator");
					String value = mutation.getString("value");

					// TODO: allow for regular filters too
					// SearchEntity.StringFilter stringFilter =
					// SearchEntity.convertOperatorToStringFilter(operator);
					SearchEntity.StringFilter stringFilter = SearchEntity.StringFilter.EQUAL;
					String mergedValue = MergeUtils.merge(value, ctxMap);
					log.info( "Adding filter: " + attributeCode + " " 
							+ stringFilter.toString() + " " + mergedValue);
					baseSearch.addFilter(attributeCode, stringFilter, mergedValue);
				}
			}

			// perform Search
			baseSearch.setPageSize(100000);
			log.info("Performing search for " + baseSearch.getCode());
			List<BaseEntity> results = beUtils.getBaseEntitys(baseSearch);

			JsonArray targetedBuckets = bucketMap.getJsonArray("targetedBuckets");

			if (targetedBuckets == null) {
				log.error("No targetedBuckets field for " + bucketMapCode);
			}

			// handle Post Search Mutations
			for (Object b : targetedBuckets) {

				JsonObject bkt = (JsonObject) b;

				String targetedBucketCode = bkt.getString("code");

				if (targetedBucketCode == null) {
					log.error("No code field present in targeted bucket!");
				} else {
					log.info("Handling targeted bucket " + targetedBucketCode);
				}

				JsonArray postSearchMutations = bkt.getJsonArray("mutations");

				log.info("postSearchMutations = " + postSearchMutations);

				List<BaseEntity> finalResultList = new ArrayList<>();

				for (BaseEntity item : results) {

					if (postSearchMutations != null) {

						for (Object m : postSearchMutations) {

							JsonObject mutation = (JsonObject) m;

							JsonArray conditions = mutation.getJsonArray("conditions");

							if (conditions == null) {
								if (jsonConditionMet(mutation, item)) {
									log.info("Post condition met");
									finalResultList.add(item);
								}
							} else {
								log.info("Testing conditions: " + conditions.toString());
								if (jsonConditionsMet(conditions, target) && jsonConditionMet(mutation, item)) {
									log.info("Post condition met");
									finalResultList.add(item);
								}
							}
						}

					} else {
						finalResultList.add(item);
					}
				}

				// fetch each search from cache
				SearchEntity searchBE = CacheUtils.getObject(realm, targetedBucketCode + "_" + sessionCode, SearchEntity.class);

				if (searchBE == null) {
					log.error("Null SBE in cache for " + targetedBucketCode);
					continue;
				}

				// Attach any extra filters from SearchFilters rulegroup
				List<EntityAttribute> filters = getUserFilters(beUtils, searchBE);

				if (!filters.isEmpty()) {
					log.info("User Filters are NOT empty");
					log.info("Adding User Filters to searchBe  ::  " + searchBE.getCode());
					for (EntityAttribute filter : filters) {
						searchBE.getBaseEntityAttributes().add(filter);
					}
				} else {
					log.info("User Filters are empty");
				}

				// process the associated columns
				List<EntityAttribute> cals = searchBE.findPrefixEntityAttributes("COL__");

				for (BaseEntity be : finalResultList) {

					for (EntityAttribute calEA : cals) {

						Answer ans = getAssociatedColumnValue(beUtils, be, calEA.getAttributeCode());

						if (ans != null) {
							try {
								be.addAnswer(ans);
							} catch (BadDataException e) {
								e.printStackTrace();
							}
						}
					}
				}

				// send the results
				log.info("Sending Results: " + finalResultList.size());
				QDataBaseEntityMessage msg = new QDataBaseEntityMessage(finalResultList);
				msg.setToken(gToken);
				msg.setReplace(true);
				msg.setParentCode(searchBE.getCode());
				KafkaUtils.writeMsg("webcmds", msg);

				// update and send the SearchEntity
				// updateBaseEntity(searchBE, "PRI_TOTAL_RESULTS", Long.valueOf(finalResultList.size()) + "");

				Attribute attribute = QwandaUtils.getAttribute("PRI_TOTAL_RESULTS");
				try {
					searchBE.addAnswer(new Answer(searchBE, searchBE, attribute, Long.valueOf(finalResultList.size()) + ""));
				} catch (BadDataException e) {
					log.error("Could not update total results");
				}
				CacheUtils.putObject(beUtils.getGennyToken().getRealm(), searchBE.getCode(), searchBE);

				if (searchBE != null) {
					log.info("Sending Search Entity : " + searchBE.getCode());
				} else {
					log.error("SearchEntity is NULLLLL!!!!");
				}
				QDataBaseEntityMessage searchMsg = new QDataBaseEntityMessage(searchBE);
				searchMsg.setToken(gToken);
				searchMsg.setReplace(true);
				KafkaUtils.writeMsg("webcmds", searchMsg);
			}
		}

		Instant end = Instant.now();
		log.info("Finished Quick Search: " + Duration.between(start, end).toMillis() + " millSeconds.");
	}

	/**
	 * Evaluate whether a set of conditions are met for a specific BaseEntity.
	 *
	 * @param conditions the conditions to check
	 * @param target the target entity to check against
	 * @return Boolean
	 */
	public static Boolean jsonConditionsMet(JsonArray conditions, BaseEntity target) {

		// TODO: Add support for roles and context map

		if (conditions != null) {

			// log.info("Bulk Conditions = " + conditions.toString());

			for (Object c : conditions) {

				JsonObject condition = (JsonObject) c;

				if (!jsonConditionMet(condition, target)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Evaluate whether the condition is met for a specific BaseEntity.
	 *
	 * @param condition the condition to check
	 * @param target the target entity to check against
	 * @return Boolean
	 */
	public static Boolean jsonConditionMet(JsonObject condition, BaseEntity target) {

		// TODO: Add support for roles and context map

		if (condition != null) {

			// log.info("Single Condition = " + condition.toString());

			String attributeCode = condition.getString("attributeCode");
			String operator = condition.getString("operator");
			String value = condition.getString("value");

			EntityAttribute ea = target.findEntityAttribute(attributeCode).orElse(null);

			if (ea == null) {
				log.info(
						"Could not evaluate condition: Attribute "
						+ attributeCode
						+ " for "
						+ target.getCode()
						+ " returned Null!");
				return false;
			} else {
				log.info("Found Attribute " + attributeCode + " for " + target.getCode());
			}
			log.info(ea.getValue().toString() + " = " + value);

			if (!ea.getValue().toString().toUpperCase().equals(value.toUpperCase())) {
				return false;
			}
		}

		return true;
	}

}
