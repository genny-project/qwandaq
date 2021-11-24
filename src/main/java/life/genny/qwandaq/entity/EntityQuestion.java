package life.genny.qwandaq.entity;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.Convert;

import org.jboss.logging.Logger;
import org.javamoney.moneta.Money;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.converter.MoneyConverter;

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
