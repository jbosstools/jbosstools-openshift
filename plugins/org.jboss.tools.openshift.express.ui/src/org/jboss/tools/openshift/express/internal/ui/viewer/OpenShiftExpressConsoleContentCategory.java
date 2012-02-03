package org.jboss.tools.openshift.express.internal.ui.viewer;

import com.openshift.express.client.IUser;

public class OpenShiftExpressConsoleContentCategory {

	private final IUser user;
	
	public OpenShiftExpressConsoleContentCategory(final IUser user) {
		this.user = user;
	}

	/**
	 * @return the user
	 */
	public IUser getUser() {
		return user;
	}
	
	
}
