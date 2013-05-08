/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractDelegatingMonitorJob extends Job {

	public static final int OK = 0;
	public static final int TIMEOUTED = 1;

	protected DelegatingProgressMonitor delegatingMonitor;

	public AbstractDelegatingMonitorJob(String name) {
		super(name);
		this.delegatingMonitor = new DelegatingProgressMonitor();
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		delegatingMonitor.add(monitor);
		return doRun(delegatingMonitor);
	}

	public DelegatingProgressMonitor getDelegatingProgressMonitor() {
		return delegatingMonitor;
	}

	protected abstract IStatus doRun(IProgressMonitor monitor);
		
	public boolean isTimeouted(IStatus status){
		return status.getCode() == TIMEOUTED;
	}
}