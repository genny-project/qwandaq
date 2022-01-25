package life.genny.qwandaq.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LoadTestJobs {

	static Map<Integer, TestJob> jobs = new ConcurrentHashMap<Integer, TestJob>();

	TestJob getJob(Integer id) {
		return jobs.get(id);
	}

	void putJob(TestJob job) {
		jobs.put(job.getId(), job);
	}
}
