package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.io.Serializable;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.intf.KafkaInterface;

@RegisterForReflection
public class KafkaUtils implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(KafkaUtils.class);
	private static Jsonb jsonb = JsonbBuilder.create();
	private static KafkaInterface kafkaInterface;

	/**
	* Initialise the kafka interface
	*
	* @param kInterface
	 */
	public void init(KafkaInterface kInterface) {
		kafkaInterface = kInterface;
	}

	/**
	* Write an Object to a kafka channel as a payload
	*
	* @param channel
	* @param payload
	 */
	public static void writeMsg(String channel, Object payload) {

		// Jsonify the payload and write
		String json = jsonb.toJson(payload);
		writeMsg(channel, json);
	}

	/**
	* Write a String to a kafka channel as a payload
	*
	* @param channel
	* @param payload
	 */
	public static void writeMsg(String channel, String payload) {
		// Write to kafka channel through interface
		kafkaInterface.write(channel, payload);
	}

}
