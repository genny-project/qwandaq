package life.genny.qwandaq.utils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
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

	
	/** 
	 * @param token
	 */
	public static void init(GennyToken token) {
		gennyToken = token;
		loadAllAttributes();
	}

	
	/** 
	 * @param token
	 * @param attributeList
	 */
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
		List<Attribute> attributeList = null;

		log.info("About to load all attributes for realm " + realm);

		try {
			log.info("Fetching Attributes from database...");
			attributeList = DatabaseUtils.findAttributes(realm, null, null);

			log.info("Loaded all attributes for realm " + realm);
			if (attributeList == null) {
				log.error("Null attributeList, not putting in map!!!");
				return;
			}

			// check for existing map
			if (!attributes.containsKey(realm)) {
				attributes.put(realm, new ConcurrentHashMap<String, Attribute>());
			}
			Map<String, Attribute> attributeMap = attributes.get(realm);

			// insert attributes into map
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

		// fetch from cache
		Question question = CacheUtils.getObject(userToken.getRealm(), code, Question.class);

		if (question == null) {

			// fetch from database if not found in cache
			log.warn("Could NOT read " + code + " from cache! Checking Database...");
			question = DatabaseUtils.findQuestionByCode(userToken.getRealm(), code);

			if (question == null) {
				log.error("Could not find question " + code + " in database!");
				return null;
			}

			// cache the fetched question for quicker access
			CacheUtils.writeCache(userToken.getRealm(), code, jsonb.toJson(question));
			log.info(question.getCode() + " written to cache!");
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

		String uri = GennySettings.shleemyServiceUrl() + "/api/schedule/code/" + code;
		HttpUtils.delete(uri, userToken.getToken());
	}

	/**
	* Update the status of the disabled field for an Ask on the web.
	*
	* @param ask
	* @param disabled
	* @param userToken
	 */
	public static void updateAskDisabled(Ask ask, Boolean disabled, GennyToken userToken) {

		ask.setDisabled(disabled);

		QDataAskMessage askMsg = new QDataAskMessage(ask);
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		String json = jsonb.toJson(askMsg);
		KafkaUtils.writeMsg("webcmds", json);
	}

	/**
	* Send an updated entity for each unique target in answers.
	*
	* @param userToken
	* @param answers
	 */
    static public void sendToFrontEnd(GennyToken userToken, Answer... answers) {

        if (answers == null) {
			log.error("Answers is null!");
			return;
        }

        if (answers.length == 0) {
			log.error("Number of Answers is 0!");
			return;
        }

		String realm = userToken.getRealm();

		// sort answers into target BaseEntitys
		Map<String, List<Answer>> answersPerTargetCodeMap = Stream.of(answers)
			.collect(Collectors.groupingBy(Answer::getTargetCode));

		for (String targetCode : answersPerTargetCodeMap.keySet()) {

			// find the baseentity
			BaseEntity target = CacheUtils.getObject(realm, targetCode, BaseEntity.class);
			if (target != null) {

				BaseEntity be = new BaseEntity(target.getCode(), target.getName());
				be.setRealm(userToken.getRealm());

				for (Answer answer : answers) {

					try {
						Attribute att = getAttribute(answer.getAttributeCode());
						be.addAttribute(att);
						be.setValue(att, answer.getValue());
					} catch (BadDataException e) {
						e.printStackTrace();
					}
				}
				QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be);
				msg.setReplace(true);
				msg.setToken(userToken.getToken());
				KafkaUtils.writeMsg("webcmds", msg);
			}
		}
    }

	/**
	* Send feedback for answer data. ERROR, WARN, SUSPICIOUS or HINT.
	*
	* @param userToken
	* @param answer
	* @param prefix
	* @param message
	 */
    public static void sendFeedback(GennyToken userToken, Answer answer, String prefix, String message) {

        // find the baseentity
        BaseEntity target = CacheUtils.getObject(userToken.getRealm(), answer.getTargetCode(), BaseEntity.class);

        BaseEntity be = new BaseEntity(target.getCode(), target.getName());
        be.setRealm(userToken.getRealm());

        try {

            Attribute att = getAttribute(answer.getAttributeCode());
            be.addAttribute(att);
            be.setValue(att, answer.getValue());
            Optional<EntityAttribute> ea = be.findEntityAttribute(answer.getAttributeCode());

            if (ea.isPresent()) {
                ea.get().setFeedback(prefix+":"+message);

                QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be);
                msg.setReplace(true);
                msg.setToken(userToken.getToken());
                KafkaUtils.writeMsg("webcmds", msg);
            }
        } catch (BadDataException e) {
            e.printStackTrace();
        }
    }

	/**
	* Is the number a valid ABN.
	*
	* @param abn
	* @return
	 */
    public static boolean isValidAbnFormat(final String abn) {
        if (NumberUtils.isDigits(abn) && abn.length() != 11) {
            return false;
        }
        final int[] weights = {10, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19};
        // split abn number string by digits to get int array
        int[] abnDigits = Stream.of(abn.split("\\B")).mapToInt(Integer::parseInt).toArray();
        // reduce by applying weight[index] * abnDigits[index] (NOTE: substract 1 for
        // the first digit in abn number)
        int sum = IntStream.range(0, weights.length).reduce(0,
                (total, idx) -> total + weights[idx] * (idx == 0 ? abnDigits[idx] - 1 : abnDigits[idx]));
        return (sum % 89 == 0);
    }
}
