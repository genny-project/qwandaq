package life.genny.qwandaq.entity;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

import life.genny.qwandaq.Link;

@RegisterForReflection
public class EntityQuestion implements java.io.Serializable, Comparable<Object> {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(EntityQuestion.class);

	private String valueString;

	private Double weight;

	private Link link;

	public EntityQuestion() {}

	public EntityQuestion(Link link) {
		this.link = link;
	}

	@Override
	public int compareTo(Object o) {
		return 0;
	}
}
