package org.jboss.tools.openshift.express.internal.core.console;

import com.openshift.express.client.IUser;

public interface IUserModelListener {
	public void userAdded(IUser user);
	public void userRemoved(IUser user);
	public void userChanged(IUser user);
}
