package life.genny.qwandaq.utils;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import life.genny.qwandaq.Context;
import life.genny.qwandaq.ContextList;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
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
import life.genny.qwandaq.validation.Validation;

@RegisterForReflection
public class QuestionUtils implements Serializable {

	static final Logger log = Logger.getLogger(QuestionUtils.class);

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
	public static Boolean doesQuestionGroupExist(String sourceCode, String targetCode,
			final String questionCode,
			BaseEntityUtils beUtils) {

		// we grab the question group using the questionCode
		QDataAskMessage questions = getAsks(sourceCode, targetCode, questionCode, beUtils);

		// we check if the question payload is not empty
		if (questions != null) {

			// we check if the question group contains at least one question
			if (questions.getItems() != null && questions.getItems().length > 0) {

				Ask firstQuestion = questions.getItems()[0];

				// we check if the question is a question group and contains at least one
				// question
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
	public static JsonObject toJson(String string) {
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
	public static void setCachedQuestionsRecursively(Ask ask, BaseEntityUtils beUtils) {

		// call recursively if ask represents a question group
		if (ask.getAttributeCode().equals("QQQ_QUESTION_GROUP")) {

			for (Ask childAsk : ask.getChildAsks()) {
				setCachedQuestionsRecursively(childAsk, beUtils);
			}

		} else {

			// otherwise we fetch the question and update the ask
			Question question = ask.getQuestion();

			Question cachedQuestion = CacheUtils.getObject(beUtils.getRealm(), question.getCode(), Question.class);

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

	
	/** 
	 * @param rootQuestion
	 * @param source
	 * @param target
	 * @param beUtils
	 * @return List<Ask>
	 */
	public static List<Ask> findAsks2(final Question rootQuestion, final BaseEntity source,
			final BaseEntity target,
			BaseEntityUtils beUtils) {
		return findAsks2(rootQuestion, source, target, false, false, false, false, beUtils);
	}

	
	/** 
	 * @param rootQuestion
	 * @param source
	 * @param target
	 * @param childQuestionIsMandatory
	 * @param childQuestionIsReadonly
	 * @param childQuestionIsFormTrigger
	 * @param childQuestionIsCreateOnTrigger
	 * @param beUtils
	 * @return List<Ask>
	 */
	public static List<Ask> findAsks2(final Question rootQuestion, final BaseEntity source,
			final BaseEntity target,
			Boolean childQuestionIsMandatory, Boolean childQuestionIsReadonly, Boolean childQuestionIsFormTrigger,
			Boolean childQuestionIsCreateOnTrigger, BaseEntityUtils beUtils) {
		if (rootQuestion == null) {
			log.error(
					"rootQuestion for findAsks2 is null - source=" + source.getCode() + ": target " + target.getCode());
			return new ArrayList<Ask>();
		}
		List<Ask> asks = new ArrayList<>();
		Boolean mandatory = rootQuestion.getMandatory() || childQuestionIsMandatory;
		Boolean readonly = rootQuestion.getReadonly() || childQuestionIsReadonly;
		Ask ask = null;
		// check if this already exists?
		List<Ask> myAsks = DatabaseUtils.findAsksByQuestionCode(beUtils.getRealm(), rootQuestion.getCode(), source.getCode(), target.getCode());
		if (!(myAsks == null || myAsks.isEmpty())) {
			ask = myAsks.get(0);
			ask.setMandatory(mandatory);
			ask.setReadonly(readonly);
			ask.setFormTrigger(childQuestionIsFormTrigger);
			ask.setCreateOnTrigger(childQuestionIsCreateOnTrigger);

		} else {
			ask = new Ask(rootQuestion, source.getCode(), target.getCode(), mandatory, 1.0, false, false, readonly);
			ask.setCreateOnTrigger(childQuestionIsMandatory);
			ask.setFormTrigger(childQuestionIsFormTrigger);
			ask.setRealm(beUtils.getRealm());

			// Now merge ask name if required
			ask = performMerge(ask);

			// ask = upsert(ask);
		}
		// create one
		if (rootQuestion.getAttributeCode().startsWith(Question.QUESTION_GROUP_ATTRIBUTE_CODE)) {
			// Recurse!
			List<QuestionQuestion> qqList = new ArrayList<>(rootQuestion.getChildQuestions());
			Collections.sort(qqList); // sort by priority
			List<Ask> childAsks = new ArrayList<>();
			for (QuestionQuestion qq : qqList) {
				String qCode = qq.getPk().getTargetCode();
				log.info(qq.getPk().getSourceCode() + " -> Child Question -> " + qCode);
				Question childQuestion = DatabaseUtils.findQuestionByCode(beUtils.getRealm(), qCode);
				// Grab whatever icon the QuestionQuestion has set
				childQuestion.setIcon(qq.getIcon());
				if (childQuestion != null) {
					List<Ask> askChildren = null;
					try {
						askChildren = findAsks2(childQuestion, source, target, qq.getMandatory(),
								qq.getReadonly(),
								qq.getFormTrigger(), qq.getCreateOnTrigger(), beUtils);
						for (Ask child : askChildren) {
							child.setQuestion(childQuestion);
							child.setHidden(qq.getHidden());
							child.setDisabled(qq.getDisabled());
							child.setReadonly(qq.getReadonly());
						}
					} catch (Exception e) {
						log.error("Error with QuestionQuestion: " + rootQuestion.getCode());
						log.error("Problem Question: " + childQuestion.getCode());
						e.printStackTrace();
					}
					childAsks.addAll(askChildren);
				}
			}
			Ask[] asksArray = childAsks.toArray(new Ask[0]);
			ask.setChildAsks(asksArray);

			ask.setRealm(beUtils.getRealm());
			// ask.setChildAsks(childAsks);

			// ask = upsert(ask); // save
		}

		asks.add(ask);
		return asks;
	}

	
	/** 
	 * @param rootQuestion
	 * @param source
	 * @param target
	 * @param beUtils
	 * @return List<Ask>
	 */
	public static List<Ask> createAsksByQuestion2(final Question rootQuestion, final BaseEntity source,
			final BaseEntity target, BaseEntityUtils beUtils) {
		List<Ask> asks = findAsks2(rootQuestion, source, target, beUtils);
		return asks;
	}

	
	/** 
	 * @param questionCode
	 * @param sourceCode
	 * @param targetCode
	 * @param beUtils
	 * @return List<Ask>
	 */
	public static List<Ask> createAsksByQuestionCode2(final String questionCode, final String sourceCode,
			final String targetCode, BaseEntityUtils beUtils) {

		Question rootQuestion = DatabaseUtils.findQuestionByCode(beUtils.getRealm(), questionCode);
		BaseEntity source = null;
		BaseEntity target = null;

		if ("PER_SOURCE".equals(sourceCode) && "PER_TARGET".equals(targetCode)) {
			source = new BaseEntity(sourceCode, "SourceCode");
			target = new BaseEntity(targetCode, "TargetCode");
		} else {
			source = beUtils.getBaseEntityByCode(sourceCode);
			target = beUtils.getBaseEntityByCode(targetCode);
		}

		return createAsksByQuestion2(rootQuestion, source, target, beUtils);
	}

	
	/** 
	 * @param sourceCode
	 * @param targetCode
	 * @param questionCode
	 * @param beUtils
	 * @return QDataAskMessage
	 */
	public static QDataAskMessage getDirectAsks(String sourceCode, String targetCode, String questionCode,
			BaseEntityUtils beUtils) {
		List<Ask> asks = null;

		asks = createAsksByQuestionCode2(questionCode, sourceCode, targetCode, beUtils);
		log.debug("Number of asks=" + asks.size());
		log.debug("Number of asks=" + asks);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));

		return askMsgs;
	}

	
	/** 
	 * @param sourceCode
	 * @param targetCode
	 * @param questionCode
	 * @param beUtils
	 * @return QDataAskMessage
	 */
	public static QDataAskMessage getAsks(String sourceCode, String targetCode, 
			String questionCode, BaseEntityUtils beUtils) {

		// List<Ask> asks = null;
		// asks = createAsksByQuestionCode2(questionCode, sourceCode,
		// targetCode, beUtils);
		// log.info("Number of asks=" + asks.size());
		// log.info("Number of asks=" + asks);
		// final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new
		// Ask[0]));
		// askMsgs.setToken(beUtils.getToken());
		// return askMsgs;

		HttpResponse<String> response = HttpUtils.get(GennySettings.qwandaServiceUrl +
				"/qwanda/baseentitys/"
				+ sourceCode + "/asks2/" + questionCode + "/" + targetCode,
				beUtils.getGennyToken().getToken());

		String json = response.body();

		if (json != null) {
			if (!json.contains("<title>Error")) {
				QDataAskMessage msg = jsonb.fromJson(json, QDataAskMessage.class);

				if (true) {
					// Identify all the attributeCodes and build up a working active Set
					Set<String> activeAttributeCodes = new HashSet<String>();
					for (Ask ask : msg.getItems()) {
						activeAttributeCodes.addAll(getAttributeCodes(ask));

						// Go down through the child asks and get cached questions
						setCachedQuestionsRecursively(ask, beUtils);
					}
					// Now fetch the set from cache and add it....
					Type type = new TypeToken<Set<String>>() {
					}.getType();

					Set<String> activeAttributesSet = CacheUtils.getObject(beUtils.getRealm(),
							"ACTIVE_ATTRIBUTES",
							type);

					if (activeAttributesSet == null) {
						activeAttributesSet = new HashSet<String>();
					}
					activeAttributesSet.addAll(activeAttributeCodes);

					CacheUtils.putObject(beUtils.getRealm(), "ACTIVE_ATTRIBUTES",
							activeAttributesSet);

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

	
	/** 
	 * @param sourceCode
	 * @param targetCode
	 * @param questionCode
	 * @param beUtils
	 * @return QwandaMessage
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static QwandaMessage getQuestions(String sourceCode, String targetCode,
			String questionCode,
			BaseEntityUtils beUtils)
			throws ClientProtocolException, IOException {
		return getQuestions(sourceCode, targetCode, questionCode, beUtils, null, true);
	}

	/**
	 * Get Questions for a given souce, target and code.
	 *
	 * @param sourceCode
	 * @param targetCode
	 * @param questionCode
	 * @param token
	 * @param stakeholderCode
	 * @param pushSelection
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static QwandaMessage getQuestions(String sourceCode, String targetCode,
			String questionCode,
			BaseEntityUtils beUtils,
			String stakeholderCode, Boolean pushSelection) throws ClientProtocolException, IOException {

		QBulkMessage bulk = new QBulkMessage();
		QwandaMessage qwandaMessage = new QwandaMessage();

		Instant start = Instant.now();

		// get the ask data
		QDataAskMessage questions = getAsks(sourceCode, targetCode, questionCode, beUtils);

		Instant middle = Instant.now();
		log.info("getAsks duration = " + Duration.between(start, middle).toMillis() + " ms");

		if (questions == null) {
			log.error("Questions Msg is null " + sourceCode + "/asks2/" + questionCode + "/" + targetCode);
			return null;
		}

		// if we have the questions, loop through the asks and send required data to
		// front end
		Ask[] asks = questions.getItems();
		if (asks != null && pushSelection) {
			QBulkMessage askData = sendAsksRequiredData(asks, beUtils, stakeholderCode);
			for (QDataBaseEntityMessage message : askData.getMessages()) {
				bulk.add(message);
			}
		}

		Instant end = Instant.now();
		log.info("sendAsksRequiredData duration = " + Duration.between(middle, end).toMillis() + " ms");

		qwandaMessage.askData = bulk;
		qwandaMessage.asks = questions;

		return qwandaMessage;
	}

	
	/** 
	 * @param sourceCode
	 * @param targetCode
	 * @param questionGroupCode
	 * @param beUtils
	 * @return QwandaMessage
	 */
	public static QwandaMessage askQuestions(final String sourceCode,
			final String targetCode,
			final String questionGroupCode, BaseEntityUtils beUtils) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, beUtils, null,
				true);
	}

	
	/** 
	 * @param sourceCode
	 * @param targetCode
	 * @param questionGroupCode
	 * @param beUtils
	 * @param stakeholderCode
	 * @return QwandaMessage
	 */
	public static QwandaMessage askQuestions(final String sourceCode,
			final String targetCode,
			final String questionGroupCode, BaseEntityUtils beUtils, String stakeholderCode) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, beUtils,
				stakeholderCode, true);
	}

	
	/** 
	 * @param sourceCode
	 * @param targetCode
	 * @param questionGroupCode
	 * @param pushSelection
	 * @param beUtils
	 * @return QwandaMessage
	 */
	public static QwandaMessage askQuestions(final String sourceCode,
			final String targetCode,
			final String questionGroupCode, Boolean pushSelection, BaseEntityUtils beUtils) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, beUtils, null,
				pushSelection);
	}

	
	/** 
	 * @param sourceCode
	 * @param targetCode
	 * @param questionGroupCode
	 * @param beUtils
	 * @param pushSelection
	 * @return QwandaMessage
	 */
	public static QwandaMessage askQuestions(final String sourceCode,
			final String targetCode,
			final String questionGroupCode, BaseEntityUtils beUtils, Boolean pushSelection) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, beUtils, null,
				pushSelection);
	}

	
	/** 
	 * @param sourceCode
	 * @param targetCode
	 * @param questionGroupCode
	 * @param beUtils
	 * @param stakeholderCode
	 * @param pushSelection
	 * @return QwandaMessage
	 */
	public static QwandaMessage askQuestions(final String sourceCode,
			final String targetCode,
			final String questionGroupCode, final BaseEntityUtils beUtils, final String stakeholderCode,
			final Boolean pushSelection) {

		try {

			// if sending the questions worked, we ask user
			return getQuestions(sourceCode, targetCode, questionGroupCode, beUtils,
					stakeholderCode, pushSelection);

		} catch (Exception e) {
			log.info("Ask questions exception: ");
			e.printStackTrace();
			return null;
		}
	}

	
	/** 
	 * @param questions
	 * @param questionAttributeCode
	 * @param customTemporaryQuestion
	 * @return QwandaMessage
	 */
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
	private static QBulkMessage sendAsksRequiredData(Ask[] asks, BaseEntityUtils beUtils, String stakeholderCode) {

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
										BaseEntity parent = CacheUtils.getObject(beUtils.getRealm(),
												validationString, BaseEntity.class);

										// we have a GRP. we push it to FE
										List<BaseEntity> bes = getChildren(validationString, 2, beUtils);
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

				QBulkMessage newBulk = sendAsksRequiredData(childAsks, beUtils, stakeholderCode);

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
	public static Ask createQuestionForBaseEntity(BaseEntity be, Boolean isQuestionGroup, BaseEntityUtils beUtils) {

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
	 * @param sourceAlias
	 * @param targetAlias
	 * @return
	 */
	public static Ask createQuestionForBaseEntity2(BaseEntity be, Boolean isQuestionGroup, BaseEntityUtils beUtils,
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
		ask.setRealm(beUtils.getRealm());

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
	static public Question getQuestion(String code, BaseEntityUtils beUtils) {

		String realm = beUtils.getRealm();

		// fetch from cache
		Question question = CacheUtils.getObject(realm, code, Question.class);

		if (question != null) {
			return question;
		}

		log.warn("No Question in cache for " + code + ", trying to grab from database...");

		// fetch from DB
		question = DatabaseUtils.findQuestionByCode(realm, code);

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
	public static List<BaseEntity> getChildren(String beCode, Integer level, BaseEntityUtils beUtils) {

		if (level == 0) {
			return null;
		}

		List<BaseEntity> result = new ArrayList<>();

		BaseEntity parent = CacheUtils.getObject(beUtils.getRealm(),
				beCode, BaseEntity.class);

		if (parent != null) {
			for (EntityEntity ee : parent.getLinks()) {
				String childCode = ee.getLink().getTargetCode();
				BaseEntity child = CacheUtils.getObject(beUtils.getRealm(), childCode,
						BaseEntity.class);
				result.add(child);
			}
		}

		return result;
	}

	/**
	* Perform a merge of ask data.
	*
	* @param ask
	* @return
	 */
	private static Ask performMerge(Ask ask) {
		if (ask.getName().contains("{{")) {
			// now merge in data
			String name = ask.getName();

			Map<String, Object> templateEntityMap = new HashMap<>();
			ContextList contexts = ask.getContextList();
			for (Context context : contexts.getContexts()) {
				BaseEntity be = context.getEntity();
				templateEntityMap.put(context.getName(), be);
			}
			String mergedName = MergeUtils.merge(name, templateEntityMap);
			ask.setName(mergedName);
		}
		return ask;

	}

}
