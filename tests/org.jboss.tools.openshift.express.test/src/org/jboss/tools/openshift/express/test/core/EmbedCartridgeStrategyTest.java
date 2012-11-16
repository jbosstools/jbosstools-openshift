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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.Session;
import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IApplicationGear;
import com.openshift.client.IApplicationPortForwarding;
import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftSSHOperationException;

/**
 * @author Andre Dietisheim
 */
public class EmbedCartridgeStrategyTest {

	private EmbedCartridgeStrategy embedStrategy;
	private DomainFake domainFake;

	@Before
	public void setUp() throws OpenShiftException {
		this.domainFake = new DomainFake();
		this.embedStrategy = new EmbedCartridgeStrategy(domainFake);
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
		assertTrue(diff.getAdditions().size() == 1);
		assertEquals(IEmbeddableCartridge.MYSQL_51, diff.getAdditions().get(0));
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 0);
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
		assertTrue(diff.getAdditions().size() == 0);
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 0);
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
		assertTrue(diff.getAdditions().size() == 0);
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 1);
		assertTrue(diff.getRemovals().contains(IEmbeddableCartridge.PHPMYADMIN_34));
	}

	@Test
	public void shouldRemovePostgresAndAddMySql() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(IEmbeddableCartridge.POSTGRESQL_84);

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.MYSQL_51, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.MYSQL_51, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertTrue(diff.getAdditions().size() == 0);
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 1);
		assertEquals(IEmbeddableCartridge.POSTGRESQL_84, diff.getRemovals().get(0));
	}

	@Test
	public void shouldRemovePostgresAndAddPhpMyAdminAndMySql() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = new HashSet<IEmbeddableCartridge>();
		currentCartridges.add(IEmbeddableCartridge.POSTGRESQL_84);

		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.PHPMYADMIN_34, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.PHPMYADMIN_34, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertTrue(diff.getAdditions().size() == 1);
		assertEquals(IEmbeddableCartridge.MYSQL_51, diff.getAdditions().get(0));
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 1);
		assertEquals(IEmbeddableCartridge.POSTGRESQL_84, diff.getRemovals().get(0));
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
		assertTrue(diff.getAdditions().size() == 1);
		assertEquals(IEmbeddableCartridge.MONGODB_22, diff.getAdditions().get(0));
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 0);
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
		assertTrue(diff.getAdditions().size() == 0);
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 2);
		assertTrue(diff.getRemovals().contains(IEmbeddableCartridge.ROCKMONGO_11));
		assertTrue(diff.getRemovals().contains(IEmbeddableCartridge._10GEN_MMS_AGENT_01));
	}

	@Test
	public void shouldNotAddJenkinsApplication() throws OpenShiftException{
		// given
		Set<IEmbeddableCartridge> currentCartridges = Collections.<IEmbeddableCartridge>emptySet();
		domainFake.createApplication("adietish", ICartridge.JENKINS_14);
		
		// when
		EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff = embedStrategy.add(IEmbeddableCartridge.JENKINS_14, currentCartridges);

		// then
		assertEquals(IEmbeddableCartridge.JENKINS_14, diff.getCartridge());
		assertNotNull(diff.getAdditions());
		assertTrue(diff.getAdditions().size() == 0);
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 0);
		assertNotNull(diff.getApplicationAdditions());
		assertTrue(diff.getApplicationAdditions().size() == 0);
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
		assertTrue(diff.getAdditions().size() == 0);
		assertNotNull(diff.getRemovals());
		assertTrue(diff.getRemovals().size() == 0);
		assertNotNull(diff.getApplicationAdditions());
		assertTrue(diff.getApplicationAdditions().size() == 1);
		assertTrue(diff.getApplicationAdditions().contains(ICartridge.JENKINS_14));
	}

	private static final class ApplicationFake implements IApplication {

		private ICartridge cartridge;
		private IDomain domain;

		public ApplicationFake(ICartridge cartridge, IDomain domain) {
			this.cartridge = cartridge;
			this.domain = domain;
		}

		@Override
		public String getCreationLog() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasCreationLog() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getUUID() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getGitUrl() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getApplicationUrl() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ApplicationScale getApplicationScale() {
			throw new UnsupportedOperationException();
		}

		@Override
		public IGearProfile getGearProfile() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getHealthCheckUrl() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ICartridge getCartridge() {
			return cartridge;
		}

		@Override
		public IEmbeddedCartridge addEmbeddableCartridge(IEmbeddableCartridge cartridge) 
				throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IEmbeddedCartridge> addEmbeddableCartridges(List<IEmbeddableCartridge> cartridge)
				throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IEmbeddedCartridge> getEmbeddedCartridges() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasEmbeddedCartridge(IEmbeddableCartridge cartridge) 
				throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasEmbeddedCartridge(String cartridgeName) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IEmbeddedCartridge getEmbeddedCartridge(String cartridgeName) 
				throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IEmbeddedCartridge getEmbeddedCartridge(IEmbeddableCartridge cartridge) 
				throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IApplicationGear> getGears() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Date getCreationTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void destroy() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void start() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void restart() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void stop() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void stop(boolean force) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean waitForAccessible(long timeout) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IDomain getDomain() {
			return domain;
		}

		@Override
		public void exposePort() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void concealPort() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void showPort() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void scaleDown() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void scaleUp() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addAlias(String string) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> getAliases() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasAlias(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeAlias(String alias) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void refresh() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasSSHSession() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isPortFowardingStarted() throws OpenShiftSSHOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IApplicationPortForwarding> getForwardablePorts() throws OpenShiftSSHOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IApplicationPortForwarding> startPortForwarding() throws OpenShiftSSHOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IApplicationPortForwarding> stopPortForwarding() throws OpenShiftSSHOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IApplicationPortForwarding> refreshForwardablePorts() throws OpenShiftSSHOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> getEnvironmentProperties() throws OpenShiftSSHOperationException {
			throw new UnsupportedOperationException();
		}

		public void setSSHSession(Session session) {
			throw new UnsupportedOperationException();
		}

		public Session getSSHSession() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Future<Boolean> waitForAccessibleAsync(long timeout) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}
	}

	private static final class DomainFake implements IDomain {

		private List<IApplication> applications;

		public DomainFake() {
			this.applications = new ArrayList<IApplication>();
		}

		@Override
		public String getCreationLog() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasCreationLog() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void refresh() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getSuffix() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void rename(String id) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IUser getUser() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void destroy() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void destroy(boolean force) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean waitForAccessible(long timeout) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IApplication createApplication(String name, ICartridge cartridge, ApplicationScale scale,
				IGearProfile gearProfile) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IApplication createApplication(String name, ICartridge cartridge, ApplicationScale scale)
				throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IApplication createApplication(String name, ICartridge cartridge, IGearProfile gearProfile)
				throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IApplication createApplication(String name, ICartridge cartridge) {
			IApplication application = new ApplicationFake(cartridge, this);
			applications.add(application);
			return application;
		}

		@Override
		public List<IApplication> getApplications() throws OpenShiftException {
			return applications;
		}

		@Override
		public List<String> getAvailableCartridgeNames() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IApplication getApplicationByName(String name) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasApplicationByName(String name) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IApplication> getApplicationsByCartridge(ICartridge cartridge) throws OpenShiftException {
			List<IApplication> matchingApplications = new ArrayList<IApplication>();
			for (IApplication application : applications) {
				if (cartridge.equals(application.getCartridge())) {
					matchingApplications.add(application);
					break;
				}
			}
			return matchingApplications;
		}

		@Override
		public boolean hasApplicationByCartridge(ICartridge cartridge) throws OpenShiftException {
			return getApplicationsByCartridge(cartridge).size() > 0;
		}

		@Override
		public List<IGearProfile> getAvailableGearProfiles() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}
		
	}
}
