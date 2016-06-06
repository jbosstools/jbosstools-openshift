/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.job;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.utils.ResourceProcessor;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * Job to delete a resource from a project/OpenShift namespace
 * 
 * @author jeff.cantrill
 * @author Jeff Maury
 */
public class DeleteResourceJob<T> extends AbstractDelegatingMonitorJob {

	private Collection<T> resources;
	private boolean willDeleteSubResources;
    private Callback<T> callback;

	public interface Callback<T> {
	    default IResource getResource(T wrapper) {
	        return (IResource) wrapper;
	    }
	    
	    default void preProcess(T wrapper) {}
	    
	    default void postProcess(T wrapper) {}
	}
	
	public DeleteResourceJob(Collection<T> resources, boolean willDeleteSubResources, Callback<T> callback) {
		super("Delete Resource Job");
		this.resources = resources;
		this.willDeleteSubResources = willDeleteSubResources;
		this.callback = callback;
	}

    public DeleteResourceJob(T resource, boolean willDeleteSubResources) {
        this(Collections.singletonList(resource), willDeleteSubResources, new Callback<T>() {
        });
    }

    @Override
	protected IStatus doRun(IProgressMonitor monitor) {
	    MultiStatus status = new MultiStatus(OpenShiftUIActivator.PLUGIN_ID, 0, "", null);
        SubMonitor sub = SubMonitor.convert(monitor, resources.size());
	    for(T resource : resources) {
            IResource wrappedResource = callback.getResource(resource);
            IProgressMonitor subMonitor = SubMonitor.convert(sub.newChild(1, SubMonitor.SUPPRESS_NONE), 1);
            subMonitor.beginTask(NLS.bind(OpenShiftUIMessages.ResourceDeletionMessage, wrappedResource.getName(), wrappedResource.getKind()), 1);

            try {
	            Connection connection = ConnectionsRegistryUtil.getConnectionFor(wrappedResource);
	            ResourceProcessor processor = Platform.getAdapterManager().getAdapter(wrappedResource, ResourceProcessor.class);
	            ConnectionsRegistry registry = ConnectionsRegistrySingleton.getInstance();
	            if(connection != null) {
	                callback.preProcess(resource);
	                processor.handleDelete(registry, connection, wrappedResource, willDeleteSubResources, sub);
	                callback.postProcess(resource);
	            }
	            status.merge(Status.OK_STATUS);
	        } catch(OpenShiftException e) {
	            status.merge(new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, NLS.bind("Error deleting {0} named  {1}.", wrappedResource.getKind(), wrappedResource.getName()), e));
	        } finally {
                subMonitor.worked(1);
            }
	    }
	    monitor.done();
	    return status;
	}

}
