package org.jboss.tools.openshift.test.internal.ui.handler;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.handler.DeleteResourceHandler;
import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ WatchManager.class, ConnectionsRegistryUtil.class})
public class DeleteResourceHandlerTest {

	@Test
	public void testStopWatchProject() throws Exception {		
		IProject project = mock(IProject.class);
		when(project.getName()).thenReturn("ProjectName");
		when(project.getKind()).thenReturn(ResourceKind.PROJECT);
		
		IProjectWrapper projectWrapper = mock(IProjectWrapper.class);
		when(projectWrapper.getWrapped()).thenReturn(project);
		
		WatchManager watchManager = mock(WatchManager.class);
		PowerMockito.mockStatic(WatchManager.class);
		PowerMockito.when(WatchManager.getInstance()).thenReturn(watchManager);
		
		Connection connection = mock(Connection.class);
		PowerMockito.mockStatic(ConnectionsRegistryUtil.class);
		PowerMockito.when(ConnectionsRegistryUtil.getConnectionFor(eq(project))).thenReturn(connection);

		DeleteResourceHandlerTestExtension handler = new DeleteResourceHandlerTestExtension();
		handler.deleteResources(new IResourceWrapper<?, ?>[] { projectWrapper });
		//TODO get rid of timeout here. Need to wait for callback from DeleteResourceJob for project
		//difficult, because it's a job (runs async) and has overriden `doRun` method in OpenShiftJobs
		verify(watchManager, timeout(200).times(1)).stopWatch(eq(project), eq(connection));
	}
	
	class DeleteResourceHandlerTestExtension extends DeleteResourceHandler {
	    
	    public void deleteResources(final IResourceWrapper<?, ?>[] uiResources) {
	        super.deleteResources(uiResources);
	    }
	}
}
