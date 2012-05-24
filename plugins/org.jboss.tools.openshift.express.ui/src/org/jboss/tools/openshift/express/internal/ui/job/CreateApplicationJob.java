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
package org.jboss.tools.openshift.express.internal.ui.job;

import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IGearProfile;

/**
 * @author Andre Dietisheim
 */
public class CreateApplicationJob extends AbstractDelegatingMonitorJob {

	private UserDelegate user;
	private String name;
	private ICartridge cartridge;
	private ApplicationScale scale;
	private IGearProfile gear;
	private IApplication application;
	private ReentrantLock lock = new ReentrantLock();

	public CreateApplicationJob(final String name, final ICartridge cartridge, final ApplicationScale scale,
			final IGearProfile gear, UserDelegate user) {
		super(NLS.bind(OpenShiftExpressUIMessages.CREATING_APPLICATION, name));
		this.name = name;
		this.cartridge = cartridge;
		this.scale = scale;
		this.gear = gear;
		this.user = user;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			lock.lock();
			this.application = user.createApplication(name, cartridge, scale, gear);
			return Status.OK_STATUS;
		} catch (Exception e) {
			return OpenShiftUIActivator.createErrorStatus(
					OpenShiftExpressUIMessages.COULD_NOT_CREATE_APPLICATION, e, name);
		} finally {
			lock.unlock();
		}
	}

	public IApplication getApplication() {
		try {
			lock.lock();
			return application;
		} finally {
			lock.unlock();
		}
	}
}
