package life.genny.qwandaq.utils;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.QuestionQuestionId;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.validation.Validation;

@RegisterForReflection
@ApplicationScoped
public class DatabaseUtils {

	static final Logger log = Logger.getLogger(DatabaseUtils.class);
	static Jsonb jsonb = JsonbBuilder.create();
	static EntityManager entityManager;

	/**
	 * Initialise the EntityManager interface
	 *
	 * @param entityManager
	 */
	public static void init(EntityManager em) {
		entityManager = em;
	}

	/**
	 * Check if entityManager is present.
	 */
	public static void checkEntityManager() {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return;
		}
	}

	/**
	* Fetch Validations from the database using page size and num.
	* If pageSize and pageNumber are both null, all results will be returned at once.
	* 
	* @param realm
	* @param pageSize
	* @param pageNumber
	* @return
	 */
	@Transactional
	public static List<Validation> findValidations(String realm, Integer pageSize, Integer pageNumber) {

		checkEntityManager();

		try {
			Query query = entityManager
					.createQuery("FROM Validation WHERE realm=:realmStr", Validation.class)
					.setParameter("realmStr", realm);

			if (pageNumber != null && pageSize != null) {
				query = query.setFirstResult((pageNumber-1) * pageSize)
					.setMaxResults(pageSize);
			}
				
			return query.getResultList();

		} catch (NoResultException e) {
			log.error("No attributes found from DB search");
			log.error(e.getStackTrace());
		}

		return null;
	}

	/**
	* Fetch Attributes from the database using page size and num.
	* If pageSize and pageNumber are both null, all results will be returned at once.
	*
	* @param realm
	* @param pageSize
	* @param pageNumber
	* @return
	 */
	@Transactional
	public static List<Attribute> findAttributes(String realm, Integer pageSize, Integer pageNumber) {

		checkEntityManager();

		try {
			Query query = entityManager
					.createQuery("FROM Attribute WHERE realm=:realmStr AND name not like 'App\\_%'",
							Attribute.class)
					.setParameter("realmStr", realm);

			if (pageNumber != null && pageSize != null) {
				query = query.setFirstResult((pageNumber-1) * pageSize)
					.setMaxResults(pageSize);
			} else {
				log.info("Fetching all Attributes (unset pageNumber or pageSize)");
			}
				
			return query.getResultList();

		} catch (NoResultException e) {
			log.error("No attributes found from DB search");
			log.error(e.getStackTrace());
		}

		return null;
	}

	/**
	* Fetch a list of {@link BaseEntity} types from the database using a realm.
	* If pageSize and pageNumber are both null, all results will be returned at once.
	* 
	* @param realm
	* @return
	 */
	@Transactional
	public static List<BaseEntity> findBaseEntitys(String realm, Integer pageSize, Integer pageNumber) {

		checkEntityManager();

		try {

			Query query = entityManager
					.createQuery("FROM BaseEntity WHERE realm=:realmStr", BaseEntity.class)
					.setParameter("realmStr", realm);

			if (pageNumber != null && pageSize != null) {
				query = query.setFirstResult((pageNumber-1) * pageSize)
					.setMaxResults(pageSize);
			}
				
			return query.getResultList();

		} catch (NoResultException e) {
			log.errorv("No BaseEntitys found in DB for realm {}", realm);
		}

		return null;
	}

	/**
	* Fetch a list of {@link Question} types from the database using a realm, page size and page number.
	* If pageSize and pageNumber are both null, all results will be returned at once.
	* 
	* @param realm
	* @return
	 */
	@Transactional
	public static List<Question> findQuestions(String realm, Integer pageSize, Integer pageNumber) {

		checkEntityManager();

		try {

			Query query = entityManager
					.createQuery("FROM Question WHERE realm=:realmStr", Question.class)
					.setParameter("realmStr", realm);

			if (pageNumber != null && pageSize != null) {
				query = query.setFirstResult((pageNumber-1) * pageSize)
					.setMaxResults(pageSize);
			}
				
			return query.getResultList();

		} catch (NoResultException e) {
			log.errorv("No Question found in DB for realm {}", realm);
		}

		return null;
	}

	/**
	* Fetch a list of {@link QuestionQuestion} types from the database using a realm, page size and page number.
	* If pageSize and pageNumber are both null, all results will be returned at once.
	* 
	* @param realm
	* @param pageSize
	* @param pageNumber
	* @return
	 */
	@Transactional
	public static List<QuestionQuestion> findQuestionQuestions(String realm, Integer pageSize, Integer pageNumber) {

		checkEntityManager();

		try {

			Query query = entityManager
					.createQuery("FROM QuestionQuestion WHERE realm=:realmStr", QuestionQuestion.class)
					.setParameter("realmStr", realm);

			if (pageNumber != null && pageSize != null) {
				query = query.setFirstResult((pageNumber-1) * pageSize)
					.setMaxResults(pageSize);
			}
				
			return query.getResultList();

		} catch (NoResultException e) {
			log.errorv("No QuestionQuestion found in DB for realm {}", realm);
		}

		return null;
	}


	/**
	* Grab a Validation from the database using a code and a realm.
	*
	* @param code
	* @param realm
	* @return
	 */
	@Transactional
	public static Validation findValidationByCode(String realm, String code) {

		checkEntityManager();

		try {

			return entityManager
				.createQuery("FROM Validation WHERE realm=:realmStr AND code=:code",
						Validation.class)
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.getSingleResult();

		} catch (NoResultException e) {
			log.errorv("No Validation found in DB for {} in realm {}", code, realm);
			log.error(e.getStackTrace());
		}
		return null;
	}

	/**
	* Fetch an Attribute from the database using a realm and a code.
	*
	* @param realm
	* @param code
	* @return
	 */
	@Transactional
	public static Attribute findAttributeByCode(String realm, String code) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery("FROM Attribute WHERE realm=:realmStr AND code =:code", Attribute.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.errorv("No Attribute found in DB for {} in realm {}", code, realm);
		}
		return null;
	}

	/**
	 * Fetch A {@link BaseEntity} from the database using the entity code.
	 *
	 * @param realm The realm that the {@link BaseEntity} is saved under
	 * @param code  The code of the {@link BaseEntity} to fetch
	 * @return The corresponding BaseEntity, or null if not found.
	 */
	@Transactional
	public static BaseEntity findBaseEntityByCode(String realm, String code) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery("FROM BaseEntity WHERE realm=:realmStr AND code=:code", BaseEntity.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.errorv("No BaseEntity found in DB for {} in realm {}", code, realm);
		}
		return null;
	}

	/**
	 * Fetch A {@link Question} from the database using the question code.
	 *
	 * 
	 * @param realm
	 * @param code
	 * @return
	 */
	@Transactional
	public static Question findQuestionByCode(String realm, String code) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery("FROM Question WHERE realm=:realmStr AND code=:code", Question.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.errorv("No Question found in DB for {} in realm {}", code, realm);
		}
		return null;
	}

	/**
	* Find a QuestionQuestion using a realm, a sourceCode and a targetCode.
	*
	* @param realm
	* @param sourceCode
	* @param targetCode
	* @return
	 */
	@Transactional
	public static QuestionQuestion findQuestionQuestionBySourceAndTarget(String realm, String sourceCode, String targetCode) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery("FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode AND targetCode = :targetCode"
							, QuestionQuestion.class)
					.setParameter("realmStr", realm)
					.setParameter("sourceCode", sourceCode)
					.setParameter("targetCode", targetCode)
					.getSingleResult();

		} catch (NoResultException e) {
			log.errorv("No QuestionQuestion found in DB for {}:{} in realm {}", sourceCode, targetCode, realm);
		}
		return null;
	}

	/**
	* Find a list of QuestionQuestions using a realm and a sourceCode
	*
	* @param realm
	* @param sourceCode
	* @return
	 */
	@Transactional
	public static List<QuestionQuestion> findQuestionQuestionsBySourceCode(String realm, String sourceCode) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery("FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode",
							QuestionQuestion.class)
					.setParameter("realmStr", realm)
					.setParameter("sourceCode", sourceCode)
					.getResultList();

		} catch (NoResultException e) {
			log.errorv("No QuestionQuestion found in DB for {}", sourceCode);
		}
		return null;
	}

	/**
	* Find a list of Asks using question code, sourceCode and targetCode.
	*
	* @param realm
	* @param questionCode
	* @param sourceCode
	* @param targetCode
	* @return
	 */
	@Transactional
	public static List<Ask> findAsksByQuestionCode(String realm, String questionCode, String sourceCode, String targetCode) {

		checkEntityManager();

		try {
			return entityManager
				.createQuery("FROM Ask WHERE realm=:realmStr AND sourceCode=:sourceCode"
						+ " AND targetCode=:targetCode AND questionCode=:questionCode", Ask.class)
					.setParameter("questionCode", questionCode)
					.setParameter("sourceCode", sourceCode)
					.setParameter("realmStr", realm)
					.setParameter("targetCode", targetCode)
					.getResultList();

		} catch (NoResultException e) {
			log.errorv("No Asks found in DB for {}:{}:{} in realm {}", questionCode, sourceCode, targetCode, realm);
		}
		return null;
	}

	/**
	 * Save a {@link Validation} to the database.
	 *
	 * @param validation A {@link Validation} object to save
	 */
	@Transactional
	public static void saveValidation(Validation validation) {

		log.info("Saving Validation " + validation.getCode());

		checkEntityManager();

		Validation existingValidation = findValidationByCode(validation.getRealm(), validation.getCode());

		try {

			if (existingValidation == null) {
				entityManager.persist(validation);
			} else {
				entityManager.merge(validation);
			}

			log.info("Successfully saved Validation " + validation.getCode());

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Save an {@link Attribute} to the database.
	 *
	 * @param attribute An {@link Attribute} object to save
	 */
	@Transactional
	public static void saveAttribute(Attribute attribute) {

		log.info("Saving Attribute " + attribute.getCode());

		checkEntityManager();

		Attribute existingAttribute = findAttributeByCode(attribute.getRealm(), attribute.getCode());

		try {

			if (existingAttribute == null) {
				entityManager.persist(attribute);
			} else {
				entityManager.merge(attribute);
			}

			log.info("Successfully saved Attribute " + attribute.getCode());

		} catch (Exception e) {
			log.error(e);
		}
	}


	/**
	 * Save a {@link BaseEntity} to the database.
	 *
	 * @param entity A {@link BaseEntity} object to save
	 */
	@Transactional
	public static void saveBaseEntity(BaseEntity entity) {

		log.info("Saving BaseEntity " + entity.getCode());

		checkEntityManager();

		BaseEntity existingEntity = findBaseEntityByCode(entity.getRealm(), entity.getCode());

		try {

			if (existingEntity == null) {
				entityManager.persist(entity);
			} else {
				entityManager.merge(entity);
			}

			log.info("Successfully saved BaseEntity " + entity.getCode());

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	* Save a {@link Question} to the database.
	*
	* @param question A {@link Question} object to save
	 */
	@Transactional
	public static void saveQuestion(Question question) {

		log.info("Saving Question " + question.getCode());

		checkEntityManager();

		Question existingQuestion = findQuestionByCode(question.getRealm(), question.getCode());

		try {

			if (existingQuestion == null) {
				entityManager.persist(question);
			} else {
				entityManager.merge(question);
			}

			log.info("Successfully saved Question " + question.getCode());

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	* Save a {@link QuestionQuestion} to the database.
	*
	* @param questionQuestion A {@link QuestionQuestion} object to save
	 */
	@Transactional
	public static void saveQuestionQuestion(QuestionQuestion questionQuestion) {

		QuestionQuestionId pk = questionQuestion.getPk();

		log.infov("Saving QuestionQuestion {}:{}", pk.getSourceCode(), pk.getTargetCode());

		checkEntityManager();

		QuestionQuestion existingQuestionQuestion = findQuestionQuestionBySourceAndTarget(
				questionQuestion.getRealm(),
				pk.getSourceCode(),
				pk.getTargetCode()
			);

		try {

			if (existingQuestionQuestion == null) {
				entityManager.persist(questionQuestion);
			} else {
				entityManager.merge(questionQuestion);
			}

			log.infov("Successfully saved QuestionQuestion {}:{}", pk.getSourceCode(), pk.getTargetCode());

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Delete a Validation from the database.
	 *
	 * @param code Code of the Validation to delete.
	 */
	@Transactional
	public static void deleteValidation(String realm, String code) {

		log.info("Deleting Validation " + code);

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE Validation WHERE realm=:realmStr AND code=:code")
					.setParameter("realmStr", realm)
					.setParameter("code", code);

			q.executeUpdate();
			log.infov("Successfully deleted Validation {} in realm {}", code, realm);

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Delete an atttribute from the database.
	 *
	 * @param code Code of the attribute to delete.
	 */
	@Transactional
	public static void deleteAttribute(String realm, String code) {

		log.info("Deleting Attribute " + code);

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE Attribute WHERE realm=:realmStr AND code=:code")
					.setParameter("realmStr", realm)
					.setParameter("code", code);

			q.executeUpdate();
			log.infov("Successfully deleted Attribute {} in realm {}", code, realm);

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Delete a BaseEntity from the database.
	 *
	 * @param code Code of the BaseEntity to delete.
	 */
	@Transactional
	public static void deleteBaseEntity(String realm, String code) {

		log.info("Deleting BaseEntity " + code);

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE BaseEntity WHERE realm=:realmStr AND code=:code")
					.setParameter("realmStr", realm)
					.setParameter("code", code);

			q.executeUpdate();
			log.infov("Successfully deleted BaseEntity {} in realm {}", code, realm);

		} catch (Exception e) {
			log.error(e);
		}
	}


	/**
	 * Delete a Question from the database.
	 *
	 * @param code Code of the Question to delete.
	 */
	@Transactional
	public static void deleteQuestion(String realm, String code) {

		log.info("Deleting Question " + code);

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE Question WHERE realm=:realmStr AND code=:code")
					.setParameter("realmStr", realm)
					.setParameter("code", code);

			q.executeUpdate();
			log.infov("Successfully deleted Question {} in realm {}", code, realm);

		} catch (Exception e) {
			log.error(e);
		}
	}


	/**
	 * Delete a QuestionQuestion from the database.
	 * 
	 * @param realm
	 * @param sourceCode
	 * @param targetCode
	 */
	@Transactional
	public static void deleteQuestionQuestion(String realm, String sourceCode, String targetCode) {

		log.infov("Deleting QuestionQuestion {}:{} in realm {}", sourceCode, targetCode, realm);

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE QuestionQuestion WHERE realm=:realmStr AND sourceCode=:sourceCode AND targetCode=:targetCode")
					.setParameter("realmStr", realm)
					.setParameter("sourceCode", sourceCode)
					.setParameter("targetCode", targetCode);

			q.executeUpdate();
			log.infov("Successfully deleted QuestionQuestion {}:{} in realm {}", sourceCode, targetCode, realm);

		} catch (Exception e) {
			log.error(e);
		}
	}

}
