package org.jboss.tools.openshift.core;

import org.jboss.tools.openshift.core.internal.KubernetesConnection;

public interface ConnectionVisitor {
	
	void visit(org.jboss.tools.openshift.express.internal.core.connection.Connection connection);
	
	void visit(KubernetesConnection connection);
}
