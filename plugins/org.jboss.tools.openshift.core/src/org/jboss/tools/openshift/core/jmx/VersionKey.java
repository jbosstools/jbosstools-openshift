/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.jmx;

/**
 * Identifies a particular version of a web server
 * @author Thomas Mäder
 *
 */
public final class VersionKey {
	public VersionKey(ServerType serverType, String version) {
		this.serverType = serverType;
		this.version = version;
	}

	private ServerType serverType;
	private String version;
	
	public ServerType getServerType() {
		return serverType;
	}
	
	public String getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != getClass()) {
			return false;
		}
		VersionKey other = (VersionKey) o;
		return serverType.equals(other.serverType) && version.equals(other.version);
	}

	@Override
	public int hashCode() {
		return serverType.hashCode() ^ version.hashCode();
	}
}