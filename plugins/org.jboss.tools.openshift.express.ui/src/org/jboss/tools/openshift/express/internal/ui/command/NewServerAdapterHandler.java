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
package org.jboss.tools.openshift.express.internal.ui.command;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.ui.internal.Messages;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.internal.wizard.fragment.ModifyModulesWizardFragment;
import org.eclipse.wst.server.ui.internal.wizard.fragment.NewServerWizardFragment;
import org.eclipse.wst.server.ui.internal.wizard.fragment.TasksWizardFragment;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.server.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.server.ExpressServerWizardTaskModelUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * @author Rob Stryker
 * @author Andre Dietisheim
 * 
 */
public class NewServerAdapterHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IApplication application = 
				UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		if (application == null) {
			return ExpressUIActivator
					.createCancelStatus("Cannot create server adapter: no application selected.");
		}
		
		try {
			NewServerWizard wizard = new NewServerWizard(ExpressServerUtils.EXPRESS_SERVER_TYPE);
			ExpressServerWizardTaskModelUtil.put(application, wizard.getTaskModel());
			final IDomain domain = application.getDomain();
			ExpressServerWizardTaskModelUtil.put(domain, wizard.getTaskModel());
			ExpressConnection connection = ExpressConnectionUtils.getByResource(application, ConnectionsRegistrySingleton.getInstance());
			if (connection == null) {
				return ExpressUIActivator.createCancelStatus(NLS.bind(
						"Cannot create server adapter: connection for application {0} not found.",
						application.getName()));
			}
			ExpressServerWizardTaskModelUtil.put(connection, wizard.getTaskModel());
			WizardUtils.openWizardDialog(400, 700, wizard, HandlerUtil.getActiveShell(event));
			return Status.OK_STATUS;
		} catch (OpenShiftException e) {
			return ExpressUIActivator.createErrorStatus(NLS.bind("Could not create OpenShift server adapter for application {0}", application.getName()), e);
		}
	}

	public class NewServerWizard extends TaskWizard implements INewWizard {
		public NewServerWizard(final String serverType) {
			super(Messages.wizNewServerWizardTitle,
					new WizardFragment() {
						protected void createChildFragments(List<WizardFragment> list) {
							list.add(new NewServerWizardFragment(null, serverType));

							list.add(WizardTaskUtil.TempSaveRuntimeFragment);
							list.add(WizardTaskUtil.TempSaveServerFragment);

							list.add(new ModifyModulesWizardFragment());
							list.add(new TasksWizardFragment());

							list.add(WizardTaskUtil.SaveRuntimeFragment);
							list.add(WizardTaskUtil.SaveServerFragment);
							list.add(WizardTaskUtil.SaveHostnameFragment);
						}
					});
		}

		public void init(IWorkbench newWorkbench, IStructuredSelection newSelection) {
			// do nothing
		}
	}

}
