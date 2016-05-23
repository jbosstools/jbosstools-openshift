package org.jboss.tools.openshift.core.jmx;

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnectorProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

public class ConnectorProviderRegistry {

	public static final String CONNECTOR_PROVIDER_EXT_POINT = "org.jboss.tools.jmx.core.remoting.jmxprovider";
	public static final String CLASS_ATTRIBUTE = "class";
	private Map<VersionKey, JMXConnectorProvider> extensions;

	public ConnectorProviderRegistry() {
		extensions= loadExtensions();
	}

	private Map<VersionKey, JMXConnectorProvider> loadExtensions() {
		Map<VersionKey, JMXConnectorProvider> result = new HashMap<VersionKey, JMXConnectorProvider>();
		IExtension[] extensions = findExtension(CONNECTOR_PROVIDER_EXT_POINT);
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement elements[] = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				try {
					JMXConnectorProvider o = (JMXConnectorProvider) elements[j]
							.createExecutableExtension(CLASS_ATTRIBUTE);
					for (IConfigurationElement serverVersion : elements[j].getChildren()) {
						String serverType = serverVersion.getAttribute("servertype");
						String version = serverVersion.getAttribute("version");
						result.put(new VersionKey(ServerType.fromString(serverType), version), o);
					}
				} catch (InvalidRegistryObjectException e) {
					OpenShiftCoreActivator.logError("Could not load extension", e);
				} catch (CoreException e) {
					OpenShiftCoreActivator.logError("Could not load extension", e);
				}
			}
		}
		return result;
	}

	JMXConnectorProvider getProvider(VersionKey version) {
		return extensions.get(version);
	}

	private static IExtension[] findExtension(String extensionId) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(extensionId);
		return extensionPoint.getExtensions();
	}


}
