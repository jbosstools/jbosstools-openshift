/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.jboss.dmr.ModelNode;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.ImportImageMetaData;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.resources.IImageStreamImportCapability;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.image.IImageStreamImport;

/**
 * @author Andre Dietisheim
 */
public class PodDeploymentPathProvider {

	// default fallback
	private static final String DEFAULT_DEPLOYMENT_DIR = "/opt/app-root/src";
	// "image->"dockerImageMetadata"->"Config"->"Labels"->"com.redhat.deployments-dir"
	private static final Pattern PATTERN_REDHAT_DEPLOYMENTS_DIR = Pattern
			.compile("\"com\\.redhat\\.deployments-dir\"[^\"]*\"([^\"]*)\",");
	// "image->"dockerImageMetadata"->"Config"->"Labels"->"com.redhat.deployments-dir"
	private static final Pattern PATTERN_JBOSS_DEPLOYMENTS_DIR = Pattern
			.compile("\"org\\.jboss\\.deployments-dir\"[^\"]*\"([^\"]*)\",");
	// "image->"dockerImageMetadata"->"Config"->"WorkginDir"
	private static final Pattern PATTERN_WOKRING_DIR = Pattern.compile("\"WorkingDir\"[^\"]*\"([^\"]*)\",");

	private static final String DOCKER_IMAGE_DIGEST_IDENTIFIER = "sha256:";

	public String load(IResource resource, Connection connection) throws CoreException {
		IProject project = resource.getProject();
		IPod pod = getPod(resource, project, connection);
		String imageRef = getImageRef(connection, project, pod);
		int imageDigestIndex = imageRef.indexOf(DOCKER_IMAGE_DIGEST_IDENTIFIER);
		if (imageDigestIndex > 0) {
			String imageDigest = imageRef.substring(imageDigestIndex);
			IResource imageStreamTag = getImageStreamTag(connection, project, imageRef, imageDigest);
			return getPodPath(imageStreamTag);
		} else {
		    String imageMetaData = getImageMetaData(imageRef, project);
	        return getPodPath(imageMetaData);
		}
	}

	private String getImageRef(Connection connection, IProject project, IPod pod) throws CoreException {
		Collection<String> images = pod.getImages();
		if (images.isEmpty()) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind("No images found for pod {0} in project {1} on server {2}",
							new Object[] { pod.getName(), project.getName(), connection.getHost() })));
		}
		// TODO: handle if there are 2+ images
		String imageRef = images.iterator().next();
		return imageRef;
	}

	private IPod getPod(IResource resource, IProject project, Connection connection) throws CoreException {
		List<IPod> allPods = project.getResources(ResourceKind.POD);
		List<IPod> pods = ResourceUtils.getPodsFor(resource, allPods);
		if (pods.isEmpty()) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind("No pods found for {0} {1} in project {2} on server {3}. "
							+ "Ensure your build is finished and a pod has been deployed.",
							new Object[] { resource.getKind(), resource.getName(), project.getName(), connection.getHost() })));
		}
		// TODO: handle if there are 2+ pods
		IPod pod = pods.get(0);
		return pod;
	}

	private IResource getImageStreamTag(Connection connection, IProject project, String imageRef, String imageDigest)
			throws CoreException {
		List<IResource> imageStreamTags = project.getResources(ResourceKind.IMAGE_STREAM_TAG);
		IResource imageStreamTag = imageStreamTags.stream()
				.filter(istag -> {
					String imageName = ModelNode.fromJSONString(istag.toJson()).get("image").get("metadata").get("name").asString();
					return imageDigest.equals(imageName);
				})
				.findFirst().orElse(null);
		if (imageStreamTag == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind("No metadata found for docker image {0} in project {1} on server {2}",
							new Object[] { imageRef, project.getName(), connection.getHost() })));
		}
		// load image stream tag individually to get full ContainerConfig metadata
		imageStreamTag = connection.getResource(ResourceKind.IMAGE_STREAM_TAG, project.getName(), imageStreamTag.getName());
		return imageStreamTag;
	}

	private String getImageMetaData(String imageRef, IProject project) {
		IImageStreamImportCapability imageStreamImportCapability = ((IProject) project).getCapability(IImageStreamImportCapability.class);
		DockerImageURI uri = new DockerImageURI(imageRef);
		IImageStreamImport imageStreamImport = imageStreamImportCapability.importImageMetadata(uri);
		if (ResourceUtils.isSuccessful(imageStreamImport)) {
			return imageStreamImport.getImageJsonFor(uri);
		} else {
			return null;
		}
	}

	protected String useDefaultPathIfEmpty(String podPath) {
		if (StringUtils.isEmpty(podPath)) {
			return DEFAULT_DEPLOYMENT_DIR;
		}
		return podPath;
	}

	private String getPodPath(IResource imageStreamTag) {
		if (imageStreamTag == null) {
			return null;
		}
		return getPodPath(imageStreamTag.toJson(true));
	}

    private String getPodPath(String json) {
        if (json == null) {
            return null;
        }
        String podPath = null;
        if ((podPath = matchFirstGroup(json, PATTERN_REDHAT_DEPLOYMENTS_DIR)) == null) {
            if ((podPath = matchFirstGroup(json, PATTERN_JBOSS_DEPLOYMENTS_DIR)) == null) {
                podPath = matchFirstGroup(json, PATTERN_WOKRING_DIR);
            }
        }

        return podPath;
    }

    private String matchFirstGroup(String imageStreamTag, Pattern pattern) {
		Matcher matcher = pattern.matcher(imageStreamTag);
		if (matcher.find() && matcher.groupCount() == 1) {
			return matcher.group(1);
		} else {
			return null;
		}
	}

}
