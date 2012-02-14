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

import org.jboss.tools.openshift.express.internal.core.console.UserModel;

import com.openshift.express.client.IUser;

/**
 * @author André Dietisheim
 */
public class ConnectToOpenShiftWizardModel implements IUserAwareModel {

	@Override
	public IUser getUser() {
		return UserModel.getDefault().getRecentUser();
	}

	@Override
	public IUser setUser(IUser user) {
		UserModel.getDefault().addUser(user);
		return user;
	}

}
