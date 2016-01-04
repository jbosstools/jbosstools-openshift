package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection.Builder;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstantUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.VagrantLaunchUtility;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ConnectionsFactoryTracker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.core.connection.Connection;

public class ConfigureDependentFrameworksListener extends UnitedServerListener {
	public void serverChanged(final ServerEvent event) {
		if( serverSwitchesToState(event, IServer.STATE_STARTED) && canHandleServer(event.getServer())) {
			new Thread("Loading ADBInfo to configure dependent frameworks") {
				public void run() {
					launchChange(event.getServer());
				}
			}.start();
		}
	}
	
	private void launchChange(IServer server) {
		ADBInfo adb = loadADBInfo(server);
		if( adb != null ) {
			configureOpenshift(server, adb);
			configureDocker(server, adb);
		}
	}
	

	public boolean canHandleServer(IServer server) {
		if( server.getServerType().getId().equals(CDKServer.CDK_SERVER_TYPE)) 
			return true;
		return false;
	}
	
	private String getNextName(IServer server, DockerConnectionManager mgr) {
		// Cache all names
		IDockerConnection[] cons = mgr.getConnections();
		ArrayList<String> names = new ArrayList<String>();
		for( int i = 0; i < cons.length; i++ ) {
			names.add(cons[i].getName());
		}
		
		// Find a name that doesnt match existing connection
		final String nameBase = server.getName();
		String name = nameBase;
		int count = 0;
		boolean done = false;
		while(!done) {
			if( names.contains(name)) {
				count++;
				name = nameBase + " (" + count + ")";
			} else {
				done = true;
			}
		}
		return name;
	}
	
	private boolean dockerConnectionExists(DockerConnectionManager mgr, String dockerHost) {
		IDockerConnection[] cons = mgr.getConnections();
		String httpHost = dockerHost.replace("tcp://", "http://");
		String httpsHost = dockerHost.replace("tcp://", "https://");
		for( int i = 0; i < cons.length; i++ ) {
			if( cons[i].getUri().equals(dockerHost) || cons[i].getUri().equals(httpHost) || cons[i].getUri().equals(httpsHost)) {
				return true;
			}
		}
		return false;
	}
	private void configureDocker(IServer server, ADBInfo adb) {
		try {
			DockerConnectionManager mgr = org.eclipse.linuxtools.docker.core.DockerConnectionManager.getInstance();
			final String dockerHost = adb.env.get("DOCKER_HOST");
			if( !dockerConnectionExists(mgr, dockerHost)) {
				final Builder tcpConnectionBuilder = new DockerConnection.Builder()
						.name(getNextName(server, mgr)).tcpHost(dockerHost);
				String tlsVerifyString = adb.env.get("DOCKER_TLS_VERIFY");
				boolean tlsVerify = (Integer.parseInt(tlsVerifyString) != 0);
				if( tlsVerify ) {
					String tlsCertPath = adb.env.get("DOCKER_CERT_PATH");
					tcpConnectionBuilder.tcpCertPath(tlsCertPath);
				}
				DockerConnection con = tcpConnectionBuilder.build();
				IDockerConnection[] other = mgr.getConnections();
				mgr.addConnection(con);
			}
		} catch(DockerException de) {
			de.printStackTrace();
		}
	}
	
	private static String DOTCDK_AUTH_SCHEME = "openshift.auth.scheme";
	private static String DOTCDK_AUTH_USERNAME = "openshift.auth.username";

	private Properties getDotCDK(IServer server) {
		String cdkFolder = server.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		if( cdkFolder != null && new File(cdkFolder).exists()) {
			File dotcdk = new File(cdkFolder, ".cdk");
			if( dotcdk.exists()) {
				try {
					Properties props = new Properties();
					props.load(new FileInputStream(dotcdk));
					return props;
				} catch(IOException ioe) {
					CDKCoreActivator.pluginLog().logError("Error loading properties from .cdk file " + dotcdk.getAbsolutePath(), ioe);
				}
			}
		}
		return new Properties();
	}
	
