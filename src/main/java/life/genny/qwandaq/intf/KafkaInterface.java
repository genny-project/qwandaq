package life.genny.qwandaq.intf;

import org.jboss.logging.Logger;

/**
 * A Kafka interface to write to kafka channels.
 *
 * This interface should be implemented seperately in any project that requires sending through kafka.
 *
 * */
public interface KafkaInterface {

	public static final Logger log = Logger.getLogger(KafkaInterface.class);

	/**
	* A Dummy write method.
	*
	* @param channel
	* @param payload
	 */
	public default void write(String channel, String payload) {
		log.error("Default KafkaInterface... not writing Message!!!");
	}
}
