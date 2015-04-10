/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.job;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIMessages;

import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class LoadKeysJob extends Job {

	private ExpressConnection user;
	private List<IOpenShiftSSHKey> keys = new ArrayList<IOpenShiftSSHKey>();

	public LoadKeysJob(ExpressConnection user) {
		super("Loading SSH keys... ");
		this.user = user;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try{
			this.keys = user.getSSHKeys();
			return Status.OK_STATUS; 
		}catch(OpenShiftException ex){
			return ExpressUIActivator.createErrorStatus(ExpressUIMessages.COULD_NOT_LOAD_SSH_KEYS, ex);
		}
	}
	
	public List<IOpenShiftSSHKey> getKeys() {
		return keys;
	}
}
