package life.genny.qwandaq.message;

import java.io.Serializable;

import life.genny.qwandaq.entity.SearchEntity;

public class QSearchMessage extends QMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final String MESSAGE_TYPE = "SCH_MSG";
	public SearchEntity searchEntity;

	public QSearchMessage() {
		super(MESSAGE_TYPE);
	}

	public QSearchMessage(SearchEntity searchEntity) {
		super(MESSAGE_TYPE);
		this.searchEntity = searchEntity;
	}

	@Override
	public String toString() {
		return "QSearchMessage [searchEntity=" + searchEntity.getCode() + "]";
	}
	public void setSearchEntity(SearchEntity searchEntity) {
		this.searchEntity = searchEntity;
	}
	public SearchEntity getSearchEntity() {
		return this.searchEntity;
	}
	
}
