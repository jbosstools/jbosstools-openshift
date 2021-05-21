/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models.applicationexplorer;

import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.servicecatalog.api.model.ServiceInstance;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.core.odo.Application;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.ComponentKind;
import org.jboss.tools.openshift.core.odo.ComponentState;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.ComponentTypeInfo;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.DevfileRegistry;
import org.jboss.tools.openshift.core.odo.DebugInfo;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.Storage;
import org.jboss.tools.openshift.core.odo.URL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class OdoProjectDecorator implements Odo {
  private final Odo delegate;
  private final ApplicationExplorerUIModel model;

  public OdoProjectDecorator(Odo delegate, ApplicationExplorerUIModel model) {
    this.delegate = delegate;
    this.model = model;
  }

  @Override
  public List<io.fabric8.openshift.api.model.Project> getProjects(OpenShiftClient client) {
    return delegate.getProjects(client);
  }

  @Override
  public void describeApplication(String project, String application) throws IOException {
    delegate.describeApplication(project, application);
  }

  @Override
  public void deleteApplication(OpenShiftClient client, String project, String application) throws IOException {
    final IOException[] exception = { null };
    getComponents(client, project, application).forEach(component -> {
      try {
        deleteComponent(project, application, component.getPath(), component.getName(),
            component.getInfo().getComponentKind());
      } catch (IOException e) {
        exception[0] = e;
      }
    });
    if (exception[0] != null) {
      throw exception[0];
    }
  }

  @Override
  public void push(String project, String application, String context, String component) throws IOException {
    delegate.push(project, application, context, component);
  }

  @Override
  public void describeComponent(String project, String application, String context, String component)
      throws IOException {
    delegate.describeComponent(project, application, context, component);
  }

  @Override
  public void watch(String project, String application, String context, String component) throws IOException {
    delegate.watch(project, application, context, component);
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
  public void createComponentLocal(String project, String application, String componentType, String componentVersion,
      String registryName, String component, String source, String devfile, String starter, boolean push) throws IOException {
    if (StringUtils.isNotBlank(starter)) {
      File tmpdir = Files.createTempDirectory("odotmp").toFile();
      delegate.createComponentLocal(project, application, componentType, componentVersion, registryName, component, tmpdir.getAbsolutePath(), devfile, starter, push);
      FileUtils.copyDirectory(tmpdir, new File(source));
      FileUtils.deleteQuietly(tmpdir);
      refreshProject(source);
    } else {
      delegate.createComponentLocal(project, application, componentType, componentVersion, registryName, component, source, devfile, starter, push);
    }
  }

  @Override
  public void createComponentGit(String project, String application, String context, String componentType,
      String componentVersion, String component, String source, String reference, boolean push) throws IOException {
    delegate.createComponentGit(project, application, context, componentType, componentVersion, component, source,
        reference, push);
  }

  @Override
  public void createComponentBinary(String project, String application, String context, String componentType,
      String componentVersion, String component, String source, boolean push) throws IOException {
    delegate.createComponentBinary(project, application, context, componentType, componentVersion, component, source,
        push);
  }

  @Override
  public void createService(String project, String application, String serviceTemplate, String servicePlan,
      String service, boolean wait) throws IOException {
    delegate.createService(project, application, serviceTemplate, servicePlan, service, wait);
  }

  @Override
  public String getServiceTemplate(OpenShiftClient client, String project, String application, String service) {
    return delegate.getServiceTemplate(client, project, application, service);
  }

  @Override
  public void deleteService(String project, String application, String service) throws IOException {
    delegate.deleteService(project, application, service);
  }

  @Override
  public List<ComponentType> getComponentTypes() throws IOException {
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
  public List<Integer> getServicePorts(OpenShiftClient client, String project, String application, String component) {
    List<Integer> ports = delegate.getServicePorts(client, project, application, component);
    model.getComponents().forEach((path, comp) -> {
      if (comp.getProject().equals(project) && comp.getApplication().equals(application)
          && comp.getName().equals(component)) {
        comp.getPorts().forEach(port -> {
          if (!ports.contains(port)) {
            ports.add(port);
          }
        });
      }
    });
    return ports;
  }

  @Override
  public List<URL> listURLs(String project, String application, String context, String component) throws IOException {
    return delegate.listURLs(project, application, context, component);
  }

  @Override
  public ComponentInfo getComponentInfo(OpenShiftClient client, String project, String application, String component, String path, ComponentKind kind)
      throws IOException {
    return delegate.getComponentInfo(client, project, application, component, path, kind);
  }

  @Override
  public void createURL(String project, String application, String context, String component, String name, Integer port,
      boolean secure) throws IOException {
    delegate.createURL(project, application, context, component, name, port, secure);
  }

  @Override
  public void deleteURL(String project, String application, String context, String component, String name)
      throws IOException {
    delegate.deleteURL(project, application, context, component, name);
  }

  @Override
  public void undeployComponent(String project, String application, String context, String component, ComponentKind kind)
      throws IOException {
    delegate.undeployComponent(project, application, context, component, kind);
  }

  @Override
  public void deleteComponent(String project, String application, String context, String component, ComponentKind kind)
      throws IOException {
    delegate.deleteComponent(project, application, context, component, kind);
  }

  @Override
  public void follow(String project, String application, String context, String component) throws IOException {
    delegate.follow(project, application, context, component);
  }

  @Override
  public void log(String project, String application, String context, String component) throws IOException {
    delegate.log(project, application, context, component);
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
  public void login(String url, String userName, char[] password, String token) throws IOException {
    delegate.login(url, userName, password, token);
  }

  @Override
  public void logout() throws IOException {
    delegate.logout();
  }

  @Override
  public List<Application> getApplications(String project) throws IOException {
    List<Application> applications = delegate.getApplications(project);
    model.getComponents().forEach((path, component) -> {
      if (component.getProject().equals(project)
          && applications.stream().noneMatch(application -> application.getName().equals(component.getApplication()))) {
        applications.add(Application.of(component.getApplication()));
      }
    });
    return applications;
  }

  @Override
  public List<Component> getComponents(OpenShiftClient client, String project, String application) throws IOException {
    final IOException[] exceptions = { null };
    List<Component> components = delegate.getComponents(client, project, application);
    model.getComponents().forEach((path, comp) -> {
      if (comp.getProject().equals(project) && comp.getApplication().equals(application)) {
        Optional<Component> found = components.stream().filter(comp1 -> comp1.getName().equals(comp.getName()))
            .findFirst();
        if (found.isPresent()) {
          found.get().setState(ComponentState.PUSHED);
          found.get().setPath(path);
        } else {
          try {
            components.add(Component.of(comp.getName(), getComponentInfo(client, project, application, comp.getName(), comp.getPath(), comp.getKind()), ComponentState.NOT_PUSHED, path));
          } catch (IOException e) {
            exceptions[0] = e;
          }
        }
      }
    });
    if (exceptions[0] != null) {
      throw exceptions[0];
    }
    return components;
  }

  @Override
  public List<ServiceInstance> getServices(OpenShiftClient client, String project, String application) {
    return delegate.getServices(client, project, application);
  }

  @Override
  public List<Storage> getStorages(OpenShiftClient client, String project, String application, String context,
      String component) throws IOException {
    return delegate.getStorages(client, project, application, context, component);
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
  public void createStorage(String project, String application, String context, String component, String name,
      String mountPath, String storageSize) throws IOException {
    delegate.createStorage(project, application, context, component, name, mountPath, storageSize);
  }

  @Override
  public void deleteStorage(String project, String application, String context, String component, String storage)
      throws IOException {
    delegate.deleteStorage(project, application, context, component, storage);
  }

  @Override
  public void link(String project, String application, String component, String context, String source, Integer port)
      throws IOException {
    delegate.link(project, application, component, context, source, port);
  }

  @Override
  public void debug(String project, String application, String context, String component, Integer port)
      throws IOException {
    delegate.debug(project, application, component, context, port);
  }

  @Override
  public DebugInfo debugInfo(String project, String application, String context, String component) throws IOException {
    return delegate.debugInfo(project, application, context, component);
  }

  @Override
  public List<Project> getPreOdo10Projects(OpenShiftClient client) {
    return delegate.getPreOdo10Projects(client);
  }

  @Override
  public List<Exception> migrateProjects(OpenShiftClient client, List<Project> projects,
      BiConsumer<String, String> reporter) {
    return delegate.migrateProjects(client, projects, reporter);
  }

  @Override
  public String consoleURL(OpenShiftClient client) throws IOException {
    return delegate.consoleURL(client);
  }

  @Override
  public List<ComponentDescriptor> discover(String path) throws IOException {
    return delegate.discover(path);
  }

  @Override
  public boolean isServiceCatalogAvailable(OpenShiftClient client) {
    return delegate.isServiceCatalogAvailable(client);
  }

  @Override
  public List<DevfileRegistry> listDevfileRegistries() throws IOException {
    return delegate.listDevfileRegistries();
  }

  @Override
  public void createDevfileRegistry(String name, String url, boolean secure) throws IOException {
    delegate.createDevfileRegistry(name, url, secure);
  }

  @Override
  public void deleteDevfileRegistry(String name) throws IOException {
    delegate.deleteDevfileRegistry(name);
  }
}
