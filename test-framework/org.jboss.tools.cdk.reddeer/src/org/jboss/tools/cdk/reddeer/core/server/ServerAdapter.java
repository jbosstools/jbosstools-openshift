/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.core.server;

import java.nio.file.Path;

import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.jboss.tools.cdk.reddeer.core.enums.CDKHypervisor;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * Class representing server adapter object within devstudio
 * @author odockal
 *
 */
public class ServerAdapter {

	private CDKVersion version;
	private Path ocPath;
	private Path minishiftHome;
	private Path installationFolder;
	private Path minishiftBinary;
	private String adapterName;
	private String profile;
	private String hypervisorPropertyKey;
	private String usernamePropertyKey;
	private String passwordPropertyKey;
	
	public ServerAdapter(CDKVersion version, String adapterName) {
		this.version = version;
		this.adapterName = adapterName;
	}
	
	/**
	 * Sets field values representing system property keys for username and password
	 * @param username_property_key
	 * @param password_property_key
	 */
	public void setCredentialsKeys(String usernamePropertyKey, String passwordPropertyKey) {
		this.usernamePropertyKey = usernamePropertyKey;
		this.passwordPropertyKey = passwordPropertyKey;
	}
	
	public void setHypervisor(String property) {
		hypervisorPropertyKey = property;
	}
	
	public String getHypervisor() {
		String prop = CDKUtils.getSystemProperty(hypervisorPropertyKey);
		return StringUtils.isEmptyOrNull(prop) ? CDKHypervisor.getDefaultHypervisor().toString() : prop;		
	}
	
	public String getAdapterName() {
		return adapterName;
	}

	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
	}

	public CDKVersion getVersion() {
		return version;
	}

	public void setVersion(CDKVersion version) {
		this.version = version;
	}

	public Path getOcPath() {
		return ocPath;
	}

	public void setOcPath(Path ocPath) {
		this.ocPath = ocPath;
	}

	public Path getMinishiftBinary() {
		return minishiftBinary;
	}

	public void setMinishiftBinary(Path minishiftBinary) {
		this.minishiftBinary = minishiftBinary;
	}		
	
	/**
	 * Returns constructed CDKServer instance based on {@link #adapterName}
	 * @return CDKServer instance
	 */
	public Server getServer() {
		CDKServersView view = new CDKServersView();
		view.open();
		return view.getServer(adapterName);
	}
	
	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public Path getMinishiftHome() {
		return minishiftHome;
	}

	public void setMinishiftHome(Path minishiftHome) {
		this.minishiftHome = minishiftHome;
	}

	public Path getInstallationFolder() {
		return installationFolder;
	}

	public void setInstallationFolder(Path installationFolder) {
		this.installationFolder = installationFolder;
	}
	
	/**
	 * Returns string value of system property
	 * @return username
	 */
	public String getUsername() {
		if (usernamePropertyKey != null) {
			return System.getProperty(usernamePropertyKey);
		}
		return null;
	}

	/**
	 * Returns string value of system property
	 * @return password
	 */
	public String getPassword() {
		if (passwordPropertyKey != null) {
			return System.getProperty(passwordPropertyKey);
		}
		return null;
	}
}
