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
package org.jboss.tools.openshift.express.internal.ui.wizard.application.template;

import java.util.Collections;
import java.util.List;

import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public class CartridgeApplicationTemplate extends AbstractApplicationTemplate implements ICartridgeApplicationTemplate {

	private IStandaloneCartridge cartridge;

	public CartridgeApplicationTemplate(IStandaloneCartridge cartridge) {
		super(cartridge.getDisplayName(), cartridge.getDescription());
		this.cartridge = cartridge;
	}

	@Override
	public IStandaloneCartridge getCartridge() {
		return cartridge;
	}
	
	@Override
	public boolean isDownloadable() {
		return cartridge.isDownloadable();
	}
	
	@Override
	public List<IApplicationTemplate> getChildren() {
		return Collections.emptyList();
	}
}