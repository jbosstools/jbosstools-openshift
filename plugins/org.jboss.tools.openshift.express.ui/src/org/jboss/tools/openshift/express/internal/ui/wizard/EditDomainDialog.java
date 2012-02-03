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

import org.eclipse.jface.wizard.Wizard;

import com.openshift.express.client.IUser;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class EditDomainDialog extends Wizard {

	private EditDomainWizardPageModel model;

	public EditDomainDialog(IUser user) {
		this.model = new EditDomainWizardPageModel(user);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
//		final ArrayBlockingQueue<Boolean> queue = new ArrayBlockingQueue<Boolean>(1);
//		try {
//			WizardUtils.runInWizard(new Job("Renaming domain...") {
//
//				@Override
//				protected IStatus run(IProgressMonitor monitor) {
//					try {
//						model.renameDomain();
//						queue.offer(true);
//					} catch (Exception e) {
//						queue.offer(false);
//						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
//								NLS.bind("Could not create domain \"{0}\"", model.getNamespace()), e);
//					}
//					return Status.OK_STATUS;
//				}
//			}, getContainer());
//		} catch (Exception e) {
//			// ignore
//		}
//		return queue.poll();
		return true;
	}

	@Override
	public void addPages() {
		addPage(new EditDomainWizardPage(model, this));
	}
}
