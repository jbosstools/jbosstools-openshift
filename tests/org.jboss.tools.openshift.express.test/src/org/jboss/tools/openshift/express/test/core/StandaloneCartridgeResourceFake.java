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

import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.internal.client.StandaloneCartridgeResource;

/**
 * @author Andre Dietisheim
 */
public class StandaloneCartridgeResourceFake extends StandaloneCartridgeResource {

	public StandaloneCartridgeResourceFake(IStandaloneCartridge cartridge) {
		super(cartridge.getName(), cartridge.getDisplayName(),
				cartridge.getDisplayName(), cartridge.getUrl(),
				cartridge.getType(), cartridge.isObsolete(), null, null, null, null);
		}
}
