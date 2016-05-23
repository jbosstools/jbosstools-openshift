package org.jboss.tools.openshift.core.jmx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;

public class VersionDetector {
	private static final Map<String, VersionKey> knowImages= buildVersionMap();
	
	private IServer server;
	public VersionDetector(IServer server) {
		this.server= server;
	}
	
	private static Map<String, VersionKey> buildVersionMap() {
		Map<String, VersionKey> result= new HashMap<String, VersionKey>();
		result.put("jboss-eap64-openshift", new VersionKey(ServerType.EAP, "6.4.0"));	
		return result;
	}

	VersionKey guess() {
		IService service = OpenShiftServerUtils.getService(server);
		if (service != null) {
			IProject project = service.getProject();
			if (project != null) {
				List<IBuildConfig> buildConfigs = project.getResources(ResourceKind.BUILD_CONFIG);
				if (buildConfigs != null) {
					IBuildConfig buildConfig = ResourceUtils.getBuildConfigForService(service, buildConfigs);
					DockerImageURI imageUri = OpenShiftServerUtils.getImageUri(buildConfig);
					return knowImages.get(imageUri.getName());
				}
			}
		}
		return null;
	}
}
