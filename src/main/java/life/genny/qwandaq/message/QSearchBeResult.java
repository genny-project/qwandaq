package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.runtime.annotations.RegisterForReflection;

import life.genny.qwandaq.entity.BaseEntity;

@RegisterForReflection
public class QSearchBeResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List<String> codes;
	Long total;

	// Added 19/08/2021
	// Users may want to just return the Entities instead of 
	// A list of codes
	List<BaseEntity> entities;

	public QSearchBeResult() {}
	
	public QSearchBeResult(List<String> codes, Long total) {
		this.codes = new ArrayList<>(codes);
		this.total = total;
	}

	public QSearchBeResult(BaseEntity[] entities, Long total) {
		this.codes = Stream.of(entities).map(x -> x.getCode()).collect(Collectors.toList());
		this.entities = Arrays.asList(entities);
		this.total = total;
	}

	/**
	 * @param total the total to set
	 */
	public QSearchBeResult(Long total) {
		this.total = total;
	}
	/**
	 * @return the codes
	 */
	public List<String> getCodes() {
		return codes;
	}
	/**
	 * @param codes the codes to set
	 */
	public void setCodes(List<String> codes) {
		this.codes = new ArrayList<>(codes);
	}
	/**
	 * @return the total
	 */
	public Long getTotal() {
		return total;
	}
	/**
	 * @param total the total to set
	 */
	public void setTotal(Long total) {
		this.total = total;
	}

	
	/** 
	 * @param entities the entities to set
	 */
	public void setEntities(BaseEntity[] entities) {
		this.entities = Arrays.asList(entities);
	}

	
	/** 
	 * @return BaseEntity[]
	 */
	public BaseEntity[] getEntities() {
		if (entities != null) {
			return entities.toArray(new BaseEntity[0]);
		} else {
			return null;
		}
	}
	
	
}
