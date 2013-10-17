/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.openshift.express.internal.ui.console;

import java.util.Iterator;

import com.openshift.client.IGearGroup;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author xcoulon
 *
 */
public class GearGroupsUtils {

	/**
	 * Returns a concatenation of the display names of the cartridges of the given {@link IGearGroup}
	 * @param gearGroup the Gear Group
	 * @return the cartridges display names, separated by a comma ({@code ,}) character
	 */
	public static String getCartridgeDisplayNames(final IGearGroup gearGroup) {
		final StringBuffer cartridgeNamesBuffer = new StringBuffer();
		for(Iterator<ICartridge> iterator = gearGroup.getCartridges().iterator(); iterator.hasNext();) {
			ICartridge cartridge = iterator.next();
			cartridgeNamesBuffer.append(cartridge.getDisplayName());
			if(iterator.hasNext()) {
				cartridgeNamesBuffer.append(", ");
			}
		}
		return cartridgeNamesBuffer.toString();
	}
}
