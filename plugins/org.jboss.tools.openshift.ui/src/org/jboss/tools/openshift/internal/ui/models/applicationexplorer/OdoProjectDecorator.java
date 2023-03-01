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
package org.jboss.tools.openshift.internal.ui.models.applicationexplorer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.core.odo.Binding;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.ComponentFeature;
import org.jboss.tools.openshift.core.odo.ComponentFeatures;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.ComponentKind;
import org.jboss.tools.openshift.core.odo.ComponentMetadata;
import org.jboss.tools.openshift.core.odo.ComponentTypeInfo;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.DevfileRegistry;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.OperatorCRD;
import org.jboss.tools.openshift.core.odo.Service;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.URL;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class OdoProjectDecorator implements Odo {
	private final Odo delegate;
	private final ApplicationExplorerUIModel model;

	public OdoProjectDecorator(Odo delegate, ApplicationExplorerUIModel model) {
		this.delegate = delegate;
		this.model = model;
	}

	@Override
	public List<String> getNamespaces() throws IOException {
		return delegate.getNamespaces();
	}

	@Override
	public String getNamespace() throws IOException {
		return delegate.getNamespace();
	}

	@Override
	public void start(String project, String context, String component, ComponentFeature feature,
			Consumer<Boolean> callback) throws IOException {
		delegate.start(project, context, component, feature, callback);
	}

	@Override
	public void stop(String project, String context, String component, ComponentFeature feature,
			Consumer<Boolean> callback) throws IOException {
		delegate.stop(project, context, component, feature, callback);
	}

	@Override
	public boolean isStarted(String project, String context, String component, ComponentFeature feature)
			throws IOException {
		return delegate.isStarted(project, context, component, feature);
	}

	@Override
	public void describeComponent(String project, String context, String component) throws IOException {
		delegate.describeComponent(project, context, component);
	}

	private void refreshProject(String location) {
		Job.create("Refreshing project " + location, monitor -> {
			IContainer project = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(location));
			if (project instanceof IProject) {
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
		}).schedule();
	}

	@Override
	public List<ComponentMetadata> analyze(String path) throws IOException {
		return delegate.analyze(path);
	}

	@Override
	public void createComponent(String project, String componentType, String registryName, String component,
			String source, String devfile, String starter) throws IOException {
		if (StringUtils.isNotBlank(starter)) {
			File tmpdir;
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				tmpdir = Files.createTempDirectory("odotmp").toFile();
				tmpdir.setReadable(true, true);
				tmpdir.setWritable(true, true);
				tmpdir.setExecutable(true, true);
			} else {
				FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
						.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
				tmpdir = Files.createTempDirectory("odotmp", attr).toFile();
			}
			delegate.createComponent(project, componentType, registryName, component, tmpdir.getAbsolutePath(), devfile,
					starter);
			FileUtils.copyDirectory(tmpdir, new File(source));
			FileUtils.deleteQuietly(tmpdir);
			refreshProject(source);
		} else {
			delegate.createComponent(project, componentType, registryName, component, source, devfile, starter);
		}
	}

	@Override
	public void createService(String project, ServiceTemplate serviceTemplate, OperatorCRD serviceCRD, String service,
			ObjectNode spec, boolean wait) throws IOException {
		delegate.createService(project, serviceTemplate, serviceCRD, service, spec, wait);
	}

	@Override
	public String getServiceTemplate(String project, String service) throws IOException {
		return delegate.getServiceTemplate(project, service);
	}

	@Override
	public void deleteService(String project, Service service) throws IOException {
		delegate.deleteService(project, service);
	}

	@Override
	public List<DevfileComponentType> getComponentTypes() throws IOException {
		return delegate.getComponentTypes();
	}

	@Override
	public List<DevfileComponentType> getComponentTypes(String registryName) throws IOException {
		return delegate.getComponentTypes(registryName);
	}

	@Override
	public ComponentTypeInfo getComponentTypeInfo(String componentType, String registryName) throws IOException {
		return delegate.getComponentTypeInfo(componentType, registryName);
	}

	@Override
	public List<ServiceTemplate> getServiceTemplates() throws IOException {
		return delegate.getServiceTemplates();
	}

	@Override
	public void describeServiceTemplate(String template) throws IOException {
		delegate.describeServiceTemplate(template);
	}

	@Override
	public List<URL> listURLs(String project, String context, String component) throws IOException {
		return delegate.listURLs(project, context, component);
	}

	@Override
	public ComponentInfo getComponentInfo(String project, String component, String path, ComponentKind kind)
			throws IOException {
		return delegate.getComponentInfo(project, component, path, kind);
	}

	@Override
	public void deleteURL(String project, String context, String component, String name) throws IOException {
		delegate.deleteURL(project, context, component, name);
	}

	@Override
	public void deleteComponent(String project, String context, String component, ComponentKind kind)
			throws IOException {
		model.getComponents().forEach((path, desc) -> {
			if (desc.getName().equals(component)) {
				model.getComponents().remove(path);
			}
		});
		delegate.deleteComponent(project, context, component, kind);
	}

	@Override
	public void follow(String project, String context, String component, boolean deploy) throws IOException {
		delegate.follow(project, context, component, deploy);
	}

	@Override
	public void log(String project, String context, String component, boolean deploy) throws IOException {
		delegate.log(project, context, component, deploy);
	}

	@Override
	public boolean isLogRunning(String context, String component, boolean deploy) throws IOException {
		return delegate.isLogRunning(context, component, deploy);
	}

	@Override
	public void createProject(String project) throws IOException {
		delegate.createProject(project);
	}

	@Override
	public void deleteProject(String project) throws IOException {
		delegate.deleteProject(project);
	}

	@Override
	public void login(String url, String userName, char[] password, char[] token) throws IOException {
		delegate.login(url, userName, password, token);
	}

	@Override
	public void logout() throws IOException {
		delegate.logout();
	}

	@Override
	public List<Component> getComponents(String project) throws IOException {
		final IOException[] exceptions = { null };
		List<Component> components = delegate.getComponents(project);
		model.getComponents().forEach((path, comp) -> {
			Optional<Component> found = components.stream().filter(comp1 -> comp1.getName().equals(comp.getName()))
					.findFirst();
			try {
				if (found.isPresent()) {
					found.get().setPath(path);
					found.get().setInfo(getComponentInfo(project, comp.getName(), path, ComponentKind.DEVFILE));
				} else {
					components.add(Component.of(comp.getName(), new ComponentFeatures(), path,
							getComponentInfo(project, comp.getName(), path, ComponentKind.DEVFILE)));
				}
			} catch (IOException e) {
				exceptions[0] = e;
			}
		});
		if (exceptions[0] != null) {
			throw exceptions[0];
		}
		return components;
	}

	@Override
	public List<Service> getServices(String project) throws IOException {
		return delegate.getServices(project);
	}

	@Override
	public void listComponents() throws IOException {
		delegate.listComponents();
	}

	@Override
	public void listServices() throws IOException {
		delegate.listServices();
	}

	@Override
	public void about() throws IOException {
		delegate.about();
	}

	@Override
	public Binding link(String project, String context, String component, String target) throws IOException {
		return delegate.link(project, context, component, target);
	}

	@Override
	public List<Binding> listBindings(String project, String context, String component) throws IOException {
		return delegate.listBindings(project, context, component);
	}

	@Override
	public void deleteBinding(String project, String context, String component, String binding) throws IOException {
		delegate.deleteBinding(project, context, component, binding);
	}

	@Override
	public String consoleURL() throws IOException {
		return delegate.consoleURL();
	}

	@Override
	public List<ComponentDescriptor> discover(String path) throws IOException {
		return delegate.discover(path);
	}

	@Override
	public java.net.URL getMasterUrl() {
		return delegate.getMasterUrl();
	}

	@Override
	public List<DevfileRegistry> listDevfileRegistries() throws IOException {
		return delegate.listDevfileRegistries();
	}

	@Override
	public void createDevfileRegistry(String name, String url, String token) throws IOException {
		delegate.createDevfileRegistry(name, url, token);
	}

	@Override
	public void deleteDevfileRegistry(String name) throws IOException {
		delegate.deleteDevfileRegistry(name);
	}

	@Override
	public boolean isOpenShift() {
		return delegate.isOpenShift();
	}

	@Override
	public void migrateComponent(String context, String name) throws IOException {
		delegate.migrateComponent(context, name);
	}
}
