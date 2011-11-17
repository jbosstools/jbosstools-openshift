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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.IUser;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 */
public class NewApplicationDialog extends Wizard {

	private NewApplicationWizardModel newApplicationWizardModel;

	public NewApplicationDialog(IUser user) {
		this.newApplicationWizardModel = new NewApplicationWizardModel(user);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		final ArrayBlockingQueue<Boolean> queue = new ArrayBlockingQueue<Boolean>(1);
		try {
			WizardUtils.runInWizard(new Job("Creating application...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						newApplicationWizardModel.createApplication();
						queue.offer(true);
					} catch (OpenShiftException e) {
						queue.offer(false);
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								NLS.bind("Could not create application \"{0}\"", newApplicationWizardModel.getName()), e);
					}
					return Status.OK_STATUS;
				}
			}, getContainer());
			return queue.poll(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void addPages() {
		addPage(new NewApplicationWizardPage(newApplicationWizardModel, this));
		addPage(new EmbedCartridgeWizardPage(newApplicationWizardModel, this));
	}
	
	public IApplication getApplication() {
		return newApplicationWizardModel.getApplication();
	}
}
