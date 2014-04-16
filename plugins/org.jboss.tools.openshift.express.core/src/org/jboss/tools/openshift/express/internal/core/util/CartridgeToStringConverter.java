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

import org.jboss.tools.openshift.express.internal.core.util.StringUtils.ToStringConverter;

import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public class CartridgeToStringConverter implements ToStringConverter<ICartridge> {

		@Override
		public String toString(ICartridge cartridge) {
			if (cartridge == null) {
				return null;
			}
			return cartridge.getName();
		}
	}