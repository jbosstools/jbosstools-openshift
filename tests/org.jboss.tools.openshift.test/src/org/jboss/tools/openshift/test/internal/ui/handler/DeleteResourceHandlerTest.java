package org.jboss.tools.openshift.test.internal.ui.handler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.handler.DeleteResourceHandler;
import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ UIUtils.class, MessageDialog.class, WatchManager.class, ConnectionsRegistryUtil.class, NLS.class})
@SuppressStaticInitializationFor( {"org.eclipse.swt.SWT", "org.eclipse.swt.widgets.Canvas", "org.eclipse.swt.widgets.Shell"})
public class DeleteResourceHandlerTest {

	@Test
	public void testStopWatchProject() throws Exception {		
		IProject project = mock(IProject.class);
		when(project.getName()).thenReturn("ProjectName");
		when(project.getKind()).thenReturn(ResourceKind.PROJECT);
		
		IProjectWrapper projectWrapper = mock(IProjectWrapper.class);
		when(projectWrapper.getWrapped()).thenReturn(project);
		
		IResourceWrapper<?, ?>[] resourceWrappers = new IResourceWrapper<?, ?>[] { projectWrapper };
		PowerMockito.mockStatic(UIUtils.class);
		PowerMockito.when(UIUtils.getElements(any(), any())).thenReturn(resourceWrappers);
		
		PowerMockito.mockStatic(MessageDialog.class);
		PowerMockito.when(MessageDialog.openConfirm(any(), any(), any())).thenReturn(true);
		
		WatchManager watchManager = mock(WatchManager.class);
		PowerMockito.mockStatic(WatchManager.class);
		PowerMockito.when(WatchManager.getInstance()).thenReturn(watchManager);
		
		Connection connection = mock(Connection.class);
		PowerMockito.mockStatic(ConnectionsRegistryUtil.class);
		PowerMockito.when(ConnectionsRegistryUtil.getConnectionFor(eq(project))).thenReturn(connection);
		
		PowerMockito.mockStatic(NLS.class);
		PowerMockito.doNothing().when(NLS.class, "initializeMessages", new Object[] {
				"org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages", OpenShiftUIMessages.class });

		DeleteResourceHandler handler = new DeleteResourceHandler();
		handler.execute(new ExecutionEvent());
		//TODO get rid of timeout here. Need to wait for callback from DeleteResourceJob for project
		//difficult, because it's a job (runs async) and has overriden `doRun` method in OpenShiftJobs
		verify(watchManager, timeout(200).times(1)).stopWatch(eq(project), eq(connection));
	}
}
