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

	public static class NullCondition implements ISchedulingCondition {

		@Override
		public boolean isFullfilled(Job preceedingJob) {
			return true;
		}
	}

	public static class SuccessfullyDoneCondition implements ISchedulingCondition {

		@Override
		public boolean isFullfilled(Job preceedingJob) {
			return preceedingJob != null
					&& preceedingJob.getResult() != null
					&& preceedingJob.getResult().isOK();
		}
		
	}
	
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
	 * Runs the given job if it's predecessor was executed (successfully or not) and the given condition is fullfilled.
	 *  
	 * @param job to be scheduled if its predecessor was executed
	 * @return
	 */
	public JobChainBuilder runWhenDoneIf(final ISchedulingCondition condition, Job constrainedJob) {
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				if(!progressMonitor.isCanceled()
						&& (condition == null 
							|| condition.isFullfilled(job))) {
					constrainedJob.schedule();
				}
			}
			
		});
		return new JobConstraint(constrainedJob, this);
	}

	/**
	 * Runs the given job if it's predecessor was executed (successfully or not).
	 *  
	 * @param job to be scheduled if its predecessor was executed
	 * @return
	 */
	public JobChainBuilder runWhenDone(final Job constrainedJob) {
		return runWhenDoneIf(new NullCondition(), constrainedJob);
	}

	/**
	 * Runs the given job if it's predecessor executed successfully.
	 *  
	 * @param job to be scheduled if its predecessor was executed successfully
	 * @return
	 */
	public JobChainBuilder runWhenSuccessfullyDone(final Job constrainedJob) {
		return runWhenDoneIf(new SuccessfullyDoneCondition(), constrainedJob);
	}

	/**
	 * Builds the chain of jobs. Wont schedule anything.
	 * 
	 * @return
	 */
	public Job build() {
		return job;
	}

	/**
	 * Schedules the whole chain of jobs
	 */
	public void schedule() {
		if(!progressMonitor.isCanceled()) {
			job.schedule();
		}
	}

	/**
	 * Cancels jobs that are not executed yet.
	 */
	public void cancel() {
		progressMonitor.setCanceled(true);
	}

	public class JobConstraint extends JobChainBuilder {

		private JobChainBuilder builder;

		private JobConstraint(Job job, JobChainBuilder builder) {
			super(job, builder.progressMonitor);
			this.builder = builder;
		}
		
		/**
		 * Builds the chain of jobs. Wont schedule anything.
		 * 
		 * @return
		 */
		public Job build() {
			return builder.build();
		}

		/**
		 * Schedules the whole chain of jobs
		 */
		public void schedule() {
			builder.schedule();
		}

	}
	
	public static interface ISchedulingCondition {
		public boolean isFullfilled(Job preceedingJob);
	}
}
