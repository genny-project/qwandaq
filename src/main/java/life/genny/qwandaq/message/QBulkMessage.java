package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;


@RegisterForReflection
public class QBulkMessage implements Serializable {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private String token;

	private String data_type = QBulkMessage.class.getSimpleName();

	private List<QDataBaseEntityMessage> messages;

	private List<QDataAskMessage> asks;
	
	private List<String> recipientCodeArray;

	public QBulkMessage() {
		messages = new ArrayList<QDataBaseEntityMessage>();
		asks = new ArrayList<QDataAskMessage>();
	}

	public QBulkMessage(QDataBaseEntityMessage[] qMessages) {
		setMessages(qMessages);
		asks = new ArrayList<QDataAskMessage>();
	}

	public QBulkMessage(QDataAskMessage[] qAskMessages) {
		setAsks(qAskMessages);
		messages = new ArrayList<QDataBaseEntityMessage>();
	}

	public QBulkMessage(QDataBaseEntityMessage qMessage) {
		QDataBaseEntityMessage[] messages = new QDataBaseEntityMessage[1];
		messages[0] = qMessage;
		setMessages(messages);
		asks = new ArrayList<QDataAskMessage>();
		recipientCodeArray = new ArrayList<>();
	}

	public QBulkMessage(QDataAskMessage qAsk) {
		QDataAskMessage[]asks = new QDataAskMessage[1];
		asks[0] = qAsk;
		setAsks(asks);
		messages = new ArrayList<QDataBaseEntityMessage>();
	}

	public QBulkMessage(List<QDataBaseEntityMessage> qMessages) {
		this.messages = new ArrayList<>(qMessages);
		this.asks = new ArrayList<>();
	}

	public QBulkMessage(List<QDataBaseEntityMessage> qMessages, List<QDataAskMessage> qAsks) {
		this.messages = new ArrayList<>(qMessages);
		this.asks = new ArrayList<>(qAsks);
	}

	
	/** 
	 * @param qMessageArray the array of entities to add
	 */
	public void add(QDataBaseEntityMessage[] qMessageArray) {
		if (messages != null) {
			this.messages.addAll(Arrays.asList(qMessageArray));
		}
	}

	
	/** 
	 * @param qAskArray the array of asks to add
	 */
	public void add(QDataAskMessage[] qAskArray) {
		if (asks != null) {
			this.asks.addAll(Arrays.asList(qAskArray));
		}
	}

	
	/** 
	 * @param qMessageList the list of entities to add
	 */
	public void add(List<QDataBaseEntityMessage> qMessageList) {
		if (messages != null) {
			Set<QDataBaseEntityMessage> set = new HashSet<>(this.messages);
			set.addAll(qMessageList);
			setMessages(set.toArray(new QDataBaseEntityMessage[0]));
		} else {
			messages = new ArrayList<>(qMessageList);
		}

	}

	
	/** 
	 * @param qAskList the list of asks to add
	 */
	public void addAsks(List<QDataAskMessage> qAskList) {
		if (asks != null) {
			Set<QDataAskMessage> set = new HashSet<>(this.asks);
			set.addAll(qAskList);
			setAsks(set.toArray(new QDataAskMessage[0]));
		} else {
			asks = new ArrayList<>(qAskList);
		}

	}

	/** 
	 * @param qMessage the entity message to add
	 */
	public void add(QDataBaseEntityMessage qMessage) {

		Set<QDataBaseEntityMessage> set = new HashSet<QDataBaseEntityMessage>(this.messages);
		set.add(qMessage);
		setMessages(set.toArray(new QDataBaseEntityMessage[0]));
	}
	
	/** 
	 * @param qMessage the ask message to add
	 */
	public void add(QDataAskMessage qMessage) {
		Set<QDataAskMessage> set = new HashSet<>(this.asks);
		set.add(qMessage);
		setAsks(set.toArray(new QDataAskMessage[0]));
	}

	/** 
	 * @param qBulkMessage the bulk message to add
	 */
	public void add(QBulkMessage qBulkMessage) {
		if ((qBulkMessage.getAsks() != null)) {
			this.add(qBulkMessage.getAsks());
		}

		if ((qBulkMessage.getMessages() != null) && (qBulkMessage.getMessages().length > 0)) {
			this.add(qBulkMessage.getMessages());
		}
	}

	/**
	 * @return the messages
	 */
	public QDataBaseEntityMessage[] getMessages() {
		if (messages != null) {
			return messages.toArray(new QDataBaseEntityMessage[0]);
		} else {
			return null;
		}
		
	}

	/**
	 * @param messages the messages to set
	 */
	public void setMessages(QDataBaseEntityMessage[] messages) {
		this.messages = Arrays.asList(messages);
	}

	/**
	 * @return the asks
	 */
	public QDataAskMessage[] getAsks() {
		if (asks != null) {
			return asks.toArray(new QDataAskMessage[0]);
		} else {
			return null;
		}
		
	}

	/**
	 * @param asks the asks to set
	 */
	public void setAsks(QDataAskMessage[] asks) {
		this.asks = Arrays.asList(asks);
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @return the data_type
	 */
	public String getData_type() {
		return data_type;
	}

	/**
	 * @param data_type the data_type to set
	 */
	public void setData_type(String data_type) {
		this.data_type = data_type;
	}

	/**
	 * @return the recipientCodeArray
	 */
	public String[] getRecipientCodeArray() {
		if (recipientCodeArray != null) {
			return recipientCodeArray.toArray(new String[0]);
		} else {
			return null;
		}
		
	}

	/**
	 * @param recipientCodeArray the recipientCodeArray to set
	 */
	public void setRecipientCodeArray(String[] recipientCodeArray) {
		this.recipientCodeArray = Arrays.asList(recipientCodeArray);
	}

	
	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		int messageCount = (messages == null) ? 0 : messages.size();
		int asksCount = (asks == null) ? 0 : asks.size();
		return "QBulkMessage [" + (data_type != null ? "data_type=" + data_type : "") + " QDataBaseEntityMsgs= "
				+ messageCount + " AskMsgs=" + asksCount + "]";
	}

}
