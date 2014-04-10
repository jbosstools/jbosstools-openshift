/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.core;

import java.util.List;
import java.util.Map;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.IUser;
import com.openshift.client.Messages;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public class NoopDomainFake implements IDomain {

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
	public IApplication createApplication(String name, IStandaloneCartridge cartridge, ApplicationScale scale,
			IGearProfile gearProfile) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IApplication createApplication(String name, IStandaloneCartridge cartridge, ApplicationScale scale)
			throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IApplication createApplication(String name, IStandaloneCartridge cartridge, IGearProfile gearProfile)
			throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IApplication createApplication(String name, IStandaloneCartridge cartridge) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IApplication> getApplications() throws OpenShiftException {
		throw new UnsupportedOperationException();
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
	public List<IApplication> getApplicationsByCartridge(IStandaloneCartridge cartridge) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasApplicationByCartridge(IStandaloneCartridge cartridge) throws OpenShiftException {
		return getApplicationsByCartridge(cartridge).size() > 0;
	}

	@Override
	public List<IGearProfile> getAvailableGearProfiles() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Messages getMessages() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IApplication createApplication(String name, IStandaloneCartridge cartridge, String initialGitUrl)
			throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IApplication createApplication(String name, IStandaloneCartridge cartridge, ApplicationScale scale,
			IGearProfile gearProfile, String initialGitUrl) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IApplication createApplication(String name, IStandaloneCartridge standaloneCartridge, ApplicationScale scale,
			IGearProfile profile, String initialGitUrl, int timeout, IEmbeddableCartridge... embeddableCartridges) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IApplication createApplication(String arg0, IStandaloneCartridge arg1, ApplicationScale arg2,
			IGearProfile arg3, String arg4, int arg5, Map<String, String> arg6, IEmbeddableCartridge... arg7)
			throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IApplication createApplication(String name, ApplicationScale scale, IGearProfile gearProfile,
			String initialGitUrl, int timeout, Map<String, String> environmentVariable, ICartridge... cartridges)
			throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canCreateApplicationWithEnvironmentVariables() {
		throw new UnsupportedOperationException();
	}
}