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
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.IUser;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 */
public class NewApplicationWizardModel extends ObservableUIPojo {

	private IUser user;

	private String name;
	private ICartridge cartridge;
	
	private IApplication application;

	public NewApplicationWizardModel(IUser user) {
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
	
	public ICartridge getCartridge() {
		return cartridge;
	}

	public void setCartridge(ICartridge cartridge) {
		this.cartridge = cartridge;
	}

	public void setApplication(IApplication application) {
		this.application = application;
	}

	public void createApplication() throws OpenShiftException {
		IApplication application = user.createApplication(name, cartridge);
		setApplication(application);
	}

	public IApplication getApplication() {
		return application;
	}

	public boolean hasApplication(String name) {
		try {
			return user.getApplicationByName(name) != null;
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log(
					OpenShiftUIActivator.createErrorStatus("Could not get application by name", e));
			return false;
		}
	}
}
