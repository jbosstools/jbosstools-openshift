package org.jboss.tools.openshift.test.internal.ui.models;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.core.WatchManager.WatchListener;
import org.jboss.tools.openshift.internal.ui.models.ConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;
import org.jboss.tools.openshift.internal.ui.models.ProjectWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import com.openshift.restclient.IOpenShiftWatchListener.ChangeType;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConnectionWrapper.class, ProjectWrapper.class, Display.class })
@SuppressStaticInitializationFor("org.eclipse.swt.widgets.Display")
public class ConnectionWrapperTest {
	
	private static final String NAMESPACE = "myproject";
	
	private IResource resource;
	private IProject project;
	private ProjectWrapper projectWrapper;
	private WatchListenerTestable watchListener;
	
	@Before
	public void prepareData() throws Exception {
		IOpenShiftConnection connection = mock(IOpenShiftConnection.class);
		when(connection.isDefaultHost()).thenReturn(true);
		when(connection.getUsername()).thenReturn("bdshadow");
		
		PowerMockito.mockStatic(Display.class);
		PowerMockito.when(Display.getCurrent()).thenReturn(mock(Display.class));
		
		OpenshiftUIModel.getInstance();
		ConnectionsRegistrySingleton.getInstance().add(connection);
		ConnectionWrapper connectionWrapper = OpenshiftUIModel.getInstance().getConnectionWrapperForConnection(connection);
		
		this.project = mock(IProject.class);
		when(project.getName()).thenReturn(NAMESPACE);
		when(project.getNamespace()).thenReturn(NAMESPACE);
		when(connection.getResources(eq(ResourceKind.PROJECT))).thenReturn(Arrays.asList(new IResource[] { project }));
		this.projectWrapper = PowerMockito.spy(new ProjectWrapper(connectionWrapper, project));
		PowerMockito.whenNew(ProjectWrapper.class).withArguments(connectionWrapper, project).thenReturn(projectWrapper);
		connectionWrapper.refresh();
		
		this.resource = mock(IBuildConfig.class);
		when(this.resource.getKind()).thenReturn(ResourceKind.BUILD_CONFIG);
		when(this.resource.getNamespace()).thenReturn(NAMESPACE);
		when(this.resource.getProject()).thenReturn(project);
		
		WatchManager watchManager = WatchManager.getInstance();
		this.watchListener = new WatchListenerTestable(
				watchManager, project, connection, ResourceKind.BUILD_CONFIG, 0, 0);
		
		watchListener.setState("CONNECTED");
	}

	@Test
	public void testWatchManagerRecievedUpdateFromOpenshift() throws Exception {
		// given	
		// when
		this.watchListener.received(this.project, ChangeType.ADDED);
		this.watchListener.received(this.resource, ChangeType.ADDED);
		this.watchListener.received(this.resource, ChangeType.MODIFIED);
		this.watchListener.received(this.resource, ChangeType.DELETED);
		this.watchListener.received(this.project, ChangeType.DELETED);
		// then
		//deleting project doesn't call updateWithResources
		PowerMockito.verifyPrivate(projectWrapper, times(4)).invoke("updateWithResources", any()); 
		assertTrue(projectWrapper.getResources().size() == 0);
	}

	private static class WatchListenerTestable extends WatchListener {
		
		public void setState(String state) {
			super.setState(state);
		}
		
		protected WatchListenerTestable(WatchManager watchManager, IProject project, IOpenShiftConnection conn,
				String kind, int backoff, long lastConnect) {
			watchManager.super(project, conn, kind, backoff, lastConnect);
		}
	}
}
