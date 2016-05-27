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
package org.jboss.tools.openshift.test.ui.wizard.deployimage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DeployImageWizardModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.openshift.restclient.capability.resources.IImageStreamImportCapability;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IStatus;
import com.openshift.restclient.model.image.IImageStreamImport;

/**
 * Testing the {@link DeployImageWizardModel} class
 */
public class DeployImageWizardModelTest {

    private static final String LATEST_TAG = "latest";

    private static final String WILDFLY_IMAGE = "jboss/wildfly";

    private static final String WILDFLY_IMAGE_URI = WILDFLY_IMAGE + ':' + LATEST_TAG;

    private static final String JBOSS_INFINISPAN_SERVER_IMAGE = "jboss/infinispan-server";

    private static final String JBOSS_INFINISPAN_SERVER_URI = JBOSS_INFINISPAN_SERVER_IMAGE +':' + LATEST_TAG;

    private static final String NON_EXISTANT_IMAGE = "bad/badimage";

    private static final String NON_EXISTANT_IMAGE_URI = NON_EXISTANT_IMAGE + ':' + LATEST_TAG;

    private static final String JENKINS_IMAGE = "openshift3/jenkins-1-rhel7";

    private static final String JENKINS_IMAGE_URI = JENKINS_IMAGE + ':' + LATEST_TAG;

    private IDockerConnection dockerConnection;
    
    private IProject project;
    
    private DeployImageWizardModel model;
    
    @Before
    public void setUp() {
        model = new DeployImageWizardModel();
        dockerConnection = mock(IDockerConnection.class);
        project = Mockito.mock(IProject.class);
        model.setDockerConnection(dockerConnection);
        model.setProject(project);
    }
    
	@Test
	public void shouldInitializeContainerInfoFromLocalDockerImage() {
		// assume Docker image is on local
		final IDockerImageInfo dockerImageInfo = Mockito.mock(IDockerImageInfo.class, Mockito.RETURNS_DEEP_STUBS);
		when(dockerConnection.hasImage(WILDFLY_IMAGE, LATEST_TAG)).thenReturn(true);
		when(dockerConnection.getImageInfo(WILDFLY_IMAGE_URI)).thenReturn(dockerImageInfo);
		when(dockerImageInfo.containerConfig().env()).thenReturn(Arrays.asList("foo1=bar1", "foo2=bar2"));
		when(dockerImageInfo.containerConfig().exposedPorts())
				.thenReturn(new HashSet<>(Arrays.asList("8080/tcp", "9990/tcp")));
		when(dockerImageInfo.containerConfig().volumes()).thenReturn(Collections.emptySet());

		// when
		model.setImageName(WILDFLY_IMAGE_URI);
		final boolean result = model.initializeContainerInfo();
		// then
		assertThat(result).isTrue();
		assertThat(model.getEnvironmentVariables()).contains(new EnvironmentVariable("foo1", "bar1"),
				new EnvironmentVariable("foo2", "bar2"));
		assertThat(model.getPortSpecs()).contains(new PortSpecAdapter("8080/tcp"), new PortSpecAdapter("9990/tcp"));

	}

	@Test
	public void shouldInitializeContainerInfoFromRemoteDockerImage() throws IOException {
		// no Docker image on local
		when(dockerConnection.hasImage(JBOSS_INFINISPAN_SERVER_IMAGE, LATEST_TAG)).thenReturn(false);

		final IImageStreamImportCapability cap = Mockito.mock(IImageStreamImportCapability.class);
		when(project.supports(IImageStreamImportCapability.class)).thenReturn(true);
		when(project.getCapability(IImageStreamImportCapability.class)).thenReturn(cap);
		final IStatus status = Mockito.mock(IStatus.class);
		final IImageStreamImport streamImport = Mockito.mock(IImageStreamImport.class);
		final DockerImageURI dockerImageURI = new DockerImageURI(JBOSS_INFINISPAN_SERVER_URI);
		when(status.isSuccess()).thenReturn(true);
		when(cap.importImageMetadata(dockerImageURI)).thenReturn(streamImport);
		when(streamImport.getImageJsonFor(dockerImageURI.getTag()))
				.thenReturn(getImageStreamImport("/resources/jboss_infinispan-server_ImageStreamImport.json"));
		when(streamImport.getImageStatus()).thenReturn(Arrays.asList(status));
		// when
		model.setImageName(JBOSS_INFINISPAN_SERVER_URI);
		final boolean result = model.initializeContainerInfo();
		// then
		assertThat(result).isTrue();
		assertThat(model.getEnvironmentVariables()).contains(new EnvironmentVariable("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"),
				new EnvironmentVariable("JAVA_HOME", "/usr/lib/jvm/java"),
				new EnvironmentVariable("INFINISPAN_SERVER_HOME", "/opt/jboss/infinispan-server"),
				new EnvironmentVariable("INFINISPAN_VERSION", "8.2.0.Final"));
		assertThat(model.getPortSpecs()).isEmpty();
		assertThat(model.getVolumes()).isEmpty();
	}

