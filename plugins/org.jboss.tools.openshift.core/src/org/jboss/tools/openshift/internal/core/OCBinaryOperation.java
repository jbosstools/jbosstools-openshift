/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;

import com.openshift.restclient.OpenShiftContext;
import com.openshift.restclient.capability.IBinaryCapability;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public abstract class OCBinaryOperation {

	/**
	 * Runs the operation
	 * @param status the status to update during the operation
	 */
	public void run(final MultiStatus status) {
		String oldLocation = OpenShiftContext.get().get(IBinaryCapability.OPENSHIFT_BINARY_LOCATION);
		String location = OCBinary.getInstance().getLocation();
		OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, location);
		try {
			runOCBinary(status);
		} finally {
			if (!StringUtils.isEmpty(oldLocation)) {
				OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, oldLocation);
			}
		}
	}
	
	protected abstract void runOCBinary(MultiStatus multiStatus);
	
	/**
	 * Creates an {@link RSync}
	 * @param server the {@link IServer} on which the {@code rsync} operation will be performed
	 * @return the {@link RSync} to be used to execute the command.
	 * @throws CoreException
	 */
	public static RSync createRSync(final IServer server) throws CoreException {
		final String location = OCBinary.getInstance().getLocation();
		if( location == null ) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					"Binary for oc-tools could not be found. Please open the OpenShift 3 Preference Page and set the location of the oc binary."));
		}
		
		final IService service = OpenShiftServerUtils.getService(server);
		if (service == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the service to publish to.", server.getName())));
		}

		final String podPath = OpenShiftServerUtils.getPodPath(server);
		if (StringUtils.isEmpty(podPath)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the destination directory to publish to.", server.getName())));
		}
		
		return new RSync(service, podPath, server);
	}


}