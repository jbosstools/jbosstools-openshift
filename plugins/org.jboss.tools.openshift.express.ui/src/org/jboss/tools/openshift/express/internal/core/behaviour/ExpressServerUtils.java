/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethodType;
import org.jboss.ide.eclipse.as.core.util.DeploymentPreferenceLoader;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.core.util.RuntimeUtils;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
import org.jboss.ide.eclipse.as.core.util.ServerCreationUtils;
import org.jboss.ide.eclipse.as.core.util.ServerUtil;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * This class holds the attribute names whose values will be
 * stored inside a server object, as well as the utility methods
 * used to get and set them for a server. 
 *
 * @author Rob Stryker
 */
public class ExpressServerUtils {
	public static final String ATTRIBUTE_EXPRESS_MODE = "org.jboss.tools.openshift.express.internal.core.behaviour.ExpressMode";
	public static final String EXPRESS_BINARY_MODE =  "publishBinary";
	public static final String EXPRESS_SOURCE_MODE =  "publishSource";
	public static final String ATTRIBUTE_APPLICATION_NAME =  "org.jboss.tools.openshift.express.internal.core.behaviour.ApplicationName";
	public static final String ATTRIBUTE_APPLICATION_ID =  "org.jboss.tools.openshift.express.internal.core.behaviour.ApplicationId";
	public static final String ATTRIBUTE_DEPLOY_PROJECT =  "org.jboss.tools.openshift.express.internal.core.behaviour.binary.deployProject";
	public static final String ATTRIBUTE_DOMAIN =  "org.jboss.tools.openshift.express.internal.core.behaviour.Domain";
	public static final String ATTRIBUTE_USERNAME =  "org.jboss.tools.openshift.express.internal.core.behaviour.Username";
	// Legacy, not to be used
	public static final String ATTRIBUTE_PASSWORD =  "org.jboss.tools.openshift.express.internal.core.behaviour.Password";
	public static final String ATTRIBUTE_REMOTE_NAME =  "org.jboss.tools.openshift.express.internal.core.behaviour.RemoteName";
	public static final String ATTRIBUTE_REMOTE_NAME_DEFAULT =  "origin";
	
	public static final String PREFERENCE_IGNORE_CONTEXT_ROOT = "org.jboss.tools.openshift.express.internal.core.behaviour.IgnoreContextRoot";
	
	/** the OpensHift Server Type as defined in the plugin.xml.*/
	public static final String OPENSHIFT_SERVER_TYPE = "org.jboss.tools.openshift.express.openshift.server.type";
	
