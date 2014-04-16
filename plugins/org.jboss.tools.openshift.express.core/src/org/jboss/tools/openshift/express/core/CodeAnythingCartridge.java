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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((urlString == null) ? 0 : urlString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CodeAnythingCartridge other = (CodeAnythingCartridge) obj;
		if (urlString == null) {
			if (other.urlString != null)
				return false;
		} else if (!urlString.equals(other.urlString))
			return false;
		return true;
	}
}