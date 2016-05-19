/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.Test;

import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftResourceUniqueIdTest {

	@Test
	public void should_return_unique_uniqueId() {
		// given
		IResource resource1 = ResourceMocks.PROJECT1;
		IResource resource2 = ResourceMocks.PROJECT2;
		// when
		String uniqueId1 = OpenShiftResourceUniqueId.get(resource1);
		String uniqueId2 = OpenShiftResourceUniqueId.get(resource2);
		// then
		assertThat(uniqueId1).isNotEmpty();
		assertThat(uniqueId2).isNotEmpty();
		assertThat(uniqueId1).isNotEqualTo(uniqueId2);
	}

	@Test
	public void should_return_null_if_resource_has_no_name() {
		// given
		IResource resource = ResourceMocks.createProject(null);
		// when
		String uniqueId = OpenShiftResourceUniqueId.get(resource);
		// then
		assertThat(uniqueId).isNull();
	}

	@Test
	public void should_return_correct_resource_by_uniqueId() {
		// given
		IResource toBeFound = ResourceMocks.PROJECT2;
		List<IResource> resources = Arrays.asList(ResourceMocks.PROJECT1, toBeFound, ResourceMocks.PROJECT3);
		String uniqueId = OpenShiftResourceUniqueId.get(toBeFound);
		// when
		IResource found = OpenShiftResourceUniqueId.getByUniqueId(uniqueId, resources);
		// then
		assertThat(found).isSameAs(toBeFound);
	}

	@Test
	public void should_return_correct_correct_projectname() {
		// given
		IResource resource = ResourceMocks.PROJECT2_SERVICES[1];
		String uniqueId = OpenShiftResourceUniqueId.get(resource);
		// when
		String projectName = OpenShiftResourceUniqueId.getProjectName(uniqueId);
		// then
		assertThat(projectName).isEqualTo(resource.getProject().getName());
	}

	@Test
	public void should_return_correct_correct_resourcename() {
		// given
		IResource resource = ResourceMocks.PROJECT2_SERVICES[0];
		String uniqueId = OpenShiftResourceUniqueId.get(resource);
		// when
		String resourceName = OpenShiftResourceUniqueId.getResourceName(uniqueId);
		// then
		assertThat(resourceName).isEqualTo(resource.getName());
	}
}
