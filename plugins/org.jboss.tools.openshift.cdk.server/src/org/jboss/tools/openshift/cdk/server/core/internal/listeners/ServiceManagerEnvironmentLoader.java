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
package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CommandTimeoutException;

public abstract class ServiceManagerEnvironmentLoader {
	
	public static final String SHARED_INFO_KEY = "cdk.sharedinfo.serviceManagerEnvironment";
	protected static int TYPE_VAGRANT = 1;
	protected static int TYPE_MINISHIFT = 2;
	
	public static ServiceManagerEnvironmentLoader getVagrantLoader() {
		return new VagrantServiceManagerEnvironmentLoader();
	}
	public static ServiceManagerEnvironmentLoader getMinishiftLoader() {
		return new MinishiftServiceManagerEnvironmentLoader();
	}
	
	public static ServiceManagerEnvironmentLoader type(IServer s) {
		if( s.getServerType().getId().equals(CDKServer.CDK_SERVER_TYPE)) {
			return getVagrantLoader();
		}
		return getMinishiftLoader();
	}
	
	
	private int type;
	public ServiceManagerEnvironmentLoader(int type) {
		this.type = type;
	}
	
	
	public  ServiceManagerEnvironment getOrLoadServiceManagerEnvironment(IServer server, boolean save) {
		return getOrLoadServiceManagerEnvironment(server, save, 1);
	}
	
	public  ServiceManagerEnvironment getOrLoadServiceManagerEnvironment(IServer server, boolean save, int maxTries) {
		IControllableServerBehavior behavior = JBossServerBehaviorUtils.getControllableBehavior(server);
		Object o = behavior.getSharedData(SHARED_INFO_KEY);
		ServiceManagerEnvironment ret = null;
		if( !(o instanceof ServiceManagerEnvironmentLoader )) {
			ret = loadServiceManagerEnvironment(server, maxTries);
		} else {
			ret = (ServiceManagerEnvironment)o;
		}
		if( save ) {
			behavior.putSharedData(SHARED_INFO_KEY, ret);
		}
		return ret;
	}
	
	public  void clearServiceManagerEnvironment(IServer server) {
		IControllableServerBehavior behavior = JBossServerBehaviorUtils.getControllableBehavior(server);
		behavior.putSharedData(SHARED_INFO_KEY, null);
	}
	
	
	public  ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server, int maxTries) {
		for( int i = 0; i < maxTries; i++ ) {
			ServiceManagerEnvironment env = loadServiceManagerEnvironment(server);
			if( env != null )
				return env;
		}
		
		return null;
	}
	
	

	protected HashMap<String, String> callAndParseEnvVar(Map<String, String> env, String[] args, 
			String cmdLoc, File wd) throws IOException {
		String[] lines = null;
		try {
			lines = CDKLaunchUtility.call(cmdLoc, args, wd, env, 30000, false);
		} catch (IOException ce) {
			throw ce;
		} catch (CommandTimeoutException ce) {
			// Try to salvage it, it could be the process never terminated but it got all the output
			if( ce.getInLines() == null ) {
				throw new IOException(ce);
			}
			// Try to salvage it, it could be the process never terminated but it got all the output
			lines = ce.getInLines() == null ? null : (String[]) ce.getInLines().toArray(new String[ce.getInLines().size()]); 
		}
		
		HashMap<String, String> adbEnv = ServiceManagerUtility.parseLines(lines);
		if( adbEnv == null ) {
			throw new IOException("Error calling " + cmdLoc + " with args " + String.join(", ", args));
		}
		return adbEnv;
	}

	protected Properties callAndParseProperties(Map<String, String> env, 
			String[] args, String cmdLoc, File wd) throws IOException {
		String[] lines = null;
		try {
			lines = CDKLaunchUtility.call(cmdLoc, args,  wd, env,30000, false);
		} catch(IOException ioe) {
			throw ioe;
		} catch (CommandTimeoutException ce) {
			if( ce.getInLines() == null ) {
				throw new IOException(ce);
			}
			// Try to salvage it, it could be the process never terminated but it got all the output
			lines = ce.getInLines() == null ? null : (String[]) ce.getInLines().toArray(new String[ce.getInLines().size()]); 
		}
		
		String imploded = String.join("\n", Arrays.asList(lines));
		Properties ret = new Properties();
		ret.load(new ByteArrayInputStream(imploded.getBytes()));
		return ret;
	}


	protected Map<String, String> merge(Map<String, String> env, Properties props) {
		// Merge the two together
		Set<String> propNames = props.stringPropertyNames();
		Iterator<String> nameIt = propNames.iterator();
		while (nameIt.hasNext()) {
			String k = nameIt.next();
			env.put(k, props.getProperty(k));
		}
		return env;
	}
	
	/**
	 * Run the remote vagrant command to create an service-manager from a server
	 * @param server
	 * @return
	 */
	public abstract ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server);

}