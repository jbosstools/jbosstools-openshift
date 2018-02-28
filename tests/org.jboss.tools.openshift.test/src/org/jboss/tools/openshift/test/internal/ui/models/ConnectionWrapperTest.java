package org.jboss.tools.openshift.test.internal.ui.models;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.core.WatchManager.WatchListener;
import org.jboss.tools.openshift.internal.ui.models.ConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.models.IExceptionHandler;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;
import org.jboss.tools.openshift.internal.ui.models.ProjectWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.IOpenShiftWatchListener.ChangeType;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class ConnectionWrapperTest {

	private static final String NAMESPACE = "myproject";

	private IResource resource;
	private IProject project;
	private ProjectWrapper projectWrapper;
	private WatchListenerTestable watchListener;

	@Before
	public void prepareData() throws Exception {
	    this.project = mock(IProject.class);
        when(project.getName()).thenReturn(NAMESPACE);
        when(project.getNamespace()).thenReturn(NAMESPACE);
        
		IOpenShiftConnection connection = mock(IOpenShiftConnection.class);
		when(connection.isDefaultHost()).thenReturn(true);
		when(connection.getUsername()).thenReturn("bdshadow");
		when(connection.getResources(eq(ResourceKind.PROJECT))).thenReturn(Arrays.asList(new IResource[] { project }));

        OpenshiftUIModel.getInstance(); // by doing this we call a protected OpenshiftUIModel constructor, which adds listener to the
                                        // ConnectionsRegistrySingleton
		ConnectionsRegistrySingleton.getInstance().add(connection);
        ConnectionWrapper connectionWrapper = OpenshiftUIModel.getInstance().getConnectionWrapperForConnection(connection);
		connectionWrapper.refresh();
		
		this.projectWrapper = (ProjectWrapper)connectionWrapper.getResources().iterator().next();

		this.resource = mock(IBuildConfig.class);
		when(this.resource.getKind()).thenReturn(ResourceKind.BUILD_CONFIG);
		when(this.resource.getNamespace()).thenReturn(NAMESPACE);
		when(this.resource.getProject()).thenReturn(project);

		this.watchListener = new WatchListenerTestable(WatchManager.getInstance(), project, connection, ResourceKind.BUILD_CONFIG, 0,
				0);

		watchListener.setState("CONNECTED");
	}

	@Test
	public void testWatchManagerRecievedUpdateFromOpenshift() throws Exception {
		this.watchListener.received(this.project, ChangeType.ADDED);
		this.watchListener.received(this.resource, ChangeType.ADDED);		
		this.watchListener.received(this.resource, ChangeType.MODIFIED);
		
		assertEquals(1, projectWrapper.getResources().size());
		
		this.watchListener.received(this.resource, ChangeType.DELETED);
		this.watchListener.received(this.project, ChangeType.DELETED);
		
		assertEquals(0, projectWrapper.getResources().size());
	}

	private static class WatchListenerTestable extends WatchListener {

		@Override
		public void setState(String state) {
			super.setState(state);
		}

		protected WatchListenerTestable(WatchManager watchManager, IProject project, IOpenShiftConnection conn,
				String kind, int backoff, long lastConnect) {
			watchManager.super(project, conn, kind, backoff, lastConnect);
		}
	}
}
