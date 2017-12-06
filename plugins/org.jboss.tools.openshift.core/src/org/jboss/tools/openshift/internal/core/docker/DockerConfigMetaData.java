/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.docker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.linuxtools.docker.core.IDockerContainerConfig;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;

/**
 * Metadata about an image fetched from a docker connection.
 * 
 * @author Jeff Maury
 *
 */
public class DockerConfigMetaData implements IDockerImageMetadata {

	private IDockerImageInfo info;

	public DockerConfigMetaData(IDockerImageInfo info) {
		// TODO: throw illegal arg exception if info is null?
		this.info = info;
	}

	/**
	 * Select info from either config or container config and provides a
	 * default non null value if nothing is found.
	 * 
	 * @param config the image config object
	 * @param containerConfig the image container config object
	 * @param accessor the accessor for the target property
	 * @param defaultFactory the factory for the default value
	 * @return the mapped value
	 */
	private <C extends Collection<String>> C select(IDockerContainerConfig config,
			IDockerContainerConfig containerConfig, Function<IDockerContainerConfig, C> accessor,
			Supplier<C> defaultFactory) {
		C result = null;
		if (config != null) {
			result = accessor.apply(config);
		}
		if (((result == null) || result.isEmpty()) && (containerConfig != null)) {
			result = accessor.apply(containerConfig);
		}
		if (result == null) {
			result = defaultFactory.get();
		}
		return result;
	}

	@Override
	public Set<String> exposedPorts() {
		if (info == null) {
			return Collections.emptySet();
		}
		return select(info.config(), info.containerConfig(), IDockerContainerConfig::exposedPorts,
				Collections::emptySet);
	}

	@Override
	public List<String> env() {
		if (info == null) {
			return Collections.emptyList();
		}
		return select(info.config(), info.containerConfig(), IDockerContainerConfig::env, Collections::emptyList);
	}

	@Override
	public Set<String> volumes() {
		if (info == null) {
			return Collections.emptySet();
		}
		return select(info.config(), info.containerConfig(), IDockerContainerConfig::volumes, Collections::emptySet);
	}

	@Override
	public Map<String, String> labels() {
		Map<String, String> result = null;
		if (info.config() != null) {
			result = info.config().labels();
		}
		if (((result == null) || result.isEmpty()) && (info.containerConfig() != null)) {
			result = info.containerConfig().labels();
		}
		if (result == null) {
			result = Collections.emptyMap();
		}
		return result;
	}
}