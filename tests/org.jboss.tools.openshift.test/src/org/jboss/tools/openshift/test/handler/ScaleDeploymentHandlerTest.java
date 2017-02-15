/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.handler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.ui.handler.ScaleDeploymentHandler;
import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models.IServiceWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IService;

/**
 * @author jeff.cantrill
 * @author Andre Dietisheim
 * @author Viacheslav Kabanovich
 */
@RunWith(MockitoJUnitRunner.class)
public class ScaleDeploymentHandlerTest {
	
	private static final String SCALE_DOWN_1 = "-1";
	private static final String SCALE_UP_1 = "1";
	private static final String SCALE_REQUEST_USERINPUT = null;

	private static final int DESIRED_REPLICA_COUNT = 2;
	private static final int CURRENT_REPLICA_COUNT = 1;
	
	private static final String DEPLOYMENT_CONFIG_NAME = "aDeploymentConfig";
	
	@Mock private IReplicationController rc;
	@Mock private IDeploymentConfig dc;
	@Mock private IService service;
	@Mock private IProjectWrapper project;
	@Mock private IResourceWrapper<IReplicationController, ?> rcWrapper;
	@Mock private IResourceWrapper<IDeploymentConfig, ?> dcWrapper;
	@Mock private IPod pod;
	@Mock private IServiceWrapper serviceWrapper;
	@Mock private IResourceWrapper<IPod, IServiceWrapper> podWrapper;
	
	private TestScaleDeploymentHandler handler;
	
	@Before
	public void setUp() throws Exception {
		this.handler = spy(new TestScaleDeploymentHandler());
		givenAUserConfirmsStoppingAllPods();

		doReturn("aService").when(service).getName();

		doReturn(service).when(serviceWrapper).getWrapped();
		doReturn(Arrays.asList(rcWrapper)).when(serviceWrapper).getResourcesOfKind(ResourceKind.REPLICATION_CONTROLLER);
		doReturn(Arrays.asList(dcWrapper)).when(serviceWrapper).getResourcesOfKind(ResourceKind.DEPLOYMENT_CONFIG);

		doReturn(serviceWrapper).when(dcWrapper).getParent();
		doReturn(dc).when(dcWrapper).getWrapped();
		doReturn(DESIRED_REPLICA_COUNT).when(dc).getDesiredReplicaCount();
		doReturn(CURRENT_REPLICA_COUNT).when(dc).getCurrentReplicaCount();
		doReturn(DEPLOYMENT_CONFIG_NAME).when(dc).getName();
		
		doReturn(serviceWrapper).when(rcWrapper).getParent();
		doReturn(rc).when(rcWrapper).getWrapped();
		doReturn(DESIRED_REPLICA_COUNT).when(rc).getDesiredReplicaCount();
		doReturn(CURRENT_REPLICA_COUNT).when(rc).getCurrentReplicaCount();
		doReturn(DEPLOYMENT_CONFIG_NAME).when(rc).getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME);

