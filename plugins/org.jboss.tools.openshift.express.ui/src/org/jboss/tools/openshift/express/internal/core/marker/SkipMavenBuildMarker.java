/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.marker;


/**
 * @author Andre Dietisheim
 */
public class SkipMavenBuildMarker extends AbstractOpenShiftMarker {

	private static final String MARKER_NAME = "skip_maven_build";
	
	public SkipMavenBuildMarker() {
	}

	@Override
	protected String getMarkerName() {
		return MARKER_NAME;
	}

	@Override
	protected byte[] getMarkerContent() {
		return new byte[]{};
	}
}
