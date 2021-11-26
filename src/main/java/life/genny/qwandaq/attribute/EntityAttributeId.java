package life.genny.qwandaq.attribute;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

import javax.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import life.genny.qwandaq.entity.BaseEntity;

@Embeddable
public class EntityAttributeId implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	@ManyToOne ( )
//	@JsonBackReference(value="entityAttribute")
	@JsonManagedReference(value="entityAttribute")
	@JsonIgnoreProperties("baseEntityAttributes")
	@JsonbTransient
	public BaseEntity baseEntity;

	@ManyToOne
	@JsonBackReference(value="attribute")
//	@JsonIgnore
	public Attribute attribute;

	public BaseEntity getBaseEntity() {
		return baseEntity;
	}

	public void setBaseEntity(final BaseEntity baseEntity) {
		this.baseEntity = baseEntity;
	}

	public Attribute getAttribute() {
		return attribute;
	}

	public void setAttribute(final Attribute attribute) {
		this.attribute = attribute;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + ((baseEntity == null) ? 0 : baseEntity.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityAttributeId other = (EntityAttributeId) obj;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (baseEntity == null) {
			if (other.baseEntity != null)
				return false;
		} else if (!baseEntity.equals(other.baseEntity))
			return false;
		return true;
	}




}
