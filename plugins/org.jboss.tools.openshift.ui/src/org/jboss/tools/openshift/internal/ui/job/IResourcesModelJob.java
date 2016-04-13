/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.job;

import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.foundation.core.jobs.DelegatingProgressMonitor;

public interface IResourcesModelJob extends IResourcesModel {
	
	void addJobChangeListener(IJobChangeListener listener);
	
	DelegatingProgressMonitor getDelegatingProgressMonitor();
	
	Job getJob();
	
	Runnable getSummaryRunnable(Shell shell);

}
