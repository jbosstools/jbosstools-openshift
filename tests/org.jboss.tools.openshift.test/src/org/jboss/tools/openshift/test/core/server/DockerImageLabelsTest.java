/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createConnection;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.DockerImageLabels;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;

@RunWith(MockitoJUnitRunner.class)
public class DockerImageLabelsTest {

    private static final String NODEJS_IMAGESTREAM_TAG_URL = "/imageStreamTag_nodejs_latest.json";

    private Connection connection;
    private IDeploymentConfig dc;
    private TestableDockerImageLabels labels;
    private TestableDockerImageLabels labelsThatFailsToLoad;

    @Before
    public void setup() throws IOException {
        this.connection = createConnection("https://localhost:8181", "aUser");

        this.labelsThatFailsToLoad = spy(new TestableDockerImageLabels(null, connection));

        this.dc = ResourceMocks.createDeploymentConfig("aDeploymentConfig", ResourceMocks.createProject("aProject"), null, null);
        IDeploymentImageChangeTrigger trigger = ResourceMocks.createDeploymentImageChangeTrigger(DeploymentTriggerType.IMAGE_CHANGE,
                "nodejs:latest");
        ResourceMocks.mockGetTriggers(Collections.singletonList(trigger), dc);

        IResource imageStreamTag = mockImageStreamTag(NODEJS_IMAGESTREAM_TAG_URL);
        doReturn(imageStreamTag).when(connection).getResource(eq(ResourceKind.IMAGE_STREAM_TAG), anyString(), anyString());

        this.labels = spy(new TestableDockerImageLabels(dc, connection));
    }

    private IResource mockImageStreamTag(String url) throws IOException {
        IResource imageStreamTag = mock(IResource.class);
        doReturn(IOUtils.toString(DockerImageLabelsTest.class.getResourceAsStream(url))).when(imageStreamTag).toJson();
        return imageStreamTag;
    }

    @Test
    public void shouldNotBeAvailableGivenItIsNotLoaded() throws CoreException {
        // given
        // when
        boolean available = labels.isAvailable();
        // then
        assertThat(available).isFalse();
        verify(labels, never()).load();
    }

    @Test
    public void loadShouldReturnFalseGivenLoadFails() throws CoreException {
        // given
        // when
        boolean success = labelsThatFailsToLoad.load();
        // then
        assertThat(success).isFalse();
    }

    @Test
    public void shouldNotBeAvailableGivenLoadFailsToLoadMetadata() throws CoreException {
        // given
        // when
        labelsThatFailsToLoad.load();
        // then
        verify(labelsThatFailsToLoad, times(1)).load(any(IResource.class));
        assertThat(labels.isAvailable()).isFalse();
    }

    @Test
    public void shouldTryToLoadGivenDevmodeKeyIsRequested() throws CoreException {
        // given
        // when
        String devmodeKey = labelsThatFailsToLoad.getDevmodeKey();
        // then
        verify(labelsThatFailsToLoad, times(1)).load(any(IResource.class));
        assertThat(devmodeKey).isNull();
    }

    @Test
    public void shouldTryToLoadAgainGivenPriorLoadFailed() throws CoreException {
        // given
        // when
        labelsThatFailsToLoad.getDevmodeKey();
        labelsThatFailsToLoad.getDevmodeKey();
        // then
        verify(labelsThatFailsToLoad, times(2)).load(any(IResource.class));
    }

    @Test
    public void shouldNotLoadAgainGivenPriorLoadSucceeded() throws CoreException {
        // given
        // when
        labels.getDevmodeKey();
        labels.getDevmodeKey();
        // then
        verify(labels, times(1)).load(any(IResource.class));
    }

    @Test
    public void shouldTryToLoadGivenDevmodePortKeyIsRequested() throws CoreException {
        // given
        // when
        String devmodePortKey = labelsThatFailsToLoad.getDevmodePortKey();
        // then
        verify(labelsThatFailsToLoad, times(1)).load(any(IResource.class));
        assertThat(devmodePortKey).isNull();
    }

    @Test
    public void shouldTryToLoadGivenDevmodePortValueIsRequested() throws CoreException {
        // given
        // when
        String devmodePortValue = labelsThatFailsToLoad.getDevmodePortValue();
        // then
        verify(labelsThatFailsToLoad, times(1)).load(any(IResource.class));
        assertThat(devmodePortValue).isNull();
    }

    @Test
    public void shouldTryToLoadGivenPodPathIsRequested() throws CoreException {
        // given
        // when
        String podPath = labelsThatFailsToLoad.getPodPath();
        // then
        verify(labelsThatFailsToLoad, times(1)).load(any(IResource.class));
        assertThat(podPath).isNull();
    }

    public class TestableDockerImageLabels extends DockerImageLabels {

        protected TestableDockerImageLabels(IResource resource, Connection connection) {
            super(resource, connection);
        }

        @Override
        protected String load(IResource resource) {
            return super.load(resource);
        }

    }
}
