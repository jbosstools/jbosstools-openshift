/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.page;

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.wizard.WizardPage;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * Create service wizard page implemented with RedDeer(for OpenShift Application Explorer).
 * 
 * @author jkopriva@redhat.com
 *
 */
public class CreateServiceWizadPage  extends WizardPage {

	public CreateServiceWizadPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	public void setServiceName(String name) {
		new LabeledText(OpenShiftLabel.TextLabels.NAME).setText(name);	
	}
	
	public String getServiceName() {
		return new LabeledText(OpenShiftLabel.TextLabels.NAME).getText();	
	}
	
	public void setService(String service) {
		new LabeledCombo(OpenShiftLabel.TextLabels.SERVICE).setSelection(service);
	}
	
	public String getService() {
		return new LabeledCombo(OpenShiftLabel.TextLabels.SERVICE).getSelection();		
	}

	public void setComponentType(String componentType) {
		new LabeledCombo(OpenShiftLabel.TextLabels.TYPE).setSelection(componentType);	
	}
	
	public String getComponentType() {
		return new LabeledCombo(OpenShiftLabel.TextLabels.TYPE).getSelection();	
	}

	public void setApplication(String applicationName) {
		new LabeledText(OpenShiftLabel.TextLabels.APPLICATION).setText(applicationName);
	}
	
	public String getApplication() {
		return new LabeledText(OpenShiftLabel.TextLabels.APPLICATION).getText();	
	}
}
