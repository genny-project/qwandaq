package life.genny.qwandaq.test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbTransient;

import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QSearchMessage;

public class TestJob {

	@JsonbTransient
	Jsonb jsonBuilder = JsonbBuilder.create();

	final String uuid;
	final String code;
	final Instant start;
	
	public final String searchJSON;
	
	Instant end = null;

	/**
	 * Creates a new TestJob and adds it to a {@link LoadTestJobs} ConcurrentHashMap
	 * @param jobLoader - {@link LoadTestJobs} to use
	 * @param code - {@link SearchEntity#code}
	 */
	public TestJob(LoadTestJobs jobLoader, QSearchMessage searchMessage) {
		this.start = Instant.now();
		this.uuid = UUID.randomUUID().toString().toUpperCase();
		
		SearchEntity entity = searchMessage.getSearchEntity();
    	entity.setCode(entity.getCode() + "_" + this.getUuid());
    	
    	searchJSON = jsonBuilder.toJson(searchMessage);
    	
		this.code = entity.getCode();
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
		if(end != null) {
			return Duration.between(start, end).toMillis();
		} else return 0L;
	}

	public String toString(boolean showJson) {
		return "TestJob ["
				+ "code=" + code 
				+ ", start=" + start 
				+ (end != null ? ", end=" + end : "")
				+ (end != null ? ", duration=" + getDuration() : "")
				+ ", json=" + searchJSON
				+ "]";
	}
	
	@Override
	public String toString() {
		return toString(false);		
	}
	
}
