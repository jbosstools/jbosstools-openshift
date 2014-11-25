package org.jboss.tools.openshift.core;

import com.openshift.client.OpenShiftException;

public interface Connection {
	
	boolean connect() throws OpenShiftException;
	
	void accept(ConnectionVisitor visitor);
	
	String getHost();
	
	String getPassword();

	String getUsername();

	boolean isDefaultHost();

	String getScheme();
}
