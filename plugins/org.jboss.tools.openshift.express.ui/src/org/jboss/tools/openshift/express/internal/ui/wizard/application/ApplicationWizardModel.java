/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IApplication;

/**
 * @author André Dietisheim
 */
public class ApplicationWizardModel extends ObservableUIPojo {

	private IApplication application;
	private Connection user;

	public ApplicationWizardModel(IApplication application, Connection user) {
		this.application = application;
		this.user = user;
	}

	public IApplication getApplication() {
		return application;
	}

	public Connection getUser() {
		return user;
	}
}
