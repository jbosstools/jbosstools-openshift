/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.importapp;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.YesButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * @author jnovak@redhat.com
 * @contributor jkopriva@redhat.com
 */
public class ImportApplicationWizard extends WizardDialog {
	
	public ImportApplicationWizard() {
		super(OpenShiftLabel.Shell.IMPORT_APPLICATION);
	}
	
	public void selectExistingBuildConfiguration(String buildConfigurationName) {
		for(TreeItem ti : new DefaultTree().getItems()) {
			if (ti.getText().contains(buildConfigurationName)) {
				ti.select();
				break;
			}	
		}
	}
	
	public void finishAndOverrideExisting(){
		new FinishButton().click();
		
		new DefaultShell("Overwrite project(s) ?");
		new YesButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	public void finish(){
		new FinishButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
}
