package life.genny.qwandaq.datatype;

import java.io.Serializable;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum CapabilityMode implements Serializable {

	NONE("NONE", 0),
	VIEW("VIEW", 1),
	EDIT("EDIT", 2),
	ADD("ADD", 3),
	DELETE("DELETE", 4),
	SELF("SELF", 5);

	private final String name;
	private final Integer priority;

	private CapabilityMode(String s,Integer p) {
		name = s;
		priority = p;
	}

	public boolean equalsName(String otherName) {
		return name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}
	
	public boolean greaterThan(CapabilityMode other)
	{
		return (this.priority > other.priority);
	}
	
	public static CapabilityMode getMode(final String modeString)
	{
        for (CapabilityMode b : CapabilityMode.values()) {
            if (b.name.equalsIgnoreCase(modeString)) {
                return b;
            }
        }
        return null;
	}
}
