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
import java.util.Set;

import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.client.cartridge.ICartridge;
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
	public ICartridge getStandaloneCartridge() {
		return cartridge;
	}
	
	@Override
	public Set<ICartridge> getAllCartridges() {
		return Collections.<ICartridge> singleton(cartridge);
	}
	
	@Override
	public boolean canAddRemoveCartridges() {
		return true;
	}

	@Override
	public String getInitialGitUrl() {
		return null;
	}

	@Override
	public boolean isCodeAnything() {
		return cartridge.isDownloadable();
	}
	
	@Override
	public boolean isMatching(String expression) {
		if (super.isMatching(expression)) {
			return true;
		}

		if (cartridge == null) {
			return false;
		}

		return isMatching(
				StringUtils.toLowerCase(expression), StringUtils.toLowerCase(cartridge.getName()));
	}

	@Override
	public boolean isInitialGitUrlEditable() {
		return true;
	}
}