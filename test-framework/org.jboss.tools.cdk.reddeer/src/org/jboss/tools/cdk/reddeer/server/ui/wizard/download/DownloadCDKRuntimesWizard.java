/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui.wizard.download;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.api.Shell;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Represent download runtime wizard reddeer implementation
 * @author odockal
 *
 */
public class DownloadCDKRuntimesWizard extends WizardDialog {
	
	public DownloadCDKRuntimesWizard() {
		super(CDKLabel.Shell.DOWNLOAD_RUNTIMES);
	}

	@Override
	public void finish() {
		log.info("Finish wizard");
		Button button = new PushButton(this, "Finish");
		button.click();
		new WaitUntil(new JobIsRunning(), TimePeriod.LONG, false);
		new WaitWhile(new RuntimeIsDownloading(), TimePeriod.getCustom(600));
	}
	
	private class RuntimeIsDownloading extends AbstractWaitCondition{

		@Override
		public boolean test() {
			if(new ShellIsAvailable("Question").test()){
				Shell questionShell = new DefaultShell("Question");
				new PushButton(questionShell, "Yes To All").click();
				new WaitWhile(new ShellIsAvailable(questionShell));
			}
			return new JobIsRunning().test();
		}

		@Override
		public String description() {
			return "Runtime is downloading";
		}

	}
	
}
