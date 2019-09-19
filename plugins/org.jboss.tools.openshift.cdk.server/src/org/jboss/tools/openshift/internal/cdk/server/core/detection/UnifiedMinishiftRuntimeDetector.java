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
package org.jboss.tools.openshift.internal.cdk.server.core.detection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.dmr.ModelNode;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKConstants;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDK32Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDK3Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.Minishift17Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.VersionUtil;
import org.jboss.tools.openshift.internal.cdk.server.core.detection.MinishiftVersionLoader.MinishiftVersions;
import org.jboss.tools.openshift.internal.cdk.server.ui.detection.MissingMinishiftResolutionProvider;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;
import org.jboss.tools.runtime.core.model.RuntimeDetectionProblem;

public class UnifiedMinishiftRuntimeDetector extends AbstractCDKRuntimeDetector {
	private static final String CONFIG = "config";
	private static final String CONFIG_JSON = "config.json";
	private static final String VM_DRIVER = "vm-driver";
	private static final String MINISHIFT = "minishift";
	
	public static final String CDK_RUNTIME_TYPE = "CDK 3.0+";
	public static final String CDK_32_RUNTIME_TYPE = "CDK 3.2+";
	public static final String MS_17_RUNTIME_TYPE = "Minishift 1.7+";
	
	
	public static final String PROP_CDK_VERSION = "cdk.version";
	public static final String PROP_MINISHIFT_VERSION_RESPONSE = "minishift.version.response";
	public static final String PROP_SERVER_TYPE = "minishift.definition.servertype";
	
	public static final String OVERRIDE_MINISHIFT_LOCATION = "OVERRIDE_MINISHIFT_LOCATION";

	// Generated from whitelistGenerator.sh
	
	private static final Pattern WHITELIST_PATTERN = Pattern.compile("cdk-[0-9][.][0-9].*-minishift-(linux|darwin|windows)-amd64(.exe)?");
	
	
	@Override
	public RuntimeDefinition getRuntimeDefinition(File root, IProgressMonitor monitor) {
		if( isValidMinishiftHome(root)) {
			return createHomeDefinition(root);
		}
		
		if( folderContainsMinishiftBinary(root)) {
			return createBinaryDefinition(root);
		}
		return null;
	}

	public RuntimeDefinition createBinaryDefinition(File root) {
		File minishiftBin = getMinishiftBinary(root);
		if( minishiftBin == null )
			return null;
		
		
		String baseName = null;
		String type = null;
		String serverType = null;
		
		minishiftBin.setExecutable(true);
		String minishiftPath = minishiftBin.getAbsolutePath();
		MinishiftVersions minishiftVersionProps = MinishiftVersionLoader.getVersionProperties(minishiftPath);
		String vers = null;
		if (VersionUtil.matchesCDK30(minishiftVersionProps) == null) {
			baseName = CDK3Server.getServerTypeBaseName();
			type = CDK_RUNTIME_TYPE;
			vers = minishiftVersionProps.getCDKVersion();
			serverType = CDK3Server.CDK_V3_SERVER_TYPE;
		} else if (VersionUtil.matchesCDK32(minishiftVersionProps) == null) {
			baseName = CDK32Server.getServerTypeBaseName();
			type = CDK_32_RUNTIME_TYPE;
			serverType = CDK3Server.CDK_V32_SERVER_TYPE;
			vers = minishiftVersionProps.getCDKVersion();
		} else if (VersionUtil.matchesMinishift17(minishiftVersionProps) == null) {
			baseName = Minishift17Server.getServerTypeBaseName();
			type = MS_17_RUNTIME_TYPE;
			serverType = CDK3Server.MINISHIFT_1_7_SERVER_TYPE;
			vers = minishiftVersionProps.getMinishiftVersion();
		}
		RuntimeDefinition def = createDefinition(baseName, vers, type, root);
		def.setProperty(PROP_MINISHIFT_VERSION_RESPONSE, minishiftVersionProps);
		def.setProperty(OVERRIDE_MINISHIFT_LOCATION, minishiftPath);
		def.setProperty(PROP_SERVER_TYPE, serverType);
		calculateProblems(def);
		return def;
	}
	
