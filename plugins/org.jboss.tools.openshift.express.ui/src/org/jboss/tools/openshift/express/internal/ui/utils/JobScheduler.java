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
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * @author Andre Dietisheim
 */
public class JobScheduler {

	private Job job;

	public JobScheduler(Job job) {
		this.job = job;
	}

	public class ChainedJob {
		private Job job;

		private ChainedJob(Job job) {
			this.job = job;
		}

		public ChainedJob andWhenDone(final Job constrainedJob) {
			job.addJobChangeListener(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					constrainedJob.schedule();
				}});
			return new ChainedJob(constrainedJob);
		}
	}
}
