/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder.JobConstraint;
import org.junit.Assert;
import org.junit.Test;

public class JobChainBuilderTest {
	static int MAX_WAIT_TIME = 60 * 1000;

	@Test
	public void testCancelingJobChain() {
		JobCounter jc = new JobCounter(2);
		JobChainBuilder jcb = createJobChainBuilder(jc);
		jcb.schedule();
		wait(jc, 2, 1000);
		Assert.assertEquals(2, jc.done);

		jc = new JobCounter(-1);
		jcb = createJobChainBuilder(jc);
		jcb.schedule();
		wait(jc, 4, 0);
		Assert.assertEquals(4, jc.done);		
	}

	void wait(JobCounter jc, int waitForJobs, int waitThenTime) {
		long t = System.currentTimeMillis();
		long dt = 0;
		while(jc.done < waitForJobs && dt < MAX_WAIT_TIME) {
			try {
				 Thread.sleep(200);
			} catch (InterruptedException e) {
				Assert.fail("Wait interrupted");
			}
			dt = System.currentTimeMillis() - t;
		}
		if(dt > MAX_WAIT_TIME) {
			Assert.fail("Failed to wait for " + waitForJobs + " jobs. " 
					+ jc.done + " jobs are done."); 
		}
		try {
			 Thread.sleep(waitThenTime);
		} catch (InterruptedException e) {
			Assert.fail("Wait interrupted");
		}
	}

	class JobCounter {
		int done = 0;
		JobChainBuilder builder;
		int cancelAt;
		public JobCounter(int cancelAt) {
			this.cancelAt = cancelAt;
		}

		void jobDone(String name) {
			done++;
			if(done == cancelAt) {
				builder.cancel();
			}
		}
	}

	public JobChainBuilder createJobChainBuilder(JobCounter jc) {
		JobChainBuilder result = new JobChainBuilder(createJob("Job 1", jc));
		JobConstraint c1 =	result.runWhenDone(createJob("Job 2", jc));
		JobConstraint c2 =	c1.runWhenDone(createJob("Job 3", jc));
		JobConstraint c3 =	c2.runWhenDone(createJob("Job 4", jc));
		jc.builder = result;
		return result;
	}

	Job createJob(final String name, final JobCounter jc) {
		return new Job(name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				jc.jobDone(getName());
				return Status.OK_STATUS;
			}
			
		};
	}
}