	private RuntimeDefinition createHomeDefinition(File root) {
		String baseName = null;
		String type = null;
		String serverType = null;
		String vers = getVersionFromMinishiftHome(root);
		if( CDK3Server.matchesCDK3(vers)) {
			baseName = CDK3Server.getServerTypeBaseName();
			type = CDK_RUNTIME_TYPE;
			serverType = CDK3Server.CDK_V3_SERVER_TYPE;
		} else if( CDK32Server.matchesCDK32(vers) ) {
			baseName = CDK32Server.getServerTypeBaseName();
			type = CDK_32_RUNTIME_TYPE;
			serverType = CDK3Server.CDK_V32_SERVER_TYPE;
		}
		RuntimeDefinition def = createDefinition(
				baseName, vers, type, root);
		def.setProperty(PROP_SERVER_TYPE, serverType);
		calculateProblems(def);
		return def;
	}
	
	private File getMinishiftBinary(File root) {
		File ms = new File(root, MINISHIFT);
		if( ms.exists()) 
			return ms;
		return folderWhiteListBin(root);
		
	}
	
	private boolean folderContainsMinishiftBinary(File f) {
		File bin = getMinishiftBinary(f);
		return bin != null && bin.exists() && bin.isFile();
	}
	
	private File folderWhiteListBin(File folder) {
		String[] children = folder.list();
		if( children == null )
			return null;

		for( int i = 0; i < children.length; i++ ) {
		     Matcher m = WHITELIST_PATTERN.matcher(children[i]);
		     if( m.matches()) {
		    	 return new File(folder, children[i]);
		     }
		}
		return null;
	}

	
	private boolean isValidMinishiftHome(File f) {
		return hasChildFiles(f, getRequiredMinishiftHomeChildren());
	}
	
	protected String[] getRequiredMinishiftHomeChildren() {
		return new String[] { CDKConstants.CDK_RESOURCE_CDK, CONFIG, CONFIG + "/" + CONFIG_JSON };
	}


	@Override
	protected boolean matches(RuntimeDefinition def, IServer server) {
		return minishiftFileMatches(def, server) && minishiftHomeMatches(def, server);
	}
	
