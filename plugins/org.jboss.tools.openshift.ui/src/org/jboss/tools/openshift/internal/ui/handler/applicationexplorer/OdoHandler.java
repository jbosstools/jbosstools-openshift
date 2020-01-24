/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import java.util.function.Consumer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author Red Hat Developers
 */
public abstract class OdoHandler extends AbstractHandler {


	protected void executeInJob(String name, Runnable action, Consumer<IStatus> notification) {
		Job job = Job.create(name, monitor -> {
			try {
				action.run();
				return Status.OK_STATUS;
			}
			catch (Exception e) {
				return OpenShiftUIActivator.statusFactory().errorStatus(e);
			}
		});
		if (notification != null) {
			job.addJobChangeListener(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					notification.accept(event.getResult());
				}
			});
		}
		job.schedule();
	}
	
	protected void executeInJob(String name, Runnable action) {
		executeInJob(name, action, null);
	}
}
