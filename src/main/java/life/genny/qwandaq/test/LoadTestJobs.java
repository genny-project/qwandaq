package life.genny.qwandaq.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LoadTestJobs {

	static Map<String, TestJob> jobs = new ConcurrentHashMap<String, TestJob>();

	TestJob getJob(String id) {
		return jobs.get(id);
	}

	void putJob(TestJob job) {
		jobs.put(job.getUuid(), job);
	}
}
