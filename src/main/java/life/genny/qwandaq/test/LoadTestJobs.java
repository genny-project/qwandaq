package life.genny.qwandaq.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.SearchEntity;

@ApplicationScoped
public class LoadTestJobs {
	private static final Logger log = Logger.getLogger(LoadTestJobs.class);

	static Map<String, TestJob> jobs = new ConcurrentHashMap<String, TestJob>();

	public final Jsonb jsonb = JsonbBuilder.create();

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
		jobs.put(entity.getCode(), job);
	}
}
