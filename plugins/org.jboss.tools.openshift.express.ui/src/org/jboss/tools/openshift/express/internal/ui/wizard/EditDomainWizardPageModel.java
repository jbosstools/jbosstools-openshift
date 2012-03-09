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

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 * @author Xavier Coulon
 */
public class EditDomainWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_NAMESPACE = "namespace";

	private String namespace;

	private final UserDelegate user;

	public EditDomainWizardPageModel(UserDelegate user) {
		this.user = user;
		try {
			if (user.getDomain() != null) {
				setNamespace(user.getDomain().getNamespace());
			}
		} catch (OpenShiftException e) {
			Logger.error("Fail to retrieve OpenShift domain", e);
		}
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		firePropertyChange(PROPERTY_NAMESPACE, this.namespace,
				this.namespace = namespace);
	}

	public void renameDomain() throws OpenShiftException {
		if (user.getDomain() != null) {
			user.getDomain().setNamespace(namespace);
		} else {
			Logger.warn("Attempting to rename missing user domain...");
		}
	}
}
