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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftApplicationNotAvailableException;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class ApplicationWizardModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION = "application";

	/**
	 * Timeout in seconds when trying to contact an application after it had
	 * been created.
	 */
	private static final int APP_CREATION_TIMEOUT = 10;

	private IUser user;
	private IApplication application;
	private String name;
	private ICartridge cartridge;

	public ApplicationWizardModel(IUser user) {
		this(null, user);
	}

	public ApplicationWizardModel(IApplication application, IUser user) {
		this.application = application;
		this.user = user;
	}

	public IUser getUser() {
		return user;
	}

	public String getName() {
		return name;
	}

	public String setName(String name) {
		return this.name = name;
	}

	public void setCartridge(ICartridge cartridge) {
		this.cartridge = cartridge;
	}


	public void setApplication(IApplication application) {
		firePropertyChange(PROPERTY_APPLICATION, this.application, this.application = application);
	}

	public IApplication getApplication() {
		return application;
	}

	public void createApplication(IProgressMonitor monitor) throws OpenShiftException {
		IApplication application = createApplication(name, cartridge, monitor);
		setApplication(application);
	}
	public IApplication createApplication(String name, ICartridge cartridge, IProgressMonitor monitor) throws OpenShiftException {
//		monitor.subTask("creating application...");
		IApplication application = getUser().createApplication(name, cartridge);
		waitForAccessible(application, monitor);
		return application;
	}

	public boolean hasApplication(String name) throws OpenShiftException {
		return user.hasApplication(name);
	}

	private void waitForAccessible(IApplication application, IProgressMonitor monitor) throws OpenShiftApplicationNotAvailableException, OpenShiftException {
//		monitor.subTask("waiting for application to become accessible...");
		if (!application.waitForAccessible(APP_CREATION_TIMEOUT * 1000)) {
			throw new OpenShiftApplicationNotAvailableException(NLS.bind(
					OpenShiftExpressUIMessages.HOSTNAME_NOT_ANSWERING,
					application.getApplicationUrl()));
		}
	}

}
