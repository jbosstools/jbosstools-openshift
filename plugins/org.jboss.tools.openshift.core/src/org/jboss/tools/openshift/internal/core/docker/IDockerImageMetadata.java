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

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IDockerImageMetadata {

	Set<String> exposedPorts();
	
	List<String> env();

	/**
	 * Returns the labels that are defined for the Config section this docker image
	 * 
	 * @return
	 */
	Map<String, String> labels();

	Set<String> volumes();
}
