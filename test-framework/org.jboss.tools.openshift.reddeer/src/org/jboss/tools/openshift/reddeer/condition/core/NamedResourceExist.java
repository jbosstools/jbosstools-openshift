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

import static org.junit.Assert.assertNotNull;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift.restclient.OpenShiftException;

/**
 * Wait condition to wait for existence of a resource of a given kind and name
 * in the given project. The condition operates on the client-lib, not on the
 * UI.
 * 
 * @author adietish@redhat.com
 *
 */
public class NamedResourceExist extends AbstractWaitCondition {

	private final String kind;
	protected final String name;
	private String project;
	protected Connection connection;

	public NamedResourceExist(String kind, String name, String project, Connection connection) {
		assertNotNull(kind);
		assertNotNull(name);
		assertNotNull(connection);

		this.kind = kind;
		this.name = name;
		this.project = project;
		this.connection = connection;
	}

	@Override
	public boolean test() {
		try {
			connection.getResource(kind, project, name);
			return true;
		} catch(OpenShiftException e) {
			return false;
		}
	}

	@Override
	public String description() {
		return kind + " named " + name + " in project " + project + " gets available.";
	}
}
