/******************************************************************************* 
 * Copyright (c) 2017-2019 Red Hat, Inc. 
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.DockerImageLabels;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;

@RunWith(MockitoJUnitRunner.class)
public class DockerImageLabelsTest {

	private static final String DOCKER_IMAGE_TAG = "nodejs:latest";

	private static final String NODEJS_IMAGESTREAM_TAG_URI = "/resources/imageStreamTag_nodejs_latest.json";

	private TestableDockerImageLabels labels;
	private TestableDockerImageLabels labelsThatFailsToLoadImageStreamTag;
	private TestableDockerImageLabels labelsThatHaveNoDc;
	private TestableDockerImageLabels labelsThatHaveNoImage;
	private TestableDockerImageLabels labelsThatHaveOnlyTrigger;
	private TestableDockerImageLabels labelsThatHaveOnlyContainer;

	@Before
	public void setup() throws IOException {
		Connection connection = ResourceMocks.createConnection("https://localhost:8181", "aUser");
		this.labelsThatHaveNoDc = new TestableDockerImageLabels(null, connection);

		IDeploymentConfig dc = ResourceMocks.createDeploymentConfig("aDeploymentConfig",
				ResourceMocks.createProject("aProject"), null, null);
		this.labelsThatHaveNoImage = new TestableDockerImageLabels(dc, connection);

		connection = ResourceMocks.createConnection("https://localhost:8282", "aUser");
		dc = mockDeploymentConfig(null, null);
		this.labelsThatHaveNoImage = new TestableDockerImageLabels(dc, connection);

		connection = ResourceMocks.createConnection("https://localhost:8383", "aUser");
		dc = mockDeploymentConfig(DOCKER_IMAGE_TAG, null);
		mockImageStreamTag(NODEJS_IMAGESTREAM_TAG_URI, connection);
		this.labelsThatHaveOnlyTrigger = spy(new TestableDockerImageLabels(dc, connection));

		connection = ResourceMocks.createConnection("https://localhost:8484", "aUser");
		dc = mockDeploymentConfig(null, DOCKER_IMAGE_TAG);
		mockImageStreamTag(NODEJS_IMAGESTREAM_TAG_URI, connection);
		this.labelsThatHaveOnlyContainer = spy(new TestableDockerImageLabels(dc, connection));

		connection = ResourceMocks.createConnection("https://localhost:8585", "aUser");
		dc = mockDeploymentConfig(DOCKER_IMAGE_TAG, DOCKER_IMAGE_TAG);
		mockImageStreamTag(NODEJS_IMAGESTREAM_TAG_URI, connection);
		this.labels = spy(new TestableDockerImageLabels(dc, connection));

		connection = ResourceMocks.createConnection("https://localhost:8686", "aUser");
		dc = mockDeploymentConfig(DOCKER_IMAGE_TAG, DOCKER_IMAGE_TAG);
		this.labelsThatFailsToLoadImageStreamTag = spy(new TestableDockerImageLabels(dc, connection));
		doReturn(null)
			.when(labelsThatFailsToLoadImageStreamTag)
				.getImageStreamTag(any(DockerImageURI.class), anyString(), any(IProgressMonitor.class));
	}

	private IDeploymentConfig mockDeploymentConfig(String triggerImageUri, String containerImageUri) {
		IDeploymentConfig withChangeTriggerAndContainerImage = 
				ResourceMocks.createDeploymentConfig("dc", 
						ResourceMocks.createProject("aProject"), null, null);
		mockImageChangeTrigger(triggerImageUri, withChangeTriggerAndContainerImage);
		mockContainer(containerImageUri, withChangeTriggerAndContainerImage);
		return withChangeTriggerAndContainerImage;
	}

	private void mockImageChangeTrigger(String imageUri, IDeploymentConfig dc) {
		if (imageUri == null) {
			return;
		}
		IDeploymentImageChangeTrigger trigger = ResourceMocks
				.createDeploymentImageChangeTrigger(DeploymentTriggerType.IMAGE_CHANGE, imageUri);
		ResourceMocks.mockGetTriggers(Collections.singletonList(trigger), dc);
	}

	private void mockContainer(String imageUri, IDeploymentConfig dc) {
		if (StringUtils.isEmpty(imageUri)) {
			return;
		}
		ResourceMocks.mockGetImages(Collections.singletonList(imageUri), dc);
	}

	private void mockImageStreamTag(String url, Connection connection) throws IOException {
		IResource imageStreamTag = mockImageStreamTag(url);
		doReturn(imageStreamTag)
			.when(connection).getResource(eq(ResourceKind.IMAGE_STREAM_TAG), anyString(), anyString());
	}

	private IResource mockImageStreamTag(String url) throws IOException {
		IResource imageStreamTag = mock(IResource.class);
		doReturn(IOUtils.toString(DockerImageLabelsTest.class.getResourceAsStream(url), Charset.defaultCharset()))
				.when(imageStreamTag).toJson();
		return imageStreamTag;
	}

	@Test(expected = CoreException.class)
	public void shouldThrowIfHasNoDc() throws CoreException {
		// given
		// when
		labelsThatHaveNoDc.load(new NullProgressMonitor());
	}

	@Test(expected = CoreException.class)
	public void shouldThrowIfCannotFindImage() throws CoreException {
		// given
		// when
		labelsThatHaveNoImage.load(new NullProgressMonitor());
	}

	@Test
	public void shouldHaveImageIfDcHasOnlyTrigger() throws CoreException {
		// given
		// when
		labelsThatHaveOnlyTrigger.load(new NullProgressMonitor());
		// then
		verify(labelsThatHaveOnlyTrigger).getImageStreamTag(
				argThat(new DockerImageURIArgumentMatcher()), anyString(), any(IProgressMonitor.class));
	}

	@Test
	public void shouldHaveImageIfDcHasOnlyContainer() throws CoreException {
		// given
		// when
		labelsThatHaveOnlyContainer.load(new NullProgressMonitor());
		// then
		verify(labelsThatHaveOnlyContainer).getImageStreamTag(
				argThat(new DockerImageURIArgumentMatcher()), anyString(), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotBeAvailableGivenItIsNotLoaded() throws CoreException {
		// given
		// when
		boolean available = labels.isAvailable();
		// then
		assertThat(available).isFalse();
		verify(labels, never()).load(any(IProgressMonitor.class));
	}

	@Test
	public void loadShouldReturnFalseGivenLoadFails() throws CoreException {
		// given
		// when
		boolean success = labelsThatFailsToLoadImageStreamTag.load(new NullProgressMonitor());
		// then
		assertThat(success).isFalse();
	}

	@Test
	public void loadShouldReturnTrueGivenLoadSucceeds() throws CoreException {
		// given
		// when
		boolean success = labels.load(new NullProgressMonitor());
		// then
		assertThat(success).isTrue();
	}

	@Test
	public void shouldNotBeAvailableGivenLoadFailsToLoadMetadata() throws CoreException {
		// given
		// when
		labelsThatFailsToLoadImageStreamTag.load(new NullProgressMonitor());
		// then
		verify(labelsThatFailsToLoadImageStreamTag, times(1)).load(any(IResource.class), any(IProgressMonitor.class));
		assertThat(labelsThatFailsToLoadImageStreamTag.isAvailable()).isFalse();
	}

	@Test
	public void shouldTryToLoadGivenDevmodeKeyIsRequested() throws CoreException {
		// given
		// when
		String devmodeKey = labelsThatFailsToLoadImageStreamTag.getDevmodeKey(new NullProgressMonitor());
		// then
		verify(labelsThatFailsToLoadImageStreamTag, times(1)).load(any(IResource.class), any(IProgressMonitor.class));
		assertThat(devmodeKey).isNull();
	}

	@Test
	public void shouldTryToLoadAgainGivenPriorLoadFailed() throws CoreException {
		// given
		// when
		labelsThatFailsToLoadImageStreamTag.getDevmodeKey(new NullProgressMonitor());
		labelsThatFailsToLoadImageStreamTag.getDevmodeKey(new NullProgressMonitor());
		// then
		verify(labelsThatFailsToLoadImageStreamTag, times(2)).load(any(IResource.class), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotLoadAgainGivenPriorLoadSucceeded() throws CoreException {
		// given
		// when
		labels.getDevmodeKey(new NullProgressMonitor());
		labels.getDevmodeKey(new NullProgressMonitor());
		// then
		verify(labels, times(1)).load(any(IResource.class), any(IProgressMonitor.class));
	}

	@Test
	public void shouldTryToLoadGivenDevmodePortKeyIsRequested() throws CoreException {
		// given
		// when
		String devmodePortKey = labelsThatFailsToLoadImageStreamTag.getDevmodePortKey(new NullProgressMonitor());
		// then
		verify(labelsThatFailsToLoadImageStreamTag, times(1)).load(any(IResource.class), any(IProgressMonitor.class));
		assertThat(devmodePortKey).isNull();
	}

	@Test
	public void shouldTryToLoadGivenDevmodePortValueIsRequested() throws CoreException {
		// given
		// when
		String devmodePortValue = labelsThatFailsToLoadImageStreamTag.getDevmodePortValue(new NullProgressMonitor());
		// then
		verify(labelsThatFailsToLoadImageStreamTag, times(1)).load(any(IResource.class), any(IProgressMonitor.class));
		assertThat(devmodePortValue).isNull();
	}

	@Test
	public void shouldTryToLoadGivenPodPathIsRequested() throws CoreException {
		// given
		// when
		String podPath = labelsThatFailsToLoadImageStreamTag.getPodPath(new NullProgressMonitor());
		// then
		verify(labelsThatFailsToLoadImageStreamTag, times(1)).load(any(IResource.class), any(IProgressMonitor.class));
		assertThat(podPath).isNull();
	}

	private final class DockerImageURIArgumentMatcher extends ArgumentMatcher<DockerImageURI> {

		@Override
		public boolean matches(Object argument) {
			if (!(argument instanceof DockerImageURI)) {
				return false;
			}
			return DOCKER_IMAGE_TAG.equals(((DockerImageURI) argument).getAbsoluteUri());
		}

		@Override
		public void describeTo(Description description) {				
		}
	}

	public class TestableDockerImageLabels extends DockerImageLabels {

		protected TestableDockerImageLabels(IResource resource, Connection connection) {
			super(resource, connection);
		}

		@Override
		protected String load(IResource resource, IProgressMonitor monitor) throws CoreException {
			return super.load(resource, monitor);
		}

		@Override
		public String getImageStreamTag(DockerImageURI uri, String namespace, IProgressMonitor monitor) {
			return super.getImageStreamTag(uri, namespace, monitor);
		}

	}
}