	private boolean minishiftHomeMatches(RuntimeDefinition def, IServer server) {
		String serverMinishiftHome = server.getAttribute(CDK3Server.MINISHIFT_HOME, (String) null);
		File loc = def.getLocation();
		if( isValidMinishiftHome(loc)) {
			File fromServerFile = serverMinishiftHome == null ? null : new File(serverMinishiftHome);
			return loc.equals(fromServerFile);
		} else if( folderContainsMinishiftBinary(loc)) {
			// Check if the assumed minishift home that will be used on server creation matches the existing server
			String folder = loc.getAbsolutePath();
			File msHome = new File(folder, "MINISHIFT_HOME");
			if(serverMinishiftHome != null && new File(serverMinishiftHome).equals(msHome)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean minishiftFileMatches(RuntimeDefinition def, IServer server) {
		String fromServer = server.getAttribute(CDK3Server.MINISHIFT_FILE, (String) null);
		String fromProblemResolver = (String) def.getProperty(OVERRIDE_MINISHIFT_LOCATION);
		String fromPath = BinaryUtility.MINISHIFT_BINARY.getLocation();

		// If all are null... go for it 
		if (fromServer == null) {
			return  (fromPath == null && fromProblemResolver == null);
		}

		// fromServer is not null
		if (fromProblemResolver != null) {
			return fromProblemResolver.equals(fromServer);
		}

		return fromPath == null ? true : fromPath.equals(fromServer);
	}


	@Override
	protected String getServerType(RuntimeDefinition def) {
		return (String)def.getProperty(PROP_SERVER_TYPE);
	}
	private String getVersionFromMinishiftHome(File root) {
		File cdkFile = new File(root, CDKConstants.CDK_RESOURCE_CDK);
		Properties props = readProperties(cdkFile);
		String version = props.getProperty(PROP_CDK_VERSION);
		version = (version == null ? "3.0" : version);
		return version;
	}

	@Override
	protected void initializeServer(IServerWorkingCopy wc, RuntimeDefinition runtimeDefinition) throws CoreException {
		if( runtimeDefinition.getProperty(PROP_MINISHIFT_VERSION_RESPONSE) != null ) {
			// This is a definition based on a minishift binary, not a minishift home
			String folder = runtimeDefinition.getLocation().getAbsolutePath();
			wc.setAttribute(CDK3Server.PROP_HYPERVISOR, getHypervisor(folder));
			wc.setAttribute(CDK3Server.MINISHIFT_FILE, getMinishiftLoc(runtimeDefinition));
			
			// In this case, we create a new minishift home on-the-fly for this new runtime
			File msHome = new File(folder, "MINISHIFT_HOME");
			msHome.mkdirs();
			wc.setAttribute(CDK3Server.MINISHIFT_HOME, msHome.getAbsolutePath());
		} else {
			// This is a definition based on a minishift home
			String folder = runtimeDefinition.getLocation().getAbsolutePath();
			File cdkFile = new File(folder, CDKConstants.CDK_RESOURCE_CDK);
			Properties props = readProperties(cdkFile);
			String user = props.getProperty(DOT_CDK_SUBSCRIPTION_USERNAME);
			String password = System.getenv(DOT_CDK_SUBSCRIPTION_PASSWORD);
			if (user != null) {
				addToCredentialsModel(CredentialService.REDHAT_ACCESS, user, password);
			}
			wc.setAttribute(CDK3Server.MINISHIFT_HOME, folder);
			wc.setAttribute(CDK3Server.PROP_HYPERVISOR, getHypervisor(folder));
			wc.setAttribute(CDK3Server.MINISHIFT_FILE, getMinishiftLoc(runtimeDefinition));
			wc.setAttribute(CDKServer.PROP_USERNAME, user);
		}

	}
	
	private String getHypervisor(String folder) {
		String[] validHypervisors = CDK3Server.getHypervisors();
		String hyperV = validHypervisors[0];

		File config = new File(folder, CONFIG);
		File configJson = new File(config, CONFIG_JSON);
		if (!configJson.exists() || !configJson.isFile()) {
			return hyperV;
		}

		String path = configJson.getAbsolutePath();
		try {
			String content = new String(Files.readAllBytes(Paths.get(path)));
			ModelNode mn = ModelNode.fromJSONString(content);
			ModelNode o = mn.get(VM_DRIVER);
			String val = (o == null ? null : o.asString());
			if (val != null && Arrays.asList(validHypervisors).contains(val)) {
				return val;
			}
		} catch (IOException e) {
			CDKCoreActivator.pluginLog().logError("Error parsing " + path, e);
		} catch(IllegalArgumentException iae) {
			CDKCoreActivator.pluginLog().logError("Error parsing " + path, iae);
		}
		return hyperV;
	}

	private String getMinishiftLoc(RuntimeDefinition runtimeDefinition) {
		String fromDef = (String) runtimeDefinition.getProperty(OVERRIDE_MINISHIFT_LOCATION);
		if (doesNotExist(fromDef)) {
			return BinaryUtility.MINISHIFT_BINARY.getLocation();
		}
		return fromDef;
	}

	@Override
	public void calculateProblems(RuntimeDefinition def) {
		String override = (String) def.getProperty(OVERRIDE_MINISHIFT_LOCATION);
		String minishiftLoc = BinaryUtility.MINISHIFT_BINARY.getLocation();
		if (doesNotExist(override) && doesNotExist(minishiftLoc)) {
			RuntimeDetectionProblem p = createDetectionProblem("Set minishift binary location.",
					"The minishift binary could not be located on the system path.", IStatus.ERROR,
					MissingMinishiftResolutionProvider.MISSING_MINISHIFT_PROBLEM_ID);
			def.setProblems(new RuntimeDetectionProblem[] { p });
		} else {
			def.setProblems(new RuntimeDetectionProblem[] {});
		}
	}

	private boolean doesNotExist(String s) {
		return s == null || s.isEmpty() || !(new File(s).exists());
	}

	@Override
	protected String[] getRequiredChildren() {
		return new String[] {};
	}

	@Override
	protected String getServerType() {
		return null;
	}
	@Override
	protected String getDefinitionName(File root) {
		return null;
	}

	@Override
	protected String getRuntimeDetectionType() {
		return null;
	}

	@Override
	protected String getDefinitionVersion(File root) {
		return null;
	}
	
}