		doReturn(serviceWrapper).when(podWrapper).getParent();
		doReturn(pod).when(podWrapper).getWrapped();
		doReturn(false).when(pod).isAnnotatedWith(OpenShiftAPIAnnotations.BUILD_NAME);
	}

	@Test
	public void shouldScaleReplicasWhenTheCommandReceivesReplicaDiffCountForDeployConfig() throws ExecutionException {
		givenADeploymentConfigIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT - 1);
	}

	@Test
	public void shouldNotScaleWhenReplicasWouldGetNegative() throws ExecutionException {
		givenADeploymentConfigIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);
		doReturn(0).when(dc).getCurrentReplicaCount();

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	@Test
	public void shouldScaleDownReplicasWhenUserScalesDownRepController() throws Exception {
		givenAReplicationControllerIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT - 1);
	}

	@Test
	public void shouldScaleDownWhenUserScalesDownDeployments() throws ExecutionException {
		givenAReplicationControllerIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT - 1);
	}

	@Test
	public void shouldScaleUpWhenUserScalesUpDeployments() throws ExecutionException {
		givenAServiceIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_UP_1);

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT + 1);
	}

	@Test
	public void shouldNotScaleIfUserDoesNotConfirmStopAll() throws ExecutionException {
		givenAServiceIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);
		givenAUserDoesNotConfirmStopAllPods();

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	@Test
	public void shouldScaleWhenUserSpecifiesTheDesiredCount() throws ExecutionException {
		givenAServiceIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);
		givenAUserChoosesDesiredReplicas(4);

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(4);
	}

	@Test
	public void shouldNotScaleWhenUserCancelsInput() throws ExecutionException {
		givenAServiceIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);
		givenAUserCancelsTheReplicaInputDialog();

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	@Test
	public void shouldNotScaleWhenDeploymentHasNoDeploymentConfig() throws ExecutionException {
		givenAServiceIsSelected();
		givenNoDeploymentConfigExist();
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	@Test
	public void shouldScaleDownWhenPodIsSelectedAndTheCommandReceivesReplicaDiffCount() throws ExecutionException {
		givenAPodIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT - 1);
	}

	@Test
	public void shouldScaleWhenPodIsSelectedAndUserSpecifiesTheDesiredCount() throws ExecutionException {
		givenAPodIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);
		givenAUserChoosesDesiredReplicas(4);

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(4);
	}
	
	@Test
	public void shouldNotScaleWhenNothingIsSelected() throws Exception {
		givenNothingIsSelected();
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	private void givenNothingIsSelected() {
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), any());
	}

	private void givenAServiceIsSelected() {
		doReturn(serviceWrapper).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IResourceWrapper.class));
	}

	private void givenAReplicationControllerIsSelected() {
		doReturn(rcWrapper).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IResourceWrapper.class));
	}

	private void givenADeploymentConfigIsSelected() {
		doReturn(dcWrapper).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IResourceWrapper.class));
	}

	private void givenAPodIsSelected() {
		doReturn(podWrapper).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IResourceWrapper.class));
	}

	private void givenAUserCancelsTheReplicaInputDialog() {
		doReturn(-1).when(handler).showScaleReplicasDialog(anyString(), anyInt(), any());
	}

	private void givenAUserChoosesDesiredReplicas(int desiredReplicas) {
		doReturn(desiredReplicas).when(handler).showScaleReplicasDialog(anyString(), anyInt(), any());
	}

	private void givenAUserConfirmsStoppingAllPods() {
		doReturn(true).when(handler).showStopDeploymentWarning(anyString(), any(Shell.class));
	}

	private void givenAUserDoesNotConfirmStopAllPods() {
		doReturn(false).when(handler).showStopDeploymentWarning(anyString(), any(Shell.class));
	}

	private void givenNoDeploymentConfigExist() {
		when(serviceWrapper.getResourcesOfKind(ResourceKind.DEPLOYMENT_CONFIG))
				.thenReturn(Collections.emptyList());
	}

	private void thenTheReplicasShouldNotBeUpdated() {
		verify(handler, times(0)).scaleDeployment(any(), any(), any(), anyInt());
	}

	private void thenTheReplicasShouldBeUpdated(int replicas) {
		verify(handler, times(1)).scaleDeployment(any(), any(), any(), eq(replicas));
	}

	private ExecutionEvent createExecutionEvent(String scalePodDiff) {
		Map<String, String> parameters = new HashMap<>();
		parameters.put(ScaleDeploymentHandler.REPLICA_DIFF, scalePodDiff);

		return new ExecutionEvent(null, parameters, null, null);
	}

	public static class TestScaleDeploymentHandler extends ScaleDeploymentHandler {

		@Override
		public <T> T getSelectedElement(ExecutionEvent event, Class<T> klass) {
			throw new UnsupportedOperationException("#getSelectedElement is not mocked.");
		}

		@Override
		protected int showScaleReplicasDialog(String name, int current, Shell shell) {
			throw new UnsupportedOperationException("#showScaleReplicasDialog is not mocked.");
		}

		@Override
		protected void scaleDeployment(ExecutionEvent event, String name, IReplicationController rc, int replicas) {
			// dont do anything, verify call parameters via mockito verify
		}

		@Override
		protected boolean showStopDeploymentWarning(String name, Shell shell) {
			throw new UnsupportedOperationException("#showStopDeploymentWarning is not mocked.");
		}
	}
}