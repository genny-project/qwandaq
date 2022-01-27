package life.genny.qwandaq.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.SearchEntity;

@ApplicationScoped
public class LoadTestJobs {
	private static final Logger log = Logger.getLogger(LoadTestJobs.class);

	static Map<String, TestJob> jobs = new ConcurrentHashMap<String, TestJob>();

	public Map<String, TestJob> getJobs() {
		return jobs;
	}
	
	/**
	 * Retrieve a {@link TestJob} from the job cache.<lb>
	 * Expecting Base Entity code + "_" + {@link TestJob#uuid}
	 * @return
	 */
	public TestJob getJob(String uniqueCode) {
		return jobs.get(uniqueCode);
	}

	void putJob(SearchEntity entity, TestJob job) {
		log.info(entity.getCode() + "_" + job.getUuid());	
		jobs.put(entity.getCode() + "_" + job.getUuid(), job);
	}
}
