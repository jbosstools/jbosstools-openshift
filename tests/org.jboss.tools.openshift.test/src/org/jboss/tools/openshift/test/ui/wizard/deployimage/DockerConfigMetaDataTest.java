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

import org.eclipse.linuxtools.docker.core.IDockerContainerConfig;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DockerConfigMetaData;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

/**
 * @author Jeff Maury
 *
 */
public class DockerConfigMetaDataTest {
    
    private IDockerImageInfo imageInfo;
    
    @Before
    public void before() {
        imageInfo = mock(IDockerImageInfo.class);
        doReturn(null).when(imageInfo).config();
        doReturn(null).when(imageInfo).containerConfig();
    }
    
    /**
     * Mock an IDockerContainerConfig object.
     * 
     * @param exposedPorts the exposed ports to return (maybe null)
     * @param envs the env variables to return (may be null)
     * @param volumes the volumes to return (may be null)
     * @return the mock object
     */
    private IDockerContainerConfig createContainerConfig(Set<String> exposedPorts, List<String> envs, Set<String> volumes) {
        IDockerContainerConfig containerConfig = mock(IDockerContainerConfig.class);
        doReturn(exposedPorts).when(containerConfig).exposedPorts();
        doReturn(envs).when(containerConfig).env();
        doReturn(volumes).when(containerConfig).volumes();
        return containerConfig;
    }
    
    @Test
    public void checkThatContainerConfigPortsAreReturnedWhenNoConfig() {
        doReturn(createContainerConfig(Collections.singleton("8080"), null, null)).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.exposedPorts()).isEqualTo(Collections.singleton("8080"));
    }

    @Test
    public void checkThatContainerConfigEnvAreReturnedWhenNoConfig() {
        doReturn(createContainerConfig(null, Collections.singletonList("PATH=a"), null)).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.env()).isEqualTo(Collections.singletonList("PATH=a"));
    }

    @Test
    public void checkThatContainerConfigVolumesAreReturnedWhenNoConfig() {
        doReturn(createContainerConfig(null, null, Collections.singleton("Work"))).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.volumes()).isEqualTo(Collections.singleton("Work"));
    }

    @Test
    public void checkThatContainerConfigPortsAreReturnedWhenEmptyConfig() {
        doReturn(createContainerConfig(null, null, null)).when(imageInfo).config();
        doReturn(createContainerConfig(Collections.singleton("8080"), null, null)).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.exposedPorts()).isEqualTo(Collections.singleton("8080"));
    }

    @Test
    public void checkThatContainerConfigEnvAreReturnedWhenEmptyConfig() {
        doReturn(createContainerConfig(null, null, null)).when(imageInfo).config();
        doReturn(createContainerConfig(null, Collections.singletonList("PATH=a"), null)).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.env()).isEqualTo(Collections.singletonList("PATH=a"));
    }

    @Test
    public void checkThatContainerConfigVolumesAreReturnedWhenEmptyConfig() {
        doReturn(createContainerConfig(null, null, null)).when(imageInfo).config();
        doReturn(createContainerConfig(null, null, Collections.singleton("Work"))).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.volumes()).isEqualTo(Collections.singleton("Work"));
    }

    @Test
    public void checkThatConfigPortsAreReturnedWhenConfig() {
        doReturn(createContainerConfig(Collections.singleton("8080"), null, null)).when(imageInfo).config();
        doReturn(createContainerConfig(Collections.singleton("8081"), null, null)).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.exposedPorts()).isEqualTo(Collections.singleton("8080"));
    }

    @Test
    public void checkThatConfigEnvAreReturnedWhenConfig() {
        doReturn(createContainerConfig(null, Collections.singletonList("PATH=a"), null)).when(imageInfo).config();
        doReturn(createContainerConfig(null, Collections.singletonList("PATH=b"), null)).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.env()).isEqualTo(Collections.singletonList("PATH=a"));
    }

    @Test
    public void checkThatConfigVolumesAreReturnedWhenConfig() {
        doReturn(createContainerConfig(null, null, Collections.singleton("Work1"))).when(imageInfo).config();
        doReturn(createContainerConfig(null, null, Collections.singleton("Work2"))).when(imageInfo).containerConfig();
        DockerConfigMetaData meta = new DockerConfigMetaData(imageInfo);
        assertThat(meta.volumes()).isEqualTo(Collections.singleton("Work1"));
    }
}
