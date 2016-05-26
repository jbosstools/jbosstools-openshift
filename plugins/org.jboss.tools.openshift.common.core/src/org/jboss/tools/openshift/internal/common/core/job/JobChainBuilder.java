/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.core.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * @author Andre Dietisheim
 */
public class JobChainBuilder {

	private Job job;
	private IProgressMonitor progressMonitor;

	public JobChainBuilder(Job job) {
		this(job, new NullProgressMonitor());
	}

	public JobChainBuilder(Job job, IProgressMonitor progressMonitor) {
		this.job = job;
		setProgressMonitor(progressMonitor);
	}

	/**
	 * Sets progress monitor to provide canceling of jobs that are not yet executed.
	 * Provides an alternative approach to cancel() method.
	 * Always sets the inner instance to a non-null value.
	 * @param progressMonitor
	 */
	public JobChainBuilder setProgressMonitor(IProgressMonitor progressMonitor) {
		this.progressMonitor = (progressMonitor == null) ? new NullProgressMonitor() : progressMonitor;
		return this;
	}

	/**
	 * Cancel jobs that are not yet executed.
	 */
	public void cancel() {
		progressMonitor.setCanceled(true);
	}

	public JobConstraint runWhenSuccessfullyDone(Job constrainedJob) {
		return new JobConstraint(job).runWhenSuccessfullyDone(constrainedJob);
	}

	public JobConstraint runWhenDone(Job constrainedJob) {
		return new JobConstraint(job).runWhenDone(constrainedJob);
	}
		
	public class JobConstraint {
		private Job job;

		private JobConstraint(Job job) {
			this.job = job;
		}

		public JobConstraint runWhenDone(final Job constrainedJob) {
			job.addJobChangeListener(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					if(!progressMonitor.isCanceled()) {
						constrainedJob.schedule();
					}
				}});
			return new JobConstraint(constrainedJob);
		}

		public JobConstraint runWhenSuccessfullyDone(final Job constrainedJob) {
			job.addJobChangeListener(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK() && !progressMonitor.isCanceled()) {
						constrainedJob.schedule();
					}
				}});
			return new JobConstraint(constrainedJob);
		}

		public void schedule() {
			if(!progressMonitor.isCanceled()) {
				JobChainBuilder.this.job.schedule();
			}
		}
		
		public Job build() {
			return JobChainBuilder.this.job;
		}
	}
	
	public Job build() {
	    return job;
	}
	
	public void schedule() {
		if(!progressMonitor.isCanceled()) {
			job.schedule();
		}
	}
}
