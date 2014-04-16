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
package org.jboss.tools.openshift.express.internal.core;

import java.text.Collator;
import java.util.Comparator;

import com.openshift.client.IQuickstart;

/**
 * @author Andre Dietisheim
 */
public class QuickstartNameComparator implements Comparator<IQuickstart> {

	private Collator collator;

	public QuickstartNameComparator() {
		this.collator = Collator.getInstance();
	}

	@Override
	public int compare(IQuickstart thisQuickstart, IQuickstart thatQuickstart) {
		if (thisQuickstart == null) {
			if (thatQuickstart == null) {
				return 0;
			} else {
				return -1;
			}
		} else if (thatQuickstart == null) {
			return 1;
		}
		return collator.compare(thisQuickstart.getName(), thatQuickstart.getName());
	}
}
