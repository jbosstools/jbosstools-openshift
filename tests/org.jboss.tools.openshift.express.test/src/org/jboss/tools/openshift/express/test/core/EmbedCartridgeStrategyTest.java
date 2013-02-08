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

import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class EmbedCartridgeStrategyTest {

	
	
	private EmbedCartridgeStrategy embedStrategy;

	@Before
	public void setUp() throws OpenShiftException {
		createEmbeddableCartridgeStrategy();
	}

	protected void createEmbeddableCartridgeStrategy(ICartridge... cartridges) {
		List<IEmbeddableCartridge> allEmbeddableCartridgeList = createCartridgesList(
				IEmbeddableCartridge.MYSQL_51, 
				IEmbeddableCartridge.PHPMYADMIN_34,
				IEmbeddableCartridge.POSTGRESQL_84, 
				IEmbeddableCartridge.ROCKMONGO_11, 
				IEmbeddableCartridge.MONGODB_22, 
				IEmbeddableCartridge.JENKINS_14,
				IEmbeddableCartridge._10GEN_MMS_AGENT_01);
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
		currentCartridges.add(IEmbeddableCartridge.MYSQL_51);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.MYSQL_51, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.MYSQL_51, diff.getCartridge());
		assertFalse(diff.hasAdditions());
		assertFalse(diff.hasRemovals());
		assertFalse(diff.hasApplicationAdditions());
	}

	@Test
	public void shouldAddMySql() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.MYSQL_51, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.MYSQL_51, diff.getCartridge());
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
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.PHPMYADMIN_34, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.PHPMYADMIN_34, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(1, diff.getAdditions().size());
		assertEquals(IEmbeddableCartridge.MYSQL_51, diff.getAdditions().get(0));
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
	}

	@Test
	public void shouldRemovePhpMyAdmin() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(IEmbeddableCartridge.MYSQL_51);
		currentCartridges.add(IEmbeddableCartridge.PHPMYADMIN_34);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.remove(IEmbeddableCartridge.PHPMYADMIN_34, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.PHPMYADMIN_34, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
	}

	@Test
	public void shouldRemovePhpMyAdminAndMySql() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(IEmbeddableCartridge.MYSQL_51);
		currentCartridges.add(IEmbeddableCartridge.PHPMYADMIN_34);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.remove(IEmbeddableCartridge.MYSQL_51, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.MYSQL_51, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(1, diff.getRemovals().size());
		assertTrue(diff.getRemovals().contains(IEmbeddableCartridge.PHPMYADMIN_34));
	}

	@Test
	public void shouldAddRockmongoAndMongoDb() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.ROCKMONGO_11, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.ROCKMONGO_11, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(1, diff.getAdditions().size());
		assertEquals(IEmbeddableCartridge.MONGODB_22, diff.getAdditions().get(0));
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
	}

	@Test
	public void shouldRemoveRockmongoAnd10genAndMongoDb() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(IEmbeddableCartridge.MONGODB_22);
		currentCartridges.add(IEmbeddableCartridge.ROCKMONGO_11);
		currentCartridges.add(IEmbeddableCartridge._10GEN_MMS_AGENT_01);

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.remove(IEmbeddableCartridge.MONGODB_22, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.MONGODB_22, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(2, diff.getRemovals().size());
		assertTrue(diff.getRemovals().contains(IEmbeddableCartridge.ROCKMONGO_11));
		assertTrue(diff.getRemovals().contains(IEmbeddableCartridge._10GEN_MMS_AGENT_01));
	}

	@Test
	public void shouldNotAddJenkinsApplication() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();
		createEmbeddableCartridgeStrategy(ICartridge.JENKINS_14);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.JENKINS_14, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.JENKINS_14, diff.getCartridge());
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
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.JENKINS_14, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.JENKINS_14, diff.getCartridge());
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
