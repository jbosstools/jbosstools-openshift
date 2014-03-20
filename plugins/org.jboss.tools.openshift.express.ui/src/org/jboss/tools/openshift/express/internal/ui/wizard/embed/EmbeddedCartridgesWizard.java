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
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.EmbedCartridgesJob;
import org.jboss.tools.openshift.express.internal.ui.job.RefreshConnectionsModelJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.LogEntryFactory;

import com.openshift.client.IApplication;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author André Dietisheim
 */
public class EmbeddedCartridgesWizard extends Wizard {

	private static final long EMBED_CARTRIDGES_TIMEOUT = 10 * 60 * 1000;

	private ApplicationWizardModel wizardModel;
	private EmbeddedCartridgesWizardPage embeddedCartridgesWizardPage;

	public EmbeddedCartridgesWizard(IApplication application, Connection connection) {
		this.wizardModel = new ApplicationWizardModel(application, connection);
		setNeedsProgressMonitor(true);
		setWindowTitle("Edit Embedded Cartridges");
	}

	@Override
	public boolean performFinish() {
		return processCartridges();
	}

	public boolean processCartridges() {
		try {
			EmbedCartridgesJob job = 
					new EmbedCartridgesJob(
							new ArrayList<IEmbeddableCartridge>(wizardModel.getCheckedEmbeddableCartridges()),
							wizardModel.getApplication());
			IStatus result = WizardUtils.runInWizard(job, job.getDelegatingProgressMonitor(), getContainer(), EMBED_CARTRIDGES_TIMEOUT);
			if (!result.isOK()) {
				safeRefreshSelectedEmbeddedCartridges();
			} else {
				openLogDialog(job.getAddedCartridges(), job.isTimeouted(result));
			}
			new RefreshConnectionsModelJob(wizardModel.getConnection()).schedule();
			return result.isOK();
		} catch (Exception e) {
			String errorMessage = NLS.bind("Could not embed cartridge(s) for application {0}", wizardModel.getApplication().getName());
			IStatus status = OpenShiftUIActivator.createErrorStatus(errorMessage, e);
			ErrorDialog.openError(getShell(), "Error while embedding cartridges",
					errorMessage + ": " + StringUtils.null2emptyString(e.getMessage()), status);
			return false;
		}
	}

	private void openLogDialog(final List<IEmbeddedCartridge> cartridges, final boolean isTimeouted) {
		if (cartridges.size() == 0) {
			return;
		}

		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), LogEntryFactory.create(cartridges, isTimeouted)).open();
			}
		});
	}

	private void safeRefreshSelectedEmbeddedCartridges() {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				try {
					embeddedCartridgesWizardPage.setCheckedEmbeddableCartridges(wizardModel.getEmbeddedCartridges());
				} catch (Exception e) {
					OpenShiftUIActivator.log(e);
				}
			}
		});
	}

	@Override
	public void addPages() {
		addPage(this.embeddedCartridgesWizardPage = new EmbeddedCartridgesWizardPage(wizardModel, this));
	}
}
