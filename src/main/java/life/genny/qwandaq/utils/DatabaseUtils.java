package life.genny.qwandaq.utils;

import java.io.Serializable;
import java.util.List;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;

public class DatabaseUtils {

	private static final Logger log = Logger.getLogger(DatabaseUtils.class);
	private static Jsonb jsonb = JsonbBuilder.create();
	private static EntityManager entityManager;

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
	 * @return All {@link Attribute} objects found in the DB
	 */
	@Transactional
	public static List<Attribute> fetchAttributes(String realm) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {
			log.info("about to query");
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
	public static Question fetchQuestion(String realm, String code) {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return null;
		}

		try {

			return entityManager
					.createQuery("SELECT * FROM Question where realm=:realmStr and code = :code", Question.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.error("No Question found in DB for " + code);
		}
		return null;
	}

}
