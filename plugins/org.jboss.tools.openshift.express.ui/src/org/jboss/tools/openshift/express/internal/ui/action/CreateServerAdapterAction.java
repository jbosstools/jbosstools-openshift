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
package org.jboss.tools.openshift.express.internal.ui.action;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.server.ui.internal.Messages;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.internal.wizard.fragment.ModifyModulesWizardFragment;
import org.eclipse.wst.server.ui.internal.wizard.fragment.NewServerWizardFragment;
import org.eclipse.wst.server.ui.internal.wizard.fragment.TasksWizardFragment;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.explorer.OpenShiftExplorerUtils;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * @author Rob Stryker
 * 
 */
public class CreateServerAdapterAction extends AbstractOpenShiftAction {

	public CreateServerAdapterAction() {
		super(OpenShiftExpressUIMessages.CREATE_SERVER_ADAPTER_ACTION, true);
		setImageDescriptor(OpenShiftImages.EDIT);
	}

	@Override
	public void run() {
		try {
			if (!(getSelection() instanceof ITreeSelection)) {
				return;
			}
			ITreeSelection treeSelection = (ITreeSelection) getSelection();
			IApplication application = UIUtils.getFirstElement(treeSelection, IApplication.class);
			if (application == null) {
				return;
			}
			NewServerWizard w = new NewServerWizard(ExpressServerUtils.OPENSHIFT_SERVER_TYPE);
			ExpressServerUtils.put(application, w.getTaskModel());
			final IDomain domain = application.getDomain();
			ExpressServerUtils.put(domain, w.getTaskModel());
			final IUser user = domain.getUser();
			Assert.isNotNull(user, NLS.bind("application {0} does not reference any user", application.getName()));
			Connection connection = OpenShiftExplorerUtils.getConnectionFor(treeSelection);
			ExpressServerUtils.put(connection, w.getTaskModel());
			WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), w);
			dialog.open();
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log("Could not create OpenShift server", e);
		}
	}

	public class NewServerWizard extends TaskWizard implements INewWizard {
		public NewServerWizard(final String serverType) {
			super(Messages.wizNewServerWizardTitle, new WizardFragment() {
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
