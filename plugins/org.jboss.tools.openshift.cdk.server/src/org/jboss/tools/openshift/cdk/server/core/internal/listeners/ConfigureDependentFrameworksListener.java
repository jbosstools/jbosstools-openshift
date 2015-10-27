package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection.Builder;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CDKLaunchConfigUtility;
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
		configureOpenshift(adb);
		configureDocker(server, adb);
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
		for( int i = 0; i < cons.length; i++ ) {
			System.out.println(dockerHost + " vs " + cons[i].getUri());
			if( cons[i].getUri().equals(dockerHost) || cons[i].getUri().equals(httpHost)) {
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
				// For some reason, the connection fails if i set this. 
				// Not sure why the disconnect between what adbinfo tells me to use and what works upstream
//				if( tlsVerify ) {
//					String tlsCertPath = adb.env.get("DOCKER_CERT_PATH");
//					tcpConnectionBuilder.tcpCertPath(tlsCertPath);
//				}
				DockerConnection con = tcpConnectionBuilder.build();
				IDockerConnection[] other = mgr.getConnections();
				mgr.addConnection(con);
			}
		} catch(DockerException de) {
			de.printStackTrace();
		}
	}
	private void configureOpenshift(ADBInfo adb) {
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
			((Connection)con).setAuthScheme("Basic");
			((Connection)con).setUsername("test-admin");
			((Connection)con).setPassword("password");
			ConnectionsRegistrySingleton.getInstance().add(con);
		}
	}
	
	private ADBInfo loadADBInfo(IServer server) {
		try {
			ILaunchConfigurationWorkingCopy wc = new CDKLaunchConfigUtility().
					createExternalToolsLaunchConfig(server, "adbinfo", server.getName() + " adbinfo");
			ILaunch launch2 = wc.launch("run", new NullProgressMonitor());
			final IProcess[] processes = launch2.getProcesses();
			IStreamMonitor mon = processes[0].getStreamsProxy().getOutputStreamMonitor();
			final ArrayList<String> lines = new ArrayList<String>();
			mon.addListener(new IStreamListener() {
				public void streamAppended(String text, IStreamMonitor monitor) {
					lines.add(text);
				}
			});
			
			while( !processes[0].isTerminated()) {
				try {
					Thread.sleep(500);
				} catch(InterruptedException ie) {
					// ignore
				}
			}
			
			HashMap<String,String> env = new HashMap<String,String>();
			Iterator<String> lineIterator = lines.iterator();
			while(lineIterator.hasNext()) {
				String oneAppend = lineIterator.next();
				String[] allAppends = oneAppend.split("\n");
				for( int i = 0; i < allAppends.length; i++ ) {
					if( allAppends[i].trim().startsWith("export ")) {
						int eq = allAppends[i].indexOf("=");
						if( eq != -1 ) {
							String k = allAppends[i].substring("export ".length(), eq);
							String v = allAppends[i].substring(eq+1);
							env.put(k, v);
						}
					}
				}
			}
			
			
			System.out.println(lines);
			return new ADBInfo(env);
		} catch(CoreException ce) {
			ce.printStackTrace(); // TODO log
		}
		return null;
	}
	
	private static class ADBInfo {
		// Mocked
		private int openshiftPort = 8443;
		private String openshiftHost = "https://10.1.2.2";
		
		private HashMap<String,String> env;
		public ADBInfo(HashMap<String,String> env) {
			this.env = env;
			try {
				String dockerHost = env.get("DOCKER_HOST");
				URI url = new URI(dockerHost);
				String h = url.getHost();
				openshiftHost = "https://" + h;
				//openshiftPort = url.getPort();  // i don't believe this is the correct port. This is for docker
			} catch(URISyntaxException murle) {
				murle.printStackTrace();
			}
		}
		
	}
}
