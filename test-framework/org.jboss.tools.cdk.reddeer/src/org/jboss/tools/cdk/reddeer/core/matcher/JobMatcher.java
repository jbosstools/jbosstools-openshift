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
package org.jboss.tools.cdk.reddeer.core.matcher;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jboss.reddeer.common.exception.RedDeerException;

/**
 * Matcher class matching given string parameter, it is used in SystemJobIsRunning wait condition
 * @author odockal
 *
 */
public class JobMatcher extends BaseMatcher<Job> {

	private Job jobToMatch;
	
	public JobMatcher(String jobName) {
		this.jobToMatch = new Job(jobName) {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
	
	public JobMatcher(Job job) {
		this.jobToMatch = job;
	}
	
	@Override
	public void describeTo(Description description) {
		description.appendText("Job with name ");
		description.appendValue(jobToMatch.getName());
		description.appendText(" matches running job");
	}

	@Override
	public boolean matches(Object item) {
		if (jobToMatch != null && item != null) {
			return jobToMatch.getName().equalsIgnoreCase(item.toString());
		} else {
			throw new RedDeerException("Null parameter was passed into matches method");
		}
	}
}
