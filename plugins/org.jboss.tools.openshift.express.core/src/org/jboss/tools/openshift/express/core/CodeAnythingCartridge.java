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
package org.jboss.tools.openshift.express.core;

import java.net.MalformedURLException;
import java.net.URL;

import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.client.cartridge.StandaloneCartridge;
import com.openshift.internal.client.cartridge.BaseCartridge;

/**
 * A mutable standalone and embeddable cartridge (BaseCartridge is a non-mutable value object).
 * 
 * @author Andre Dietisheim
 * 
 * @see StandaloneCartridge
 */
public class CodeAnythingCartridge extends BaseCartridge implements IStandaloneCartridge {

	public static final String PROPERTY_URL_STRING = "urlString";
	
	private String urlString;
	
	public CodeAnythingCartridge() {
		super("Downloadable cartridge", "Code Anything", 
				"Have your own framework? Provide the URL of a cartridge here, and OpenShift will download and provision it automatically into your new app!");
	}

	public void setUrlString(String url) {
		this.urlString = url;
	}
	
	public String getUrlString() {
		return urlString;
	}
	
	@Override
	public URL getUrl() {
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	@Override
	public boolean isDownloadable() {
		return true;
	}
}