	private void configureOpenshift(IServer server, ADBInfo adb) {
		Properties dotcdkProps = getDotCDK(server);
		String authScheme = dotcdkProps.containsKey(DOTCDK_AUTH_SCHEME) ? dotcdkProps.getProperty(DOTCDK_AUTH_SCHEME) : "Basic";
		String username = dotcdkProps.containsKey(DOTCDK_AUTH_USERNAME) ? dotcdkProps.getProperty(DOTCDK_AUTH_USERNAME) : "test-admin";
		String password = "password";
		Collection<IConnection> connections = ConnectionsRegistrySingleton.getInstance().getAll();
		Iterator<IConnection> it = connections.iterator();
		String soughtHost = adb.openshiftHost + ":" + adb.openshiftPort;
		boolean found = false;
		while(it.hasNext() && !found) {
			IConnection c = it.next();
			if( c.getType() == ConnectionType.Kubernetes) {
				String host = c.getHost();
				if( host.equals(soughtHost)) {
					found = true;
				}
			}
		}
		
		if( !found ) {
			ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
			connectionsFactory.open();
			IConnectionFactory factory = connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_OPENSHIFT_ID);
			IConnection con = factory.create(soughtHost);
			((Connection)con).setAuthScheme(authScheme);
			((Connection)con).setUsername(username);
			((Connection)con).setPassword(password);
			ConnectionsRegistrySingleton.getInstance().add(con);
		}
	}
	

	private File getWorkingDirectory(IServer s) {
		String str = s.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		if( str != null && new File(str).exists()) {
			return new File(str);
		}
		return null;
	}
	
	private ADBInfo loadADBInfo(IServer server) {
		
		String[] args = new String[]{CDKConstants.VAGRANT_CMD_ADBINFO, CDKConstants.VAGRANT_FLAG_NO_COLOR};
		HashMap<String,String> env = new HashMap<String,String>(System.getenv());
		
    	String vagrantcmdloc = CDKConstantUtility.getVagrantLocation(server);
		
    	CDKServer cdkServer = (CDKServer)server.loadAdapter(CDKServer.class, new NullProgressMonitor());
    	boolean passCredentials = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
		if( passCredentials ) {
			String userKey = cdkServer.getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
			String passKey = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK_ENV_SUB_PASSWORD);
			env.put(userKey, cdkServer.getUsername());
			env.put(passKey, cdkServer.getPassword());
		}
		
		HashMap<String,String> adbEnv = new HashMap<String,String>();
	    try {
	    	String[] lines = VagrantLaunchUtility.call(vagrantcmdloc, args,  getWorkingDirectory(server), env);
			String setEnvVarCommand = Platform.getOS().equals(Platform.OS_WIN32) ? "setx " : "export ";
			String setEnvVarDelim = Platform.getOS().equals(Platform.OS_WIN32) ? " " : "=";
			Iterator<String> lineIterator = Arrays.asList(lines).iterator();
			while(lineIterator.hasNext()) {
				String oneAppend = lineIterator.next();
				String[] allAppends = oneAppend.split("\n");
				for( int i = 0; i < allAppends.length; i++ ) {
					if( allAppends[i].trim().startsWith(setEnvVarCommand)) {
						String lineRemainder = allAppends[i].trim().substring(setEnvVarCommand.length());
						int eq = lineRemainder.indexOf(setEnvVarDelim);
						if( eq != -1 ) {
							String k = lineRemainder.substring(0, eq);
							String v = lineRemainder.substring(eq+1);
							adbEnv.put(k, v);
						}
					}
				}
			}
			return new ADBInfo(adbEnv);
		} catch( URISyntaxException urise) {
			CDKCoreActivator.pluginLog().logError("Environment variable DOCKER_HOST is not a valid uri:  " + env.get("DOCKER_HOST"), urise);
		} catch(IOException | TimeoutException ce) {
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to vagrant adbinfo. ", ce);
		}
		return null;
	}
	
	private static class ADBInfo {
		private int openshiftPort = 8443;
		private String openshiftHost = "https://10.1.2.2";
		
		private HashMap<String,String> env;
		public ADBInfo(HashMap<String,String> env) throws URISyntaxException {
			this.env = env;
			String dockerHost = env.get("DOCKER_HOST");
			URI url = new URI(dockerHost);
			String h = url.getHost();
			openshiftHost = "https://" + h;
		}
		
	}
}
