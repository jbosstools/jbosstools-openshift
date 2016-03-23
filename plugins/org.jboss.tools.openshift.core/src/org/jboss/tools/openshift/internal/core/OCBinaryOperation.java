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

import org.eclipse.core.runtime.MultiStatus;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;

import com.openshift.restclient.OpenShiftContext;
import com.openshift.restclient.capability.IBinaryCapability;

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

}