package org.jboss.tools.openshift.core.jmx;

public enum ServerType {
	EAP("eap"), WILDFLY("wildfly"), JBOSS("jboss");
	
	private String idValue;

	private ServerType(String idValue) {
		this.idValue = idValue;
	};
	
	String getIdValue() {
		return idValue;
	}

	public static ServerType fromString(String serverType) {
		for (ServerType type : ServerType.values()) {
			if (type.idValue.equals(serverType)) {
				return type;
			}
		}
		throw new IllegalArgumentException("unknown server type: "+serverType);
	}

}
