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
		this.handle = handle;
		return super.createComposite(parent, handle);
	}

	public IWizardHandle getWizardHandle() {
		return handle;
	}

}
