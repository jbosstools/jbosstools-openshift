package org.jboss.tools.openshift.core.server.behavior.eap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.adapter.IOpenshiftServerAdapterProfileDetector;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.build.IBuildStrategy;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.ISTIBuildStrategy;
import com.openshift.restclient.model.build.ISourceBuildStrategy;

public class OpenshiftEapProfileDetector implements IOpenshiftServerAdapterProfileDetector {

	public static final String PROFILE = "openshift3.eap";
	
	private static final Collection<String> EAP_LIKE_KEYWORDS = 
			Collections.unmodifiableCollection(Arrays.asList("eap", "wildfly"));
	

	public OpenshiftEapProfileDetector() {
		super();
	}

	@Override
	public String getProfile() {
		return PROFILE;
	}

	@Override
	public boolean detect(IConnection connection, IResource resource,
			org.eclipse.core.resources.IProject eclipseProject) {
		IBuildConfig buildConfig = getBuildConfig(connection, resource);
		if (buildConfig == null) {
			return false;
		}
		return isEapStyle(buildConfig);
	}

	private IBuildConfig getBuildConfig(IConnection connection, IResource resource) {
		if (resource == null || resource.getProject() == null || !(connection instanceof Connection)) {
			return null;
		}
		Connection connectionInstance = (Connection) connection;
		List<IBuildConfig> buildConfigs = 
				connectionInstance.getResources(ResourceKind.BUILD_CONFIG, resource.getProject().getName());
		if (buildConfigs == null) {
			return null;
		}
		return ResourceUtils.getBuildConfigFor(resource, buildConfigs);
	}

	@SuppressWarnings({ "deprecation" })
	public boolean isEapStyle(IBuildConfig buildConfig) {
		if (buildConfig == null) {
			return false;
		}
		// First check buildconfig docker image name
		IBuildStrategy strategy = buildConfig.getBuildStrategy();
		DockerImageURI image = null;
		boolean isEapStyle = false;
		if (strategy instanceof ISourceBuildStrategy) {
			image = ((ISourceBuildStrategy) strategy).getImage();
		} else if (strategy instanceof ICustomBuildStrategy) {
			image = ((ICustomBuildStrategy) strategy).getImage();
		} else if (strategy instanceof IDockerBuildStrategy) {
			image = ((IDockerBuildStrategy) strategy).getBaseImage();
		} else if (strategy instanceof ISTIBuildStrategy) {
			image = ((ISTIBuildStrategy) strategy).getImage();
		}
		if (image != null) {
			isEapStyle = containsEapLikeKeywords(image.getName());
		}
		if (!isEapStyle) {
			// Check template labels as a last resort
			// not sure it's even possible to reach this point
			Map<String, String> labels = buildConfig.getLabels();
			if (labels != null) {
				String template = labels.get("template");
				isEapStyle = containsEapLikeKeywords(template);
			}
		}
		return isEapStyle;
	}

	public boolean containsEapLikeKeywords(String label) {
		if (org.apache.commons.lang.StringUtils.isBlank(label)) {
			return false;
		}
		String lcLabel = label.toLowerCase();
		boolean isEapLike = EAP_LIKE_KEYWORDS.stream().filter(kw -> lcLabel.contains(kw)).findFirst().isPresent();
		return isEapLike;
	}

}
