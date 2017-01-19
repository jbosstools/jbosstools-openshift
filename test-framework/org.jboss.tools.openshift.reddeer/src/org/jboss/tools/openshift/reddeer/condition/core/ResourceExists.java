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

import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Matcher;
import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * Wait condition to wait for existence of a 
 * resource(s) of a given kind that match a given matcher.
 * 
 * @author adietish@redhat.com
 *
 */
public class ResourceExists extends AbstractWaitCondition {

	private final String kind;
	private Matcher<List<IResource>> matcher;
	private String project;
	protected Connection connection;

	public ResourceExists(String kind, BaseMatcher<List<IResource>> baseMatcher, String project, Connection connection) {
		assertNotNull(kind);
		assertNotNull(baseMatcher);
		assertNotNull(connection);

		this.kind = kind;
		this.matcher = baseMatcher;
		this.project = project;
		this.connection = connection;
	}

	@Override
	public boolean test() {
		try {
			List<IResource> resources = connection.getResources(kind, project);
			return matcher.matches(resources);
		} catch(OpenShiftException e) {
			return false;
		}
	}

	@Override
	public String description() {
		return kind + " matching " + matcher + " in project " + project + " gets available.";
	}
}