	public static String getExpressMode(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_EXPRESS_MODE, EXPRESS_SOURCE_MODE);
	}
	
	public static String getExpressModeAsString(IServerAttributes attributes) {
		String mode = getExpressMode(attributes);
		if( mode.equals(EXPRESS_SOURCE_MODE))
			return "Source";
		return "Binary";
	}
	
	public static IServer setExpressMode(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_EXPRESS_MODE, val);
		return wc.save(false, new NullProgressMonitor());
	}
	
	public static String getExpressApplicationName(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_APPLICATION_NAME, (String)null);
	}

	public static IServer setExpressApplication(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_APPLICATION_NAME, val);
		return wc.save(false, new NullProgressMonitor());
	}

	public static String getExpressDeployProject(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_DEPLOY_PROJECT, (String)null);
	}
	public static IServer setExpressDeployProject(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_DEPLOY_PROJECT, val);
		return wc.save(false, new NullProgressMonitor());
	}

	public static String getExpressApplicationId(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_APPLICATION_ID, (String)null);
	}

	public static String getExpressDomain(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_DOMAIN, (String)null);
	}

	public static IServer setExpressDomain(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_DOMAIN, val);
		return wc.save(false, new NullProgressMonitor());
	}

	public static String getExpressRemoteName(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_REMOTE_NAME, (String)null);
	}

	public static IServer setExpressRemoteName(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_REMOTE_NAME, val);
		return wc.save(false, new NullProgressMonitor());
	}

	public static String getExpressUsername(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_USERNAME, (String)null);
	}

	public static IServer setExpressUsername(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_USERNAME, val);
		return wc.save(false, new NullProgressMonitor());
	}

	public static String getExpressPassword(IServerWorkingCopy server ) {
		return getExpressPassword(server.getOriginal());
	}
	
	public static String getExpressPassword(IServer server ) {
		if( server == null )
			return null;
		String s = ServerUtil.getFromSecureStorage(server, ExpressServerUtils.ATTRIBUTE_PASSWORD);
		if( s == null )
			return server.getAttribute(ExpressServerUtils.ATTRIBUTE_PASSWORD, (String)null);
		return s;
	}
	
	public static void setExpressPassword(IServerAttributes server, String val) throws CoreException {
		ServerUtil.storeInSecureStorage(server, ExpressServerUtils.ATTRIBUTE_PASSWORD, val);
	}
	
	
	public static boolean getIgnoresContextRoot(IServerAttributes server) {
		return server.getAttribute(PREFERENCE_IGNORE_CONTEXT_ROOT, true);
	}

	public static IServer setIgnoresContextRoot(IServerAttributes server, boolean val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_REMOTE_NAME, val);
		return wc.save(false, new NullProgressMonitor());
	}

	
	/**
	 * To be used if you must create a local runtime adapter 
	 * as the user does not have one in their workspace yet
	 * 
	 * @param host
	 * @param username
	 * @param password
	 * @param domain
	 * @param appName
	 * @param sourceOrBinary
	 * @param localRuntimeHomeDir
	 * @return
	 * @throws CoreException
	 */
	public static IServer createAS7OpenShiftServer(
			String host, String username, String password, 
			String domain, String appName, String appId,
			String sourceOrBinary, String remoteName,
			String localRuntimeHomeDir) throws CoreException {
		IServer server = createServerAndRuntime(IJBossToolingConstants.AS_70,
				IJBossToolingConstants.SERVER_AS_70, 
				localRuntimeHomeDir, /* irrelevant */ "default");
		return fillServerWithOpenShiftDetails(server, host, username, password, domain, appName, appId, sourceOrBinary, remoteName);
	}
	
	/**
	 * To be used if a runtime is already provided, for example if a user has 
	 * selected the proper as7 local runtime delegate from a combo box
	 * 
	 * @param host
	 * @param username
	 * @param password
	 * @param domain
	 * @param appName
	 * @param sourceOrBinary
	 * @param runtime
	 * @return
	 * @throws CoreException
	 */
	public static IServer createAS7OpenShiftServer(
			String host, String username, String password, 
			String domain, String appName, String appId, String sourceOrBinary, String remoteName,
			IRuntime runtime) throws CoreException {
		IServer server = createServer(runtime, IJBossToolingConstants.SERVER_AS_70);
		return fillServerWithOpenShiftDetails(server, host, username, password, domain, appName, appId, sourceOrBinary, remoteName);
	}

	
	public static IServer fillServerWithOpenShiftDetails(IServer server, IApplication application, 
			IUser user, String mode, String remoteName) throws CoreException, OpenShiftException {
		return fillServerWithOpenShiftDetails(server, application.getApplicationUrl(),
				user.getRhlogin(), user.getPassword(), user.getDomain().getNamespace(), 
				application.getName(), application.getUUID(), mode, remoteName);
	}
	
	public static void fillServerWithOpenShiftDetails(IServerWorkingCopy wc, IApplication application, 
			IUser user, String mode, String remoteName) throws CoreException, OpenShiftException {
		fillServerWithOpenShiftDetails(wc, application.getApplicationUrl(),
				user.getRhlogin(), user.getDomain().getNamespace(), 
				application.getName(), application.getUUID(), mode, remoteName);
	}

	/**
	 * Fills an already-created server with the proper openshift details. 
	 * 
	 * @param server
	 * @param host
	 * @param username
	 * @param password
	 * @param domain
	 * @param appName
	 * @param sourceOrBinary
	 * @return
	 * @throws CoreException
	 */
	@SuppressWarnings("restriction")
	public static IServer fillServerWithOpenShiftDetails(IServer server, String host, 
			String username, String password, String domain, String appName, String appId,
			String mode, String remoteName) throws CoreException {
		ServerWorkingCopy wc = (ServerWorkingCopy)server.createWorkingCopy();
		fillServerWithOpenShiftDetails(wc, host, username, domain, appName, appId, mode, remoteName);
		IServer saved = wc.save(true, new NullProgressMonitor());
		ExpressServerUtils.setExpressPassword(wc, password);
		return saved;
	}
	
	public static void fillServerWithOpenShiftDetails(IServerWorkingCopy wc, String host, 
			String username, String domain, String appName, String appId,
			String mode, String remoteName) throws CoreException {

		if( host.indexOf("://") != -1)
			host = host.substring(host.indexOf("://") + 3);
		if( host.endsWith("/"))
			host = host.substring(0, host.length()-1);
		wc.setHost(host);
		if(mode.equals(ExpressServerUtils.EXPRESS_SOURCE_MODE)) {
			wc.setAttribute(IDeployableServer.SERVER_MODE, ExpressBehaviourDelegate.OPENSHIFT_ID);
		} else {
			wc.setAttribute(IDeployableServer.SERVER_MODE, ExpressBinaryBehaviourDelegate.OPENSHIFT_BINARY_ID);
			
		}
		wc.setAttribute(ATTRIBUTE_USERNAME, username);
		wc.setAttribute(ATTRIBUTE_DOMAIN, domain);
		wc.setAttribute(ATTRIBUTE_APPLICATION_NAME, appName);
		wc.setAttribute(ATTRIBUTE_APPLICATION_ID, appId);
		wc.setAttribute(ATTRIBUTE_EXPRESS_MODE, mode);
		wc.setAttribute(ATTRIBUTE_REMOTE_NAME, remoteName);
		((ServerWorkingCopy)wc).setAutoPublishSetting(Server.AUTO_PUBLISH_DISABLE);
		wc.setAttribute(IJBossToolingConstants.IGNORE_LAUNCH_COMMANDS, "true");
		wc.setAttribute(IJBossToolingConstants.WEB_PORT, 80);
		wc.setAttribute(IJBossToolingConstants.WEB_PORT_DETECT, "false");
	}
	
	
	public static IServer createServerAndRuntime(String runtimeID, String serverID,
			String location, String configuration) throws CoreException {
		IRuntime runtime = RuntimeUtils.createRuntime(runtimeID, location, configuration);
		return createServer(runtime, serverID);
	}
	
	public static IServer createServer(IRuntime runtime, String serverID) throws CoreException {
		return ServerCreationUtils.createServer2(runtime, serverID, serverID, "openshift");
	}
	
	public static IServer createServer(IRuntime runtime, IServerType serverType, String serverName) throws CoreException {
		return ServerCreationUtils.createServer2(runtime, serverType, serverName, "openshift");
	}
	
	/**
	 * Returns true if the given server is an OpenShift one, false otherwise.
	 * @param server the server to check
	 * @return true or false
	 */
	public static boolean isOpenShiftRuntime(IServer server) {
		final String serverTypeId = server.getServerType().getId();
		return (OPENSHIFT_SERVER_TYPE.equals(serverTypeId));
	}

	/**
	 * Returns true if the given server is a server using an openshift behaviour
	 * @param server the server to check
	 * @return true or false
	 */
	public static boolean isInOpenshiftBehaviourMode(IServer server) {
		IDeployableServer ds = ServerConverter.getDeployableServer(server);
		if( ds != null ) {
			IJBossServerPublishMethodType type = DeploymentPreferenceLoader.getCurrentDeploymentMethodType(server);
			if( type != null ) {
				String id = type.getId();
				if( ExpressBinaryBehaviourDelegate.OPENSHIFT_BINARY_ID.equals(id) || ExpressBehaviourDelegate.OPENSHIFT_ID.equals(id))
					return true;
			}
		}
		return false;
	}

	
}
