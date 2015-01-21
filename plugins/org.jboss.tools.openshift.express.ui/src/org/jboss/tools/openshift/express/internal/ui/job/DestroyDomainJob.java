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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class DestroyDomainJob extends AbstractDelegatingMonitorJob {

	private IDomain domain;
	private boolean force;

	public DestroyDomainJob(final IDomain domain, final boolean force) {
		super(NLS.bind(OpenShiftExpressUIMessages.DESTROYING_DOMAIN, domain.getId()));
		this.domain = domain;
		this.force = force;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			domain.destroy(force);
			ExpressConnection connection = ExpressConnectionUtils.getByResource(domain.getUser(), ConnectionsRegistrySingleton.getInstance());
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection);
			return Status.OK_STATUS;
		} catch (OpenShiftException e) {
			return new Status(Status.ERROR, ExpressUIActivator.PLUGIN_ID, NLS.bind(
					"Failed to destroy domain \"{0}\"", domain.getId()), e);
		} finally {
			monitor.done();
		}
	}
}
