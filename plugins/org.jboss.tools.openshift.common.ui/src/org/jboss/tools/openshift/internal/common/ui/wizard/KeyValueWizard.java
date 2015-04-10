/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.wizard;

import org.eclipse.jface.wizard.Wizard;

/**
 * Wizard for editing key value pairs (e.g. Env vars, labels)
 * 
 * @author jeff.cantrill
 * 
 * @param <T> A specific key value item 
 */
public class KeyValueWizard<T extends IKeyValueItem> extends Wizard {

	private IKeyValueWizardModel<T> model;
	
	/**
	 * Used to create a new key/value pairs
	 * 
	 * @param model  the wizard model
	 */
	public KeyValueWizard(IKeyValueWizardModel<T> model) {
		this(null, model);
	}
	
	/**
	 * Used to edit an existing key value pair
	 * 
	 * @param variable the variable that shall get edited
	 * @param model 
	 */
	public KeyValueWizard(T variable, IKeyValueWizardModel<T> model) {
		this.model = model;
		setWindowTitle(model.getWindowTitle());
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	public void addPages() {
		addPage(new KeyValueWizardPage<T>(this, model));
	}
}
