/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.ICompletable;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.NewServerWizardBehaviourCallback;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.internal.ui.server.FormPresenterSupport.IFormPresenter;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsWizardFragment extends WizardHandleAwareFragment implements ICompletable {

	private ServerSettingsView serverSettingsView;
	
	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		updateHandle(handle);
		IServerModeUICallback callback = new NewServerWizardBehaviourCallback(getTaskModel(), handle, this) {
			public void executeLongRunning(Job job) {
				IWizardContainer container = ((WizardPage)handle).getWizard().getContainer();
					try {
						WizardUtils.runInWizard(job, container);
					} catch (InvocationTargetException | InterruptedException e) {
						// swallowed intentionally
					}
			}
		};
		this.serverSettingsView = new ServerSettingsView(false, callback);
		Composite composite = (Composite) serverSettingsView.createControls(parent);
		DataBindingContext dbc = serverSettingsView.getDataBindingContext();
		new FormPresenterSupport(
				new IFormPresenter() {

					@Override
					public void setMessage(String message, int type) {
						((WizardPage) handle).setMessage(message, type);
					}

					@Override
					public void setComplete(boolean complete) {
						((WizardPage) handle).setPageComplete(complete);
						ServerSettingsWizardFragment.this.setComplete(complete);
						handle.update();
					}

					@Override
					public Control getControl() {
						return parent;
					}
				}, 
			dbc);

		return composite;
	}

	private void updateHandle(IWizardHandle handle) {
		handle.setTitle("Create an OpenShift Server Adapter");
		handle.setDescription("Create an OpenShift Server Adapter by choosing your connection, service and deploy project.");
	}
	
	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		super.performFinish(monitor);
		if (serverSettingsView != null) {
			serverSettingsView.performFinish(monitor);
			serverSettingsView = null;
		}
	}

	@Override
	public void performCancel(IProgressMonitor monitor) throws CoreException {
		serverSettingsView = null;
	}

	@Override
	public void setComplete(boolean complete) {
		super.setComplete(complete);
	}

}
