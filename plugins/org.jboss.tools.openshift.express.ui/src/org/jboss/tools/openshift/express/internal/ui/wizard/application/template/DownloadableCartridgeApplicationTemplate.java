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

import org.jboss.tools.openshift.express.core.CodeAnythingCartridge;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public class DownloadableCartridgeApplicationTemplate
		extends CartridgeApplicationTemplate implements IDownloadableCartridgeApplicationTemplate {

	public DownloadableCartridgeApplicationTemplate() {
		super(new CodeAnythingCartridge());
	}

	@Override
	public boolean isDownloadable() {
		return true;
	}

	@Override
	public String getUrl() {
		return getCartridgeUrl((CodeAnythingCartridge) getCartridge());
	}
	
	@Override
	public void setUrl(String url) {
		String oldUrl = getUrl();
		((CodeAnythingCartridge) getCartridge()).setUrlString(url);
		firePropertyChange(PROPERTY_CARTRIDGE_URL, oldUrl, url);
		String oldName = getName();
		firePropertyChange(PROPERTY_NAME, oldName, getName());
	}
	
	@Override()
	public String getName() {
		StringBuilder builder = new StringBuilder(super.getName());
		String cartridgeUrl = getCartridgeUrl((CodeAnythingCartridge) getCartridge());
		if (!StringUtils.isEmpty(cartridgeUrl)) {
			builder.append(" (").append(cartridgeUrl).append(')');
		}
		return builder.toString();
	}

	protected String getCartridgeUrl(CodeAnythingCartridge cartridge) {
		if (cartridge == null
				|| cartridge.getUrlString() == null) {
			return null;
		}
		return cartridge.getUrlString();
	}

	@Override
	public boolean isMatching(String expression) {
		if (super.isMatching(expression)) {
			return true;
		}
		ICartridge cartridge = getCartridge();
		if (cartridge == null) {
			return false;
		}

		String lowerCaseExpression = StringUtils.toLowerCase(expression);

		return isMatching(lowerCaseExpression, StringUtils.toLowerCase(cartridge.getName()))
				|| isMatching(lowerCaseExpression, StringUtils.toLowerCase(cartridge.getDisplayName()))
				|| isMatching(lowerCaseExpression, StringUtils.toLowerCase(cartridge.getDescription()));
	}
}