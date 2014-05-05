/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EditEmbeddedCartridgesWizard;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Andre Dietisheim
 */
public class EditCartridgesHandler extends AbstractDomainHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IApplication application = getApplication(HandlerUtil.getCurrentSelection(event));
		if (application != null) {
			// openshift explorer
			return openEditEmbeddedCartridgesWizard(application, HandlerUtil.getActiveShell(event));
		} else {
			// servers view
			IServer server = (IServer) 
					UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IServer.class);
			if (server == null) {
				return OpenShiftUIActivator.createErrorStatus("Could not find application to restart");
			}
			final LoadApplicationJob job = new LoadApplicationJob(server);
			new JobChainBuilder(job)
					.runWhenSuccessfullyDone(new UIJob("Open Edit Embedded Cartridges wizard") {

						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							IApplication application = job.getApplication();
							if (application == null) {
								return OpenShiftUIActivator
										.createCancelStatus("Could not find application to edit the embedded cartridges of");
							}
							return openEditEmbeddedCartridgesWizard(application, HandlerUtil.getActiveShell(event));
						}
					})
					.schedule();
			return Status.OK_STATUS;
		}
	}

	protected IApplication getApplication(ISelection selection) {
		IApplication application = UIUtils.getFirstElement(selection, IApplication.class);
		if (application == null) {
			IEmbeddedCartridge cartridge = UIUtils.getFirstElement(selection, IEmbeddedCartridge.class);
			if (cartridge != null) {
				application = cartridge.getApplication();
			}
		}
		return application;
	}
	
	protected IStatus openEditEmbeddedCartridgesWizard(IApplication application, Shell shell) {
		try {
			WizardUtils.openWizardDialog(
					new EditEmbeddedCartridgesWizard(application, 
							ConnectionsModelSingleton.getInstance().getConnectionByResource(application)), 
							shell);
			return Status.OK_STATUS;
		} catch (OpenShiftException e) {
			Logger.error("Failed to edit cartridges", e);
			return OpenShiftUIActivator.createErrorStatus(
					NLS.bind("Failed to edit cartridges for application {0}", application.getName()), e);
		}
	}
}
