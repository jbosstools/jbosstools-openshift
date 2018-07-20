/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.test.core.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createDeploymentConfig;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createPod;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createReplicationController;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createSecret;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.tools.openshift.internal.core.job.DeleteResourcesJob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.secret.ISecret;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class DeleteResourcesJobTest {

	private IDeploymentConfig dc1 = createDeploymentConfig("dc1", null, null, null);
	private IReplicationController rc1 = createReplicationController("rc1", null, null);
	private IPod pod1 = createPod("pod1", null, null);
	private ISecret secret1 = createSecret("secret1");
	
	private Comparator<IResource> comparator = new DeleteResourcesJob.ResourcesRemovalOrdering();

	@Test
	public void shouldNotNPEWhenNull() {
		// given
		List<IResource> resources = Arrays.asList(null, null);
		// when
		Collections.sort(resources, comparator);
		// then
		assertThat(resources).containsExactly(null, null);
	}

	@Test
	public void shouldHaveNullLast() {
		// given
		List<IResource> resources = Arrays.asList(null, pod1);
		// when
		Collections.sort(resources, comparator);
		// then
		assertThat(resources).containsExactly(pod1, null);
	}

	@Test
	public void shouldhaveDCThenRCThenPodThenSecret() {
		// given
		List<IResource> resources = Arrays.asList(pod1, rc1, secret1, dc1);
		// when
		Collections.sort(resources, comparator);
		// then
		assertThat(resources).containsExactly(dc1, rc1, pod1, secret1);
	}
}
