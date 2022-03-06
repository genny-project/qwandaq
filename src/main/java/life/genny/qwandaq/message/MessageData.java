package life.genny.qwandaq.message;

import java.io.Serializable;

public class MessageData implements Serializable {

	private static final long serialVersionUID = 1L;

	
	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return " MessageData [code=" + code + "   " + id + "]";
	}

	private String code;

	private String parentCode;

	private String rootCode;

	private String targetCode;
	
	private String sourceCode;
	
	private Long id;
	private String value;
	private String content;

	public MessageData(String code) {
		this.code = code;
	}
	

	
	/** 
	 * @return String
	 */
	public String getCode() {
		return code;
	}

	
	/** 
	 * @param code
	 */
	public void setCode(String code) {
		this.code = code;
	}

	
	/** 
	 * @return Long
	 */
	public Long getId() {
		return id;
	}

	
	/** 
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}

	
	/** 
	 * @return String
	 */
	public String getValue() {
		return value;
	}

	
	/** 
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	
	/** 
	 * @return String
	 */
	public String getContent() {
		return content;
	}

	
	/** 
	 * @param content
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the parentCode
	 */
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * @param parentCode the parentCode to set
	 */
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * @return the rootCode
	 */
	public String getRootCode() {
		return rootCode;
	}

	/**
	 * @param rootCode the rootCode to set
	 */
	public void setRootCode(String rootCode) {
		this.rootCode = rootCode;
	}

	/**
	 * @return the targetCode
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/**
	 * @param targetCode the targetCode to set
	 */
	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}
	
	/**
	 * @return the targetCode
	 */
	public String getSourceCode() {
		return sourceCode;
	}

	/**
	 * @param targetCode the targetCode to set
	 */
	public void getSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

}
