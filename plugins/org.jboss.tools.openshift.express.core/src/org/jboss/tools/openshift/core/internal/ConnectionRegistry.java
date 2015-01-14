package org.jboss.tools.openshift.core.internal;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.tools.openshift.core.Connection;
import org.jboss.tools.openshift.express.core.OpenShiftCoreException;
import org.jboss.tools.openshift.express.internal.core.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils.UrlPortions;

/**
 * Registry of possible connections to a kubernetes server
 */
public class ConnectionRegistry {

	private List<org.jboss.tools.openshift.core.Connection> connections = new ArrayList<org.jboss.tools.openshift.core.Connection>();
	
	public ConnectionRegistry(){
		load();
	}
	
	/**
	 * A copy of the list of connections.
	 * @return java.util.List<String>
	 */
	public  List<org.jboss.tools.openshift.core.Connection> getConnections(){
		return new ArrayList<Connection>(connections);
	}
	
	/**
	 * Add the connection to the registry
	 * @param conn
	 * @return true if the connection was added; false if it already exists
	 */
	public boolean add(Connection conn){
		if(connections.contains(conn)) return false;
		connections.add(conn);
		return true;
	}

	/**
	 * Remove the given connection from the registry
	 * @param connection  the connection to remove
	 * @return true if the connection was removed; otherwise false
	 */
	public boolean remove(Connection connection) {
		if(!connections.contains(connection)) return false;
		connections.remove(connection);
		return true;
	}

	/**
	 * Save the user list to preferences and secure storage
	 */
	public void save() {
		final List<String> strings = new ArrayList<String>();
		for (Connection conn : connections) {
			strings.add(conn.toString());
		}
		OpenShiftPreferences.INSTANCE.saveKubernetesConnections(strings);
	}
	
	private void load(){
		List<String> connectionStrings = OpenShiftPreferences.INSTANCE.getKubernetesConnections();
		for (String conn : connectionStrings) {
			try {
				UrlPortions parts = UrlUtils.toPortions(conn);
				String host = String.format("%s%s",parts.getScheme(), parts.getHost());
				if(parts.getPort() != 0){
					host = String.format("%s:%s", host, parts.getPort());
				}
				connections.add(new KubernetesConnection(host, parts.getUsername(), parts.getPassword()));
			} catch (UnsupportedEncodingException e) {
				throw new OpenShiftCoreException(e, "Unable to Load Connections");
			} catch (MalformedURLException e) {
				throw new OpenShiftCoreException(e, "Unable to Load Connections");
			}
		}
	}
}
