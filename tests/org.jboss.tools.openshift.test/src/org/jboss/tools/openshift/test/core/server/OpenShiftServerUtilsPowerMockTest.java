package org.jboss.tools.openshift.test.core.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;

/**
 * This test class is created additionally to OpenShiftServerUtilsTest, because
 * it is impossible to use PowerMockito in there. OpenShiftServerUtilsTest
 * implicitly uses org.assertj.core.internal.ComparisonStrategy class, which is
 * a Private-Package of org.assertj.core bundle. But PowerMockito works only
 * with exported packages, that's why trying to
 * use @RunWith(PowerMockRunner.class) in OpenShiftServerUtilsTest causes
 * NoClassDefFoundError.
 * 
 * If you're able to walk around it or rewrite this test class without
 * PowerMockito, please, feel free to put this tests into
 * OpenShiftServerUtilsTest and delete this class.
 * 
 * @author Dmitrii Bocharov
 *
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ WatchManager.class })
public class OpenShiftServerUtilsPowerMockTest {

	private IServerWorkingCopy server;
	private Connection connection;

	@Before
	public void setUp() throws UnsupportedEncodingException, MalformedURLException {
		this.connection = createConnection();
		this.server = createServer(ResourceMocks.PROJECT2_SERVICES[1]);
	}

	private IServerWorkingCopy createServer(IService serverService)
			throws UnsupportedEncodingException, MalformedURLException {
		IServerWorkingCopy server = mock(IServerWorkingCopy.class);
		doReturn(OpenShiftResourceUniqueId.get(serverService))
				.when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_SERVICE), anyString());
		return server;
	}

	private Connection createConnection() {
		Connection connection = ResourceMocks.createConnection("http://localhost:8443", "dev@openshift.com");
		when(connection.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(ResourceMocks.PROJECTS));
		when(ResourceMocks.PROJECT2.getResources(ResourceKind.SERVICE))
				.thenReturn(Arrays.asList(ResourceMocks.PROJECT2_SERVICES));
		when(connection.getResources(ResourceKind.SERVICE, ResourceMocks.PROJECT2.getName()))
				.thenReturn(Arrays.asList(ResourceMocks.PROJECT2_SERVICES));
		return connection;
	}

	@Test
	public void getServiceShouldStartWatchingProjectIfServiceNotNull() {
		// given
		WatchManager watchManager = mock(WatchManager.class);
		PowerMockito.mockStatic(WatchManager.class);
		PowerMockito.when(WatchManager.getInstance()).thenReturn(watchManager);
		// when
		OpenShiftServerUtils.getResource(server, connection);
		// then
		verify(watchManager, times(1)).startWatch(any(IProject.class), eq(connection));
	}
}
