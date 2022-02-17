package life.genny.qwandaq.utils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;

@RegisterForReflection
public class QwandaUtils {

	static final Logger log = Logger.getLogger(QwandaUtils.class);
	private static final ExecutorService executorService = Executors.newFixedThreadPool(200);

	static Map<String, Map<String, Attribute>> attributes = new ConcurrentHashMap<>();

	static Jsonb jsonb = JsonbBuilder.create();

	static GennyToken gennyToken;

	public static void init(GennyToken token) {
		gennyToken = token;
		log.info("GENNY_API_URL = " + System.getenv("GENNY_API_URL"));
		loadAllAttributes();
	}

	public static void init(GennyToken token, List<Attribute> attributeList) {
		gennyToken = token;

		attributes.put(token.getRealm(), new ConcurrentHashMap<String, Attribute>());
		Map<String, Attribute> attributeMap = attributes.get(token.getRealm());

		for (Attribute attribute : attributeList) {
			attributeMap.put(attribute.getCode(), attribute);
		}
	}

	/**
	 * Get an attribute from the in memory attribute map. If realm not found, it
	 * will try to fetch attributes from the DB.
	 *
	 * @param attributeCode
	 * @param gennyToken
	 * @return
	 */
	public static Attribute getAttribute(final String attributeCode) {

		String realm = gennyToken.getRealm();

		if (attributes.get(gennyToken.getRealm()) == null) {
			loadAllAttributes();
		}

		Attribute attribute = attributes.get(realm).get(attributeCode);

		if (attribute == null) {
			log.error("Bad Attribute in Map for realm " + realm + " and code " + attributeCode);
		}

		return attribute;
	}

	/**
	 * Load all attributes into the in memory map.
	 *
	 * @return
	 */
	public static void loadAllAttributes() {

		String realm = gennyToken.getRealm();

		log.info("About to load all attributes for realm " + realm);

		List<Attribute> attributeList = null;

		try {
			attributeList = DatabaseUtils.fetchAttributes(realm);
			log.info("Loaded all attributes for realm " + realm);
			if (attributeList == null) {
				log.error("Null attributeList, not putting in map!!!");
				return;
			}

			// Check for existing map
			if (!attributes.containsKey(realm)) {
				attributes.put(realm, new ConcurrentHashMap<String, Attribute>());
			}
			Map<String, Attribute> attributeMap = attributes.get(realm);

			// Insert attributes into map
			for (Attribute attribute : attributeList) {
				attributeMap.put(attribute.getCode(), attribute);
			}

			log.info("All attributes have been loaded: " + attributeMap.size() + " attributes");
		} catch (Exception e) {
			log.error("Error loading attributes for realm " + realm);
		}

	}

	/**
	 * Remove an atttribute from the in memory set using the code.
	 *
	 * @param code Code of the attribute to remove.
	 */
	public static void removeAttributeFromMemory(String code) {

		String realm = gennyToken.getRealm();
		attributes.get(realm).remove(code);
	}

	/**
	* Get a Question using a code.
	*
	* @param code
	* @param userToken
	* @return
	 */
	static public Question getQuestion(String code, GennyToken userToken) {

		if (code == null) {
			log.error("Code must not be null!");
			return null;
		}

		Question question = CacheUtils.getObject(userToken.getRealm(), code, Question.class);

		if (question == null) {
			log.warn("COULD NOT READ " + code + " from cache!!!");

			String uri = GennySettings.qwandaServiceUrl + "/qwanda/questioncodes/" + code;
			String json = HttpUtils.get(uri, userToken.getToken());

			if (!json.isBlank()) {

				question = jsonb.fromJson(json, Question.class);

				CacheUtils.writeCache(userToken.getRealm(), code, jsonb.toJson(question));
				log.info(question.getCode() + " written to cache!");

			} else {
				log.error("Could not find question " + code + " in database!");
				return null;
			}
		}

		return question;
	}


	/**
	 * Send a {@link QEventMessage} to shleemy for scheduling.
	 *
	 * @param userToken
	 * @param eventMsgCode
	 * @param scheduleMsgCode
	 * @param triggertime
	 * @param targetCode
	 */
	public static void scheduleEvent(GennyToken userToken, String eventMsgCode, String scheduleMsgCode,
			LocalDateTime triggertime, String targetCode) {

		// create the event message
		QEventMessage evtMsg = new QEventMessage("SCHEDULE_EVT", eventMsgCode);
		evtMsg.setToken(userToken.getToken());
		evtMsg.getData().setTargetCode(targetCode);

		// create a recipient list
		String[] rxList = new String[2];
		rxList[0] = "SUPERUSER";
		rxList[1] = userToken.getUserCode();
		evtMsg.setRecipientCodeArray(rxList);

		log.info("Scheduling event: " + eventMsgCode + ", Trigger time: " + triggertime.toString());

		// create schedule message
		QScheduleMessage scheduleMessage = new QScheduleMessage(scheduleMsgCode, jsonb.toJson(evtMsg),
				userToken.getUserCode(), "project", triggertime, userToken.getRealm());
		log.info("Sending message " + scheduleMessage.getCode() + " to shleemy for scheduling");

		// send msg to shleemy
		String json = jsonb.toJson(scheduleMessage);
		KafkaUtils.writeMsg("schedule", json);
	}

	/**
	 * Delete a currently scheduled message via shleemy.
	 *
	 * @param userToken
	 * @param code
	 */
	public static void deleteSchedule(GennyToken userToken, String code) {

		String uri = GennySettings.shleemyServiceUrl + "/api/schedule/code/" + code;
		HttpUtils.delete(uri, userToken.getToken());
	}

	/**
	* NOTE: This method should be deleted. Anything referencing it 
	* should directly reference the HttpUtils.get() method instead.
	*
	* @param url
	* @param authToken
	* @return
	 */
	static public String apiGet(String url, String authToken) {

		return HttpUtils.get(url, authToken);
	}
}
