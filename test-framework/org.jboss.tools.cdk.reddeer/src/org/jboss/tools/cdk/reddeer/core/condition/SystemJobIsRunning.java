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
package org.jboss.tools.cdk.reddeer.core.condition;

import org.eclipse.core.runtime.jobs.Job;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.core.condition.JobIsRunning;

/**
 * Specific condition class testing existence of matching system job(s)
 * @author odockal
 *
 */
public class SystemJobIsRunning extends JobIsRunning {

	public static final Logger log = Logger.getLogger(SystemJobIsRunning.class);

	@SuppressWarnings("rawtypes")
	private Matcher[] consideredJobs;

	public SystemJobIsRunning(Matcher<?> matcher) {
		this(new Matcher[] { matcher });
	}

	public SystemJobIsRunning(Matcher<?>[] consideredJobs) {
		this.consideredJobs = consideredJobs;
	}

	/* (non-Javadoc)
	 * @see org.jboss.reddeer.common.condition.WaitCondition#test()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean test() {
		Job[] currentJobs = Job.getJobManager().find(null);
		for (Job job : currentJobs) {

			if (CoreMatchers.anyOf(consideredJobs).matches(job.getName())) {
				log.debug("  job '%s' has no excuses, wait for it", job.getName());
				return true;
			}
		}
		return false;
	}
}
