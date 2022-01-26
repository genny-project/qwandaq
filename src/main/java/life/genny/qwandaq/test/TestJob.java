package life.genny.qwandaq.test;

import java.time.Instant;
import java.util.UUID;

import life.genny.qwandaq.entity.SearchEntity;

public class TestJob {

	final String uuid;
	final String code;
	Instant start = null;
	Instant end = null;

	/**
	 * Creates a new TestJob and adds it to a {@link LoadTestJobs} ConcurrentHashMap
	 * @param jobLoader - {@link LoadTestJobs} to use
	 * @param code - {@link SearchEntity#code}
	 */
	public TestJob(LoadTestJobs jobLoader, String code) {
		this.start = Instant.now();
		this.uuid = UUID.randomUUID().toString();
		this.code = code;
		jobLoader.putJob(this);
	}
	
	public String getUuid() {
		return uuid;
	}

	public String getCode() {
		return code;
	}
	
	public Instant getStart() {
		return start;
	}

	public void setStart(Instant start) {
		this.start = start;
	}

	public Instant getEnd() {
		return end;
	}

	public void setEnd(Instant end) {
		this.end = end;
	}
	
	public Boolean isComplete() {
		return end != null;
	}
}
