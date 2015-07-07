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
package org.jboss.tools.openshift.common.ui.wizard;

import org.eclipse.jface.wizard.Wizard;

/**
 * @author André Dietisheim
 */
public class AbstractOpenShiftWizard<MODEL> extends Wizard {

	private MODEL model;

	public AbstractOpenShiftWizard(String title, MODEL model) {
		this.model = model;
		setNeedsProgressMonitor(true);
		setWindowTitle(title);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	protected MODEL getModel() {
		return model;
	}
}
