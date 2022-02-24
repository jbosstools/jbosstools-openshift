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

import java.util.Arrays;

import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.TreeItemRegexMatcher;
import org.eclipse.reddeer.core.matcher.WithLabelMatcher;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.wizard.WizardPage;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.list.DefaultList;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * Create component wizard page implemented with RedDeer(for OpenShift
 * Application Explorer).
 * 
 * @author jkopriva@redhat.com
 *
 */
public class CreateComponentWizadPage extends WizardPage {

	public CreateComponentWizadPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	public void setComponentName(String newName) {
		new LabeledText(OpenShiftLabel.TextLabels.NAME).setText(newName);
	}

	public String getComponentName() {
		return new LabeledText(OpenShiftLabel.TextLabels.NAME).getText();
	}

	public void setEclipseProject(String eclipseProjectName) {
		new LabeledText(OpenShiftLabel.TextLabels.ECLIPSE_PROJECT).setText(eclipseProjectName);
	}

	public String getEclipseProject() {
		return new LabeledText(OpenShiftLabel.TextLabels.ECLIPSE_PROJECT).getText();
	}

	@SuppressWarnings("unchecked")
	public void selectComponentType(String componentType, boolean devfile) {
		try {
			DefaultTree tree = new DefaultTree(new WithLabelMatcher(OpenShiftLabel.TextLabels.COMPONENT_TYPE));
			if (devfile) {
				tree.selectItems(tree.getItem(new TreeItemRegexMatcher(".*" + componentType + ".*")));
			} else {
				tree.selectItems(tree.getItem(OpenShiftLabel.TextLabels.S2I_NODE, componentType));
			}
		} catch (CoreLayerException exc) {
			DefaultList list = new DefaultList(new WithLabelMatcher(OpenShiftLabel.TextLabels.COMPONENT_TYPE));
			String filter = "";
			if (devfile) {
				filter = Arrays.asList(list.getListItems()).stream().filter(item -> item.toLowerCase().contains(componentType)).findFirst().get();				
			} else {
				filter = Arrays.asList(list.getListItems()).stream().filter(item -> item.toLowerCase().contains(componentType)).findFirst().get();
			}
			list.select(filter);
		}
	}

	public String getSelectedComponentType() {
		return new LabeledCombo(OpenShiftLabel.TextLabels.COMPONENT_TYPE).getSelection();
	}

	public void selectComponentVersion(String componentVersion) {
		new LabeledCombo(OpenShiftLabel.TextLabels.COMPONENT_VERSION).setSelection(componentVersion);
	}

	public String getSelectedComponentVersion() {
		return new LabeledCombo(OpenShiftLabel.TextLabels.COMPONENT_VERSION).getSelection();
	}

	public void setApplication(String application) {
		new LabeledText(OpenShiftLabel.TextLabels.APPLICATION).setText(application);
	}

	public String getApplication() {
		return new LabeledText(OpenShiftLabel.TextLabels.APPLICATION).getText();
	}

	public void clickPushAfterCreate() {
		new PushButton(OpenShiftLabel.TextLabels.PUSH_AFTER_CREATE).click();
	}

	public void selectStarter(String starter) {
		new LabeledCombo(OpenShiftLabel.TextLabels.PROJECT_STARTER).setSelection(starter);
	}

	public String getSelectedStarter() {
		return new LabeledCombo(OpenShiftLabel.TextLabels.PROJECT_STARTER).getSelection();
	}
}