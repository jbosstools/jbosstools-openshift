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
package org.jboss.tools.openshift.internal.ui.odo;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.DoneablePersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimFluent;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretFluent;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceFluent;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigFluent;
import io.fabric8.openshift.api.model.BuildConfigList;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigFluent;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.DoneableBuildConfig;
import io.fabric8.openshift.api.model.DoneableDeploymentConfig;
import io.fabric8.openshift.api.model.DoneableImageStream;
import io.fabric8.openshift.api.model.DoneableRoute;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamFluent;
import io.fabric8.openshift.api.model.ImageStreamList;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteFluent;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.BuildConfigResource;
import io.fabric8.openshift.client.dsl.DeployableScalableResource;
import io.fabric8.servicecatalog.api.model.DoneableServiceInstance;
import io.fabric8.servicecatalog.api.model.ServiceInstance;
import io.fabric8.servicecatalog.api.model.ServiceInstanceFluent;
import io.fabric8.servicecatalog.api.model.ServiceInstanceList;
import io.fabric8.servicecatalog.client.ServiceCatalogClient;
import io.fabric8.servicecatalog.client.internal.ServiceInstanceResource;

import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.HOME_FOLDER;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP4_CONFIG_NAMESPACE;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP4_CONSOLE_PUBLIC_CONFIG_MAP_NAME;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP4_CONSOLE_URL_KEY_NAME;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP3_CONFIG_NAMESPACE;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP3_WEBCONSOLE_CONFIG_MAP_NAME;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP3_WEBCONSOLE_YAML_FILE_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jboss.tools.common.util.DownloadHelper;
import org.jboss.tools.openshift.core.odo.Application;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.ComponentDescriptorsDeserializer;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.ComponentSourceType;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.ComponentTypesDeserializer;
import org.jboss.tools.openshift.core.odo.KubernetesLabels;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.ServiceTemplatesDeserializer;
import org.jboss.tools.openshift.core.odo.Storage;
import org.jboss.tools.openshift.core.odo.StoragesDeserializer;
import org.jboss.tools.openshift.core.odo.URL;
import org.jboss.tools.openshift.internal.common.core.UsageStats;

