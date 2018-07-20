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
package org.jboss.tools.openshift.internal.core.ocbinary;

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.restclient.OpenShiftContext;
import com.openshift.restclient.capability.IBinaryCapability;

/**
 * @author Andre Dietisheim
 */
public abstract class OCBinaryOperation {

	public void run(IConnection connection) {
		String oldLocation = OpenShiftContext.get().get(IBinaryCapability.OPENSHIFT_BINARY_LOCATION);
		String location = OCBinary.getInstance().getPath(connection);
		OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, location);
		try {
			runOCBinary();
		} finally {
			if (!StringUtils.isEmpty(oldLocation)) {
				OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, oldLocation);
			}
		}
	}

	protected abstract void runOCBinary();

}