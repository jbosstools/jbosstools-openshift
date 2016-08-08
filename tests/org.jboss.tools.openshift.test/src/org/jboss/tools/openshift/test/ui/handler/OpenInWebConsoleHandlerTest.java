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
package org.jboss.tools.openshift.test.ui.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.handler.OpenInWebConsoleHandler;
import org.junit.BeforeClass;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IEvent;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;

@RunWith(Theories.class)
public class OpenInWebConsoleHandlerTest {
	
	private static OpenInWebConsoleHandlerTestable handlerMock;
	private static Connection connectionMock;
	
	@BeforeClass
	public static void initTestable() {
		handlerMock = new OpenInWebConsoleHandlerTestable();
		connectionMock = mock(Connection.class);
		when(connectionMock.getHost()).thenReturn("http://localhost");
	}	
	@DataPoints
	public static DataPair[] dataPoints = new DataPair[] {
			new DataPair(mock(IBuildConfig.class), "http://localhost/console/project/namespace/browse/builds/qwerty"),
			new DataPair(mock(IBuild.class), "http://localhost/console/project/namespace/browse/builds/label/qwerty"),
			new DataPair(mock(IDeploymentConfig.class), "http://localhost/console/project/namespace/browse/deployments/qwerty"),
			new DataPair(mock(IPod.class), "http://localhost/console/project/namespace/browse/pods/qwerty"),
			new DataPair(mock(IService.class), "http://localhost/console/project/namespace/browse/services/qwerty"),
			new DataPair(mock(IImageStream.class), "http://localhost/console/project/namespace/browse/images/qwerty"),
			new DataPair(mock(IPersistentVolumeClaim.class), "http://localhost/console/project/namespace/browse/persistentvolumeclaims/qwerty"),
			new DataPair(mock(IEvent.class), "http://localhost/console/project/namespace/browse/events/"),
			new DataPair(mock(IProject.class), "http://localhost/console/project/namespace"),
			new DataPair(null, "http://localhost/console")	
	};

	@Theory
	public void test(DataPair dataPair) {
		IResource resourceMock = dataPair.getResource();
		if (resourceMock != null) {
			when(resourceMock.getNamespace()).thenReturn("namespace");
			when(resourceMock.getName()).thenReturn("qwerty");
			when(resourceMock.getLabels()).thenReturn(new HashMap<String, String>() {{put("buildconfig", "label");}});
		}
		
		assertEquals(dataPair.getExpected(), handlerMock.getWebConsoleUrl(connectionMock, resourceMock));
	}
	
	static class OpenInWebConsoleHandlerTestable extends OpenInWebConsoleHandler{
		
		public OpenInWebConsoleHandlerTestable() {
			super();
		}
		
		public String getWebConsoleUrl(Connection connection, IResource resource) {
			return super.getWebConsoleUrl(connection, resource);
		}
	}
	
	static class DataPair {
		private IResource input;
		private String expected;
		
		public DataPair(IResource input, String expected) {
			this.input = input;
			this.expected = expected;
		}

		public IResource getResource() {
			return input;
		}

		public String getExpected() {
			return expected;
		}
	}

}
