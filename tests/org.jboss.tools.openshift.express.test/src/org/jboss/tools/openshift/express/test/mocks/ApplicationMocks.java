/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.mocks;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.jcraft.jsch.Session;
import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IApplicationPortForwarding;
import com.openshift.client.IDomain;
import com.openshift.client.IEnvironmentVariable;
import com.openshift.client.IGearGroup;
import com.openshift.client.IGearProfile;
import com.openshift.client.Messages;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftSSHOperationException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.internal.client.CartridgeType;

@SuppressWarnings({ "deprecation", "restriction" })
public class ApplicationMocks {

	//this is here until Mockito can be added to the target platform
	public static IApplication givenAnApplication(){
		return new IApplication(){

			@Override
			public boolean hasCreationLog() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public String getCreationLog() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Messages getMessages() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getName() {
				return "appName";
			}

			@Override
			public String getUUID() {
				return "appuuid";
			}

			@Override
			public String getGitUrl() {
				return "git://username@githost.com/project.git";
			}

			@Override
			public String getSshUrl() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getInitialGitUrl() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getDeploymentType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getApplicationUrl() {
				return "http://nowhere.appdomain.com";
			}

			@Override
			public ApplicationScale getApplicationScale() {
				return ApplicationScale.SCALE;
			}

			@Override
			public IGearProfile getGearProfile() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IStandaloneCartridge getCartridge() {
				return new IStandaloneCartridge() {
					
					@Override
					public boolean isDownloadable() {
						// TODO Auto-generated method stub
						return false;
					}
					
					@Override
					public URL getUrl() {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public CartridgeType getType() {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public String getName() {
						return "mockApplicationName";
					}
					
					@Override
					public String getDisplayName() {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public String getDescription() {
						// TODO Auto-generated method stub
						return null;
					}
				};
			}

			@Override
			public IEmbeddedCartridge addEmbeddableCartridge(ICartridge cartridge) throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<IEmbeddedCartridge> addEmbeddableCartridges(ICartridge... cartridges) throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<IEmbeddedCartridge> addEmbeddableCartridges(Collection<IEmbeddableCartridge> cartridge)
					throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<IEmbeddedCartridge> getEmbeddedCartridges() throws OpenShiftException {
				return new ArrayList<IEmbeddedCartridge>();
			}

			@Override
			public boolean hasEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean hasEmbeddedCartridge(String cartridgeName) throws OpenShiftException {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public IEmbeddedCartridge getEmbeddedCartridge(String cartridgeName) throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IEmbeddedCartridge getEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void removeEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void removeEmbeddedCartridges(Collection<IEmbeddableCartridge> cartridges) throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Collection<IGearGroup> getGearGroups() throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Date getCreationTime() {
				return new Date();
			}

			@Override
			public void destroy() throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void start() throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void restart() throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void stop() throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void stop(boolean force) throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean waitForAccessible(long timeout) throws OpenShiftException {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Future<Boolean> waitForAccessibleAsync(long timeout) throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IDomain getDomain() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void scaleDown() throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void scaleUp() throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public String setDeploymentType(String deploymentType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void addAlias(String string) throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public List<String> getAliases() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean hasAlias(String name) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void removeAlias(String alias) throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void refresh() throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setSSHSession(Session session) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Session getSSHSession() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean hasSSHSession() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isPortFowardingStarted() throws OpenShiftSSHOperationException {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public List<IApplicationPortForwarding> getForwardablePorts() throws OpenShiftSSHOperationException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<IApplicationPortForwarding> startPortForwarding() throws OpenShiftSSHOperationException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<IApplicationPortForwarding> stopPortForwarding() throws OpenShiftSSHOperationException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<IApplicationPortForwarding> refreshForwardablePorts() throws OpenShiftSSHOperationException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<String> getEnvironmentProperties() throws OpenShiftSSHOperationException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, IEnvironmentVariable> getEnvironmentVariables() throws OpenShiftSSHOperationException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean hasEnvironmentVariable(String name) throws OpenShiftException {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public IEnvironmentVariable addEnvironmentVariable(String name, String value) throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IEnvironmentVariable updateEnvironmentVariable(String name, String value) throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, IEnvironmentVariable> addEnvironmentVariables(Map<String, String> environmentVariables)
					throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IEnvironmentVariable getEnvironmentVariable(String name) throws OpenShiftException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getEnvironmentVariableValue(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void removeEnvironmentVariable(String name) throws OpenShiftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void removeEnvironmentVariable(IEnvironmentVariable environmentVariable) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean canGetEnvironmentVariables() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean canUpdateEnvironmentVariables() {
				// TODO Auto-generated method stub
				return false;
			}
			
		};
	}

}
