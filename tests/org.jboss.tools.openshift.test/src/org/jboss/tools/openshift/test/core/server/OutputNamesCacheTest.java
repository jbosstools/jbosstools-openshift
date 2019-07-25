/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModule2;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.ProjectProperties;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.jboss.ide.eclipse.as.core.util.IWTPConstants;
import org.jboss.tools.openshift.core.server.OutputNamesCacheFactory;
import org.jboss.tools.openshift.core.server.OutputNamesCacheFactory.OutputNamesCache;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.jboss.tools.test.util.ResourcesUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OutputNamesCacheTest {

	private OutputNamesCacheFactory factory;
	private HashMap<String, OutputNamesCache> map;
	private IServer server;
	private IModule2 module1;
	private IModule2 module2;
	private IModule2 module3;
	private IModule2 danglingModule;
	private IServer serverWithoutModules;
	
	@Before
	public void before() throws CoreException, IOException {
		this.map = spy(new HashMap<String, OutputNamesCache>());
		this.factory = new OutputNamesCacheFactory(map) {};

		this.server = spy(OpenShiftServerTestUtils.createOpenshift3Server("42", "smurf", null));
		this.module1 = mockModule("smurfette", "flower");
		this.module2 = mockModule("papa smurf", "blue hat");
		this.module3 = mockModule("clumsy", "white hat");
		this.danglingModule = mockModule("gargamel", "no hat");
		doReturn(new IModule[] { module1, module2, module3 }).when(server).getModules();

		this.serverWithoutModules = OpenShiftServerTestUtils.createOpenshift3Server("84", "smurfette", null);
	}
	
	public void shouldReturnSameInstanceForSameServer() {
		// given
		OutputNamesCache cache1 = factory.get(server);
		// when
		OutputNamesCache cache2 = factory.get(server);
		// then
		assertThat(cache2).isSameAs(cache1);
	}

	@Test
	public void shouldReturnDifferentCacheForDifferentServers() throws UnsupportedEncodingException, MalformedURLException, CoreException {
		// given
		IServer server2 = OpenShiftServerTestUtils.createOpenshift3Server("84", "papa smurf");
		// when
		OutputNamesCache cache1 = factory.get(server);
		OutputNamesCache cache2 = factory.get(server2);
		// then
		assertThat(cache2).isNotEqualTo(cache1);
	}

	@Test
	public void shouldRemoveCacheWhenServerIsDeleted() throws CoreException {
		// given
		OutputNamesCache cache = factory.get(serverWithoutModules);
		assertThat(map).containsValue(cache);
		// when
		serverWithoutModules.delete();
		// then
		assertThat(map).doesNotContainValue(cache);
	}
	
	@Test
	public void collectShouldStoreAllModuleOutputNames() {
		// given
		TestableOutputNamesCache cache = new TestableOutputNamesCache(server) {};
		// when
		cache.collect();
		// then
		assertThat(cache.getOldOutputNames()).containsValues(
				cache.getOutputName(module1), 
				cache.getOutputName(module2), 
				cache.getOutputName(module3));
		assertThat(cache.getOldOutputNames()).doesNotContainValue( 
				cache.getOutputName(danglingModule));
	}

	@Test
	public void onModifiedshouldStoreAllModuleOutputNames() throws CoreException {
		// given
		TestableOutputNamesCache cache = new TestableOutputNamesCache(server) {};
		cache.collect();
		doReturn("update").when(module2).getProperty(IModule2.PROP_DEPLOY_NAME);
		// when
		cache.onModified(server);
		// then
		assertThat(cache.getNewOutputNames()).containsValues(
				cache.getOutputName(module1), 
				cache.getOutputName(module2), 
				cache.getOutputName(module3));
	}

	@Test
	public void updatedModuleShouldBeModified() throws CoreException {
		// given
		TestableOutputNamesCache cache = new TestableOutputNamesCache(server) {};
		cache.collect();
		doReturn("update").when(module2).getProperty(IModule2.PROP_DEPLOY_NAME);
		cache.onModified(server);
		// when
		boolean module1Modified = cache.isModified(module1);
		boolean module2Modified = cache.isModified(module2);
		boolean module3Modified = cache.isModified(module3);
		// then
		assertThat(module1Modified).isFalse();
		assertThat(module2Modified).isTrue();
		assertThat(module3Modified).isFalse();
	}

	@Test
	public void resetShouldResetModification() throws CoreException {
		// given
		TestableOutputNamesCache cache = new TestableOutputNamesCache(server) {};
		cache.collect();
		doReturn("update").when(module2).getProperty(IModule2.PROP_DEPLOY_NAME);
		cache.onModified(server);
		assertThat(cache.isModified(module2)).isTrue();
		// when
		cache.reset(module2);
		// then
		assertThat(cache.isModified(module2)).isFalse();
	}

	private IModule2 mockModule(String id, String outputName) {
		IModule2 module = mock(IModule2.class);
		doReturn(id).when(module).getId();
		doReturn(outputName).when(module).getProperty(IModule2.PROP_DEPLOY_NAME);
		IModuleType type = mockModuleType();
		doReturn(type).when(module).getModuleType();
		return module;
	}

	private IModuleType mockModuleType() {
		IModuleType type = mock(IModuleType.class);
		doReturn(IWTPConstants.EXT_WAR).when(type).getId();
		return type;
	}

	public static class TestableOutputNamesCache extends OutputNamesCache {

		protected TestableOutputNamesCache(IServer server) {
			super(server);
		} 

		public Map<String, String> getOldOutputNames() {
			return oldOutputNames;
		}
	
		public Map<String, String> getNewOutputNames() {
			return newOutputNames;
		}

		public String getOutputName(IModule module) {
			return getOutputName(module, this.server);
		}
	}
}
