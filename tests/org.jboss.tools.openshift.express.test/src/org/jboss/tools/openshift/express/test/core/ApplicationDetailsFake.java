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
package org.jboss.tools.openshift.express.test.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IDeployedStandaloneCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.internal.client.StandaloneCartridgeResource;

/**
 * @author Jeff Cantrill
 */
@SuppressWarnings({ "deprecation", "restriction" })
public class ApplicationDetailsFake extends NoopApplicationFake {

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
	public String getApplicationUrl() {
		return "http://nowhere.appdomain.com";
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return ApplicationScale.SCALE;
	}

	@Override
	public Date getCreationTime() {
		return new Date();
	}

	@Override
	public IGearProfile getGearProfile() {
		return null;
	}

	@Override
	public IDeployedStandaloneCartridge getCartridge() {
		return new StandaloneCartridgeResource(
				"mockApplicationName", "mockApplicationName","mockApplicationName", null, null, false, null, null, null, null) {};
		}

	@Override
	public List<IEmbeddedCartridge> getEmbeddedCartridges() throws OpenShiftException {
		return new ArrayList<IEmbeddedCartridge>();
	}
}
