/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal.detection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.dmr.ModelNode;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.ui.internal.detection.MissingMinishiftResolutionProvider;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;
import org.jboss.tools.runtime.core.model.RuntimeDetectionProblem;

public class CDK3RuntimeDetector extends AbstractCDKRuntimeDetector{
	
	public static final String CDK_RUNTIME_TYPE = "CDK 3";
	public static final String PROP_CDK_VERSION = "cdk.version";
	
	public static final String OVERRIDE_MINISHIFT_LOCATION = "OVERRIDE_MINISHIFT_LOCATION";
	
	@Override
	protected boolean validate(File root) {
		boolean matchesHomeMinishift = isHomeDirectory(root.getParentFile()) && 
				".minishift".equals(root.getName()) && super.validate(root);
		if( matchesHomeMinishift )
			return true;
		
		String envvar = System.getenv("MINISHIFT_HOME");
		boolean matchesEnvVar = envvar != null && new File(envvar).exists() && super.validate(root);
		return matchesEnvVar;
		
	}
	
	@Override
	protected boolean matches(RuntimeDefinition def, IServer server) {
		String fromServer = server.getAttribute(CDK3Server.MINISHIFT_FILE, (String)null);
		String fromProblemResolver = (String)def.getProperty(OVERRIDE_MINISHIFT_LOCATION);
		String fromPath = MinishiftBinaryUtility.getMinishiftLocation();
		
		// If all are null... go for it 
		if( fromServer == null ) {
			if( fromPath == null && fromProblemResolver == null )
				return true; // all null, match
			return false; // server null, another path isn't
		}
		
		// fromServer is not null
		if( fromProblemResolver != null ) {
			return fromProblemResolver.equals(fromServer);
		}
		
		return fromPath == null ? true : fromPath.equals(fromServer);
	}
	

	@Override
	protected String getServerType() {
		return CDKServer.CDK_V3_SERVER_TYPE;
	}

	@Override
	protected String[] getRequiredChildren() {
		return new String[]{CDKConstants.CDK_RESOURCE_CDK};
	}
	

	@Override
	protected String getDefinitionName(File root) {
		return CDK3Server.getServerTypeBaseName();
	}
	
	@Override
	protected String getRuntimeDetectionType() {
		return CDK_RUNTIME_TYPE;
	}
	
	@Override
	protected String getDefinitionVersion(File root) {
		File cdkFile = new File(root, CDKConstants.CDK_RESOURCE_CDK);
		Properties props = readProperties(cdkFile);
		String version = props.getProperty(PROP_CDK_VERSION);
		version = (version == null ? "3.0" : version);
		return version;
	}
	
	@Override
	protected void initializeServer(IServerWorkingCopy wc, RuntimeDefinition runtimeDefinition) throws CoreException {
		String folder = runtimeDefinition.getLocation().getAbsolutePath();
		File cdkFile = new File(folder, CDKConstants.CDK_RESOURCE_CDK);
		Properties props = readProperties(cdkFile);
		String user = props.getProperty(DOT_CDK_SUBSCRIPTION_USERNAME);
		String password = System.getenv(DOT_CDK_SUBSCRIPTION_PASSWORD);
		if( user != null ) {
			addToCredentialsModel(CredentialService.REDHAT_ACCESS, user, password);
		}
		wc.setAttribute(CDK3Server.MINISHIFT_HOME, folder);
		wc.setAttribute(CDK3Server.PROP_HYPERVISOR, getHypervisor(folder));
		wc.setAttribute(CDKServer.PROP_USERNAME, user);
		wc.setAttribute(CDK3Server.MINISHIFT_FILE, getMinishiftLoc(runtimeDefinition));
		
	}
	
	private String getHypervisor(String folder) {
		String[] validHypervisors = CDK3Server.getHypervisors(); 
		String hyperV = validHypervisors[0];
		
		File config = new File(folder, "config");
		File configJson = new File(config, "config.json");
		if( !configJson.exists() || !configJson.isFile()) {
			return hyperV;
		}
		
		String path = configJson.getAbsolutePath();
		try {
			String content = new String(Files.readAllBytes(Paths.get(path)));
			ModelNode mn = ModelNode.fromJSONString(content);
			ModelNode o = mn.get("vm-driver");
			String val = (o == null ? null : o.asString());
			if( val != null && Arrays.asList(validHypervisors).contains(val)) {
				return val;
			}
		} catch (IOException e) {
			CDKCoreActivator.pluginLog().logError(e);
		}
		return hyperV;
	}
	
	private String getMinishiftLoc(RuntimeDefinition runtimeDefinition) {
		String fromDef = (String)runtimeDefinition.getProperty(OVERRIDE_MINISHIFT_LOCATION);
		if( fromDef != null && !fromDef.isEmpty() && new File(fromDef).exists()) {
			return fromDef;
		}
		return MinishiftBinaryUtility.getMinishiftLocation();
	}
	
	@Override
	public void calculateProblems(RuntimeDefinition def) {
		String override = (String)def.getProperty(OVERRIDE_MINISHIFT_LOCATION);
		String minishiftLoc = MinishiftBinaryUtility.getMinishiftLocation();
		if( doesNotExist(override) && doesNotExist(minishiftLoc) ) {
			RuntimeDetectionProblem p = createDetectionProblem("Set minishift binary location.", 
					"The minishift binary could not be located on the system path.", 
					IStatus.ERROR, MissingMinishiftResolutionProvider.MISSING_MINISHIFT_PROBLEM_ID);
			def.setProblems(new RuntimeDetectionProblem[] { p });
		} else {
			def.setProblems(new RuntimeDetectionProblem[] { });
		}
	}
	
	private boolean doesNotExist(String s) {
		return s == null || s.isEmpty() || !(new File(s).exists());
	}
}
