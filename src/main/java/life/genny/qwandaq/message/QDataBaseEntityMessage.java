package life.genny.qwandaq.message;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.WeightedItem;
import life.genny.qwandaq.entity.BaseEntity;

@RegisterForReflection
public class QDataBaseEntityMessage extends QDataMessage implements Comparable<QDataBaseEntityMessage> {

	private static final long serialVersionUID = 1L;

	private List<BaseEntity> items;

	private List<WeightedItem> weightedItems;

	private static final String DATATYPE_BASEENTITY = BaseEntity.class.getSimpleName();
	private String parentCode;
	private String linkCode;
	private String linkValue;
	private Long total = -1L;
	private Long returnCount;
	private BaseEntity sum;

	/**
	 * @param o the entity message to compare to
	 * @return int
	 */
	@Override
	public int compareTo(QDataBaseEntityMessage o) {

		/*
		 * 2 QDataBaseEntityMessage are the same if
		 * a. the parentCode are equals
		 * b. the number of items is the same
		 * c. items have the same code
		 */

		if (this.parentCode.equals(o.getParentCode()) == false)
			return 0;
		if (this.getItems().size() != o.getItems().size())
			return 0;

		for (BaseEntity be : this.getItems()) {

			Boolean found = false;
			for (BaseEntity oBe : o.getItems()) {

				if (oBe.getCode().equals(be.getCode())) {
					found = true;
				}
			}

			if (found == false)
				return 0;
		}

		return 1;
	}

	public QDataBaseEntityMessage(final BaseEntity item, final String alias) {
		super(DATATYPE_BASEENTITY);
		items = new ArrayList<>();
		items.add(item);
		setItems(items);
		setAliasCode(alias);
		setTotal(1L);
	}

	public QDataBaseEntityMessage() {
		super(DATATYPE_BASEENTITY);
		this.setItems(new BaseEntity[0]);
	}

	public QDataBaseEntityMessage(final BaseEntity item) {
		this(item, null);
	}

	public QDataBaseEntityMessage(final BaseEntity[] items) {
		super(DATATYPE_BASEENTITY);
		if ((items == null) || (items.length == 0)) {
			setItems(new BaseEntity[0]);
		} else {
			setItems(items);
		}
		setItems(items);
		setTotal(-1L);
	}

	public QDataBaseEntityMessage(final List<BaseEntity> items) {
		super(DATATYPE_BASEENTITY);
		if ((items == null) || (items.isEmpty())) {
			setItems(new BaseEntity[0]);
		} else {
			setItems(items.toArray(new BaseEntity[0]));
		}
		setTotal(-1L);
	}

	public QDataBaseEntityMessage(final List<BaseEntity> items, final String parentCode,
			final String linkCode) {
		super(DATATYPE_BASEENTITY);

		if ((items == null) || (items.isEmpty())) {
			setItems(new BaseEntity[0]);
		} else {
			setItems(items.toArray(new BaseEntity[0]));
		}
		this.linkCode = linkCode;
		this.parentCode = parentCode;
		setTotal(-1L);
	}

	public QDataBaseEntityMessage(final List<BaseEntity> items, final String parentCode,
			final String linkCode, final String linkValue) {
		super(DATATYPE_BASEENTITY);
		if ((items == null) || (items.isEmpty())) {
			setItems(new BaseEntity[0]);
		} else {
			setItems(items.toArray(new BaseEntity[0]));
		}
		this.linkCode = linkCode;
		this.parentCode = parentCode;
		setTotal(-1L);
		this.setLinkValue(linkValue);
	}

	public QDataBaseEntityMessage(final BaseEntity[] items, final String parentCode,
			final String linkCode) {
		this(items, parentCode, linkCode, -1L);
	}

	public QDataBaseEntityMessage(final BaseEntity[] items, final String parentCode,
			final String linkCode, String linkValue) {
		this(items, parentCode, linkCode, -1L);
		this.setLinkValue(linkValue);
	}

	public QDataBaseEntityMessage(final BaseEntity[] items, final String parentCode,
			final String linkCode, final Long total) {
		super(DATATYPE_BASEENTITY);
		if ((items == null) || (items.length == 0)) {
			setItems(new BaseEntity[0]);
		} else {
			setItems(items);
		}
		this.linkCode = linkCode;
		this.parentCode = parentCode;
		setTotal(total);
	}

	public QDataBaseEntityMessage(final WeightedItem item, final String alias) {
		super(DATATYPE_BASEENTITY);
		weightedItems = new ArrayList<>();
		weightedItems.add(item);
		setAliasCode(alias);
		setTotal(1L);
	}

