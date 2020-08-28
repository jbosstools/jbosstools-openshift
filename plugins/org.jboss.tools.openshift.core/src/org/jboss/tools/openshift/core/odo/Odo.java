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
package org.jboss.tools.openshift.core.odo;

import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.servicecatalog.api.model.ServiceInstance;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

public interface Odo {
    List<Project> getProjects(OpenShiftClient client);

    List<Project> getPreOdo10Projects(OpenShiftClient client);

    List<Exception> migrateProjects(OpenShiftClient client, List<Project> projects, BiConsumer<String, String> reporter);

    void describeApplication(String project, String application) throws IOException;

    void deleteApplication(OpenShiftClient client, String project, String application) throws IOException;

    void push(String project, String application, String context, String component) throws IOException;

    void describeComponent(String project, String application, String context, String component) throws IOException;

    void watch(String project, String application, String context, String component) throws IOException;

    void createComponentLocal(String project, String application, String componentType, String componentVersion, String component, String source, boolean push) throws IOException;

    void createComponentGit(String project, String application, String context, String componentType, String componentVersion, String component, String source, String reference, boolean push) throws IOException;

    void createComponentBinary(String project, String application, String context, String componentType, String componentVersion, String component, String source, boolean push) throws IOException;

    void createService(String project, String application, String serviceTemplate, String servicePlan, String service) throws IOException;

    String getServiceTemplate(OpenShiftClient client, String project, String application, String service);

    void deleteService(String project, String application, String service) throws IOException;

    List<ComponentType> getComponentTypes() throws IOException;

    List<ServiceTemplate> getServiceTemplates() throws IOException;

    void describeServiceTemplate(String template) throws IOException;

    List<Integer> getServicePorts(OpenShiftClient client, String project, String application, String component);

    List<URL> listURLs(String project, String application, String context, String component) throws IOException;

    ComponentInfo getComponentInfo(OpenShiftClient client, String project, String application, String component) throws IOException;

    void createURL(String project, String application, String context, String component, String name, Integer port, boolean secure) throws IOException;

    void deleteURL(String project, String application, String context, String component, String name) throws IOException;

    void undeployComponent(String project, String application, String context, String component) throws IOException;

    void deleteComponent(String project, String application, String context, String component, boolean undeploy) throws IOException;

    void follow(String project, String application, String context, String component) throws IOException;

    void log(String project, String application, String context, String component) throws IOException;

    void createProject(String project) throws IOException;

    void deleteProject(String project) throws IOException;

    void login(String url, String userName, char[] password, String token) throws IOException;

    void logout() throws IOException;

    List<Application> getApplications(String project) throws IOException;

    List<Component> getComponents(OpenShiftClient client, String project, String application);

    List<ServiceInstance> getServices(OpenShiftClient client, String project, String application);

    List<Storage> getStorages(OpenShiftClient client, String project, String application, String context, String component) throws IOException;

    void listComponents() throws IOException;

    void listServices() throws IOException;

    void about() throws IOException;

    void createStorage(String project, String application, String context, String component, String name, String mountPath, String storageSize) throws IOException;

    void deleteStorage(String project, String application, String context, String component, String storage) throws IOException;

    void link(String project, String application, String component, String context, String source, Integer port) throws IOException;

    String consoleURL(OpenShiftClient client) throws IOException;

    void debug(String project, String application, String context, String component, Integer port) throws IOException;
    
    List<ComponentDescriptor> discover(String path) throws IOException;

}
