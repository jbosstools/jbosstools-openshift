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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.client.IOpenShiftSSHKey;

/**
 * @author Andre Dietisheim
 */
public class LoadKeysJob extends Job {

	private UserDelegate user;
	private List<IOpenShiftSSHKey> keys;

	public LoadKeysJob(UserDelegate user) {
		super("Loading SSH keys... ");
		this.user = user;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		this.keys = user.getSSHKeys();
		return Status.OK_STATUS;
	}

	public List<IOpenShiftSSHKey> getKeys() {
		return keys;
	}
}
