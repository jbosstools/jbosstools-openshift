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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.ICompletable;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.NewServerWizardBehaviourCallback;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

public class ExpressWizardFragment extends WizardFragment implements ICompletable {
	private IWizardHandle handle;
	private ExpressDetailsComposite composite;
	private NewServerWizardBehaviourCallback  callback;
	
	public ExpressWizardFragment() {
		// TODO Auto-generated constructor stub
	}
	
	public boolean hasComposite() {
		return true;
	}
	public void setComplete(boolean complete) {
		super.setComplete(complete);
	}

	
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		this.handle = handle;
		handle.setTitle("Create an Openshift Server");
		handle.setDescription("Create an Openshift Server adapter by typing in your credentials and choosing an application.");
		callback = new NewServerWizardBehaviourCallback(getTaskModel(), handle, this) {
			public void executeLongRunning(Job j) {
				// depends on COMMON, DAMN
				IWizardContainer container = ((WizardPage)handle).getWizard().getContainer();
				try {
					WizardUtils.runInWizard(j, null, container);
				} catch(Exception e) {
					// TODO clean
				}
			}
		};
		composite = ExpressDetailsComposite.createComposite(parent,callback, ExpressServerUtils.EXPRESS_SOURCE_MODE, true);
		setComplete(false);
		return composite.getComposite();
	}
		
	public void enter() {
		// do nothing
	}
	
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		super.performFinish(monitor);
		IUser user = composite.getUser();
		UserModel.getDefault().addUser(user);
		IApplication app = composite.getApplication();
		try {
			// Only clone and import if there's no project already in existence
			IProject p = ExpressServerUtils.findProjectForApplication(app);
			if( p == null ) {
				// clone and import
				
				// If we had to clone and import, we also need to add the module ??
			}
		} catch(OpenShiftException ose ) {
			
		} 
	}
}
