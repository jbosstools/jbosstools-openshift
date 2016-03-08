package org.jboss.tools.openshift.test.ui.wizard.deployimage.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.linuxtools.docker.core.DockerException;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.search.DockerHubRegistry;
import org.junit.Test;

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
