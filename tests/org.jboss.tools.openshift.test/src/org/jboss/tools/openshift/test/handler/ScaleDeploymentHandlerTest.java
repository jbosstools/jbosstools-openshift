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

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.jboss.tools.openshift.internal.ui.handler.ScaleDeploymentHandler;
import org.jboss.tools.openshift.internal.ui.models.Deployment;
import org.jboss.tools.openshift.internal.ui.models.IProjectAdapter;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IService;

@RunWith(MockitoJUnitRunner.class)
public class ScaleDeploymentHandlerTest {
	
	@Mock private IReplicationController rc;
	@Mock private IDeploymentConfig dc;
	@Mock private IService service;
	@Mock private IProjectAdapter project;
	@Mock private IResourceUIModel uiModel;
	
	private TestScaleDeploymentHandler handler;
	private Deployment deployment;
	@SuppressWarnings("rawtypes")
	private Map parameters = new HashMap();
	private ExecutionEvent event;
	
	@Before
	public void setUp() throws Exception {
		handler = spy(new TestScaleDeploymentHandler());
		
		event = new ExecutionEvent(null, parameters, null, null);
		when(service.getName()).thenReturn("aService");
		
		deployment = spy(new Deployment(service, project));
		when(deployment.getReplicationControllers()).thenReturn(Arrays.asList(uiModel));
		
		when(uiModel.getResource()).thenReturn(rc);
		when(rc.getDesiredReplicaCount()).thenReturn(2);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testScaleReplicasWhenTheCommandReceivesReplicaDiffCountForDeployConfig() throws Exception{
		parameters.put(ScaleDeploymentHandler.REPLICA_DIFF, "-1");
		givenADeploymentConfigIsSelected();

		handler.execute(event);
		
		thenTheReplicasShouldBeUpdated();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testScaleReplicasWhenTheCommandReceivesReplicaDiffCountForRepController() throws Exception{
		parameters.put(ScaleDeploymentHandler.REPLICA_DIFF, "-1");
		givenAReplicationControllerIsSelected();
		
		handler.execute(event);
		
		thenTheReplicasShouldBeUpdated();
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testScaleReplicasWhenTheCommandReceivesReplicaDiffCount() throws Exception{
		parameters.put(ScaleDeploymentHandler.REPLICA_DIFF, "-1");
		givenADeploymentIsSelected();
		
		handler.execute(event);
		
		thenTheReplicasShouldBeUpdated();
	}

	@Test
	public void testScaleReplicasWhenUserSpecifiesTheDesiredCount() throws Exception{
		givenADeploymentIsSelected();
		givenAUserChoosesDesiredReplicas();
		
		handler.execute(event);
		
		thenTheReplicasShouldBeUpdated();
	}
	
	@Test
	public void testNoUpdateWhenUserCancelsInput() throws Exception{
		givenADeploymentIsSelected();
		givenAUserCancelsTheReplicaInputDialog();
		
		handler.execute(event);
		
		thenTheReplicasShouldNotBeUpdated();
	}
	
	@Test
	public void testWhenDeploymentHasNoRepControllers() throws Exception{
		givenADeploymentIsSelected();
		when(deployment.getReplicationControllers()).thenReturn(Collections.emptyList());
		
		handler.execute(event);

		thenTheReplicasShouldNotBeUpdated();
	}
	
	@Test
	public void testWhenDeploymentIsNotSelected() throws Exception{
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), any());
		
		handler.execute(event);
		thenTheReplicasShouldNotBeUpdated();
	}
	
	private void givenADeploymentIsSelected() {
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IReplicationController.class));
		doReturn(deployment).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(Deployment.class));
	}
	private void givenAReplicationControllerIsSelected() {
		doReturn(rc).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IReplicationController.class));
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(Deployment.class));
	}
	private void givenADeploymentConfigIsSelected() {
		doReturn(dc).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IReplicationController.class));
		doReturn(null).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(Deployment.class));
	}
	
	private void givenAUserCancelsTheReplicaInputDialog() {
		doReturn(-1).when(handler).showInputDialog(anyInt(), any());
	}

	private void givenAUserChoosesDesiredReplicas() {
		doReturn(4).when(handler).showInputDialog(anyInt(), any());
	}
	
	private void thenTheReplicasShouldNotBeUpdated() {
		verify(rc, times(0)).setDesiredReplicaCount(anyInt());
	}
	private void thenTheReplicasShouldBeUpdated() {
		verify(handler, times(1)).scaleDeployment(any(), any(), any(), anyInt());
	}
	
	public static class TestScaleDeploymentHandler extends ScaleDeploymentHandler{
		
		@Override
		public <T> T getSelectedElement(ExecutionEvent event, Class<T> klass) {
			return super.getSelectedElement(event, klass);
		}
		
		@Override
		protected int showInputDialog(int current, ExecutionEvent event) {
			return super.showInputDialog(current, event);
		}

		@Override
		protected void scaleDeployment(ExecutionEvent event, String name, IReplicationController rc,
				int replicas) {
			super.scaleDeployment(event, name, rc, replicas);
		}
		
		
	}
}
