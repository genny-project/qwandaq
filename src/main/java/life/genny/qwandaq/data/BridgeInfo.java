package life.genny.qwandaq.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A Bridge Info storage class used with BridgeSwitch to map individual user bridge Ids
 * 
 * @author Jasper Robison
 */
public class BridgeInfo {

	public BridgeInfo() {}

	public ConcurrentMap<String, String> mappings = new ConcurrentHashMap<>();
}
