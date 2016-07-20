package org.jboss.tools.openshift.test.ui.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.ui.models.IConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.models.IElementListener;
import org.jboss.tools.openshift.internal.ui.models.IExceptionHandler;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models.LoadingState;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;
import org.jboss.tools.openshift.test.util.UITestUtils;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class RefreshTest {
	private OpenshiftUIModel model;
	private ConnectionsRegistry registry;
	private IElementListener listener;
	
	@Before
	public void setUp() {
		registry = new ConnectionsRegistry();
		model = new OpenshiftUIModelTestable(registry);
		listener = mock(IElementListener.class);
		model.addListener(listener);
		IOpenShiftConnection connection = mock(IOpenShiftConnection.class);
		registry.add(connection);
	}
	
	@Test
	public void testRefreshConnection() throws InterruptedException, TimeoutException {
		IProject project1 = stubProject("test1", 1);
		IProject project2 = stubProject("test2", 1);
		IProject project2prime = stubProject("test2", 2);
		
		when(getConnectionMock().getResources(ResourceKind.PROJECT)).thenReturn(Collections.singletonList(project1));
		IConnectionWrapper connection = getConnection();
		getConnection().load(IExceptionHandler.NULL_HANDLER);
		UITestUtils.waitForState(connection, LoadingState.LOADED);
		assertEquals(1, connection.getResources().size());
		assertTrue(connection.getResources().stream().anyMatch(projectWrapper-> project1.equals(projectWrapper.getWrapped())));
		
		registry.fireConnectionChanged(getConnectionMock(), ConnectionProperties.PROPERTY_PROJECTS, null, Arrays.asList(project1, project2));
		
		verify(listener).elementChanged(connection);
		assertEquals(2, connection.getResources().size());
		assertTrue(connection.getResources().stream().anyMatch(projectWrapper-> project1.equals(projectWrapper.getWrapped())));
		assertTrue(connection.getResources().stream().anyMatch(projectWrapper-> project2.equals(projectWrapper.getWrapped())));
		
		registry.fireConnectionChanged(getConnectionMock(), ConnectionProperties.PROPERTY_PROJECTS, null, Arrays.asList(project2));
		verify(listener, times(2)).elementChanged(connection);
		assertEquals(1, connection.getResources().size());
		Optional<IResourceWrapper<?, ?>> project2Wrapper = connection.getResources().stream().filter(projectWrapper-> project2.equals(projectWrapper.getWrapped())).findFirst();
		assertTrue(project2Wrapper.isPresent());

		registry.fireConnectionChanged(getConnectionMock(), ConnectionProperties.PROPERTY_PROJECTS, null, Arrays.asList(project2prime));
		verify(listener, times(2)).elementChanged(connection);
		verify(listener).elementChanged(project2Wrapper.get());
		assertEquals(1, connection.getResources().size());
		assertTrue(connection.getResources().stream().anyMatch(projectWrapper-> {
			IResource resource = projectWrapper.getWrapped();
			String version = resource.getResourceVersion();
			return project2.equals(resource) && version.equals("2");
		}));

	}
	
	private IOpenShiftConnection getConnectionMock() {
		return getConnection().getWrapped();
	}

	private IConnectionWrapper getConnection() {
		return model.getConnections().iterator().next();
	}

	private <T extends IResource> T stubResource(Class<T> klazz, String kind, String name, IProject project, int version) {
		StubInvocationHandler handler = new StubInvocationHandler();
		@SuppressWarnings("unchecked")
		T instance = (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { klazz }, handler);
		handler.stub((proxy, method, args)->project).when(instance.getProject());;
		handler.stub((proxy, method, args)->project.getNamespace()).when(instance.getNamespace());;
		stubResource(handler, instance, kind, name, version);
		
		return instance;
	}
	private IProject stubProject(String name, int version) {
		StubInvocationHandler handler = new StubInvocationHandler();
		IProject instance = (IProject) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { IProject.class }, handler);
		handler.stub((proxy, method, args)->instance).when(instance.getProject());;
		handler.stub((proxy, method, args)->name).when(instance.getNamespace());;
		stubResource(handler, instance, ResourceKind.PROJECT, name, version);
		return instance;
	}
	
	private <T extends IResource> void stubResource(StubInvocationHandler handler, T instance, String kind, String name, int version) {
		handler.stub((proxy, method, args)->ResourceEquality.equals(proxy, args[0])).when(instance.equals(null));;
		handler.stub((proxy, method, args)->ResourceEquality.hashCode(proxy)).when(instance.hashCode());;
		handler.stub((proxy, method, args)->name).when(instance.getName());
		handler.stub((proxy, method, args)->kind).when(instance.getKind());
		handler.stub((proxy, method, args)->String.valueOf(version)).when(instance.getResourceVersion());;
	}
	
	private class OpenshiftUIModelTestable extends OpenshiftUIModel {
		public OpenshiftUIModelTestable(ConnectionsRegistry registry) {
			super(registry);
		}
	}

}
