/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.FireConnectionsChangedJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftResource;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class RefreshResourceHandler extends AbstractHandler {

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        Object resource = getResource(selection);
        if (resource != null) {
            refresh(resource);
        }
        return null;
    }

    private Object getResource(ISelection selection) {
        Object resource = UIUtils.getFirstElement(selection, IOpenShiftResource.class);
        if (resource == null) {
            resource = UIUtils.getFirstElement(selection, IConnection.class);
        }
        return resource;
    }

    private void refresh(final Object element) {
        Job job = new AbstractDelegatingMonitorJob("Loading OpenShift information...") {

            @Override
            protected IStatus doRun(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Loading OpenShift informations...", IProgressMonitor.UNKNOWN);
                    if (element instanceof IConnection) {
                        ((IConnection)element).refresh();
                    } else if (element instanceof IOpenShiftResource) {
                        ((IOpenShiftResource)element).refresh();
                    }
                } catch (OpenShiftException e) {
                    ExpressUIActivator.log("Failed to refresh element", e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        };

        IConnection connection = getConnection(element);
        if (connection != null) {
            new JobChainBuilder(job).runWhenSuccessfullyDone(new FireConnectionsChangedJob(connection)).schedule();
        } else {
            job.schedule();
        }
    }

    private IConnection getConnection(final Object resource) {
        IConnection connection = null;
        if (resource instanceof IConnection) {
            connection = (IConnection)resource;
        } else if (resource instanceof IDomain) {
            IDomain domain = (IDomain)resource;
            connection = ExpressConnectionUtils.getByResource(domain.getUser(), ConnectionsRegistrySingleton.getInstance());
        } else if (resource instanceof IApplication) {
            IApplication application = (IApplication)resource;
            connection = ExpressConnectionUtils.getByResource(application, ConnectionsRegistrySingleton.getInstance());
        }
        return connection;
    }
}
