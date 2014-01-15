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

import java.util.List;

import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftResourceUtils {

	public static String toString(ICartridge cartridge) {
		if (cartridge == null) {
			return null;
		}
		
		String name = cartridge.getName();
		String displayName = cartridge.getDisplayName();
		
		StringBuilder builder = new StringBuilder();
		if (StringUtils.isEmpty(displayName)) {
			builder.append(name);
		} else {
			builder
					.append(cartridge.getDisplayName())
					.append(" (").append(name).append(')');
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
