/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.ICompletable;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.NewServerWizardBehaviourCallback;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.common.ui.WizardUtils;

/**
 * @author Rob Stryker
 */
public class OpenShiftServerWizardFragment extends WizardFragment implements ICompletable {

	private OpenShiftServerWizardComposite composite;
		
	public boolean hasComposite() {
		return true;
	}

	public void setComplete(boolean complete) {
		super.setComplete(complete);
	}

	public boolean isComplete() {
		return composite != null && composite.createErrorMessage() == null;
	}
	
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		handle.setTitle("Create an OpenShift Server");
		handle.setDescription("Create an OpenShift Server by choosing your connection, application and deploy project.");
		IServerModeUICallback callback = new NewServerWizardBehaviourCallback(getTaskModel(), handle, this) {
			public void executeLongRunning(Job j) {
				// depends on COMMON, DAMN
				IWizardContainer container = ((WizardPage)handle).getWizard().getContainer();
				try {
					WizardUtils.runInWizard(j, container);
				} catch(Exception e) {
					// TODO clean
				}
			}
		};
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		this.composite = new OpenShiftServerWizardComposite(composite, callback);
		return this.composite.getComposite();
	}
		
	public void enter() {
		// do nothing
	}
	
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		super.performFinish(monitor);
		composite.performFinish(monitor);
		composite = null;
	}
	public void performCancel(IProgressMonitor monitor) throws CoreException {
		composite = null;
	}

}
