package life.genny.qwandaq.handlers;

import java.util.Map;

import org.jboss.logging.Logger;

import life.genny.qwandaq.utils.BaseEntityUtils;

public class RuleFlowGroupWorkItemHandler {

	static final Logger log = Logger.getLogger(RuleFlowGroupWorkItemHandler.class);

	public RuleFlowGroupWorkItemHandler() {
		log.error("RuleFlowGroup handler is incomplete!!!!");
	}

	
	/** 
	 * @param beUtils
	 * @param items
	 * @param ruleFlowGroup
	 * @param callingWorkflow
	 * @return Map<String, Object>
	 */
	public Map<String, Object> executeRules(BaseEntityUtils beUtils, Map<String, Object> items,
			String ruleFlowGroup, String callingWorkflow) {
		log.error("RuleFlowGroup executeRules function is incomplete!!!!");
		return null;
	}
}
