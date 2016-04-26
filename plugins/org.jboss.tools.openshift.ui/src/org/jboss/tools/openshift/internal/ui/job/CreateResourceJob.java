/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.job;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import com.openshift.restclient.IClient;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IList;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * A Job to create an application from a template
 * 
 * @author Jeff Maury
 */
public class CreateResourceJob extends AbstractDelegatingMonitorJob {

	private IProject project;
	private InputStream input;
	private Collection<IResource> resource;

	public CreateResourceJob(IProject project, InputStream input) {
		super("Create Resource Job");
		this.project = project;
		this.input = input;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		IStatus status = project.accept(new CapabilityVisitor<IClientCapability, IStatus>() {

			@Override
			public IStatus visit(IClientCapability capability) {
				
				try {
				    IClient client = capability.getClient();
				    IResource resourceIn = client.getResourceFactory().create(input);
				    if (resourceIn instanceof IList) {
				        resource = client.create((IList)resourceIn, project.getNamespace());
				    } else {
	                    resource = Collections.singletonList(client.create(resourceIn, project.getNamespace()));
				    }
				    return new Status(IStatus.OK, OpenShiftUIActivator.PLUGIN_ID, "Resource created");
				} catch(OpenShiftException e) {
					String message = e.getMessage();
					if(e.getStatus() != null) {
						message = e.getStatus().getMessage();
					}
					return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, -1, message, e);
				}
			}


		},
		new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Client processing is unsupported for this client and server combination.", null));
		return status;
	}

	/**
	 * Return the created resource(s)
	 * 
	 * @return the created resource(s)
	 */
    public Collection<IResource> getResource() {
        return resource;
    }
}
