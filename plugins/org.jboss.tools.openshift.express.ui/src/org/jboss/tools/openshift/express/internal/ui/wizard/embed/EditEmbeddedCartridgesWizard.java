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
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.IApplicationProperties;
import org.jboss.tools.openshift.express.internal.core.cartridges.CodeAnythingCartridge;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.EmbedCartridgesJob;
import org.jboss.tools.openshift.express.internal.ui.job.FireExpressConnectionsChangedJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.LogEntryFactory;
import org.jboss.tools.openshift.internal.common.core.util.CollectionUtils;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author André Dietisheim
 */
public class EditEmbeddedCartridgesWizard extends Wizard {

	private EmbeddedCartridgesWizardModel wizardModel;
	private EmbeddedCartridgesWizardPage embeddedCartridgesWizardPage;
	private IApplication application;

	public EditEmbeddedCartridgesWizard(IApplication application, ExpressConnection connection) {
		Assert.isLegal(application != null);
		
		this.wizardModel = new EmbeddedCartridgesWizardModel(
				new HashSet<ICartridge>(application.getEmbeddedCartridges())
				, CollectionUtils.addTo(
						// add code anything cartridge
						(ICartridge) new CodeAnythingCartridge(),
						new ArrayList<ICartridge>(
								CollectionUtils.addAllTo(
										// add downloadable cartridges embedded to application
										new ArrayList<ICartridge>(application.getEmbeddedCartridges()),
										new HashSet<ICartridge>(connection.getEmbeddableCartridges()))))
				, new ExistingApplicationProperties(application)
				, application.getDomain()
				, connection);
		this.application = application;
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
							new ArrayList<ICartridge>(wizardModel.getCheckedEmbeddableCartridges()),
							application);
			IStatus result = WizardUtils.runInWizard(job, job.getDelegatingProgressMonitor(), getContainer());
			if (!result.isOK()) {
				safeRefreshSelectedEmbeddedCartridges();
			} else {
				openLogDialog(job.getAddedCartridges(), job.isTimeouted(result));
			}
			new FireExpressConnectionsChangedJob(wizardModel.getConnection()).schedule();
			return result.isOK();
		} catch (Exception e) {
			String errorMessage = NLS.bind("Could not embed cartridge(s) for application {0}", application.getName());
			IStatus status = ExpressUIActivator.createErrorStatus(errorMessage, e);
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
					ExpressUIActivator.log(e);
				}
			}
		});
	}

	@Override
	public void addPages() {
		addPage(this.embeddedCartridgesWizardPage = new EmbeddedCartridgesWizardPage(wizardModel, this));
	}
	
	private static class ExistingApplicationProperties implements IApplicationProperties {
		
		private IApplication application;

		ExistingApplicationProperties(IApplication application) {
			this.application = application;
		}

		@Override
		public IStandaloneCartridge getStandaloneCartridge() {
			return application.getCartridge();
		}
		
		@Override
		public ApplicationScale getApplicationScale() {
			return application.getApplicationScale();
		}
		
		@Override
		public String getApplicationName() {
			return application.getName();
		}
	}
}
