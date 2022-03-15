package life.genny.qwandaq.message;

import java.util.Arrays;
import java.util.List;

import life.genny.qwandaq.Answer;

public class QDataAnswerMessage extends QDataMessage {

	private static final long serialVersionUID = 1L;
	private List<Answer> items;
	private static final String DATATYPE_ANSWER = Answer.class.getSimpleName();


	public QDataAnswerMessage() {

		super(DATATYPE_ANSWER);
	}
	
	public QDataAnswerMessage(Answer[] items) {

		super(DATATYPE_ANSWER);
		setItems(items);
	}
	
	public QDataAnswerMessage(List<Answer> items) {

		super(DATATYPE_ANSWER);
		setItems(items.toArray(new Answer[0]));
	}

	public QDataAnswerMessage(Answer item) {

		super(DATATYPE_ANSWER);
		Answer[] itemsArr = new Answer[1];
		itemsArr[0] = item;
		setItems(itemsArr);
	}
	
	/** 
	 * @return Answer[]
	 */
	public Answer[] getItems() {
		return items.toArray(new Answer[0]);
	}

	/** 
	 * @param items the array of answers to set
	 */
	public void setItems(Answer[] items) {
		this.items = Arrays.asList(items);
	}

	/** 
	 * @return String
	 */
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "QDataAnswerMessage [" + (items != null ? "items=" + Arrays.toString(items.toArray(new Answer[0])) : "") + "]";
	}
	
}
