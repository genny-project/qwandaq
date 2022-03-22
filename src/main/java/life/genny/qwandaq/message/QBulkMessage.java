package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.annotation.ProtoMessage;

@ProtoMessage
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
		messages = new ArrayList<>();
		asks = new ArrayList<>();
	}

	public QBulkMessage(QDataBaseEntityMessage[] qMessages) {
		this.messages = Arrays.asList(qMessages);
		asks = new ArrayList<>();
	}

	public QBulkMessage(QDataAskMessage[] qAskMessages) {
		this.asks = Arrays.asList(qAskMessages);
		messages = new ArrayList<>();
	}

	public QBulkMessage(QDataBaseEntityMessage qMessage) {
		this.messages = new ArrayList<>();
		this.messages.add(qMessage);
		asks = new ArrayList<>();
	}

	public QBulkMessage(QDataAskMessage qAsk) {
		this.asks = new ArrayList<>();
		this.asks.add(qAsk);
		messages = new ArrayList<>();
	}

	public QBulkMessage(List<QDataBaseEntityMessage> qMessages) {
		this.messages = qMessages;
		this.asks = new ArrayList<>();
	}

	public QBulkMessage(List<QDataBaseEntityMessage> qMessages, List<QDataAskMessage> qAsks) {
		this.messages = qMessages;
		this.asks = qAsks;
	}

	
	/** 
	 * @param qMessageArray the array of entities to add
	 */
	public void add(QDataBaseEntityMessage[] qMessageArray) {
		
		Set<QDataBaseEntityMessage> set = new HashSet<>(this.messages);
		set.addAll(Arrays.asList(qMessageArray));
		setMessages(set.toArray(new QDataBaseEntityMessage[0]));
	}

	
	/** 
	 * @param qAskArray the array of asks to add
	 */
	public void add(QDataAskMessage[] qAskArray) {
		Set<QDataAskMessage> set = new HashSet<>(this.asks);
		set.addAll(Arrays.asList(qAskArray));
		setAsks(set.toArray(new QDataAskMessage[0]));
	}

	
	/** 
	 * @param qMessageList the list of entities to add
	 */
	public void add(List<QDataBaseEntityMessage> qMessageList) {
		Set<QDataBaseEntityMessage> set = new HashSet<>(this.messages);
		set.addAll(qMessageList);
		setMessages(set.toArray(new QDataBaseEntityMessage[0]));

	}

	
	/** 
	 * @param qAskList the list of asks to add
	 */
	public void addAsks(List<QDataAskMessage> qAskList) {
		Set<QDataAskMessage> set = new HashSet<>(this.asks);
		set.addAll(qAskList);
		setAsks(set.toArray(new QDataAskMessage[0]));

	}

	/** 
	 * @param qMessage the entity message to add
	 */
	public void add(QDataBaseEntityMessage qMessage) {

		Set<QDataBaseEntityMessage> set = new HashSet<>(this.messages);
		set.add(qMessage);
		setMessages(set.toArray(new QDataBaseEntityMessage[0]));
	}
	
	/** 
	 * @param qMessage the ask message to add
	 */
	public void add(QDataAskMessage qMessage) {
		if (qMessage.getItems().length > 0) {
			Set<QDataAskMessage> set = new HashSet<>(this.asks);
			set.add(qMessage);
			setAsks(set.toArray(new QDataAskMessage[0]));
		}
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
		return messages.toArray(new QDataBaseEntityMessage[0]);
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
		return asks.toArray(new QDataAskMessage[0]);
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
		return recipientCodeArray.toArray(new String[0]);
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
