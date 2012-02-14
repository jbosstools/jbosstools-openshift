package org.jboss.tools.openshift.express.internal.ui.wizard;

import com.openshift.express.client.IUser;

public interface IUserAwareModel {

	public IUser getUser();

	public IUser setUser(IUser user);

}