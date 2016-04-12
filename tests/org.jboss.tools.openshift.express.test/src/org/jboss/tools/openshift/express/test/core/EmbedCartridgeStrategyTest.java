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

import org.jboss.tools.openshift.express.internal.core.cartridges.EmbedCartridgeStrategy;
import org.junit.Before;
import org.junit.Test;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.EmbeddableCartridge;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IDeployedStandaloneCartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.client.cartridge.StandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public class EmbedCartridgeStrategyTest {

	private static final ICartridge JENKINS_14 =
			new StandaloneCartridge(IStandaloneCartridge.NAME_JENKINS + ICartridge.NAME_VERSION_DELIMITER + "14");
	private static final ICartridge JBOSSAS_7 =
			new StandaloneCartridge(IStandaloneCartridge.NAME_JBOSSAS + ICartridge.NAME_VERSION_DELIMITER + "7");

	private static final ICartridge CARTRIDGE_MYSQL =
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_MYSQL + ICartridge.NAME_VERSION_DELIMITER +"51");
	private static final ICartridge CARTRIDGE_PHPMYADMIN =
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_PHPMYADMIN + ICartridge.NAME_VERSION_DELIMITER +"34");
	private static final ICartridge CARTRIDGE_POSTGRESQL =
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_POSTGRESQL + ICartridge.NAME_VERSION_DELIMITER +"84");
	private static final ICartridge CARTRIDGE_ROCKMONGO =
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_ROCKMONGO + ICartridge.NAME_VERSION_DELIMITER +"11");
	private static final ICartridge CARTRIDGE_MONGODB =
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_MONGODB + ICartridge.NAME_VERSION_DELIMITER +"22");
	private static final ICartridge CARTRIDGE_JENKINS_CLIENT =
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_JENKINS_CLIENT + ICartridge.NAME_VERSION_DELIMITER +"14");
	private static final ICartridge CARTRIDGE_10GEN_MMS_AGENT =
			new EmbeddableCartridge(IEmbeddableCartridge.NAME_10GEN_MMS_AGENT + ICartridge.NAME_VERSION_DELIMITER + "01");

	private EmbedCartridgeStrategy embedStrategy;

	@Before
	public void setUp() throws OpenShiftException {
		createEmbeddableCartridgeStrategy();
	}

	protected void createEmbeddableCartridgeStrategy(ICartridge... cartridges) {
		List<ICartridge> allEmbeddableCartridgeList = createCartridgesList(
				CARTRIDGE_MYSQL,
				CARTRIDGE_PHPMYADMIN,
				CARTRIDGE_POSTGRESQL,
				CARTRIDGE_ROCKMONGO,
				CARTRIDGE_MONGODB,
				CARTRIDGE_JENKINS_CLIENT,
				CARTRIDGE_10GEN_MMS_AGENT);
		List<ICartridge> allCartridgeList = createCartridgesList(
				JBOSSAS_7,
				JENKINS_14);
		List<IApplication> allApplications = createAllApplicationsList(new NoopDomainFake(), cartridges);
		this.embedStrategy = new EmbedCartridgeStrategy(allEmbeddableCartridgeList, allCartridgeList, allApplications);
	}

	private List<IApplication> createAllApplicationsList(IDomain domain, ICartridge... cartridges) {
		List<IApplication> applications = new ArrayList<>();
		if (cartridges != null) {
			for (ICartridge cartridge : cartridges) {
				applications.add(new ApplicationFake((IStandaloneCartridge) cartridge));
			}
		}
		return applications;
	}

	private <C> List<C> createCartridgesList(C... cartridges) {
		List<C> embeddableCartridges = new ArrayList<>();
		if (cartridges != null) {
			for (C cartridge : cartridges) {
				embeddableCartridges.add(cartridge);
			}
		}
		return embeddableCartridges;
	}

	@Test
	public void shouldNotAddMySql() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = new HashSet<>();
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
	public void shouldAddMySql() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = Collections.<ICartridge> emptySet();

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
	public void shouldAddPhpMyAdminAndMySql() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = Collections.<ICartridge> emptySet();

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy
				.add(CARTRIDGE_PHPMYADMIN, currentCartridges);

		// then
		assertEquals(CARTRIDGE_PHPMYADMIN, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(1, diff.getAdditions().size());
		assertEquals(CARTRIDGE_MYSQL, diff.getAdditions().get(0));
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
	}

	@Test
	public void shouldRemovePhpMyAdmin() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = new HashSet<>();
		currentCartridges.add(CARTRIDGE_MYSQL);
		currentCartridges.add(CARTRIDGE_PHPMYADMIN);

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.remove(CARTRIDGE_PHPMYADMIN,
				currentCartridges);

		// then
		assertEquals(CARTRIDGE_PHPMYADMIN, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
	}

	@Test
	public void shouldRemovePhpMyAdminAndMySql() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = new HashSet<>();
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
	public void shouldAddRockmongoAndMongoDb() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = Collections.<ICartridge> emptySet();

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
	public void shouldRemoveRockmongoAnd10genAndMongoDb() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = new HashSet<>();
		currentCartridges.add(CARTRIDGE_MONGODB);
		currentCartridges.add(CARTRIDGE_ROCKMONGO);
		currentCartridges.add(CARTRIDGE_10GEN_MMS_AGENT);

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy
				.remove(CARTRIDGE_MONGODB, currentCartridges);

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
	public void shouldNotAddJenkinsApplication() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = Collections.<ICartridge> emptySet();
		createEmbeddableCartridgeStrategy(JENKINS_14);

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(CARTRIDGE_JENKINS_CLIENT,
				currentCartridges);

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
	public void shouldAddJenkinsApplication() throws OpenShiftException {
		// given
		Set<ICartridge> currentCartridges = Collections.<ICartridge> emptySet();

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(CARTRIDGE_JENKINS_CLIENT,
				currentCartridges);

		// then
		assertEquals(CARTRIDGE_JENKINS_CLIENT, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertEquals(0, diff.getAdditions().size());
		assertNotNull(diff.getRemovals());
		assertEquals(0, diff.getRemovals().size());
		assertNotNull(diff.getApplicationAdditions());
		assertEquals(1, diff.getApplicationAdditions().size());
		assertTrue(diff.getApplicationAdditions().contains(JENKINS_14));
	}

	private static final class ApplicationFake extends NoopApplicationFake {

		private IDeployedStandaloneCartridge cartridge;

		private ApplicationFake(IStandaloneCartridge cartridge) {
			this.cartridge = new StandaloneCartridgeResourceFake(cartridge);
		}

		@Override
		public IDeployedStandaloneCartridge getCartridge() {
			return cartridge;
		}
	}
}
