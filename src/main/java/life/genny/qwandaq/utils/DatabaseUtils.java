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
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;

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
	 * Fetch all attributes from the database
	 *
	 * @param realm
	 * @return All {@link Attribute} objects found in the DB
	 */
	@Transactional
	public static List<Attribute> fetchAttributes(String realm) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {
			return entityManager
					.createQuery("SELECT a FROM Attribute a where a.realm=:realmStr and a.name not like 'App\\_%'",
							Attribute.class)
					.setParameter("realmStr", realm)
					.getResultList();


		} catch (NoResultException e) {
			log.error("No attributes found from DB search");
			log.error(e.getStackTrace());
		}
		return null;
	}

	/**
	* Fetch a list of {@link BaseEntity} types from the database using a realm.
	* 
	* @param realm
	* @return
	 */
	@Transactional
	public static List<BaseEntity> fetchBaseEntitys(String realm) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {

			return entityManager
					.createQuery("SELECT * FROM BaseEntity where realm=:realmStr", BaseEntity.class)
					.setParameter("realmStr", realm)
					.getResultList();

		} catch (NoResultException e) {
			log.error("No BaseEntity found in DB for realm " + realm);
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
	public static BaseEntity fetchBaseEntity(String realm, String code) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {

			return entityManager
					.createQuery("SELECT * FROM BaseEntity where realm=:realmStr and code = :code", BaseEntity.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.error("No BaseEntity found in DB for " + code);
		}
		return null;
	}

	/**
	 * Save a {@link BaseEntity} to the database.
	 *
	 * @param entity A {@link BaseEntity} object to save
	 */
	@Transactional
	public static void saveBaseEntity(BaseEntity entity) {

		log.info("Saving BaseEntity " + entity.getCode());

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return;
		}

		try {
			entityManager.persist(entity);
			log.info("Successfully saved BaseEntity " + entity.getCode());

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

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return;
		}

		try {
			entityManager.persist(attribute);
			log.info("Successfully saved attribute " + attribute.getCode());

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
	public static void deleteAttribute(String code) {

		log.info("Deleting Attribute " + code);

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return;
		}

		try {
			Query q = entityManager.createQuery("DELETE Attribute WHERE code = :code");
			q.setParameter("code", code);
			q.executeUpdate();

			log.info("Successfully deleted attribute " + code);

		} catch (Exception e) {
			log.error(e);
		}
	}

	@Transactional
	public static List<Question> fetchQuestions(String realm) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {

			return entityManager
					.createQuery("SELECT * FROM Question WHERE realm=:realmStr", Question.class)
					.setParameter("realmStr", realm)
					.getResultList();

		} catch (NoResultException e) {
			log.error("No Question found in DB for realm " + realm);
		}
		return null;
	}

	@Transactional
	public static Question fetchQuestion(String realm, String code) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {

			return entityManager
					.createQuery("SELECT * FROM Question WHERE realm=:realmStr AND code = :code", Question.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.error("No Question found in DB for " + code);
		}
		return null;
	}

	@Transactional
	public static List<QuestionQuestion> fetchQuestionQuestions(String realm) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {

			return entityManager
					.createQuery("SELECT * FROM QuestionQuestion WHERE realm=:realmStr", QuestionQuestion.class)
					.setParameter("realmStr", realm)
					.getResultList();

		} catch (NoResultException e) {
			log.error("No QuestionQuestion found in DB for realm " + realm);
		}
		return null;
	}

	@Transactional
	public static QuestionQuestion fetchQuestionQuestion(String realm, String sourceCode, String targetCode) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {

			return entityManager
					.createQuery("SELECT * FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode AND targetCode = :targetCode"
							, QuestionQuestion.class)
					.setParameter("realmStr", realm)
					.setParameter("sourceCode", sourceCode)
					.setParameter("targetCode", targetCode)
					.getSingleResult();

		} catch (NoResultException e) {
			log.error("No QuestionQuestion found in DB for " + sourceCode + ":" + targetCode);
		}
		return null;
	}

	@Transactional
	public static List<QuestionQuestion> fetchQuestionQuestionsBySource(String realm, String sourceCode) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {

			return entityManager
					.createQuery("SELECT * FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode"
							, QuestionQuestion.class)
					.setParameter("realmStr", realm)
					.setParameter("sourceCode", sourceCode)
					.getResultList();

		} catch (NoResultException e) {
			log.error("No QuestionQuestion found in DB for " + sourceCode);
		}
		return null;
	}

}
