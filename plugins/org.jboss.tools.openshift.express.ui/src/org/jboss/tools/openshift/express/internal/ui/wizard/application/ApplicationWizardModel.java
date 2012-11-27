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

import org.eclipse.core.runtime.Assert;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;

/**
 * @author André Dietisheim
 */
public class ApplicationWizardModel extends ObservableUIPojo {

	private IApplication application;

	public ApplicationWizardModel(IApplication application) {
		Assert.isLegal(application != null, "No application provided");
		this.application = application;
	}

	public IApplication getApplication() {
		return application;
	}

	public IOpenShiftConnection getConnection() {
		return application.getDomain().getUser().getConnection();
	}

}
