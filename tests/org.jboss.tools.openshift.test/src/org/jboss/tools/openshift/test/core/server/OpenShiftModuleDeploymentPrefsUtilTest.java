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
package org.jboss.tools.openshift.test.core.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModule2;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.internal.Module;
import org.jboss.ide.eclipse.as.core.util.IWTPConstants;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.behavior.eap.OpenShiftEapDeployPathController.OpenShiftModuleDeploymentPrefsUtil;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.Silent.class)
public class OpenShiftModuleDeploymentPrefsUtilTest extends TestCase {
	private IServer server;

	@Before
	public void before() throws Exception {
		this.server = OpenShiftServerTestUtils.createOpenshift3Server("example",
				OpenShiftServerBehaviour.PROFILE_OPENSHIFT3);
	}
	
	@Test
	public void shouldUseModuleNameAsOutputNameIfNoDeployName() {
		// given web module with no deploy name
		IModule module = createModuleMock("webProject", null, () -> getWebModuleType());
		OpenShiftModuleDeploymentPrefsUtil2 module1Util = getUtilForModule(module);
		// when
		String outputName = module1Util.getOutputNameFromSettings2(server, module);
		// then output name == module name
		assertThat(outputName).isEqualTo("webProject.war");		
	}

	@Test
	public void shouldUseDeployNameAsOutputNameIfPresent() {
		// given web module with deploy name
		IModule module = createModuleMock("webProject", "warName", () -> getWebModuleType());
		OpenShiftModuleDeploymentPrefsUtil2 module1Util = getUtilForModule(module);
		// when
		String outputName = module1Util.getOutputNameFromSettings2(server, module);
		// then output name == deploy name
		assertThat(outputName).isEqualTo("warName.war");		
	}

	@Test
	public void shouldUseModuleNameAsOutputNameForEJBModuleIfNoDeployName() throws Exception {
		// given ejb module with deploy name
		IModule module = createModuleMock("ejbProject", null, () -> getEjbModuleType());
		OpenShiftModuleDeploymentPrefsUtil2 module1Util = getUtilForModule(module);
		// when
		String outputName = module1Util.getOutputNameFromSettings2(server, module);
		// then output name == module name + extension 
		assertThat(outputName).startsWith("ejbProject");
		assertThat(outputName.length()).isGreaterThan("ejbProject".length());
	}

	@SuppressWarnings("restriction")
	private IModule createModuleMock(String moduleName, String deployName, Supplier<IModuleType> moduleTypeSupplier) {
		Module module = mock(Module.class);
		when(module.getName()).thenReturn(moduleName);
		when(module.getModuleType()).thenReturn(moduleTypeSupplier.get());
		when(module.getProperty(eq(IModule2.PROP_DEPLOY_NAME))).thenReturn(deployName);
		return module;
	}

	private IModuleType getWebModuleType() {
		return getModuleType("2.5", IWTPConstants.FACET_WEB);
	}

	private IModuleType getEjbModuleType() {
		return getModuleType("3.0", "jst.ejb");
	}

	private IModuleType getModuleType(final String version, final String id) {
		return new IModuleType() {
			@Override
			public String getVersion() {
				return version;
			}

			@Override
			public String getName() {
				return id;
			}

			@Override
			public String getId() {
				return id;
			}
		};
	}

	// Create a utility that 'finds' the module for the project listed on the server
	private OpenShiftModuleDeploymentPrefsUtil2 getUtilForModule(final IModule m) {
		return new OpenShiftModuleDeploymentPrefsUtil2() {
			@Override
			protected IModule findProjectModule(IServerAttributes server) {
				return m;
			}
		};
	}

	// Just expose the getOutputNameFromSettings method 
	private static class OpenShiftModuleDeploymentPrefsUtil2 extends OpenShiftModuleDeploymentPrefsUtil {
		public String getOutputNameFromSettings2(IServerAttributes server, IModule module) {
			return super.getOutputNameFromSettings(server, module);
		}
	}

	@Override
	@After
	public void tearDown() {
		OpenShiftServerTestUtils.cleanup();
	}
}
