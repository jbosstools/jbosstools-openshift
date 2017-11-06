/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui.wizard;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.workbench.core.condition.JobIsKilled;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;

/**
 * Class represents New Server Wizard, it is required because
 * here it can be overriden finish and cancel that only kills Refreshing Server adapter list job 
 * when such job has started unlike is implemented in NewServerWizard class
 * @author odockal
 *
 */
public class NewCDKServerWizard extends NewMenuWizard {
	
	public static final String TITLE = "New Server";
	
	/**
	 * Instantiates a new new server wizard dialog.
	 */
	public NewCDKServerWizard() {
		super(TITLE, "Server", "Server");
	}
	
	@Override
	public void finish(TimePeriod timeout) {
		// workaround 
		try {
			// wait for job to start
			new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM);
			// job has started - try to kill it
			new WaitUntil(new JobIsKilled("Refreshing server adapter list"), TimePeriod.LONG, false);
		} catch (WaitTimeoutExpiredException exc) {
			// do nothing because job did not start
		}
		super.finish(timeout);
	}

	@Override
	public void cancel() {
		try {
			// wait for job to start
			new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM);
			// job has started - try to kill it
			new WaitUntil(new JobIsKilled("Refreshing server adapter list"), TimePeriod.LONG, false);
		} catch (WaitTimeoutExpiredException exc) {
			// do nothing because job did not start
		}
		super.cancel();
	}
}