	public QDataBaseEntityMessage(final WeightedItem item) {
		this(item, null);
	}

	public QDataBaseEntityMessage(final WeightedItem[] items) {
		super(DATATYPE_BASEENTITY);
		setWeightedItems(items);
		setTotal(-1L);
	}

	public QDataBaseEntityMessage(final WeightedItem[] items, final String parentCode,
			final String linkCode) {
		this(items, parentCode, linkCode, -1L);
	}

	public QDataBaseEntityMessage(final WeightedItem[] items, final String parentCode,
			final String linkCode, final Long total) {
		super(DATATYPE_BASEENTITY);
		setWeightedItems(items);
		this.linkCode = linkCode;
		this.parentCode = parentCode;
		setTotal(total);
	}

	/**
	 * @param item the entity to add
	 */
	public void add(BaseEntity item) {

		List<BaseEntity> bes = this.getItems() != null ? new CopyOnWriteArrayList<>(this.getItems())
				: new CopyOnWriteArrayList<>();
		bes.add(item);
		this.setItems(bes.toArray(new BaseEntity[0]));
	}

	/**
	 * @param items the list of entities to add
	 */
	public void add(List<BaseEntity> items) {
		List<BaseEntity> bes = new CopyOnWriteArrayList<>(this.getItems());
		bes.addAll(items);
		this.setItems(bes.toArray(new BaseEntity[0]));
	}

	/**
	 * @return List&lt;BaseEntity&gt;
	 */
	public List<BaseEntity> getItems() {
		return items;
	}

	/**
	 * @param items the array of entities to set
	 */
	public void setItems(final BaseEntity[] items) {
		this.items = Arrays.asList(items);
		setReturnCount(new Long(items.length));
	}

	/**
	 * @param items the list of entities to set
	 */
	public void setItems(final List<BaseEntity> items) {
		this.items = items;
		setReturnCount(new Long(items.size()));
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
	public void setParentCode(final String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * @return the linkCode
	 */
	public String getLinkCode() {
		return linkCode;
	}

	/**
	 * @param linkCode the linkCode to set
	 */
	public void setLinkCode(final String linkCode) {
		this.linkCode = linkCode;
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
	public void setTotal(final Long total) {
		this.total = total;
	}

	/**
	 * @return the returnCount
	 */
	public Long getReturnCount() {
		return returnCount;
	}

	/**
	 * @param returnCount the returnCount to set
	 */
	public void setReturnCount(final Long returnCount) {
		this.returnCount = returnCount;
	}

	/**
	 * @return the weightedItems
	 */
	public List<WeightedItem> getWeightedItems() {
		return weightedItems;
	}

	/**
	 * @param weightedItems the weightedItems to set
	 */
	public void setWeightedItems(WeightedItem[] weightedItems) {
		this.weightedItems = Arrays.asList(weightedItems);
	}

	/**
	 * @param weightedItems the list weighted items to set
	 */
	public void setWeightedItems(List<WeightedItem> weightedItems) {
		this.weightedItems = weightedItems;
	}

	/**
	 * @return the sum
	 */
	public BaseEntity getSum() {
		return sum;
	}

	/**
	 * @param sum the sum to set
	 */
	public void setSum(BaseEntity sum) {
		this.sum = sum;
	}

	/**
	 * 
	 * @param delete     the delete status
	 * @param parentCode the parentCode to set for
	 */
	public void setDelete(final Boolean delete, final String parentCode) {
		super.setDelete(delete);
		this.parentCode = parentCode;
	}

	/**
	 * @return boolean
	 */
	public Boolean isDelete() {
		return super.getDelete();
	}

	/**
	 * @return boolean
	 */
	public Boolean isReplace() {
		return super.getReplace();
	}

	/**
	 * @return the linkValue
	 */
	public String getLinkValue() {
		return linkValue;
	}

	/**
	 * @param linkValue the linkValue to set
	 */
	public void setLinkValue(final String linkValue) {
		this.linkValue = linkValue;
	}

	/**
	 * @return String
	 */
	@Override
	public String toString() {
		return "QDataBaseEntityMessage [" + (parentCode != null ? "parentCode=" + parentCode + ", " : "")
				+ (total != null ? "total=" + total + ", " : "")
				+ (returnCount != null ? "returnCount=" + returnCount : "")
				+ (items != null ? "item count =" + items.size() + ", " : " = 0")
				+ "]";
	}

}
