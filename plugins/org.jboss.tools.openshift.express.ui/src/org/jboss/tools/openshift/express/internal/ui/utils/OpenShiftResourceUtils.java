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
package org.jboss.tools.openshift.express.internal.ui.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftResourceUtils {

	public static String toString(Object object) {
		if (object instanceof IDomain) {
			return toString((IDomain) object);
		} else if (object instanceof ICartridge) {
			return toString((ICartridge) object);
		} else if (object instanceof IApplication) {
			return toString((IApplication) object);
		} else if (object instanceof IGearProfile) {
			return toString((IGearProfile) object);
		} else {
			return null;
		}
	}

	public static String toString(IApplication application) {
		if (application == null) {
			return null;
		}

		return application.getName();
	}

	public static List<String> toString(Collection<IApplication> applications) {
		if (applications == null) {
			return null;
		}

		List<String> names = new ArrayList<String>();
		for (IApplication application : applications) {
			names.add(application.getName());
		}
		return names;
	}

	public static String toString(ICartridge cartridge) {
		if (cartridge == null) {
			return null;
		}

		if (cartridge.isDownloadable()) {
			return toDownloadableCartridgeLabel(cartridge.getName(), cartridge.getDisplayName(), cartridge.getUrl());
		} else {
			return toCatridgeLabel(cartridge.getName(), cartridge.getDisplayName());
		}
	}

	protected static String toCatridgeLabel(String name, String displayName) {
		StringBuilder builder = new StringBuilder();
		if (!StringUtils.isEmpty(displayName)) {
			builder.append(displayName)
					.append(" (").append(name).append(')');
		} else {
			builder.append(name);
		}
		return builder.toString();
	}

	protected static String toDownloadableCartridgeLabel(String name, String displayName, URL url) {
		StringBuilder builder = new StringBuilder();
		if (!StringUtils.isEmpty(displayName)) {
			builder.append(displayName);
		} else {
			builder.append(name);
		}

		if (url != null) {
			String urlString = StringUtils.shorten(url.toString(), 80);
			builder.append(" (").append(urlString).append(')');
		}
		return builder.toString();
	}

	public static String toString(List<IDomain> domains) {
		StringBuilder builder = new StringBuilder();
		if (domains == null
				|| domains.isEmpty()) {
			return builder.toString();
		}
		for (IDomain domain : domains) {
			if (domain == null) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(toString(domain));
		}
		return builder.toString();
	}

	public static String toString(IDomain domain) {
		if (domain == null) {
			return null;
		}

		return domain.getId();
	}

	public static String toString(IGearProfile gear) {
		if (gear == null) {
			return null;
		}

		return gear.getName();
	}
}
