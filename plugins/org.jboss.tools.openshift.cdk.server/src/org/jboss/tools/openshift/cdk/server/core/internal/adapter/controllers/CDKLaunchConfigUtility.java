package org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers;

import static org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.IExternalLaunchConstants.ATTR_ARGS;
import static org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.IExternalLaunchConstants.ENVIRONMENT_VARS_KEY;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class CDKLaunchConfigUtility {
	public ILaunchConfigurationWorkingCopy createExternalToolsLaunchConfig(IServer s, String args, String launchConfigName) throws CoreException {
		return setupLaunch(s, args, launchConfigName, s.getLaunchConfiguration(true, new NullProgressMonitor()));
	}
	
	public ILaunchConfigurationWorkingCopy setupLaunch(IServer s, String args, String launchConfigName, ILaunchConfiguration startupConfig) throws CoreException {
		final CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());

		ILaunchConfigurationWorkingCopy wc = findLaunchConfig(s, launchConfigName);
		
		wc.setAttributes(startupConfig.getAttributes());
		wc.setAttribute(ATTR_ARGS, args);
		
		
		// Set the environment flag
    	boolean passCredentials = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
    	if( passCredentials) {
    		Map<String,String> existingEnvironment = startupConfig.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String,String>)null);
    		if( existingEnvironment == null ) {
    			existingEnvironment = new HashMap<String,String>();
    		}
    		
    		String userName = cdkServer.getUsername();
    		if( userName == null ) {
    			// This is an error situation and the user should be made aware
    			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, 
    					"The credentials for " + s.getName() + " are invalid. No username found. Please open your server editor " 
    					+ "and set your access.redhat.com credentials."));
    		}
    		
    		HashMap<String,String> env = new HashMap<String,String>(existingEnvironment);
    		String passKey = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK_ENV_SUB_PASSWORD);
    		String pass = cdkServer.getPassword();
    		if( pass == null ) {
    			// This is an error situation and the user should be made aware
    			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, 
    					"The credentials for " + s.getName() + " are invalid. No password found for username "
    					+ cdkServer.getUsername() + " for the access.redhat.com domain. Please open your server editor " + 
    							"set your access.redhat.com credentials."));
    		}
    		if( passKey != null && pass != null )
    			env.put(passKey, pass);
    		
    		wc.setAttribute(ENVIRONMENT_VARS_KEY, env);
    	} else {
    		wc.setAttribute(ENVIRONMENT_VARS_KEY, startupConfig.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String,String>)null));
    	}
		return wc;
		
	}
	

	private ILaunchConfigurationWorkingCopy findLaunchConfig(IServer s, String launchName) throws CoreException {
		return ExternalLaunchUtil.findExternalToolsLaunchConfig(s, launchName);
	}
}
