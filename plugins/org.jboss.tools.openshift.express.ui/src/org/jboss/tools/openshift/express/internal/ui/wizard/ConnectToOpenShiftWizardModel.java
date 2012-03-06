/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;

/**
 * @author André Dietisheim
 */
public class ConnectToOpenShiftWizardModel implements IUserAwareModel {

	@Override
	public UserDelegate getUser() {
		return UserModel.getDefault().getRecentUser();
	}

	@Override
	public UserDelegate setUser(UserDelegate user) {
		UserModel.getDefault().addUser(user);
		return user;
	}

}
