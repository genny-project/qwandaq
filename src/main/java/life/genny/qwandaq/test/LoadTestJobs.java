package life.genny.qwandaq.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LoadTestJobs {

	static Map<String, TestJob> jobs = new ConcurrentHashMap<String, TestJob>();

	public Map<String, TestJob> getJobs() {
		return jobs;
	}
	
	public TestJob getJob(String uuid) {
		return jobs.get(uuid);
	}

	void putJob(TestJob job) {
		jobs.put(job.getUuid(), job);
	}
}
