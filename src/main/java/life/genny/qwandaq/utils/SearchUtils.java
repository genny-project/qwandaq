package life.genny.qwandaq.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.handlers.RuleFlowGroupWorkItemHandler;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QSearchMessage;

public class SearchUtils {

	static final Logger log = Logger.getLogger(SearchUtils.class);

	/**
	* Evaluate any conditional filters for a {@link SearchEntity}
	*
	* @param beUtils
	* @param searchBE
	* @return
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
	* @param beUtils
	* @param code
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
	* @param beUtils
	* @param code
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
	* @param beUtils
	* @param searchBE
	* @return
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

}
