/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IBuildTriggerable;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IResource;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class StartBuildJob extends AbstractDelegatingMonitorJob {

	private IResource buildsource;

	public StartBuildJob(IResource buildsource) {
		super("Start Build Job");
		this.buildsource = buildsource;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Starting build job", IProgressMonitor.UNKNOWN);
			IBuild build = buildsource.accept(new CapabilityVisitor<IBuildTriggerable, IBuild>() {

				@Override
				public IBuild visit(IBuildTriggerable triggerable) {
					return triggerable.trigger();
				}
			}, null);
			if(build == null) {
				return new Status(Status.INFO, OpenShiftUIActivator.PLUGIN_ID, "Manually triggering builds is unsupported");
			}
			return Status.OK_STATUS;
		}catch(OpenShiftException e) {
			return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, NLS.bind("Error starting build {0}.", buildsource.getName()), e);
		}finally {
			monitor.done();
		}
	}

}
