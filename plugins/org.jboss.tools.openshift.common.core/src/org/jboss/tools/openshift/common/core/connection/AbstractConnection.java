/*******************************************************************************
 * Copyright (c) 2012-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;

/**
 * @author Rob Stryker
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public abstract class AbstractConnection extends ObservablePojo implements IConnection {

	public static final String PROPERTY_HOST = "host";
	
	private String host;

	protected AbstractConnection(String host) {
		this(null, host);
	}
	
	protected AbstractConnection(String scheme, String host) {
		this.host = getHost(scheme, host);
	}

	private String getHost(String scheme, String host) {
		if (StringUtils.isEmpty(host)) {
			return host;
		}
		
		if (StringUtils.isEmpty(scheme)) {
			scheme = UrlUtils.SCHEME_HTTPS;
		}
		return UrlUtils.ensureStartsWithScheme(host, scheme);
	}

	public String getHost() {
		return host;
	}

	public String getScheme() {
		return UrlUtils.getScheme(host);
	}

	public String setHost(String host) {
		firePropertyChange(PROPERTY_HOST, 
				this.host,
				this.host = UrlUtils.ensureStartsWithScheme(host, UrlUtils.SCHEME_HTTPS));
		return host;
	}

	public abstract boolean connect();
	
	public abstract boolean isConnected();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AbstractConnection)) {
			return false;
		}
		AbstractConnection other = (AbstractConnection) obj;
		if (host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!host.equals(other.host)) {
			return false;
		}
		return true;
	}
}
