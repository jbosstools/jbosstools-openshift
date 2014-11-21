/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.job;

import org.eclipse.osgi.util.NLS;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class StopApplicationJob extends AbstratApplicationJob {

	public StopApplicationJob(IApplication application) {
		super(new ApplicationProvider(application), NLS.bind("Stopping application {0}", application.getName()));
	}

	public StopApplicationJob(LoadApplicationJob applicationJob) {
		super(new ApplicationProvider(applicationJob), NLS.bind("Stopping application {0}", applicationJob.getApplicationName()));
	}

	@Override
	protected void doRun(IApplication application) {
		application.stop();
	}
}
