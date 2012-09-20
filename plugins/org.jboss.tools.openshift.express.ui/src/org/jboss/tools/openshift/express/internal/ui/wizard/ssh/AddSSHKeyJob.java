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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author Andre Dietisheim
 */
public class AddSSHKeyJob extends Job {

	private ISSHKeyWizardPageModel model;

	public AddSSHKeyJob(ISSHKeyWizardPageModel model) {
		super("Adding SSH key " + model.getName() + "...");
		this.model = model;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			model.addSSHKey();
			return Status.OK_STATUS;
		} catch (Exception e) {
			return OpenShiftUIActivator.createErrorStatus(
					NLS.bind("Could not add SSH key {0} to OpenShift", model.getName()), e);
		}
	}
}
