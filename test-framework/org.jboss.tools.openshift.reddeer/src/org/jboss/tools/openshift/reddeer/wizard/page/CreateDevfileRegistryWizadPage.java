/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
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
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * Create registry wizard page implemented with RedDeer(for OpenShift Application Explorer).
 * 
 * @author jkopriva@redhat.com
 *
 */
public class CreateDevfileRegistryWizadPage  extends WizardPage {

	public CreateDevfileRegistryWizadPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	public void setRegistryName(String name) {
		new LabeledText(OpenShiftLabel.TextLabels.NAME).setText(name);	
	}
	
	public String getRegistryName() {
		return new LabeledText(OpenShiftLabel.TextLabels.NAME).getText();	
	}

	public void setURL(String url) {
		new LabeledText(OpenShiftLabel.TextLabels.URL).setText(url);
	}
	
	public String getURL() {
		return new LabeledText(OpenShiftLabel.TextLabels.URL).getText();	
	}

	public void setSecure(boolean secure) {
		new CheckBox(OpenShiftLabel.TextLabels.SECURE).toggle(secure);	
	}
}
