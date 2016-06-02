/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.wizard.deployimage.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.linuxtools.docker.core.DockerException;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.search.DockerHubRegistry;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Xavier Coulon
 */
@Ignore
public class DockerHubRegistryTest {

	@Test
	public void shouldFindImageTags() throws InterruptedException, ExecutionException, DockerException {
		// given
		final DockerHubRegistry dockerHubRegistrySearch = new DockerHubRegistry();
		// when
		final List<String> tags = dockerHubRegistrySearch.getTags("jboss/wildfly");
		// then
		assertThat(tags).contains("8.2.0.Final", "9.0.2.Final", "latest");
	}
}
