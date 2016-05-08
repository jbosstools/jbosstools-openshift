/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.internal.core.util;

import org.apache.commons.lang.StringUtils;
import org.jboss.tools.openshift.internal.core.IDockerImageMetadata;
import org.jboss.tools.openshift.internal.core.ImageStreamTagMetaData;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.resources.IImageStreamImportCapability;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.image.IImageStreamImport;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class OpenShiftProjectUtils {

	private OpenShiftProjectUtils() {
	}
	
	public static IDockerImageMetadata lookupImageMetadata(IProject project, DockerImageURI imageURI) {
		if (project != null && project.supports(IImageStreamImportCapability.class)) {
			final IImageStreamImportCapability cap = project.getCapability(IImageStreamImportCapability.class);
			try {
				final IImageStreamImport streamImport = cap.importImageMetadata(imageURI);
				if (ResourceUtils.isSuccessful(streamImport)) {
					String json = streamImport.getImageJsonFor(imageURI.getTag());
					if(StringUtils.isBlank(json)) {
						OpenShiftCoreActivator.logError("Did not find metadata during ImportImageStream for " + imageURI.getAbsoluteUri(), null);
						return null;
					}
					return new ImageStreamTagMetaData(json);
				}
			} catch (OpenShiftException e) {
				OpenShiftCoreActivator.logError("Error while trying to lookup image metadata", e);
			}
		}
		return null;
	}
}
