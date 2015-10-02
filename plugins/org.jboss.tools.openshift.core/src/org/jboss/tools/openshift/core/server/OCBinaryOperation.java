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
package org.jboss.tools.openshift.core.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;

import com.openshift.restclient.OpenShiftContext;
import com.openshift.restclient.capability.IBinaryCapability;

public abstract class OCBinaryOperation {

	public void run(MultiStatus status) throws CoreException {
		String oldLocation = OpenShiftContext.get().get(IBinaryCapability.OPENSHIFT_BINARY_LOCATION);
		String location = OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
		OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, location);
		try {
			runOCBinary(status);
		} finally {
			if (!StringUtils.isEmpty(oldLocation)) {
				OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, oldLocation);
			}
		}
		if (!status.isOK()) {
			throw new CoreException(status);
		};
		
	}
	
	protected abstract void runOCBinary(MultiStatus multiStatus);
}