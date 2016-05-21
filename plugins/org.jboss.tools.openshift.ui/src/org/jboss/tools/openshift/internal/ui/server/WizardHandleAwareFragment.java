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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 * @author Andre Dietisheim
 */
public class WizardHandleAwareFragment extends WizardFragment {

	private IWizardHandle handle;

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		setHandle(handle);
		return super.createComposite(parent, handle);
	}

	/**
	 * Has to be called if sub-class in overrided createComposite(Composite, IWizardHandle) 
	 * does not call super method.
	 * @param handle
	 */
	protected void setHandle(IWizardHandle handle) {
		this.handle = handle;
	}

	public IWizardHandle getWizardHandle() {
		return handle;
	}

	/**
	 * Helper method to extract wizard container from the handle.
	 * May return null, if composite is not yet created or if wizard is disposed.
	 *
	 * @return
	 */
	protected IWizardContainer getWizardContainer() {
		if(!(handle instanceof WizardPage)) {
			return null;
		}
		WizardPage page = (WizardPage)handle;
		if(page.getShell() == null || page.getShell().isDisposed()) {
			return null;
		}
		IWizard wizard = page.getWizard();
		return (wizard == null) ? null : wizard.getContainer();
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		super.performFinish(monitor);
		setHandle(null);
	}

	@Override
	public void performCancel(IProgressMonitor monitor) throws CoreException {
		setHandle(null);
	}
}
