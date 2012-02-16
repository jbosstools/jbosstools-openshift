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

import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

public class ExpressBinaryUI implements IDeploymentTypeUI {
	private ExpressDetailsComposite composite;
	private IServerModeUICallback wrapped;
	public ExpressBinaryUI() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void fillComposite(Composite parent, IServerModeUICallback callback) {
		parent.setLayout(new FillLayout());
		wrapped = wrapCallback(callback);
		
		composite = ExpressDetailsComposite.createComposite(parent,wrapped, 
				ExpressServerUtils.EXPRESS_BINARY_MODE, true);
	}

	private IServerModeUICallback wrapCallback(final IServerModeUICallback inner) {
		return new IServerModeUICallback() {
			public IServerWorkingCopy getServer() {
				return inner.getServer();
			}
			public IRuntime getRuntime() {
				return inner.getRuntime();
			}
			public void execute(IUndoableOperation operation) {
				inner.execute(operation);
			}
			public void executeLongRunning(Job j) {
				if( inner instanceof DeploymentTypeUIUtil.NewServerWizardBehaviourCallback) {
					IWizardHandle handle = ((DeploymentTypeUIUtil.NewServerWizardBehaviourCallback)inner).getHandle();
					// depends on COMMON, DAMN
					IWizardContainer container = ((WizardPage)handle).getWizard().getContainer();
					try {
						WizardUtils.runInWizard(j, null, container);
					} catch(Exception e) {
						// TODO clean
					}
				} else {
					inner.executeLongRunning(j);
				}
			}
			public void setErrorMessage(String msg) {
				inner.setErrorMessage(msg);
			}
			public Object getAttribute(String key) {
				return null;
			}
		};
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
	}
	
}
