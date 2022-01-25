package life.genny.qwandaq.test;

import java.time.Instant;
import java.util.UUID;

import life.genny.qwandaq.entity.SearchEntity;

public class TestJob {

	String uuid;
	String code;
	Instant start;
	Instant end;

	/**
	 * Creates a new TestJob and adds it to a {@link LoadTestJobs} ConcurrentHashMap
	 * @param jobLoader - {@link LoadTestJobs} to use
	 * @param code - {@link SearchEntity#code}
	 */
	public TestJob(LoadTestJobs jobLoader, String code) {
		this.start = Instant.now();
		this.uuid = UUID.randomUUID().toString();
		
		jobLoader.putJob(this);
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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
}
