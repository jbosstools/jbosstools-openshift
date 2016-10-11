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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.jboss.tools.openshift.internal.core.IDockerImageMetadata;

/**
 * Metadata about an image fetched from a docker connection.
 * 
 * @author Jeff Maury
 *
 */
public class DockerConfigMetaData implements IDockerImageMetadata {

	private IDockerImageInfo info;

	public DockerConfigMetaData(IDockerImageInfo info) {
		this.info = info;
	}
	
	private static <C extends Collection<String>> C select(C config, C containerConfig) {
	    if ((config != null) && config.size() > 0) {
	        return config;
	    } else {
	        return containerConfig;
	    }
	}

	@Override
	public Set<String> exposedPorts() {
		return select(info.config()!=null?info.config().exposedPorts():null,
		              info.containerConfig()!=null?info.containerConfig().exposedPorts():null);
	}

	@Override
	public List<String> env() {
		return select(info.config()!=null?info.config().env():null,
		              info.containerConfig()!=null?info.containerConfig().env():null);
	}

	@Override
	public Set<String> volumes() {
		return select(info.config()!=null?info.config().volumes():null,
		              info.containerConfig()!=null?info.containerConfig().volumes():null);
	}
	
}