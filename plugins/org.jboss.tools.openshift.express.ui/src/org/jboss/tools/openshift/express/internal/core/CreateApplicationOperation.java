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
package org.jboss.tools.openshift.express.internal.core;

import java.net.SocketTimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftApplicationNotAvailableException;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class CreateApplicationOperation {

	private UserDelegate user;

	public CreateApplicationOperation(UserDelegate user) {
		this.user = user;
	}

	public IApplication execute(final String name, final ICartridge cartridge, final ApplicationScale scale,
			final IGearProfile gearProfile, final IProgressMonitor monitor)
			throws OpenShiftApplicationNotAvailableException, OpenShiftException {
		if (user == null) {
			throw new OpenShiftException(OpenShiftExpressUIMessages.CANNOT_CREATE_NO_USER);
		}

		monitor.setTaskName(NLS.bind(OpenShiftExpressUIMessages.CREATING_APPLICATION, name));
		Logger.debug(NLS.bind(OpenShiftExpressUIMessages.CREATING_APPLICATION, name));
		return createApplication(name, cartridge, scale, gearProfile, monitor);
	}

	private IApplication createApplication(final String name, final ICartridge cartridge,
			final ApplicationScale scale, final IGearProfile gearProfile, final IProgressMonitor monitor)
			throws OpenShiftException {
		monitor.setTaskName(NLS.bind(OpenShiftExpressUIMessages.CREATING_APPLICATION, name));
		Logger.debug(NLS.bind(OpenShiftExpressUIMessages.CREATING_APPLICATION, name));
		return user.createApplication(name, cartridge, scale, gearProfile);
	}
}
