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
package org.jboss.tools.openshift.express.internal.core.util;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * @author Andre Dietisheim
 */
public class JobChainBuilder {

	private Job job;

	public JobChainBuilder(Job job) {
		this.job = job;
	}

	public JobConstraint andRunWhenDone(Job constrainedJob) {
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
					constrainedJob.schedule();
				}});
			return new JobConstraint(constrainedJob);
		}

		public void schedule() {
			JobChainBuilder.this.job.schedule();
		}
		
		public Job build() {
			return JobChainBuilder.this.job;
		}
	}
}
