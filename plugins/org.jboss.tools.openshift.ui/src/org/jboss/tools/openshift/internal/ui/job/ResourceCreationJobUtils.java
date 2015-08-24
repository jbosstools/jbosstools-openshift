/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * Utility class to handle common functions
 * during resource creation
 * 
 * @author jeff.cantrill
 *
 */
public class ResourceCreationJobUtils {
	
	private ResourceCreationJobUtils() {
	}
	
	@SuppressWarnings("serial")
	public static IStatus createErrorStatusForExistingResources(Collection<IResource> resources) {
		final StringBuilder b = new StringBuilder("\nThe following resource names already exist:\n");
		for (IResource r : resources) {
			b.append("\n\"").append(r.getName()).append("\" ").append(r.getKind());
		}
		b.append("\n\nYou need to use different names or create this application in a different OpenShift project.");
		String message = String.valueOf(resources.size()) + " resource name collisions found. ";
		return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, message, new Throwable() {

			@Override
			public String getMessage() {
				return b.toString();
			}
			
		});
	}
	
	/*
	 * MAYBE this should be part of Connection in an exists method?
	 */
	public static Collection<IResource> findExistingResources(Connection connection, Collection<IResource> resources) {
		List<IResource> existing = new ArrayList<IResource>(resources.size());
		for (IResource resource : resources) {
			try {
				IResource found = connection.getResource(resource);
				existing.add(found);
			}catch(OpenShiftException e) {
				//this is expected if the resource is not found
				//@TODO change to NotFoundException of some kind
			}
		}
		return existing;
	}
}
