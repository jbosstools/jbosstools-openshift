/*******************************************************************************
 * Copyright (c) 2007-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.basic;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.AfterClass;

/**
 * The purpose of this class is to set up/clean up environment to help isolate tests and to avoid code duplication.
 * @author jkopriva@redhat.com
 *
 */
public abstract class AbstractTest {
	
	@AfterClass
	public static void cleanUpAfterTest() {
		//Clean up projects from Project Explorer
		new ProjectExplorer().deleteAllProjects();
		//Close all shells if test fails (could interfere next tests)
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
		//Kill running jobs if any
		OpenShiftUtils.killJobs();
	}
	
	protected void waitForLog(OpenShiftResource pod, String podLogContextMenuItem) {
		new WaitUntil(
				new AbstractWaitCondition() {

					@Override
					public boolean test() {
						pod.select();
						new ContextMenuItem(podLogContextMenuItem).select();
						try {
							new DefaultShell(OpenShiftLabel.Shell.LOGS_UNAVAILABLE);
							new OkButton().click();
							return false;
						} catch (RedDeerException e) {
							// catched intentionnally
							System.err.println(e);
						}
						return true;
					}}
				, TimePeriod.VERY_LONG);
	}
	

}
