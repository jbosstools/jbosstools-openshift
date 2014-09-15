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

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.internal.client.CartridgeType;

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
	public IStandaloneCartridge getCartridge() {
		return new IStandaloneCartridge() {

			@Override
			public boolean isDownloadable() {
				return false;
			}

			@Override
			public URL getUrl() {
				return null;
			}

			@Override
			public CartridgeType getType() {
				return null;
			}

			@Override
			public String getName() {
				return "mockApplicationName";
			}

			@Override
			public String getDisplayName() {
				return null;
			}

			@Override
			public String getDescription() {
				return null;
			}
		};
	}

	@Override
	public List<IEmbeddedCartridge> getEmbeddedCartridges() throws OpenShiftException {
		return new ArrayList<IEmbeddedCartridge>();
	}
}
