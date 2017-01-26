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

@RunWith(MockitoJUnitRunner.class)
public class ScaleDeploymentHandlerTest {
	
	private static final String SCALE_DOWN_1 = "-1";
	private static final String SCALE_UP_1 = "1";
	private static final String SCALE_REQUEST_USERINPUT = null;

	private static final int DESIRED_REPLICA_COUNT = 2;
	private static final int CURRENT_REPLICA_COUNT = 1;
	
	@Mock private IReplicationController rc;
	@Mock private IDeploymentConfig dc;
	@Mock private IService service;
	@Mock private IProjectWrapper project;
	@Mock private IResourceWrapper<IReplicationController, ?> uiModel;
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
		doReturn(Arrays.asList(uiModel)).when(serviceWrapper).getResourcesOfKind(ResourceKind.REPLICATION_CONTROLLER);

		doReturn(rc).when(uiModel).getWrapped();
		doReturn(DESIRED_REPLICA_COUNT).when(rc).getDesiredReplicaCount();
		doReturn(CURRENT_REPLICA_COUNT).when(rc).getCurrentReplicaCount();

		doReturn(DESIRED_REPLICA_COUNT).when(dc).getDesiredReplicaCount();
		doReturn(CURRENT_REPLICA_COUNT).when(dc).getCurrentReplicaCount();

		doReturn(serviceWrapper).when(podWrapper).getParent();
		doReturn(pod).when(podWrapper).getWrapped();
		doReturn(false).when(pod).isAnnotatedWith(OpenShiftAPIAnnotations.BUILD_NAME);
	}

	@Test
	public void shouldScaleReplicasWhenTheCommandReceivesReplicaDiffCountForDeployConfig() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);
		givenADeploymentConfigIsSelected();
		
		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT - 1);
	}

	@Test
	public void shouldNotScaleWhenReplicasWouldGetNegative() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);
		givenADeploymentConfigIsSelected();
		doReturn(0).when(dc).getCurrentReplicaCount();

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	@Test
	public void shouldScaleDownReplicasWhenUserScalesDownRepController() throws Exception{
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);
		givenAReplicationControllerIsSelected();

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT - 1);
	}

	@Test
	public void shouldScaleDownReplicasWhenUserScalesDownDeployments() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);
		givenADeploymentIsSelected();

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT - 1);
	}

	@Test
	public void shouldScaleUpReplicasWhenUserScalesUpDeployments() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_UP_1);
		givenADeploymentIsSelected();

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT + 1);
	}

	@Test
	public void shouldNotScaleIfUserDoesNotConfirmStopAll() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);
		givenADeploymentIsSelected();
		givenAUserDoesNotConfirmStopAllPods();
		
		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	@Test
	public void testScaleReplicasWhenUserSpecifiesTheDesiredCount() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);
		givenADeploymentIsSelected();
		givenAUserChoosesDesiredReplicas(4);

		handler.execute(event);

		thenTheReplicasShouldBeUpdated(4);
	}
	
	@Test
	public void testNoUpdateWhenUserCancelsInput() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);
		givenADeploymentIsSelected();
		givenAUserCancelsTheReplicaInputDialog();

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	@Test
	public void testWhenDeploymentHasNoRepControllers() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);
		givenADeploymentIsSelected();
		when(serviceWrapper.getResourcesOfKind(ResourceKind.REPLICATION_CONTROLLER)).thenReturn(Collections.emptyList());

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}
	
	@Test
	public void testScaleReplicasWhenPodIsSelectedAndTheCommandReceivesReplicaDiffCount() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_DOWN_1);
		givenAPodIsSelected();
		
		handler.execute(event);
		
		thenTheReplicasShouldBeUpdated(CURRENT_REPLICA_COUNT - 1);
	}

	@Test
	public void testScaleReplicasWhenPodIsSelectedAndUserSpecifiesTheDesiredCount() throws ExecutionException {
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);
		givenAPodIsSelected();
		givenAUserChoosesDesiredReplicas(4);
		
		handler.execute(event);
		
		thenTheReplicasShouldBeUpdated(4);
	}
	
	@Test
	public void testWhenDeploymentIsNotSelected() throws Exception{
		ExecutionEvent event = createExecutionEvent(SCALE_REQUEST_USERINPUT);
		givenNothingIsSelected();

		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}

	private void givenNothingIsSelected() {
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), any());
	}

	private void givenADeploymentIsSelected() {
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IReplicationController.class));
		doReturn(serviceWrapper).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IServiceWrapper.class));
	}

	private void givenAReplicationControllerIsSelected() {
		doReturn(rc).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IReplicationController.class));
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IServiceWrapper.class));
	}

	private void givenADeploymentConfigIsSelected() {
		doReturn(dc).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IReplicationController.class));
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IServiceWrapper.class));
	}

	private void givenAPodIsSelected() {
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IReplicationController.class));
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IServiceWrapper.class));
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