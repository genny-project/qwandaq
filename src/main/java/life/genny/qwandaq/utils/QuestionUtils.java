package life.genny.qwandaq.utils;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.google.common.reflect.TypeToken;

import org.apache.http.client.ClientProtocolException;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.entity.EntityQuestion;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QwandaMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.validation.Validation;

@RegisterForReflection
public class QuestionUtils implements Serializable {
	private static final Logger log = Logger.getLogger(QuestionUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	/**
	* Check if a Question group exists in the database and cache.
	*
	* @param sourceCode
	* @param targetCode
	* @param questionCode
	* @param token
	* @return
	 */
	public static Boolean doesQuestionGroupExist(String sourceCode, String targetCode, final String questionCode,
			String token) {

		// we grab the question group using the questionCode
		QDataAskMessage questions = getAsks(sourceCode, targetCode, questionCode, token);

		// we check if the question payload is not empty
		if (questions != null) {

			// we check if the question group contains at least one question
			if (questions.getItems() != null && questions.getItems().length > 0) {

				Ask firstQuestion = questions.getItems()[0];

				// we check if the question is a question group and contains at least one question
				if (firstQuestion.getAttributeCode().contains("QQQ_QUESTION_GROUP_BUTTON_SUBMIT")) {
					return firstQuestion.getChildAsks().length > 0;
				} else {
					// if it is an ask we return true
					return true;
				}
			}
		}

		return false;
	}

	/**
	* Deserialize a json {@link String} to a {@link JsonObject}.
	*
	* @param string The string to deserialize.
	* @return The equivalent JsonObject
	 */
	public static JsonObject toJson(String string)
	{
		// open a reader and feed in the string
		JsonReader jsonReader = Json.createReader(new StringReader(string));
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();

		return jsonObject;
	}

	/**
	* Recuresively run through ask children and update the question 
	* using what is stored in the cache.
	*
	* @param ask
	* @param token
	 */
	public static void setCachedQuestionsRecursively(Ask ask, String token) {

		// call recursively if ask represents a question group
		if (ask.getAttributeCode().equals("QQQ_QUESTION_GROUP")) {

			for (Ask childAsk : ask.getChildAsks()) {
				setCachedQuestionsRecursively(childAsk, token);
			}

		} else {

			// otherwise we fetch the question and update the ask
			Question question = ask.getQuestion();
			GennyToken gennyToken = new GennyToken(token);

			Question cachedQuestion = CacheUtils.getObject(gennyToken.getRealm(), question.getCode(), Question.class);

			if (cachedQuestion != null) {

				// grab the icon too
				if (question.getIcon() != null) {
					cachedQuestion.setIcon(question.getIcon());
				}
				ask.setQuestion(cachedQuestion);
				ask.setContextList(cachedQuestion.getContextList());
			}
		}
	}

	public static QDataAskMessage getAsks(String sourceCode, String targetCode, String questionCode, String token) {

		String json = HttpUtils.get(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/" 
				+ sourceCode + "/asks2/" + questionCode + "/" + targetCode, token);

		if (json != null) {
			if (!json.contains("<title>Error")) {
				QDataAskMessage msg = jsonb.fromJson(json, QDataAskMessage.class);

				if (true) {
					// Identify all the attributeCodes and build up a working active Set
					Set<String> activeAttributeCodes = new HashSet<String>();
					for (Ask ask : msg.getItems()) {
						activeAttributeCodes.addAll(getAttributeCodes(ask));

						// Go down through the child asks and get cached questions
						setCachedQuestionsRecursively(ask, token);
					}
					// Now fetch the set from cache and add it....
					Type type = new TypeToken<Set<String>>() {}.getType();
					GennyToken gToken = new GennyToken(token);

					Set<String> activeAttributesSet = CacheUtils.getObject(gToken.getRealm(), "ACTIVE_ATTRIBUTES", type);

					if (activeAttributesSet == null) {
						activeAttributesSet = new HashSet<String>();
					}
					activeAttributesSet.addAll(activeAttributeCodes);

					CacheUtils.putObject(gToken.getRealm(), "ACTIVE_ATTRIBUTES", activeAttributesSet);

					log.debug("Total Active AttributeCodes = " + activeAttributesSet.size());
				}
				return msg;
			}
		}

		return null;
	}

	/**
	* Get all attributes used by an {@link Ask} and its children.
	*
	* @param ask The ask to traverse.
	* @return A set of Strings containing the attribute codes.
	 */
	private static Set<String> getAttributeCodes(Ask ask) {

		// grab attribute code of current ask
		Set<String> activeCodes = new HashSet<String>();
		activeCodes.add(ask.getAttributeCode());

		if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {

			// grab all child ask attribute codes
			for (Ask childAsk : ask.getChildAsks()) {

				activeCodes.addAll(getAttributeCodes(childAsk));
			}
		}
		return activeCodes;
	}

	public static QwandaMessage getQuestions(String sourceCode, String targetCode, String questionCode, String token)
			throws ClientProtocolException, IOException {
		return getQuestions(sourceCode, targetCode, questionCode, token, null, true);
	}

	public static QwandaMessage getQuestions(String sourceCode, String targetCode, String questionCode, String token,
			String stakeholderCode, Boolean pushSelection) throws ClientProtocolException, IOException {

		QBulkMessage bulk = new QBulkMessage();
		QwandaMessage qwandaMessage = new QwandaMessage();

		Instant start = Instant.now();

		// get the ask data
		QDataAskMessage questions = getAsks(sourceCode, targetCode, questionCode, token);

		Instant end = Instant.now();
		log.info("getAsks duration = " + Duration.between(start, end).toMillis() + " ms");

		if (questions != null) {

			// if we have the questions, loop through the asks and send required data to front end
			start = Instant.now();

			Ask[] asks = questions.getItems();
			if (asks != null && pushSelection) {
				QBulkMessage askData = sendAsksRequiredData(asks, token, stakeholderCode);
				for (QDataBaseEntityMessage message : askData.getMessages()) {
					bulk.add(message);
				}
			}

			end = Instant.now();
			log.info("sendAsksRequiredData duration = " + Duration.between(start, end).toMillis() + " ms");

			qwandaMessage.askData = bulk;
			qwandaMessage.asks = questions;

			return qwandaMessage;

		} else {
			log.error("Questions Msg is null "+sourceCode+"/asks2/"+questionCode+"/"+targetCode);
		}

		return null;
	}

	public static QwandaMessage askQuestions(final String sourceCode, final String targetCode,
			final String questionGroupCode, String token) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, token, null,
				true);
	}

	public static QwandaMessage askQuestions(final String sourceCode, final String targetCode,
			final String questionGroupCode, String token, String stakeholderCode) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, token,
				stakeholderCode, true);
	}

	public static QwandaMessage askQuestions(final String sourceCode, final String targetCode,
			final String questionGroupCode, Boolean pushSelection) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, null, null,
				pushSelection);
	}

	public static QwandaMessage askQuestions(final String sourceCode, final String targetCode,
			final String questionGroupCode, String token, Boolean pushSelection) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, token, null,
				pushSelection);
	}

	public static QwandaMessage askQuestions(final String sourceCode, final String targetCode,
			final String questionGroupCode, final String token, final String stakeholderCode,
			final Boolean pushSelection) {

		try {

			// if sending the questions worked, we ask user
			return getQuestions(sourceCode, targetCode, questionGroupCode, token,
					stakeholderCode, pushSelection);

		} catch (Exception e) {
			log.info("Ask questions exception: ");
			e.printStackTrace();
			return null;
		}
	}

	public static QwandaMessage setCustomQuestion(QwandaMessage questions, String questionAttributeCode,
			String customTemporaryQuestion) {

		if (questions != null && questionAttributeCode != null) {
			Ask[] askArr = questions.asks.getItems();
			if (askArr != null && askArr.length > 0) {
				for (Ask ask : askArr) {
					Ask[] childAskArr = ask.getChildAsks();
					if (childAskArr != null && childAskArr.length > 0) {
						for (Ask childAsk : childAskArr) {
							log.info("child ask code :: " + childAsk.getAttributeCode() + ", child askname :: "
									+ childAsk.getName());
							if (childAsk.getAttributeCode().equals(questionAttributeCode)) {
								if (customTemporaryQuestion != null) {
									childAsk.getQuestion().setName(customTemporaryQuestion);
									return questions;
								}
							}
						}
					}
				}
			}
		}
		return questions;
	}

	/**
	* Send out the required entity data for a set of asks.
	*
	* @param asks
	* @param token
	* @param stakeholderCode
	* @return
	 */
	private static QBulkMessage sendAsksRequiredData(Ask[] asks, String token, String stakeholderCode) {

		GennyToken gennyToken = new GennyToken(token);
		QBulkMessage bulk = new QBulkMessage();

		// we loop through the asks and send the required data if necessary
		for (Ask ask : asks) {

			// if attribute code starts with "LNK_", then it is a dropdown selection.

			String attributeCode = ask.getAttributeCode();
			if (attributeCode != null && attributeCode.startsWith("LNK_")) {

				// we get the attribute validation to get the group code
				Attribute attribute = QwandaUtils.getAttribute(attributeCode);

				if (attribute != null) {

					// grab the group in the validation
					DataType attributeDataType = attribute.getDataType();
					if (attributeDataType != null) {

						List<Validation> validations = attributeDataType.getValidationList();

						// we loop through the validations
						for (Validation validation : validations) {

							List<String> validationStrings = validation.getSelectionBaseEntityGroupList();

							if (validationStrings != null) {
								for (String validationString : validationStrings) {

									if (validationString.startsWith("GRP_")) {

										// Grab the parent
										BaseEntity parent = CacheUtils.getObject(gennyToken.getRealm(),
												validationString, BaseEntity.class);

										// we have a GRP. we push it to FE
										List<BaseEntity> bes = getChildren(validationString, 2, token);
										List<BaseEntity> filteredBes = null;

										if (bes != null && bes.isEmpty() == false) {

											// hard coding this for now. sorry
											if ("LNK_LOAD_LISTS".equals(attributeCode) && stakeholderCode != null) {

												// we filter load you only are a stakeholder of
												filteredBes = bes.stream().filter(baseEntity -> {
													return baseEntity.getValue("PRI_AUTHOR", "")
															.equals(stakeholderCode);
												}).collect(Collectors.toList());
											} else {
												filteredBes = bes;
											}

											// create message for base entities required for the validation
											QDataBaseEntityMessage beMessage = new QDataBaseEntityMessage(filteredBes);
											beMessage.setLinkCode("LNK_CORE");
											beMessage.setParentCode(validationString);
											beMessage.setReplace(true);
											bulk.add(beMessage);

											// create message for parent
											QDataBaseEntityMessage parentMessage = new QDataBaseEntityMessage(parent);
											bulk.add(parentMessage);
										}
									}
								}
							}
						}
					}
				}
			}

			// recursive call to add nested entities
			Ask[] childAsks = ask.getChildAsks();
			if (childAsks != null && childAsks.length > 0) {

				QBulkMessage newBulk = sendAsksRequiredData(childAsks, token,
						stakeholderCode);
				for (QDataBaseEntityMessage msg : newBulk.getMessages()) {
					bulk.add(msg);
				}
			}
		}

		return bulk;
	}

	/**
	* Create a question for a BaseEntity.
	* 
	* @param be
	* @param isQuestionGroup
	* @param token
	* @return
	 */
	public static Ask createQuestionForBaseEntity(BaseEntity be, Boolean isQuestionGroup, String token) {

		// create attribute code using isQuestionGroup and fetch attribute
		String attributeCode = isQuestionGroup ? "QQQ_QUESTION_GROUP_INPUT" : "PRI_EVENT";
		Attribute attribute = QwandaUtils.getAttribute(attributeCode);

		/*
		 * creating suffix according to value of isQuestionGroup. If it is a
		 * question-group, suffix "_GRP" is required"
		 */
		String questionSuffix = isQuestionGroup ? "_GRP" : "";

		// generate question then return ask
		Question newQuestion = new Question("QUE_" + be.getCode() + questionSuffix, be.getName(), attribute, false);

		return new Ask(newQuestion, be.getCode(), be.getCode(), false, 1.0, false, false, true);
	}

	/**
	* Create a question for a BaseEntity.
	*
	* @param be
	* @param isQuestionGroup
	* @param serviceToken
	* @return
	 */
	public static Ask createQuestionForBaseEntity(BaseEntity be, Boolean isQuestionGroup, GennyToken serviceToken) {

		// create attribute code using isQuestionGroup and fetch attribute
		String attributeCode = isQuestionGroup ? "QQQ_QUESTION_GROUP_INPUT" : "PRI_EVENT";
		Attribute attribute = QwandaUtils.getAttribute(attributeCode);

		/*
		 * creating suffix according to value of isQuestionGroup. If it is a
		 * question-group, suffix "_GRP" is required"
		 */
		String questionSuffix = isQuestionGroup ? "_GRP" : "";

		// We generate the question
		Question newQuestion = new Question("QUE_" + be.getCode() + questionSuffix,
				be.getName(), attribute, false);

		// generate and return the ask
		return new Ask(newQuestion, be.getCode(), be.getCode(), false, 1.0, false,
				false, true);
	}

	/**
	* Create a question for a BaseEntity.
	* 
	* @param be
	* @param isQuestionGroup
	* @param serviceToken
	* @param sourceAlias
	* @param targetAlias
	* @return
	 */
	public static Ask createQuestionForBaseEntity2(BaseEntity be, Boolean isQuestionGroup, GennyToken serviceToken,
			final String sourceAlias, final String targetAlias) {

		// create attribute code using isQuestionGroup and fetch attribute
		String attributeCode = isQuestionGroup ? "QQQ_QUESTION_GROUP_INPUT" : "PRI_EVENT";
		Attribute attribute = QwandaUtils.getAttribute(attributeCode);

		// create temporary attribute if not fetched
		if (attribute == null) {
			log.error("Attribute DOES NOT EXIST! " + attributeCode + " creating temp");
			attribute = new Attribute(attributeCode, attributeCode, new DataType("DTT_THEME"));
		}

		// generate the question
		Question newQuestion = new Question(be.getCode(), be.getName(), attribute, false);

		// generate question and return
		Ask ask = new Ask(newQuestion, (sourceAlias != null ? sourceAlias : be.getCode()),
				(targetAlias != null ? targetAlias : be.getCode()), false, 1.0, false, false, true);
		ask.setRealm(serviceToken.getRealm());

		return ask;

	}

	/**
	* Create a virtual link between a {@link BaseEntity} and an {@link Ask}.
	*
	* @param source
	* @param ask
	* @param linkCode
	* @param linkValue
	* @return
	 */
	public static BaseEntity createVirtualLink(BaseEntity source, Ask ask, String linkCode, String linkValue) {

		if (source != null) {

			Set<EntityQuestion> entityQuestionList = source.getQuestions();

			Link link = new Link(source.getCode(), ask.getQuestion().getCode(), linkCode,
					linkValue);
			link.setWeight(ask.getWeight());
			EntityQuestion ee = new EntityQuestion(link);
			entityQuestionList.add(ee);

			source.setQuestions(entityQuestionList);
		}
		return source;
	}

	/**
	* Fetch a question from the cache using the code. This will use 
	* the database as a backup if not found in cache.
	*
	* @param code
	* @param userToken
	* @return
	 */
	static public Question getQuestion(String code, GennyToken userToken) {

		String realm = userToken.getRealm();

		// fetch from cache
		Question question = CacheUtils.getObject(realm, code, Question.class);

		if (question != null) {
			return question;
		}

		log.warn("No Question in cache for " + code + ", trying to grab from database...");

		// fetch from DB
		question = DatabaseUtils.fetchQuestion(realm, code);

		if (question == null) {
			log.error("No Question found in database for " + code + " either!!!!");
		}

		return question;
	}

	/**
	* Fetch linked children entities of a given {@link BaseEntity} using its code.
	*
	* @param beCode
	* @param level
	* @param token
	* @return
	 */
	public static List<BaseEntity> getChildren(String beCode, Integer level, String token) {

		if (level == 0) {
			return null;
		}

		GennyToken gennyToken = new GennyToken(token);
		List<BaseEntity> result = new ArrayList<BaseEntity>();

		BaseEntity parent = CacheUtils.getObject(gennyToken.getRealm(),
				beCode, BaseEntity.class);

		if (parent != null) {
			for (EntityEntity ee : parent.getLinks()) {
				String childCode = ee.getLink().getTargetCode();
				BaseEntity child = CacheUtils.getObject(gennyToken.getRealm(), childCode,
						BaseEntity.class);
				result.add(child);
			}
		}

		return result;
	}
}