    @Test
    public void shouldInitializeContainerInfoFromRemoteDockerImageWithVolumes() throws IOException {
        // no Docker image on local
        when(dockerConnection.hasImage(JENKINS_IMAGE, LATEST_TAG)).thenReturn(false);

        final IImageStreamImportCapability cap = Mockito.mock(IImageStreamImportCapability.class);
        when(project.supports(IImageStreamImportCapability.class)).thenReturn(true);
        when(project.getCapability(IImageStreamImportCapability.class)).thenReturn(cap);
        final IStatus status = Mockito.mock(IStatus.class);
        final IImageStreamImport streamImport = Mockito.mock(IImageStreamImport.class);
        final DockerImageURI dockerImageURI = new DockerImageURI(JENKINS_IMAGE_URI);
        when(status.isSuccess()).thenReturn(true);
        when(cap.importImageMetadata(dockerImageURI)).thenReturn(streamImport);
        when(streamImport.getImageJsonFor(dockerImageURI.getTag()))
                .thenReturn(getImageStreamImport("/resources/openshift3_jenkins_1_rhel7_ImageStreamImport.json"));
        when(streamImport.getImageStatus()).thenReturn(Arrays.asList(status));
        // when
        model.setImageName(JENKINS_IMAGE_URI);
        final boolean result = model.initializeContainerInfo();
        // then
        assertThat(result).isTrue();
        assertThat(model.getEnvironmentVariables()).contains(
                new EnvironmentVariable("PATH", "/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin"),
                new EnvironmentVariable("JENKINS_VERSION", "1.642"),
                new EnvironmentVariable("HOME", "/var/lib/jenkins"),
                new EnvironmentVariable("JENKINS_HOME", "/var/lib/jenkins"));
        assertThat(model.getPortSpecs()).contains(
                new PortSpecAdapter("50000-tcp", "TCP", 50000),
                new PortSpecAdapter("8080-tcp", "TCP", 8080));
        assertThat(model.getVolumes()).contains("/var/lib/jenkins");
    }
    
    @Test
    public void shouldNotInitializeContainerInfoFromWrongPayload() throws IOException {
        // no Docker image on local
        when(dockerConnection.hasImage(NON_EXISTANT_IMAGE, LATEST_TAG)).thenReturn(false);

        final IImageStreamImportCapability cap = Mockito.mock(IImageStreamImportCapability.class);
        when(project.supports(IImageStreamImportCapability.class)).thenReturn(true);
        when(project.getCapability(IImageStreamImportCapability.class)).thenReturn(cap);
        final IStatus status = Mockito.mock(IStatus.class);
        final IImageStreamImport streamImport = Mockito.mock(IImageStreamImport.class);
        final DockerImageURI dockerImageURI = new DockerImageURI(NON_EXISTANT_IMAGE_URI);
        when(status.isSuccess()).thenReturn(false);
        when(cap.importImageMetadata(dockerImageURI)).thenReturn(streamImport);
        when(streamImport.getImageJsonFor(dockerImageURI.getTag()))
                .thenReturn(getImageStreamImport("/resources/failed_ImageStreamImport.json"));
        when(streamImport.getImageStatus()).thenReturn(Arrays.asList(status));
        // when
        model.setImageName(NON_EXISTANT_IMAGE_URI);
        final boolean result = model.initializeContainerInfo();
        // then
        assertThat(result).isFalse();
    }

    private String getImageStreamImport(final String filename) throws IOException {
		final InputStream imageStreamImport = getClass().getClassLoader()
				.getResourceAsStream(filename);
		return IOUtils.toString(imageStreamImport);
	}
}
