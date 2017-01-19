/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition.core;

import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift.restclient.ResourceKind;

/**
 * Wait condition to wait for existence of a (core, openshift client-lib)
 * project with the given name in the given project.
 * 
 * @author adietish@redhat.com
 *
 */
public class ProjectExists extends NamedResourceExist {
	
	public ProjectExists(String name, Connection connection) {
		super(ResourceKind.PROJECT, name, null, connection);
	}

	@Override
	public String description() {
		return ResourceKind.PROJECT + " named '" + name + "' gets available.";
	}
}
