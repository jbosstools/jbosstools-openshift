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
package org.jboss.tools.openshift.express.internal.core.util;

import java.net.URL;
import java.util.List;

import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.cartridges.CodeAnythingCartridge;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public class ExpressResourceLabelUtils {

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

	public static String toString(ExpressConnection connection) {
		if (connection == null) {
			return null;
		}

		StringBuilder builder = new StringBuilder(connection.getUsername());
		builder
			.append(" at ")
			.append(connection.getHost());
		if (connection.isDefaultHost()) {
			builder.append(" (default)");
		}
		return builder.toString();
	}

	public static String toString(IApplication application) {
		if (application == null) {
			return null;
		}

		return application.getName();
	}

	public static String toString(ICartridge cartridge) {
		if (cartridge == null) {
			return null;
		}
		
		if (cartridge instanceof CodeAnythingCartridge) {
			return toCodeAnythingLabel(cartridge.getName(), cartridge.getDisplayName(), cartridge.getUrl());
		} else if (cartridge.isDownloadable()) {
			return toDownloadableCartridgeLabel(cartridge.getName(), cartridge.getDisplayName(), cartridge.getUrl());
		} else {
			return toCatridgeLabel(cartridge.getName(), cartridge.getDisplayName(), cartridge.isObsolete());
		}
	}

	private static String toCatridgeLabel(String name, String displayName, boolean isObsolete) {
		StringBuilder builder = new StringBuilder();
		if (!StringUtils.isEmpty(displayName)) {
			builder.append(displayName).append(" (").append(name).append(')');
		} else {
			builder.append(name);
		}
		
		if (isObsolete) {
			builder.append("[obsolete]");
		}
		return builder.toString();
	}

	private static String toCodeAnythingLabel(String name, String displayName, URL url) {
		StringBuilder builder = new StringBuilder();
		if (url != null) {
			builder.append(StringUtils.shorten(url.toString(), 50));
		} else {
			builder.append(displayName);
		}
		
		return builder.append(" (Downloadable Cartridge)").toString();
	}

	private static String toDownloadableCartridgeLabel(String name, String displayName, URL url) {
		String cartridgeName = getCartridgeName(name, displayName);
		if (cartridgeName == null) {
			if (url != null) {
				cartridgeName = StringUtils.shorten(url.toString(), 50);
			}
		}
		
		StringBuilder builder = new StringBuilder();
		if (cartridgeName != null) {
			builder.append(cartridgeName);
			builder.append(" (");
		}
		builder.append("Downloadable Cartridge");
		if (cartridgeName != null) {
			builder.append(')');
		}
		return builder.toString();
	}

	private static String getCartridgeName(String name, String displayName) {
		String cartridgeName = null;
		if (!StringUtils.isEmpty(displayName)) {
			cartridgeName = displayName;
		} else if (!StringUtils.isEmpty(name)){
			cartridgeName = name;
		}
		return cartridgeName;
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

		return new StringBuilder(
				domain.getId())
				.append('.')
				.append(domain.getSuffix())
				.toString();
	}

	public static String toString(IGearProfile gear) {
		if (gear == null) {
			return null;
		}

		return gear.getName();
	}
}
