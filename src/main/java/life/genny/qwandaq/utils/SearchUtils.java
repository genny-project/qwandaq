package life.genny.qwandaq.utils;

import java.util.List;
import java.util.ArrayList;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.SearchEntity;

public class SearchUtils {

	static final Logger log = Logger.getLogger(SearchUtils.class);

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

}