public class OdoCli implements Odo {
  public static final String ODO_DOWNLOAD_FLAG = OdoCli.class.getName() + ".download";

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory());

  /**
   * Home sub folder for the plugin
   */
  public static final String PLUGIN_FOLDER = ".odo";

  private String command;

  private OdoCli() throws IOException {
    command = getCommand();
  }

  private static Odo INSTANCE;

  public static final Odo get() throws IOException {
    if (INSTANCE == null) {
      INSTANCE = new OdoCli();
    }
    return INSTANCE;
  }

  public String getCommand() throws IOException {
    if (command == null) {
      command = getOdoCommand();
    }
    return command;
  }

  private String getOdoCommand() throws IOException {
    return DownloadHelper.getInstance().downloadIfRequired("odo", OdoCli.class.getResource("/tools.json"));
  }
  
  private ObjectMapper configureObjectMapper(final JsonDeserializer deserializer) {
    final SimpleModule module = new SimpleModule();
    module.addDeserializer(List.class, deserializer);
    return JSON_MAPPER.registerModule(module);
  }

  @Override
  public List<Project> getProjects(OpenShiftClient client) {
    return client.projects().list().getItems();
  }

  private static String execute(File workingDirectory, String command, String ...args) throws IOException {
    String output = ExecHelper.execute(command, workingDirectory, args);
    try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
      BinaryOperator<String> reducer = new BinaryOperator<String>() {
        private boolean notificationFound = false;

        @Override
        public String apply(String s, String s2) {
          if (s2.startsWith("---")) {
            notificationFound = true;
          }
          return notificationFound?s:s+s2+"\n";
        }
      };
      return reader.lines().reduce("", reducer);
    }
  }

  private static String execute(String command, String ...args) throws IOException {
    return execute(new File(HOME_FOLDER), command, args);
  }

  @Override
  public void describeApplication(String project, String application) throws IOException {
    try {
      ExecHelper.executeWithTerminal(command, "app", "describe", application, "--project", project);
      UsageStats.getInstance().odoCommand("app describe", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("app describe", false);
      throw e;
    }
  }

  @Override
  public void deleteApplication(OpenShiftClient client, String project, String application) throws IOException {
    try {
      execute(command, "app", "delete", application, "-f", "--project", project);
      UsageStats.getInstance().odoCommand("app delete", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("app delete", false);
      throw e;
    }
  }

  @Override
  public void push(String project, String application, String context, String component) throws IOException {
    UsageStats.getInstance().push();
    try {
      ExecHelper.executeWithTerminal(new File(context), false, command, "push");
      UsageStats.getInstance().odoCommand("push", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("push", false);
      throw e;
    }
  }

  @Override
  public void describeComponent(String project, String application, String context, String component) throws IOException {
    try {
      if (context != null) {
        ExecHelper.executeWithTerminal(new File(context), command, "describe");
      } else {
        ExecHelper.executeWithTerminal(command, "describe", "--project", project, "--app", application, component);
      }
      UsageStats.getInstance().odoCommand("describe", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("describe", false);
      throw e;
    }
  }

  @Override
  public void watch(String project, String application, String context, String component) throws IOException {
    UsageStats.getInstance().watch();
    try {
      ExecHelper.executeWithTerminal(new File(context), false, command, "watch");
      UsageStats.getInstance().odoCommand("watch", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("watch", false);
      throw e;
    }
  }

  @Override
  public void createComponentLocal(String project, String application, String componentType, String componentVersion, String component, String source, boolean push) throws IOException {
    try {
      if (push) {
        ExecHelper.executeWithTerminal(new File(source), command, "create", componentType + ':' + componentVersion, component,
                "--project", project, "--app", application, "--now", "--s2i");
      } else {
        ExecHelper.executeWithTerminal(new File(source), command, "create", componentType + ':' + componentVersion, component,
                "--project", project, "--app", application, "--s2i");
      }
      UsageStats.getInstance().odoCommand("create", true);
      UsageStats.getInstance().createComponent(componentType, true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("create", false);
      UsageStats.getInstance().createComponent(componentType, false);
      throw e;
    }
  }

  @Override
  public void createComponentGit(String project, String application, String context, String componentType, String componentVersion, String component, String source, String reference, boolean push) throws IOException {
    try {
      if (StringUtils.isNotBlank(reference)) {
        if (push) {
          ExecHelper.executeWithTerminal(new File(context), command, "create", componentType + ':' + componentVersion, component,
                  "--git", source, "--ref", reference, "--project", project, "--app", application, "--now");
        } else {
          ExecHelper.executeWithTerminal(new File(context), command, "create", componentType + ':' + componentVersion, component,
                  "--git", source, "--ref", reference, "--project", project, "--app", application);
        }
      } else {
        if (push) {
          ExecHelper.executeWithTerminal(new File(context), command, "create", componentType + ':' + componentVersion, component,
                  "--git", source, "--project", project, "--app", application, "--now");
        } else {
          ExecHelper.executeWithTerminal(new File(context), command, "create", componentType + ':' + componentVersion, component,
                  "--git", source, "--project", project, "--app", application);
        }
      }
      UsageStats.getInstance().odoCommand("create", true);
      UsageStats.getInstance().createComponent(componentType, true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("create", false);
      UsageStats.getInstance().createComponent(componentType, false);
      throw e;
    }
  }

  @Override
  public void createComponentBinary(String project, String application, String context, String componentType, String componentVersion, String component, String source, boolean push) throws IOException {
    try {
      if (push) {
        ExecHelper.executeWithTerminal(new File(context), command, "create", componentType + ':' + componentVersion, component,
                "--binary", source, "--project", project, "--app", application, "--now");
      } else {
        ExecHelper.executeWithTerminal(new File(context), command, "create", componentType + ':' + componentVersion, component,
                "--binary", source, "--project", project, "--app", application);
      }
      UsageStats.getInstance().odoCommand("create", true);
      UsageStats.getInstance().createComponent(componentType, true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("create", false);
      UsageStats.getInstance().createComponent(componentType, false);
      throw e;
    }
  }

  /**
   * ensure that $HOME/.odo/config.yaml file exists so thar we can use service related commands.
   */
  private void ensureDefaultOdoConfigFileExists() {
    File dir = new File(HOME_FOLDER, PLUGIN_FOLDER);
    File config = new File(dir, "config.yaml");
    try {
      if (!config.exists()) {
        dir.mkdirs();
        config.createNewFile();
      }
    } catch (IOException e) {
    }
  }

  @Override
  public void createService(String project, String application, String serviceTemplate, String servicePlan, String service) throws IOException {
    try {
      ensureDefaultOdoConfigFileExists();
      ExecHelper.executeWithTerminal(new File(HOME_FOLDER), command, "service", "create", serviceTemplate, "--plan", servicePlan, service, "--app", application, "--project", project);
      UsageStats.getInstance().odoCommand("service create", true);
      UsageStats.getInstance().createService(serviceTemplate, true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("service create", false);
      UsageStats.getInstance().createService(serviceTemplate, false);
      throw e;
    }
  }


  @Override
  public String getServiceTemplate(OpenShiftClient client, String project, String application, String service) {
    ServiceCatalogClient sc = client.adapt(ServiceCatalogClient.class);
    return sc.serviceInstances().inNamespace(project).withName(service).get().getMetadata().getLabels().get(KubernetesLabels.NAME_LABEL);
  }

  @Override
  public void deleteService(String project, String application, String service) throws IOException {
    try {
      execute(command, "service", "delete", "--project", project, "--app", application, service, "-f");
      UsageStats.getInstance().odoCommand("service delete", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("service delete", false);
      throw e;
    }
  }

  @Override
  public List<ComponentType> getComponentTypes() throws IOException {
      try {
        List<ComponentType> componentTypes = configureObjectMapper(new ComponentTypesDeserializer()).readValue(
                execute(command, "catalog", "list", "components", "-o", "json"),
                new TypeReference<List<ComponentType>>() {});
        UsageStats.getInstance().odoCommand("catalog list components", true);
        return componentTypes;
      } catch (IOException e) {
        UsageStats.getInstance().odoCommand("catalog list components", false);
        throw e;
      }
  }

  @Override
  public List<ServiceTemplate> getServiceTemplates() throws IOException {
    try {
      List<ServiceTemplate> serviceTemplates = configureObjectMapper(new ServiceTemplatesDeserializer()).readValue(
              execute(command, "catalog", "list", "services", "-o", "json"),
              new TypeReference<List<ServiceTemplate>>() {
              });
      UsageStats.getInstance().odoCommand("catalog list services", true);
      return serviceTemplates;
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("catalog list services", false);
      throw e;
    }
  }

  @Override
  public void describeServiceTemplate(String template) throws IOException {
    try {
      ensureDefaultOdoConfigFileExists();
      ExecHelper.executeWithTerminal(command, "catalog", "describe", "service", template);
      UsageStats.getInstance().odoCommand("catalog describe service", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("catalog describe service", false);
      throw e;
    }
  }

  @Override
  public List<Integer> getServicePorts(OpenShiftClient client, String project, String application, String component) {
    Service service = client.services().inNamespace(project).withName(component + '-' + application).get();
    return service!=null?service.getSpec().getPorts().stream().map(ServicePort::getPort).collect(Collectors.toList()):new ArrayList<>();
  }

  private static List<URL> parseURLs(String json) {
    List<URL> result = new ArrayList<>();
    try {
      JsonNode root = JSON_MAPPER.readTree(json);
      JsonNode items = root.get("items");
      if (items != null) {
        items.forEach(item -> {
          //odo incorrecly reports urls created with the web ui without names
          if (item.get("metadata").has("name")) {
            result.add(URL.of(item.get("metadata").get("name").asText(), item.get("spec").has("protocol") ? item.get("spec").get("protocol").asText() : "", item.get("spec").has("host") ? item.get("spec").get("host").asText() : "", item.get("spec").has("port")?item.get("spec").get("port").asText():"0", item.get("status").get("state").asText(),item.get("spec").get("secure").asBoolean()));
          }
        });
      }
    } catch (IOException e) {
    }
    return result;
  }

  @Override
  public List<URL> listURLs(String project, String application, String context, String component) throws IOException {
    try {
      if (context != null) {
        List<URL> urls = parseURLs(execute(new File(context), command, "url", "list", "-o", "json"));
        UsageStats.getInstance().odoCommand("url list", true);
        return urls;
      } else {
        return Collections.emptyList();
      }
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("url list", false);
      throw e;
    }
  }

  @Override
  public ComponentInfo getComponentInfo(OpenShiftClient client, String project, String application, String component) throws IOException {
    List<DeploymentConfig> DCs = client.deploymentConfigs().inNamespace(project).withLabel(KubernetesLabels.COMPONENT_NAME_LABEL, component).withLabel(KubernetesLabels.APP_LABEL, application).list().getItems();
    if (DCs.size() == 1) {
      DeploymentConfig deploymentConfig = DCs.get(0);
      ComponentSourceType sourceType = ComponentSourceType.fromAnnotation(deploymentConfig.getMetadata().getAnnotations().get(KubernetesLabels.COMPONENT_SOURCE_TYPE_ANNOTATION));
      ComponentInfo.Builder builder = new ComponentInfo.Builder().withSourceType(sourceType).withComponentTypeName(deploymentConfig.getMetadata().getLabels().get(KubernetesLabels.RUNTIME_NAME_LABEL)).withComponentTypeVersion(deploymentConfig.getMetadata().getLabels().get(KubernetesLabels.RUNTIME_VERSION_LABEL)).withMigrated(deploymentConfig.getMetadata().getLabels().containsKey(KubernetesLabels.ODO_MIGRATED_LABEL));
      if (sourceType == ComponentSourceType.LOCAL) {
        return builder.build();
      } else if (sourceType == ComponentSourceType.BINARY) {
        return builder.withBinaryURL(deploymentConfig.getMetadata().getAnnotations().get(KubernetesLabels.VCS_URI_ANNOTATION)).build();
      } else {
        BuildConfig buildConfig = client.buildConfigs().inNamespace(project).withName(deploymentConfig.getMetadata().getName()).get();
        return builder.withRepositoryURL(deploymentConfig.getMetadata().getAnnotations().get(KubernetesLabels.VCS_URI_ANNOTATION)).withRepositoryReference(buildConfig.getSpec().getSource().getGit().getRef()).build();
      }
    } else {
      throw new IOException("Invalid number of deployment configs (" + DCs.size() + "), should be 1");
    }
  }

  @Override
  public void createURL(String project, String application, String context, String component, String name,
      Integer port, boolean secure) throws IOException {
    try {
      List<String> args = new ArrayList<>();
      args.add(command);
      args.add("url");
      args.add("create");
      if (StringUtils.isNotEmpty(name)) {
        args.add(name);
      }
      args.add("--port");
      args.add(port.toString());
      if (secure) {
        args.add("--secure");
      }
      ExecHelper.executeWithTerminal(new File(context), args.toArray(new String[args.size()]));
      UsageStats.getInstance().odoCommand("url create", true);
      UsageStats.getInstance().createURL(secure, true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("url create", false);
      UsageStats.getInstance().createURL(secure, false);
      throw e;
    }
  }

  @Override
  public void deleteURL(String project, String application, String context, String component, String name) throws IOException {
    try {
      execute(new File(context), command, "url", "delete", "-f", name);
      UsageStats.getInstance().odoCommand("url delete", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("url delete", false);
      throw e;
    }
  }

  private void undeployComponent(String project, String application, String context, String component, boolean deleteConfig) throws IOException {
    try {
      if (context != null) {
          execute(new File(context), command, "delete", "-f", deleteConfig?"-a":"", "--s2i");
      } else {
          execute(command, "delete", "-f", "--project", project, "--app", application, component, deleteConfig?"-a":"", "--s2i");
      }
      UsageStats.getInstance().odoCommand("delete", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("delete", false);
      throw e;
    }
}

  @Override
  public void undeployComponent(String project, String application, String context, String component) throws IOException {
    undeployComponent(project, application, context, component, false);
  }

  @Override
  public void deleteComponent(String project, String application, String context, String component, boolean undeploy) throws IOException {
    undeployComponent(project, application, context, component, true);
  }

  @Override
  public void follow(String project, String application, String context, String component) throws IOException {
    try {
      ExecHelper.executeWithTerminal(new File(context), false, command, "log", "-f");
      UsageStats.getInstance().odoCommand("log", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("log", false);
      throw e;
    }
  }

  @Override
  public void log(String project, String application, String context, String component) throws IOException {
    try {
      ExecHelper.executeWithTerminal(new File(context), command, "log");
      UsageStats.getInstance().odoCommand("log", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("log", false);
      throw e;
    }
  }

  @Override
  public void createProject(String project) throws IOException {
    try {
      execute(command, "project", "create", "-w", project);
      UsageStats.getInstance().odoCommand("project create", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("project create", false);
      throw e;
    }
  }

  @Override
  public void deleteProject(String project) throws IOException {
    try {
      execute(command, "project", "delete", project, "-f");
      UsageStats.getInstance().odoCommand("project delete", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("project delete", false);
      throw e;
    }
  }

  @Override
  public void login(String url, String userName, char[] password, String token) throws IOException {
    UsageStats.getInstance().login();
    try {
      if (token == null || token.isEmpty()) {
        execute(command, "login", url, "-u", userName, "-p", String.valueOf(password), " --insecure-skip-tls-verify");
      } else {
        execute(command, "login", url, "-t", token, " --insecure-skip-tls-verify");
      }
      UsageStats.getInstance().odoCommand("login", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("login", false);
      throw e;
    }
  }

  @Override
  public void logout() throws IOException {
    UsageStats.getInstance().logout();
    try {
      execute(command, "logout");
      UsageStats.getInstance().odoCommand("logout", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("logout", false);
      throw e;
    }
  }

  private static List<Application> parseApplications(String json) {
    List<Application> result = new ArrayList<>();
    try {
      JsonNode root = JSON_MAPPER.readTree(json);
      root.get("items").forEach(item -> result.add(Application.of(item.get("metadata").get("name").asText())));
    } catch (IOException e) {}
    return result;
  }

  @Override
  public List<Application> getApplications(String project) throws IOException {
    try {
      List<Application> applications = parseApplications(execute(command, "app", "list", "--project", project, "-o", "json"));
      UsageStats.getInstance().odoCommand("app list", true);
      return applications;
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("app list", false);
      throw e;
    }
  }

  @Override
  public List<Component> getComponents(OpenShiftClient client, String project, String application) {
    return client.deploymentConfigs().inNamespace(project).withLabelSelector(new LabelSelectorBuilder().addToMatchLabels(KubernetesLabels.APP_LABEL, application).build()).list().getItems().stream().map(dc -> Component.of(KubernetesLabels.getComponentName(dc))).collect(Collectors.toList());
  }

  @Override
  public List<ServiceInstance> getServices(OpenShiftClient client, String project, String application) {
    ServiceCatalogClient sc = client.adapt(ServiceCatalogClient.class);
    return sc.serviceInstances().inNamespace(project).withLabelSelector(new LabelSelectorBuilder().addToMatchLabels(KubernetesLabels.APP_LABEL, application).build()).list().getItems();
  }

  protected LabelSelector getLabelSelector(String application, String component) {
    return new LabelSelectorBuilder().addToMatchLabels(KubernetesLabels.APP_LABEL, application)
      .addToMatchLabels(KubernetesLabels.COMPONENT_NAME_LABEL, component)
      .build();
  }

  @Override
  public List<Storage> getStorages(OpenShiftClient client, String project, String application, String context, String component) throws IOException {
    if (context != null) {
      return configureObjectMapper(new StoragesDeserializer()).readValue(
              execute(new File(context), command, "storage", "list", "-o", "json"),
              new TypeReference<List<Storage>>() {});
    } else {
      return client.persistentVolumeClaims().inNamespace(project).withLabelSelector(getLabelSelector(application, component)).list().getItems()
                           .stream().filter(pvc -> pvc.getMetadata().getLabels().containsKey(KubernetesLabels.STORAGE_NAME_LABEL)).
                                   map(pvc -> Storage.of(Storage.getStorageName(pvc))).collect(Collectors.toList());    }
  }

  @Override
  public void listComponents() throws IOException {
    try {
      ExecHelper.executeWithTerminal(command, "catalog", "list", "components");
      UsageStats.getInstance().odoCommand("catalog list components", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("catalog list components", false);
      throw e;
    }
  }

  @Override
  public void listServices() throws IOException {
    try {
      ExecHelper.executeWithTerminal(command, "catalog", "list", "services");
      UsageStats.getInstance().odoCommand("catalog list services", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("catalog list services", false);
      throw e;
    }
  }

  @Override
  public void about() throws IOException {
    try {
      ExecHelper.executeWithTerminal(command, "version");
      UsageStats.getInstance().odoCommand("version", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("version", false);
      throw e;
    }
  }

  @Override
  public void createStorage(String project, String application, String context, String component, String name, String mountPath, String storageSize) throws IOException {
    try {
      execute(new File(context), command, "storage", "create", name, "--path", mountPath, "--size", storageSize);
      UsageStats.getInstance().odoCommand("storage create", true);
      UsageStats.getInstance().createStorage(storageSize, true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("storage create", false);
      UsageStats.getInstance().createStorage(storageSize, false);
      throw e;
    }
  }

  @Override
  public void deleteStorage(String project, String application, String context, String component, String storage) throws IOException {
    try {
      execute(new File(context), command, "storage", "delete", storage, "-f");
      UsageStats.getInstance().odoCommand("storage delete", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("storage delete", false);
      throw e;
    }
  }

  @Override
  public void link(String project, String application, String component, String context, String source, Integer port) throws IOException {
    UsageStats.getInstance().link();
    try {
      if (port != null) {
        execute(new File(context), command, "link", source, "--port", port.toString(), "--wait");
      } else {
        execute(new File(context), command, "link", source, "--wait");
      }
      UsageStats.getInstance().odoCommand("link", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("link", false);
      throw e;
    }
  }

  @Override
  public void debug(String project, String application, String context, String component, Integer port) throws IOException {
    UsageStats.getInstance().debug();
    try {
      ExecHelper.executeWithTerminal(new File(component), false, command, "debug", "port-forward", "--local-port", port.toString());
      UsageStats.getInstance().odoCommand("debug port-forward", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("debug port-forward", false);
      throw e;
    }
  }

  @Override
  public List<Project> getPreOdo10Projects(OpenShiftClient client) {
    return getProjects(client).stream().filter(project -> isLegacyProject(client, project)).collect(Collectors.toList());
  }

  private boolean isLegacyProject(OpenShiftClient client, Project project) {
    boolean hasLegacyResources = !client.deploymentConfigs().inNamespace(project.getMetadata().getName()).withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems().isEmpty();
    if (!hasLegacyResources) {
      try {
        hasLegacyResources = !client.adapt(ServiceCatalogClient.class).serviceInstances().inNamespace(project.getMetadata().getName()).withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems().isEmpty();
      } catch (Exception e) {}
    }
    return hasLegacyResources;
  }

  @Override
  public List<Exception> migrateProjects(OpenShiftClient client, List<Project> projects, BiConsumer<String, String> reporter) {
    List<Exception> exceptions = new ArrayList<>();
    for(Project project : projects) {
      reporter.accept(project.getMetadata().getName(), "deployment configs");
      migrateDCs(client.deploymentConfigs().inNamespace(project.getMetadata().getName()), exceptions);
      reporter.accept(project.getMetadata().getName(), "routes");
      migrateRoutes(client.routes().inNamespace(project.getMetadata().getName()), exceptions);
      reporter.accept(project.getMetadata().getName(), "build configs");
      migrateBuildConfigs(client.buildConfigs().inNamespace(project.getMetadata().getName()), exceptions);
      reporter.accept(project.getMetadata().getName(), "image streams");
      migrateImageStreams(client.imageStreams().inNamespace(project.getMetadata().getName()), exceptions);
      reporter.accept(project.getMetadata().getName(), "services");
      migrateServices(client.services().inNamespace(project.getMetadata().getName()), exceptions);
      reporter.accept(project.getMetadata().getName(), "storages");
      migratePVCs(client.persistentVolumeClaims().inNamespace(project.getMetadata().getName()), exceptions);
      reporter.accept(project.getMetadata().getName(), "secrets");
      migrateSecrets(client.secrets().inNamespace(project.getMetadata().getName()), exceptions);
      reporter.accept(project.getMetadata().getName(), "service instances");
      migrateServiceInstances(client.adapt(ServiceCatalogClient.class).serviceInstances().inNamespace(project.getMetadata().getName()), exceptions);
    }
    return exceptions;
  }

  private void editLabels(Map<String, String> labels) {
    String name = labels.get(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10);
    if (name != null) {
      labels.put(KubernetesLabels.COMPONENT_NAME_LABEL, name);
    }
    name = labels.get(KubernetesLabels.NAME_LABEL);
    if (name != null) {
      labels.put(KubernetesLabels.APP_LABEL, name);
    }
    name = labels.get(KubernetesLabels.COMPONENT_TYPE_LABEL);
    if (name != null) {
      labels.put(KubernetesLabels.NAME_LABEL, name);
    }
    name = labels.get(KubernetesLabels.COMPONENT_VERSION_LABEL);
    if (name != null) {
      labels.put(KubernetesLabels.RUNTIME_VERSION_LABEL, name);
    }
    name = labels.get(KubernetesLabels.URL_NAME_LABEL);
    if (name != null) {
      labels.put(KubernetesLabels.ODO_URL_NAME, name);
    }
    labels.put(KubernetesLabels.ODO_MIGRATED_LABEL, "true");
    labels.remove(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10);
  }

  private void migrateDCs(NonNamespaceOperation<DeploymentConfig, DeploymentConfigList, DoneableDeploymentConfig, DeployableScalableResource<DeploymentConfig, DoneableDeploymentConfig>> operation, List<Exception> exceptions) {
    try {
      for(HasMetadata dc : operation.withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems()) {
        try {
          DeploymentConfigFluent.MetadataNested<DoneableDeploymentConfig> edit = operation.withName(dc.getMetadata().getName()).edit().editMetadata();
          editLabels(edit.getLabels());
          edit.endMetadata().done();
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
    } catch (Exception e) {
      exceptions.add(e);
    }
  }


  private void migrateRoutes(NonNamespaceOperation<Route, RouteList, DoneableRoute, Resource<Route, DoneableRoute>> operation, List<Exception> exceptions) {
    try {
      for(HasMetadata dc : operation.withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems()) {
        try {
          RouteFluent.MetadataNested<DoneableRoute> edit = operation.withName(dc.getMetadata().getName()).edit().editMetadata();
          editLabels(edit.getLabels());
          edit.endMetadata().done();
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
    } catch (Exception e) {
      exceptions.add(e);
    }
  }

  private void migrateBuildConfigs(NonNamespaceOperation<BuildConfig, BuildConfigList, DoneableBuildConfig, BuildConfigResource<BuildConfig, DoneableBuildConfig, Void, Build>> operation, List<Exception> exceptions) {
    try {
      for(HasMetadata dc : operation.withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems()) {
        try {
          BuildConfigFluent.MetadataNested<DoneableBuildConfig> edit = operation.withName(dc.getMetadata().getName()).edit().editMetadata();
          editLabels(edit.getLabels());
          edit.endMetadata().done();
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
    } catch (Exception e) {
      exceptions.add(e);
    }
  }

  private void migrateImageStreams(NonNamespaceOperation<ImageStream, ImageStreamList, DoneableImageStream, Resource<ImageStream, DoneableImageStream>> operation, List<Exception> exceptions) {
    try {
      for(HasMetadata dc : operation.withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems()) {
        try {
          ImageStreamFluent.MetadataNested<DoneableImageStream> edit = operation.withName(dc.getMetadata().getName()).edit().editMetadata();
          editLabels(edit.getLabels());
          edit.endMetadata().done();
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
    } catch (Exception e) {
      exceptions.add(e);
    }
  }

  private void migrateServices(NonNamespaceOperation<Service, ServiceList, DoneableService, ServiceResource<Service, DoneableService>> operation, List<Exception> exceptions) {
    try {
      for(HasMetadata dc : operation.withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems()) {
        try {
          ServiceFluent.MetadataNested<DoneableService> edit = operation.withName(dc.getMetadata().getName()).edit().editMetadata();
          editLabels(edit.getLabels());
          edit.endMetadata().done();
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
    } catch (Exception e) {
      exceptions.add(e);
    }
  }

  private void migratePVCs(NonNamespaceOperation<PersistentVolumeClaim, PersistentVolumeClaimList, DoneablePersistentVolumeClaim, Resource<PersistentVolumeClaim, DoneablePersistentVolumeClaim>> operation, List<Exception> exceptions) {
    try {
      for (HasMetadata dc : operation.withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems()) {
        try {
          PersistentVolumeClaimFluent.MetadataNested<DoneablePersistentVolumeClaim> edit = operation.withName(dc.getMetadata().getName()).edit().editMetadata();
          editLabels(edit.getLabels());
          edit.endMetadata().done();
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
    } catch (Exception e) {
      exceptions.add(e);
    }
  }

  private void migrateSecrets(NonNamespaceOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>> operation, List<Exception> exceptions) {
    try {
      for(HasMetadata dc : operation.withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems()) {
        try {
          SecretFluent.MetadataNested<DoneableSecret> edit = operation.withName(dc.getMetadata().getName()).edit().editMetadata();
          editLabels(edit.getLabels());
          edit.endMetadata().done();
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
    } catch (Exception e) {
      exceptions.add(e);
    }
  }

  private void migrateServiceInstances(NonNamespaceOperation<ServiceInstance, ServiceInstanceList, DoneableServiceInstance, ServiceInstanceResource> operation, List<Exception> exceptions) {
    try {
      for(HasMetadata dc : operation.withLabel(KubernetesLabels.COMPONENT_NAME_LABEL_PRE10).list().getItems()) {
        try {
          ServiceInstanceFluent.MetadataNested<DoneableServiceInstance> edit = operation.withName(dc.getMetadata().getName()).edit().editMetadata();
          editLabels(edit.getLabels());
          edit.endMetadata().done();
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
    } catch (Exception e) {
      //TODO: exception is skipped because of non catalog aware cluster, need to find a way to better deal with that
    }
  }

  @Override
  public String consoleURL(OpenShiftClient client) throws IOException {
    try {
      VersionInfo version = client.getVersion();
      if (version == null || "4".equals(version.getMajor())) { // assuming null version is version 4
        ConfigMap configMap = client.configMaps().inNamespace(OCP4_CONFIG_NAMESPACE).withName(OCP4_CONSOLE_PUBLIC_CONFIG_MAP_NAME).get();
        if (configMap != null) {
          return configMap.getData().get(OCP4_CONSOLE_URL_KEY_NAME);
        }
      } else if ("3".equals(version.getMajor())) {
        ConfigMap configMap = client.configMaps().inNamespace(OCP3_CONFIG_NAMESPACE).withName(OCP3_WEBCONSOLE_CONFIG_MAP_NAME).get();
        String yaml = configMap.getData().get(OCP3_WEBCONSOLE_YAML_FILE_NAME);
        return JSON_MAPPER.readTree(yaml).path("clusterInfo").path("consolePublicURL").asText();
      }
      return client.getMasterUrl() + "console";
    } catch (KubernetesClientException e) {
      return client.getMasterUrl().toExternalForm();
    }
  }
  
  @Override
  public List<ComponentDescriptor> discover(String path) throws IOException {
    return configureObjectMapper(new ComponentDescriptorsDeserializer()).readValue(
            execute(new File(path), command, "list", "--path", ".", "-o", "json"),
            new TypeReference<List<ComponentDescriptor>>() {});
  }
  
  @Override
  public boolean isServiceCatalogAvailable(OpenShiftClient client) {
    return client.isAdaptable(ServiceCatalogClient.class);
  }
}
