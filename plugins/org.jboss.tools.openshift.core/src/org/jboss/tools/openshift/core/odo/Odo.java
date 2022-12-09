/*******************************************************************************
 * Copyright (c) 2019-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Odo {
	List<String> getNamespaces() throws IOException;

	String getNamespace() throws IOException;

	void start(String project, String context, String component, ComponentFeature feature, Consumer<Boolean> callback)
			throws IOException;

	void stop(String project, String context, String component, ComponentFeature feature, Consumer<Boolean> callback) throws IOException;

	boolean isStarted(String project, String context, String component, ComponentFeature feature) throws IOException;

	void describeComponent(String project, String context, String component) throws IOException;

	List<ComponentMetadata> analyze(String path) throws IOException;

	void createComponent(String project, String componentType, String registryName, String component, String source,
			String devfile, String starter) throws IOException;

	void createService(String project, ServiceTemplate serviceTemplate, OperatorCRD serviceCRD, String service,
			ObjectNode spec, boolean wait) throws IOException;

	String getServiceTemplate(String project, String service) throws IOException;

	void deleteService(String project, Service service) throws IOException;

	List<DevfileComponentType> getComponentTypes() throws IOException;

	List<ServiceTemplate> getServiceTemplates() throws IOException;

	void describeServiceTemplate(String template) throws IOException;

	List<URL> listURLs(String project, String context, String component) throws IOException;

	ComponentInfo getComponentInfo(String project, String component, String path, ComponentKind kind)
			throws IOException;

	void deleteURL(String project, String context, String component, String name) throws IOException;

	void deleteComponent(String project, String context, String component, ComponentKind kind) throws IOException;

	void follow(String project, String context, String component, boolean deploy) throws IOException;

	void log(String project, String context, String component, boolean deploy) throws IOException;

	boolean isLogRunning(String context, String component, boolean deploy) throws IOException;

	void createProject(String project) throws IOException;

	void deleteProject(String project) throws IOException;

	void login(String url, String userName, char[] password, char[] token) throws IOException;

	void logout() throws IOException;

	List<Component> getComponents(String project) throws IOException;

	List<Service> getServices(String project) throws IOException;

	void listComponents() throws IOException;

	void listServices() throws IOException;

	void about() throws IOException;

	void link(String project, String context, String component, String target) throws IOException;

	String consoleURL() throws IOException;

	java.net.URL getMasterUrl();

	List<ComponentDescriptor> discover(String path) throws IOException;

	ComponentTypeInfo getComponentTypeInfo(String componentType, String registryName) throws IOException;

	List<DevfileRegistry> listDevfileRegistries() throws IOException;

	void createDevfileRegistry(String name, String url, String token) throws IOException;

	void deleteDevfileRegistry(String name) throws IOException;

	List<DevfileComponentType> getComponentTypes(String name) throws IOException;

	boolean isOpenShift();

	void migrateComponent(String context, String name) throws IOException;

}
