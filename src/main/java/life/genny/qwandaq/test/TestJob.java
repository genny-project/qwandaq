package life.genny.qwandaq.test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QSearchMessage;

public class TestJob {

	final String uuid;
	final String code;
	final Instant start;

	final String qSearchMessageJson;
	final String searchEntityJson;
	Instant end = null;

	/**
	 * Creates a new TestJob and adds it to a {@link LoadTestJobs} ConcurrentHashMap
	 * 
	 * @param jobLoader - {@link LoadTestJobs} to use
	 * @param code      - {@link SearchEntity#code}
	 */
	public TestJob(LoadTestJobs jobLoader, QSearchMessage searchMessage) {
		this.start = Instant.now();
		this.uuid = UUID.randomUUID().toString().toUpperCase();

		SearchEntity entity = searchMessage.getSearchEntity();

		entity.setCode(entity.getCode() + "_" + this.getUuid());
		this.code = entity.getCode();
		
		this.searchEntityJson = jobLoader.jsonb.toJson(entity);
		this.qSearchMessageJson = jobLoader.jsonb.toJson(searchMessage);
		jobLoader.putJob(entity, this);
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

	public Instant getEnd() {
		return end;
	}

	public void setEnd(Instant end) {
		this.end = end;
	}

	public Boolean isComplete() {
		return end != null;
	}

	public Long getDuration() {
		return Duration.between(start, end).toMillis();
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean qSearchMessageJson) {
		return "TestJob ["
				+ "code=" + code
				+ ", start=" + start
				+ (end != null ? ", end=" + end : "")
				+ (end != null ? ", duration=" + getDuration() : "")
				+ (qSearchMessageJson ? "====QSearchMessage====\n" + getQSearchMessageJSON() : "")
				+ "]";
	}

	public String getSearchJSON() {
		return searchEntityJson;
	}
	
	public String getQSearchMessageJSON() {
		return qSearchMessageJson;
	}

}
