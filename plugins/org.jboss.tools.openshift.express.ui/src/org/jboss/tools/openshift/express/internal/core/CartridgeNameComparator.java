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
package org.jboss.tools.openshift.express.internal.core;

import java.text.Collator;
import java.util.Comparator;

import com.openshift.client.ICartridge;

/**
 * @author Andre Dietisheim
 */
public class CartridgeNameComparator implements Comparator<ICartridge> {

	private Collator collator;

	public CartridgeNameComparator() {
		this.collator = Collator.getInstance();
	}

	@Override
	public int compare(ICartridge thisCartridge, ICartridge thatCartridge) {
		if (thisCartridge == null) {
			if (thatCartridge == null) {
				return 0;
			} else {
				return -1;
			}
		} else if (thatCartridge == null) {
			if (thisCartridge == null) {
				return 0;
			} else {
				return 1;
			}
		}
		return collator.compare(thisCartridge.getName(), thatCartridge.getName());
	}
}
