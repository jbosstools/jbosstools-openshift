/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy;
import org.junit.Before;
import org.junit.Test;

import com.openshift.client.EmbeddableCartridge;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class EmbedCartridgeStrategyTest {

	private static final IEmbeddableCartridge CARTRIDGE_MYSQL = 
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_MYSQL, "51");
	private static final IEmbeddableCartridge CARTRIDGE_PHPMYADMIN = 
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_PHPMYADMIN, "34");
	private static final IEmbeddableCartridge CARTRIDGE_POSTGRESQL = 
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_POSTGRESQL, "84");
	private static final IEmbeddableCartridge CARTRIDGE_ROCKMONGO = 
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_ROCKMONGO, "11");
	private static final IEmbeddableCartridge CARTRIDGE_MONGODB = 
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_MONGODB, "22");
	private static final IEmbeddableCartridge CARTRIDGE_JENKINS_CLIENT = 
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_JENKINS_CLIENT, "14");
	private static final IEmbeddableCartridge CARTRIDGE_10GEN_MMS_AGENT = 
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_10GEN_MMS_AGENT, "01");
	
	private EmbedCartridgeStrategy embedStrategy;

	@Before
	public void setUp() throws OpenShiftException {
		createEmbeddableCartridgeStrategy();
	}

	protected void createEmbeddableCartridgeStrategy(ICartridge... cartridges) {
		List<IEmbeddableCartridge> allEmbeddableCartridgeList = createCartridgesList(
				CARTRIDGE_MYSQL,
				CARTRIDGE_PHPMYADMIN,
				CARTRIDGE_POSTGRESQL,
				CARTRIDGE_ROCKMONGO,
				CARTRIDGE_MONGODB,
				CARTRIDGE_JENKINS_CLIENT,
				CARTRIDGE_10GEN_MMS_AGENT);
		List<ICartridge> allCartridgeList = createCartridgesList(
				ICartridge.JBOSSAS_7, 
				ICartridge.JENKINS_14);
		List<IApplication> allApplications = createAllApplicationsList(new NoopDomainFake(), cartridges);
		this.embedStrategy = new EmbedCartridgeStrategy(allEmbeddableCartridgeList, allCartridgeList, allApplications);
	}

	private List<IApplication> createAllApplicationsList(IDomain domain, ICartridge... cartridges) {
		List<IApplication> applications = new ArrayList<IApplication>();
		if (cartridges != null) {
			for(ICartridge cartridge : cartridges) {
				applications.add(new ApplicationFake(cartridge));
			}
		}
		return applications;
	}

	private <C> List<C> createCartridgesList(C... cartridges) {
		List<C> embeddableCartridges = new ArrayList<C>();
		if (cartridges != null) {
			for(C cartridge : cartridges) {
				embeddableCartridges.add(cartridge);
			}
		}
		return embeddableCartridges;
	}

	@Test
	public void shouldNotAddMySql() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(CARTRIDGE_MYSQL);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(CARTRIDGE_MYSQL, currentCartridges);

		// then
		assertEquals(CARTRIDGE_MYSQL, diff.getCartridge());
		assertFalse(diff.hasAdditions());
		assertFalse(diff.hasRemovals());
		assertFalse(diff.hasApplicationAdditions());
	}

	@Test
	public void shouldAddMySql() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(CARTRIDGE_MYSQL, currentCartridges);

		// then
		assertEquals(CARTRIDGE_MYSQL, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertTrue(diff.getAdditions().size() == 0);
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 0);
	}

	@Test
	public void shouldAddPhpMyAdminAndMySql() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(CARTRIDGE_PHPMYADMIN, currentCartridges);

		// then
		assertEquals(CARTRIDGE_PHPMYADMIN, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(1, diff.getAdditions().size());
		assertEquals(CARTRIDGE_MYSQL, diff.getAdditions().get(0));
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
	}

	@Test
	public void shouldRemovePhpMyAdmin() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(CARTRIDGE_MYSQL);
		currentCartridges.add(CARTRIDGE_PHPMYADMIN);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.remove(CARTRIDGE_PHPMYADMIN, currentCartridges);

		// then
		assertEquals(CARTRIDGE_PHPMYADMIN, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
	}

	@Test
	public void shouldRemovePhpMyAdminAndMySql() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(CARTRIDGE_MYSQL);
		currentCartridges.add(CARTRIDGE_PHPMYADMIN);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.remove(CARTRIDGE_MYSQL, currentCartridges);

		// then
		assertEquals(CARTRIDGE_MYSQL, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(1, diff.getRemovals().size());
		assertTrue(diff.getRemovals().contains(CARTRIDGE_PHPMYADMIN));
	}

	@Test
	public void shouldAddRockmongoAndMongoDb() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(CARTRIDGE_ROCKMONGO, currentCartridges);

		// then
		assertEquals(CARTRIDGE_ROCKMONGO, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(1, diff.getAdditions().size());
		assertEquals(CARTRIDGE_MONGODB, diff.getAdditions().get(0));
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
	}

	@Test
	public void shouldRemoveRockmongoAnd10genAndMongoDb() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(CARTRIDGE_MONGODB);
		currentCartridges.add(CARTRIDGE_ROCKMONGO);
		currentCartridges.add(CARTRIDGE_10GEN_MMS_AGENT);

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.remove(CARTRIDGE_MONGODB, currentCartridges);

		// then
		assertEquals(CARTRIDGE_MONGODB, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(2, diff.getRemovals().size());
		assertTrue(diff.getRemovals().contains(CARTRIDGE_ROCKMONGO));
		assertTrue(diff.getRemovals().contains(CARTRIDGE_10GEN_MMS_AGENT));
	}

	@Test
	public void shouldNotAddJenkinsApplication() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();
		createEmbeddableCartridgeStrategy(ICartridge.JENKINS_14);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(CARTRIDGE_JENKINS_CLIENT, currentCartridges);

		// then
		assertEquals(CARTRIDGE_JENKINS_CLIENT, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
		assertNotNull(diff.getApplicationAdditions());
		assertEquals(0, diff.getApplicationAdditions().size());
	}
	
	@Test
	public void shouldAddJenkinsApplication() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(CARTRIDGE_JENKINS_CLIENT, currentCartridges);

		// then
		assertEquals(CARTRIDGE_JENKINS_CLIENT, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
		assertNotNull(diff.getApplicationAdditions());
		assertEquals(1, diff.getApplicationAdditions().size());
		assertTrue(diff.getApplicationAdditions().contains(ICartridge.JENKINS_14));
	}

	private static final class ApplicationFake extends NoopApplicationFake {

		private ICartridge cartridge;

		public ApplicationFake(ICartridge cartridge) {
			this.cartridge = cartridge;
		}

		@Override
		public ICartridge getCartridge() {
			return cartridge;
		}
	}
}
