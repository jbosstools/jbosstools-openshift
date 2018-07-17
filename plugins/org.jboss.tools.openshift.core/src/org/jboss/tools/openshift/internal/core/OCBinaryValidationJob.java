/******************************************************************************* 
 * Copyright (c) 2016-2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;

public class OCBinaryValidationJob extends Job {

	private String location;
	private IStatus ocVersionValidity = StatusFactory.cancelStatus(
			OpenShiftCoreActivator.PLUGIN_ID, "OC version not verified yet.");
	private boolean showLinks;
	private Collection<IOCBinaryRequirement> requirements;

	public OCBinaryValidationJob(String location) {
		this(location, false, OCBinaryValidator.RSYNC_REQUIREMENTS);
	}

	public OCBinaryValidationJob(String location, boolean showLinks, Collection<IOCBinaryRequirement> requirements) {
		super("Checking oc binary...");
		this.location = location;
		this.showLinks = showLinks;
		this.requirements = requirements;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		OCBinaryValidator validator = new OCBinaryValidator(location);
		this.ocVersionValidity = validator.getStatus(monitor, showLinks, requirements);
		return Status.OK_STATUS;
	}

	public IStatus getOCVersionValidity() {
		return ocVersionValidity;
	}
}