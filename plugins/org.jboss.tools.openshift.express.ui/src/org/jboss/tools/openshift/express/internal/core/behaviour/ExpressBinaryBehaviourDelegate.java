/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.ide.eclipse.as.core.server.internal.AbstractBehaviourDelegate;

/**
 * @author Rob Stryker
 */
public class ExpressBinaryBehaviourDelegate extends AbstractBehaviourDelegate {

	public static final String OPENSHIFT_BINARY_ID = "openshiftBinary";

	@Override
	public String getBehaviourTypeId() {
		return OPENSHIFT_BINARY_ID;
	}
	
	@Override
	public void publishStart(IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public void publishFinish(IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public IStatus canChangeState(String launchMode) {
		return Status.OK_STATUS;
	}


}
