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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author Red Hat Developers
 */
public abstract class OdoHandler extends AbstractHandler {

	protected static void executeInJob(String name, Consumer<IProgressMonitor> action) {
		Job job = Job.create(name, monitor -> {
			try {
				action.accept(monitor);
				return Status.OK_STATUS;
			}
			catch (Exception e) {
				return OpenShiftUIActivator.statusFactory().errorStatus(e);
			}
		});
		job.schedule();
	}
}
