package org.jboss.tools.openshift.common.core.connection;

public interface ICredentialsConnection extends IConnection {

	public String getPassword();

	public String getUsername();
}
