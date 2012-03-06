package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

public interface IUserAwareModel {

	public UserDelegate getUser();

	public UserDelegate setUser(UserDelegate user);

}