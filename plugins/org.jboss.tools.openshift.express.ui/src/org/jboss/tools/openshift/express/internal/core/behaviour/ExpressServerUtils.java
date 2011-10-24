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
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.core.util.RuntimeUtils;
import org.jboss.ide.eclipse.as.core.util.ServerCreationUtils;

/**
 * This class holds the attribute names whose values will be
 * stored inside a server object, as well as the utility methods
 * used to get and set them for a server. 
 *
 * @author Rob Stryker
 */
public class ExpressServerUtils {
	public static final String ATTRIBUTE_EXPRESS_MODE = "org.jboss.tools.openshift.express.internal.core.behaviour.ExpressMode";
	public static final String EXPRESS_BINARY_MODE =  "org.jboss.tools.openshift.express.internal.core.behaviour.ExpressBinaryMode";
	public static final String EXPRESS_SOURCE_MODE =  "org.jboss.tools.openshift.express.internal.core.behaviour.ExpressSourceMode";
	public static final String ATTRIBUTE_APPLICATION =  "org.jboss.tools.openshift.express.internal.core.behaviour.Application";
	public static final String ATTRIBUTE_DOMAIN =  "org.jboss.tools.openshift.express.internal.core.behaviour.Domain";
	public static final String ATTRIBUTE_USERNAME =  "org.jboss.tools.openshift.express.internal.core.behaviour.Username";
	public static final String ATTRIBUTE_PASSWORD =  "org.jboss.tools.openshift.express.internal.core.behaviour.Password";
	
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
	
	public static String getExpressApplication(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_APPLICATION, (String)null);
	}
	
	public static IServer setExpressApplication(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_APPLICATION, val);
		return wc.save(false, new NullProgressMonitor());
	}


	public static String getExpressDomain(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_DOMAIN, (String)null);
	}

	public static IServer setExpressDomain(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_DOMAIN, val);
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

	// TODO Must secure this!!! 
	public static String getExpressPassword(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_PASSWORD, (String)null);
	}
	
	public static IServer setExpressPassword(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_PASSWORD, val);
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
	 * @param app
	 * @param sourceOrBinary
	 * @param localRuntimeHomeDir
	 * @return
	 * @throws CoreException
	 */
	public static IServer createAS7OpenShiftServer(
			String host, String username, String password, 
			String domain, String app,
			String sourceOrBinary,
			String localRuntimeHomeDir) throws CoreException {
		IServer server = createServerAndRuntime(IJBossToolingConstants.AS_70,
				IJBossToolingConstants.SERVER_AS_70, 
				localRuntimeHomeDir, /* irrelevant */ "default");
		return fillServerWithOpenShiftDetails(server, host, username, password, domain, app, sourceOrBinary);
	}
	
	/**
	 * To be used if a runtime is already provided, for example if a user has 
	 * selected the proper as7 local runtime delegate from a combo box
	 * 
	 * @param host
	 * @param username
	 * @param password
	 * @param domain
	 * @param app
	 * @param sourceOrBinary
	 * @param runtime
	 * @return
	 * @throws CoreException
	 */
	public static IServer createAS7OpenShiftServer(
			String host, String username, String password, 
			String domain, String app, String sourceOrBinary,
			IRuntime runtime) throws CoreException {
		IServer server = createServer(runtime, IJBossToolingConstants.SERVER_AS_70);
		return fillServerWithOpenShiftDetails(server, host, username, password, domain, app, sourceOrBinary);
	}
	
	/**
	 * Fills an already-created server with the proper openshift details. 
	 * 
	 * @param server
	 * @param host
	 * @param username
	 * @param password
	 * @param domain
	 * @param app
	 * @param sourceOrBinary
	 * @return
	 * @throws CoreException
	 */
	public static IServer fillServerWithOpenShiftDetails(IServer server,
			String host, String username, String password, String domain, String app,
			String mode) throws CoreException {
		if( host.indexOf("://") != -1)
			host = host.substring(host.indexOf("://") + 3);
		if( host.endsWith("/"))
			host = host.substring(0, host.length()-1);
		ServerWorkingCopy wc = (ServerWorkingCopy)server.createWorkingCopy();
		wc.setHost(host);
		wc.setAttribute(IDeployableServer.SERVER_MODE, "openshift");
		wc.setAttribute(ATTRIBUTE_USERNAME, username);
		wc.setAttribute(ATTRIBUTE_PASSWORD, password);
		wc.setAttribute(ATTRIBUTE_DOMAIN, domain);
		wc.setAttribute(ATTRIBUTE_APPLICATION, app);
		wc.setAttribute(ATTRIBUTE_EXPRESS_MODE, mode);
		wc.setAutoPublishSetting(Server.AUTO_PUBLISH_DISABLE);
		wc.setAttribute(IJBossToolingConstants.IGNORE_LAUNCH_COMMANDS, "true");
		wc.setAttribute(IJBossToolingConstants.WEB_PORT, 80);
		wc.setAttribute(IJBossToolingConstants.WEB_PORT_DETECT, "false");
		return wc.save(true, new NullProgressMonitor());
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

}
