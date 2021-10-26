/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server.debug;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.apache.commons.lang.math.NumberUtils.toInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createConnection;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createContainer;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createDeploymentConfig;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createEnvironmentVariable;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createPort;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createProbe;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createProject;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createRoute;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createService;
import static org.jboss.tools.openshift.test.util.ResourceMocks.mockGetContainers;
import static org.jboss.tools.openshift.test.util.ResourceMocks.mockGetEnvironmentVariables;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.LivenessProbe;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;
import org.jboss.tools.openshift.internal.core.server.debug.RouteTimeout;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.probe.IProbe;
import com.openshift.restclient.model.route.IRoute;

public class OpenShiftDebugModeTest {

	private static final String KEY_DEBUGPORT = "debugPort";
	private static final String VALUE_DEBUGPORT = "42";
	private static final String KEY_DEVMODE = "DEV_MODE";

	private Connection connection;
	private IDeploymentConfig dc;
	private IProject project;
	private IServer server;
	private IServerWorkingCopy serverWorkingCopy;
	private TestableDebugContext context;
	private TestableDebugMode debugMode;

	@Before
	public void setUp() throws CoreException, UnsupportedEncodingException, MalformedURLException {
		this.connection = createConnection("https://localhost:8181", "aUser");
		ConnectionsRegistrySingleton.getInstance().add(connection);
		this.project = createProject("someProject");
		this.dc = createDeploymentConfig("someDc", project,
				// no env var
				Collections.emptyList(),
				// no containers
				Collections.emptyList(), connection);
		doReturn(true).when(connection).ownsResource(dc);
		this.serverWorkingCopy = OpenShiftServerTestUtils.mockServerWorkingCopy();
		this.server = OpenShiftServerTestUtils.mockServer(serverWorkingCopy, dc, connection);
		this.context = new TestableDebugContext(server, KEY_DEVMODE, KEY_DEBUGPORT, VALUE_DEBUGPORT);
		this.debugMode = spy((TestableDebugMode) new TestableDebugMode(context));
	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}

	@Test
	public void debuggingContextShouldDefaultToDefaultDebugPortAndDebuggingNotEnabled() {
		// given
		// when
		DebugContext context = new DebugContext(server);
		// then
		assertThat(context.getDebugPort()).isEqualTo(toInt(DebugContext.DEFAULT_DEBUG_PORT));
		assertThat(context.isDebugEnabled()).isFalse();
	}

	@Test
	public void shouldDisableContextDevmode() {
		// given
		context.setDebugEnabled(true);
		context.setDevmodeEnabled(true);
		// when
		debugMode.disableDevmode();
		// then
		assertThat(context.isDebugEnabled()).isTrue();
		assertThat(context.isDevmodeEnabled()).isFalse();
	}

	@Test
	public void shouldEnableContextDevmode() {
		// given
		context.setDebugEnabled(false);
		context.setDevmodeEnabled(false);
		// when
		debugMode.enableDevmode();
		// then
		assertThat(context.isDebugEnabled()).isFalse();
		assertThat(context.isDevmodeEnabled()).isTrue();
	}

