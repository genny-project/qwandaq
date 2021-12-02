package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


import life.genny.qwandaq.entity.BaseEntity;

public class QSearchBeResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String[] codes;
	Long total;

	// Added 19/08/2021
	// Users may want to just return the Entities instead of 
	// A list of codes
	BaseEntity[] entities;
	
	
	/**
	 * @param codes
	 * @param total
	 */
	public QSearchBeResult(List<String> codes, Long total) {
		this.codes = codes.toArray(new String[0]);
		this.total = total;
	}

	public QSearchBeResult(BaseEntity[] entities, Long total) {
		this.entities = entities;
		this.total = total;
	}

	/**
	 * @param total
	 */
	public QSearchBeResult(Long total) {
		this.total = total;
	}
	/**
	 * @return the codes
	 */
	public List<String> getCodes() {
		return Arrays.asList(codes);
	}
	/**
	 * @param codes the codes to set
	 */
	public void setCodes(List<String> codes) {
		this.codes = codes.toArray(new String[0]);
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

	public void setEntities(BaseEntity[] entities) {
		this.entities = entities;
	}

	public BaseEntity[] getEntities() {
		return entities;
	}
	
	
}
