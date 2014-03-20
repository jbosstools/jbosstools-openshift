/*******************************************************************************
 * Copyright (c) 2014 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.viewer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * @author Andre Dietisheim
 */
public class EmbeddableCartridgeViewerSorter extends ViewerSorter {
	@Override
	public int compare(Viewer viewer, Object thisCartridge, Object thatCartridge) {
		if (thisCartridge instanceof IEmbeddableCartridge
				&& thatCartridge instanceof IEmbeddableCartridge) {
			String thisDisplayName = ((IEmbeddableCartridge) thisCartridge).getDisplayName();
			String thatDisplayName = ((IEmbeddableCartridge) thatCartridge).getDisplayName();
			if (thisDisplayName == null) {
				if (thatDisplayName != null) {
					return 1;
				} else {
					return 0;
				}
			} else if (thatDisplayName == null) {
				return -1;
			} else {
				return thisDisplayName.compareTo(thatDisplayName);
			}
		}
		return super.compare(viewer, thisCartridge, thatCartridge);
	}
}