	@Test
	public void shouldEnableDevmodeGivenItIsDisabled() throws CoreException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.FALSE.toString())), dc);
		// when
		context.setDevmodeEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then		
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldEnableDevmodeAndSendItGivenUserEnablesDevmodeEnvVarDoesntExist()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		// when
		context.setDevmodeEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	public void shouldNotEnableDevmodeNorSendItGivenItIsAlreadyEnabled()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc);
		// when
		context.setDevmodeEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		// dont alter dc
		verify(dc, never()).setEnvironmentVariable(eq(KEY_DEVMODE), any());
		// dont send potentially altered dc
		verify(debugMode, never()).send(any(IDeploymentConfig.class), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDevmodeAndSendItGivenItIsEnabled() throws CoreException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc);
		// when
		context.setDevmodeEnabled(false);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableContextDebugAndDevmode() {
		// given
		context.setDebugEnabled(true);
		context.setDevmodeEnabled(true);
		// when
		debugMode.disableDebugging();
		// then
		assertThat(context.isDebugEnabled()).isFalse();
		assertThat(context.isDevmodeEnabled()).isFalse();
	}

	@Test
	public void shouldEnableContextDebugAndDevmode() {
		// given
		context.setDebugEnabled(false);
		context.setDevmodeEnabled(false);
		// when
		debugMode.enableDebugging();
		// then
		assertThat(context.isDebugEnabled()).isTrue();
		assertThat(context.isDevmodeEnabled()).isTrue();
	}

	@Test
	public void shouldSendUpdatedDebugAndDevmodeGivenUserEnablesDebugAndNoEnvVarExisted()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldSendUpdatedDebugGivenUserEnablesDebugAndOnlyDevmodeIsSet()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc);
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldOnlyGetPodGivenUserEnablesDebugAndDevmodeAndDebugEnvVarAreAlreadySet()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(dc, never()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc, never()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// dont send untouched dc
		verify(debugMode, never()).send(any(IDeploymentConfig.class), eq(connection), any(IProgressMonitor.class));
		verify(debugMode, atLeastOnce()).getExistingPod(any(IDeploymentConfig.class), eq(connection),
				any(IProgressMonitor.class));
	}

	@Test
	public void shouldSendUpdatedDebugGivenUserEnablesDebugAndDebugEnvVarIsEnabledWithDifferentPort()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, "84")), dc);
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDebugAndSendGivenUserDisablesDebugAndDebugEnvVarIsEnabled()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		context.setDebugEnabled(false);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEBUGPORT);
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDebugAndSendGivenUserDisablesDebugAndDebugEnvVarIsEnabledButOnDifferentPort()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, "99")), dc);
		context.setDebugEnabled(false);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEBUGPORT);
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotReplaceContainerDebugPortGivenExistingPortMatchesRequestedPort()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.FALSE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		Set<IPort> ports = singleton(createPort(toInt(VALUE_DEBUGPORT)));
		IContainer container = createContainer("someDc-container1", ports);
		mockGetContainers(asList(container), dc);
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(container, never()).setPorts(any());
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldReplaceContainerDebugPortIfExistingPortDiffersFromRequestedPort()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.FALSE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		// has container port 88, should have port matching env var
		Set<IPort> ports = singleton(createPort(toInt(String.valueOf("88"))));
		IContainer container = createContainer("someDc-container1", ports);
		mockGetContainers(asList(container), dc);
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(container, atLeastOnce()).setPorts(and(
				// new set of ports contains requested port
				argThat(aSetThatContainsPort(toInt(VALUE_DEBUGPORT))),
				// but not previously existing port
				argThat(not(aSetThatContainsPort(88)))));
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldAddContainerDebugPortGivenNoPortExistsYet()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.FALSE.toString())), dc);
		final IPort existingContainerPort = new PortSpecAdapter("papaSmurf", "transport", 42);
		Set<IPort> ports = singleton(existingContainerPort);
		IContainer container = createContainer("someDc-container1", ports);
		mockGetContainers(asList(container), dc);
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(container, atLeastOnce())
				.setPorts(argThat(aSetEqualTo(existingContainerPort, createPort(toInt(VALUE_DEBUGPORT)))));
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldAddRouteTimeoutIfDoesntExist() throws CoreException {
		// given
		IRoute route = createRouteFor(dc, project, connection);
		doReturn(null).when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_ROUTE_TIMEOUT), anyString());
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(route, atLeastOnce()).setAnnotation(eq(OpenShiftAPIAnnotations.TIMEOUT), anyString());
		// send updated dc
		verify(debugMode, times(1)).send(eq(route), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotAddRouteTimeoutIfExistAlready() throws CoreException {
		// given
		IRoute route = createRouteFor(dc, project, connection);
		doReturn(RouteTimeout.ROUTE_DEBUG_TIMEOUT).when(route).getAnnotation(eq(OpenShiftAPIAnnotations.TIMEOUT));
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(route, never()).setAnnotation(eq(OpenShiftAPIAnnotations.TIMEOUT), anyString());
		// dont send updated dc
		verify(debugMode, never()).send(eq(route), eq(connection), any(IProgressMonitor.class));
		// store backup
		verify(serverWorkingCopy).setAttribute(OpenShiftServerUtils.ATTR_DEBUG_ROUTE_TIMEOUT,
				RouteTimeout.ROUTE_DEBUG_TIMEOUT);
	}

	@Test
	public void shouldReplaceRouteTimeoutIfCustomTimeoutExistAlready() throws CoreException {
		// given
		IRoute route = createRouteFor(dc, project, connection);
		doReturn("4242").when(route).getAnnotation(eq(OpenShiftAPIAnnotations.TIMEOUT));
		context.setDebugEnabled(true);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(route, atLeastOnce()).setAnnotation(eq(OpenShiftAPIAnnotations.TIMEOUT), anyString());
		// send updated dc
		verify(debugMode, atLeastOnce()).send(eq(route), eq(connection), any(IProgressMonitor.class));
		// backup stored
		verify(serverWorkingCopy).setAttribute(OpenShiftServerUtils.ATTR_DEBUG_ROUTE_TIMEOUT, "4242");
	}

	@Test
	public void shouldNotCreateLivenessProbeIfDoesntExistYet() throws CoreException {
		// given
		IContainer container = createContainer("someDc-container1",
				Collections.singleton(createPort(NumberUtils.toInt(VALUE_DEBUGPORT))), null, // no lifeness probe
				createProbe(20, 21, 22, 23, 24));
		mockGetContainers(Arrays.asList(container), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		assertThat(container.getLivenessProbe()).isNull();
	}

	@Test
	public void shouldDisableLivenessProbeIfItExists() throws CoreException {
		// given
		IProbe livenessProbe = createProbe(110, 111, 112, 113, 114);
		mockGetContainers(Arrays.asList(createContainer("someDc-container1", Collections.singleton(createPort(42)),
				livenessProbe, createProbe(20, 21, 22, 23, 24))), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(livenessProbe, atLeastOnce()).setInitialDelaySeconds(LivenessProbe.INITIAL_DELAY);
		// send updated dc
		verify(debugMode, atLeastOnce()).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldSetLivenessProbeInitialDelayWhenDebuggingIfItDoesntExist() throws CoreException {
		// given
		// debugging already enabled (thus no change in dc env vars)
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		mockGetContainers(Arrays.asList(createContainer("someDc-container1", Collections.singleton(createPort(42)),
				null, createProbe(20, 21, 22, 23, 24))), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		// dont save backup in server
		verify(serverWorkingCopy, never()).setAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_LIVENESSPROBE_INITIALDELAY),
				anyString());
		// dont send unchanged dc
		verify(debugMode, never()).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldSetLivenessProbeInitialDelayWhenDebuggingIfExistingDelayIsSmaller() throws CoreException {
		// given
		// debugging already enabled (thus no change in dc env vars)
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		IProbe livenessProbe = createProbe(LivenessProbe.INITIAL_DELAY - 1, 11, 12, 13, 14);
		mockGetContainers(Arrays.asList(createContainer("someDc-container1", Collections.singleton(createPort(42)),
				livenessProbe, createProbe(20, 21, 22, 23, 24))), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(livenessProbe).setInitialDelaySeconds(LivenessProbe.INITIAL_DELAY);
		// dont save backup in server
		verify(serverWorkingCopy).setAttribute(OpenShiftServerUtils.ATTR_DEBUG_LIVENESSPROBE_INITIALDELAY,
				String.valueOf(LivenessProbe.INITIAL_DELAY - 1));
		// send changed dc
		verify(debugMode).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotSetLivenessProbeWhenDebuggingIfExistingDelayIsLarger() throws CoreException {
		// given
		// debugging already enabled (thus no change in dc env vars)
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		IProbe livenessProbe = createProbe(LivenessProbe.INITIAL_DELAY + 1, 11, 12, 13, 14);
		mockGetContainers(Arrays.asList(createContainer("someDc-container1", Collections.singleton(createPort(42)),
				livenessProbe, createProbe(20, 21, 22, 23, 24))), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		// dont set initial delay since existing is larger
		verify(livenessProbe, never()).setInitialDelaySeconds(LivenessProbe.INITIAL_DELAY);
		// dont store backup 
		verify(serverWorkingCopy, never()).setAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_LIVENESSPROBE_INITIALDELAY),
				anyString());
		// dont send unchanged dc
		verify(debugMode, never()).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotSetLivenessProbeWhenDebuggingIfItDoesntExist() throws CoreException {
		// given
		// debugging already enabled
		mockGetEnvironmentVariables(asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
				createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		mockGetContainers(Arrays.asList(createContainer("someDc-container1", Collections.singleton(createPort(42)),
				null, createProbe(20, 21, 22, 23, 24))), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		// send unchanged dc
		verify(debugMode, never()).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldRestoreLivenessProbeWhenStoppingDebuggingIfItExistedBefore() throws CoreException {
		// given
		int initialDelay = 42;
		doReturn(String.valueOf(initialDelay)).when(server)
				.getAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_LIVENESSPROBE_INITIALDELAY), anyString());
		IProbe livenessProbe = createProbe(110, 111, 112, 113, 114);
		mockGetContainers(Arrays.asList(createContainer("someDc-container1", Collections.singleton(createPort(42)),
				livenessProbe, createProbe(20, 21, 22, 23, 24))), dc);
		// when
		context.setDebugEnabled(false);
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(livenessProbe).setInitialDelaySeconds(initialDelay);
		// backup cleared
		verify(serverWorkingCopy).setAttribute(OpenShiftServerUtils.ATTR_DEBUG_LIVENESSPROBE_INITIALDELAY,
				(String) null);
		// send updated dc
		verify(debugMode).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotRestoreLivenessProbeWhenStoppingDebuggingIfItDidntExistBefore() throws CoreException {
		// given
		IProbe livenessProbe = createProbe(OpenShiftServerUtils.VALUE_LIVENESSPROBE_NODELAY, 11, 12, 13, 14);
		mockGetContainers(Arrays.asList(createContainer("someDc-container1", Collections.singleton(createPort(42)),
				livenessProbe, createProbe(20, 21, 22, 23, 24))), dc);
		// when
		context.setDebugEnabled(false);
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(livenessProbe, never()).setInitialDelaySeconds(anyInt());
		// backup cleared
		verify(serverWorkingCopy, never()).setAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_LIVENESSPROBE_INITIALDELAY),
				anyString());
		// send updated dc
		verify(debugMode, never()).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	@Test
	public void shouldRemoveRouteTimeoutWhenStoppingDebuggingIfNoneExistedBefore() throws CoreException {
		// given
		IRoute route = createRouteFor(dc, project, connection);
		doReturn(null).when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_ROUTE_TIMEOUT), anyString());
		doReturn(RouteTimeout.ROUTE_DEBUG_TIMEOUT).when(route).getAnnotation(eq(OpenShiftAPIAnnotations.TIMEOUT));
		// when
		context.setDebugEnabled(false);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(route, atLeastOnce()).removeAnnotation(eq(OpenShiftAPIAnnotations.TIMEOUT));
		// send updated dc
		verify(debugMode, times(1)).send(eq(route), eq(connection), any(IProgressMonitor.class));
		// dont remove inexistant backup
		verify(serverWorkingCopy, never()).setAttribute(OpenShiftServerUtils.ATTR_DEBUG_ROUTE_TIMEOUT, (String) null);
	}

	@Test
	public void shouldRestoreRouteTimeoutThatExistedBeforeDebugging() throws CoreException {
		// given
		IRoute route = createRouteFor(dc, project, connection);
		doReturn("4242").when(server).getAttribute(OpenShiftServerUtils.ATTR_DEBUG_ROUTE_TIMEOUT, (String) null);
		context.setDebugEnabled(false);

		// when
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(route, atLeastOnce()).setAnnotation(eq(OpenShiftAPIAnnotations.TIMEOUT), eq("4242"));
		// send updated route
		verify(debugMode, atLeastOnce()).send(eq(route), eq(connection), any(IProgressMonitor.class));
		// clear backup
		verify(serverWorkingCopy).setAttribute(OpenShiftServerUtils.ATTR_DEBUG_ROUTE_TIMEOUT, (String) null);
	}

	/**
	 * Returns a route that points to a service, that's related to the given deployment config.
	 * Creating the route it will stub 2 services and 2 routes to the given connection.
	 * 
	 * @param dc to create a route for
	 * @param connection to create the route, services and routes in
	 * @return
	 */
	private IRoute createRouteFor(IDeploymentConfig dc, IProject project, Connection connection) {
		@SuppressWarnings("serial")
		Map<String, String> selectors = new HashMap<String, String>() {
			{
				put("aSelector", "42");
			}
		};
		doReturn(selectors).when(dc).getReplicaSelector();
		IService service1 = createService("service1", project, new HashMap<String, String>()); // doesnt match dc
		IService service2 = createService("service2", project, selectors); // matches dc
		when(connection.getResources(ResourceKind.SERVICE, project.getNamespaceName()))
				.thenReturn(Arrays.asList(service1, service2));

		IRoute route1 = createRoute("route1", project, "service42"); // matches inexistent service
		IRoute route2 = createRoute("route2", project, "service2"); // matches service2
		when(connection.getResources(ResourceKind.ROUTE, project.getNamespaceName()))
				.thenReturn(Arrays.asList(route1, route2));
		return route2;
	}

	@Test
	public void shouldAddEnvVariableAndSendUpdatedDc()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		// when
		debugMode.putEnvVar("smurf", "42");
		debugMode.execute(new NullProgressMonitor());

		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable("smurf", "42");
		// send updated dc
		verify(debugMode, times(1)).send(eq(dc), eq(connection), any(IProgressMonitor.class));
	}

	private static ArgumentMatcher<Set<IPort>> aSetThatContainsPort(final int port) {
		return new ArgumentMatcher<>() {

			@Override
			public boolean matches(Set<IPort> set) {
				if (CollectionUtils.isEmpty(set)) {
					return false;
				}
				return set.stream().anyMatch(portSpec -> portSpec.getContainerPort() == port);
			}

		};
	}

	private static ArgumentMatcher<Set<IPort>> aSetEqualTo(final IPort... ports) {
		return new ArgumentMatcher<>() {

			@Override
			public boolean matches(Set<IPort> set) {
				return CollectionUtils.disjunction(Arrays.asList(ports), set).isEmpty();
			}

		};
	}

	public class TestableDebugMode extends OpenShiftDebugMode {

		public TestableDebugMode(DebugContext context) {
			super(context);
		}

		@Override
		protected void send(IResource resource, Connection connection, IProgressMonitor monitor) throws CoreException {
		}

		@Override
		protected IPod waitForNewPod(IDeploymentConfig dc, IProgressMonitor monitor) throws CoreException {
			return null;
		}

		@Override
		protected IPod getExistingPod(IDeploymentConfig dc, Connection connection, IProgressMonitor monitor) {
			return null;
		}
	}

	public class TestableDebugContext extends DebugContext {

		public TestableDebugContext(IServer server, String devmodeKey, String debugPortKey, String debugPort) {
			super(server, devmodeKey, debugPortKey, debugPort);
		}

		@Override
		public void setDevmodeEnabled(boolean devmodeEnabled) {
			super.setDevmodeEnabled(devmodeEnabled);
		}

		@Override
		public void setDebugEnabled(boolean debugEnabled) {
			super.setDebugEnabled(debugEnabled);
		}
	}
}
