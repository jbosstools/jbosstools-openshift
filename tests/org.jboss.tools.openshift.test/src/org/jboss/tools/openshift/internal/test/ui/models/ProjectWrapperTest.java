package org.jboss.tools.openshift.internal.test.ui.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.models.ConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.models.IExceptionHandler;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;
import org.jboss.tools.openshift.internal.ui.models.ProjectWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.openshift.restclient.model.IProject;

public class ProjectWrapperTest {

	private IOpenShiftConnection connection;
	private ConnectionWrapper connectionWrapper;

	private IProject project;
	private ProjectWrapper projectWrapper;

	@Before
	public void initilize() {
		this.connection = mock(IOpenShiftConnection.class);
		this.connectionWrapper = new ConnectionWrapper(mock(OpenshiftUIModel.class), connection);

		this.project = mock(IProject.class);
		when(project.getNamespaceName()).thenReturn("namespace");
		this.projectWrapper = new ProjectWrapper(connectionWrapper, project);
		WatchManager.getInstance()._getWatches().clear();
	}
	
	@After
	public void cleanUp() {
	    WatchManager.getInstance().stopWatch(project, connection);
	}

	@Test
	public void restartWatchManagerOnRefreshProjectWrapper() {
	    WatchManager.getInstance().startWatch(project, connection);
	    Map<?, ?> watchesBefore = WatchManager.getInstance()._getWatches();
		// when
		projectWrapper.refresh();
		// then
		Map<?, ?> watchesAfter = WatchManager.getInstance()._getWatches();
		
		assertTrue(watchesBefore == watchesAfter); // another watches are created
		assertEquals(watchesBefore, watchesAfter); // with the same content
		assertEquals(WatchManager.KINDS.length, WatchManager.getInstance()._getWatches().size()); // all watches are recreated
	}

	@Test
	@Ignore // project is loaded in a Job and it needs to be figured out how to track its finish state.
	public void startWatchManagerOnProjectLoad() {
		// when
		projectWrapper.load(IExceptionHandler.NULL_HANDLER);
		// then
		assertEquals(WatchManager.KINDS.length, WatchManager.getInstance()._getWatches().size());
	}

}
