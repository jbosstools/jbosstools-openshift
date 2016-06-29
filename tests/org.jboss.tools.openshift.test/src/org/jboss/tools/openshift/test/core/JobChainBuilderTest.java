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


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder.ISchedulingCondition;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Viacheslav Kabanovich
 * @author Andre Dietisheim
 *
 */
public class JobChainBuilderTest {
	static int WAIT_FOR_FINISHED = 4 * 1000;

	private JobCounter counter;
	private JobChainBuilder builder;
	
	@Before
	public void setUp() {
		this.counter = new JobCounter(4);
		this.builder = new JobChainBuilder(createJob("Job 1", counter));
		createJobs(3, null, builder, counter);
		counter.setBuilder(builder);
	}
	
	@Test
	public void should_cancel_after_2_jobs() {
		// given
		counter.setCancelAt(2);
		// when
		builder.schedule();
		// then
		assertThat(counter.getDone()).isEqualTo(2);
	}

	@Test
	public void should_schedule_all_jobs_if_no_cancel() {
		// given
		// when
		builder.schedule();
		// then
		assertThat(counter.getDone()).isEqualTo(4);
	}

	@Test
	public void should_schedule_2_jobs_given_condition_that_stops_after_2() {
		// given
		JobCounter counter = new JobCounter(4);
		JobChainBuilder builder = new JobChainBuilder(createJob("Job 1", counter));
		counter.setBuilder(builder);
		createJobs(3, 
				new ISchedulingCondition[] { 
						new JobChainBuilder.NullCondition(),
						new JobChainBuilder.ISchedulingCondition() {

							@Override
							public boolean isFullfilled(Job preceedingJob) {
								return false;
							}},
						new JobChainBuilder.NullCondition() },
				builder, counter);
		// when
		builder.schedule();
		// then
		assertThat(counter.getDone()).isEqualTo(2);
	}

	@Test
	public void should_schedule_4_jobs_given_null_condition_wont_stop_scheduling() {
		// given
		JobCounter counter = new JobCounter(4);
		JobChainBuilder builder = new JobChainBuilder(createJob("Job 1", counter));
		counter.setBuilder(builder);
		createJobs(3, 
				new ISchedulingCondition[] { null, null, null },
				builder, counter);
		// when
		builder.schedule();
		// then
		assertThat(counter.getDone()).isEqualTo(4);
	}

	@Test
	public void should_schedule_3_jobs_given_successfull_condition_not_fullfilled() {
		// given
		JobCounter counter = new JobCounter(3);
		JobChainBuilder builder = new JobChainBuilder(createJob("Job 1", counter));
		counter.setBuilder(builder);
		createJobs(3, 
				new ISchedulingCondition[] { null, null, new JobChainBuilder.SuccessfullyDoneCondition() },
				builder, counter);
		// when
		builder.schedule();
		// then
		assertThat(counter.getDone()).isEqualTo(3);
	}

	private class JobCounter {

		private CountDownLatch countDown;
		private int cancelAt;
		private int numOfJobs;
		private JobChainBuilder builder;
		
		JobCounter(int numOfJobs) {
			this.numOfJobs = numOfJobs;
			this.cancelAt = -1;
			this.countDown =  new CountDownLatch(numOfJobs);
		}
		
		void jobDone() {
			assertThat(builder).isNotNull();

			countDown.countDown();
			if (countDown.getCount() == cancelAt) {
				builder.cancel();
			}
		}
		
		void setBuilder(JobChainBuilder builder) {
			this.builder = builder;
		}
		
		void setCancelAt(int cancelAt) {
			this.cancelAt = cancelAt;
		}

		int getDone() {
			try {
				countDown.await(WAIT_FOR_FINISHED, TimeUnit.MILLISECONDS);
				return numOfJobs - (int) countDown.getCount();
			} catch (InterruptedException e) {
				fail("Waiting for lock was interrupted...");
				return -1;
			}
		}
	}

	private void createJobs(final int numOfJobs, ISchedulingCondition[] conditions, JobChainBuilder builder, JobCounter counter) {
		IStatus[] doneStatus = new IStatus[numOfJobs];
		Arrays.fill(doneStatus, Status.OK_STATUS);
		createJobs(numOfJobs, conditions, doneStatus, builder, counter);
	}

	private void createJobs(final int numOfJobs, ISchedulingCondition[] conditions, IStatus[] doneStatus, JobChainBuilder builder, JobCounter counter) {
		if (conditions == null) {
			conditions = new ISchedulingCondition[] { 
					new JobChainBuilder.NullCondition(),
					new JobChainBuilder.NullCondition(),
					new JobChainBuilder.NullCondition()
			};
		}

		assertThat(conditions.length).isEqualTo(numOfJobs);

		for (int i = 0; i < numOfJobs; i++) {
			builder = builder.runWhenDoneIf(conditions[i], createJob("Job " + (i + 1), counter));
		}
	}

	private Job createJob(final String name, JobCounter counter) {
		return createJob(name, counter, Status.OK_STATUS);
	}

	private Job createJob(final String name, JobCounter counter, IStatus doneStatus) {

		return new Job(name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				counter.jobDone();
				return doneStatus;
			}
		};
	}
}
