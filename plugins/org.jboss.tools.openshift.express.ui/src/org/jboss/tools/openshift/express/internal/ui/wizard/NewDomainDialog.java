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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.wizard.appimport.ImportProjectWizardModel;

/**
 * @author André Dietisheim
 */
public class NewDomainDialog extends Wizard {

	private String namespace;
	private NewDomainWizardPageModel model;

	public NewDomainDialog(String namespace, ImportProjectWizardModel wizardModel) {
		this.namespace = namespace;
		this.model = new NewDomainWizardPageModel(namespace, wizardModel.getUser());
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		final ArrayBlockingQueue<Boolean> queue = new ArrayBlockingQueue<Boolean>(1);
		try {
			WizardUtils.runInWizard(new Job("Creating domain...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						model.createDomain();
						queue.offer(true);
					} catch (Exception e) {
						queue.offer(false);
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								NLS.bind("Could not create domain \"{0}\"", model.getNamespace()), e);
					}
					return Status.OK_STATUS;
				}
			}, getContainer());
		} catch (Exception e) {
			// ignore
		}
		return queue.poll();
	}

	@Override
	public void addPages() {
		addPage(new NewDomainWizardPage(namespace, model, this));
	}
}
