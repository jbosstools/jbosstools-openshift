package org.jboss.tools.openshift.test.internal.ui.models;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.models.ConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.models.IExceptionHandler;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;
import org.jboss.tools.openshift.internal.ui.models.ProjectWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.openshift.restclient.model.IProject;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ WatchManager.class })
public class ProjectWrapperTest {
	
	private WatchManager watchManager;
	
	private IOpenShiftConnection connection;
	private ConnectionWrapper connectionWrapper;
	
	private IProject project;
	private ProjectWrapper projectWrapper;
	
	@Before
	public void initilize() {
		watchManager = mock(WatchManager.class);
		PowerMockito.mockStatic(WatchManager.class);
		PowerMockito.when(WatchManager.getInstance()).thenReturn(watchManager);
		
		this.connection = mock(IOpenShiftConnection.class);
		this.connectionWrapper = new ConnectionWrapper(mock(OpenshiftUIModel.class), connection);
		
		this.project = mock(IProject.class);
		when(project.getNamespace()).thenReturn("namespace");
		this.projectWrapper = new ProjectWrapper(connectionWrapper, project);
	}

	@Test
	public void restartWatchManagerOnRefreshProjectWrapper() {
		// when
		projectWrapper.refresh();
		// then
		verify(watchManager, times(1)).startWatch(eq(project), eq(connection));
		verify(watchManager, times(1)).stopWatch(eq(project), eq(connection));
	}
	
	@Test
	public void startWatchManagerOnProjectLoad() {
		// when
		projectWrapper.load(mock(IExceptionHandler.class));
		// then
		verify(watchManager, timeout(200).times(1)).startWatch(eq(project), eq(connection));
	}
	
}
