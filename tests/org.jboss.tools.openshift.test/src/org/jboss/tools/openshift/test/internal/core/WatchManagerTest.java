package org.jboss.tools.openshift.test.internal.core;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.model.IProject;

@RunWith(MockitoJUnitRunner.class)
public class WatchManagerTest {
		
	@Mock IProject project;
	@Mock IOpenShiftConnection connection;
	@Mock IClient client;

	@Test
	@SuppressWarnings("unchecked")
	public void testStartWatch() {
		when(project.accept(any(CapabilityVisitor.class), isNull())).thenReturn(client);
		WatchManager.getInstance().startWatch(project, connection);
		verify(client, times(WatchManager.KINDS.length)).watch(any(), any(), any());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testStopWatch() {
		when(project.accept(any(CapabilityVisitor.class), isNull())).thenReturn(client);
		IWatcher watchClient = mock(IWatcher.class);
		when(client.watch(any(), any(), any())).thenReturn(watchClient);
		WatchManager.getInstance().startWatch(project, connection);
		
		WatchManager.getInstance().stopWatch(project, connection);
		verify(watchClient, times(WatchManager.KINDS.length)).stop();
	}
}
