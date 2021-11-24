/*******************************************************************************
 * Copyright (c) 2019-2021 Red Hat, Inc.
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
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.OpenShiftClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.core.OpenShiftCoreConstants.DebugStatus;
import org.jboss.tools.openshift.core.odo.Application;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.ComponentDescriptorsDeserializer;
import org.jboss.tools.openshift.core.odo.ComponentDeserializer;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.ComponentKind;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.ComponentTypeInfo;
import org.jboss.tools.openshift.core.odo.ComponentTypesDeserializer;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.DevfileRegistriesDeserializer;
import org.jboss.tools.openshift.core.odo.DevfileRegistry;
import org.jboss.tools.openshift.core.odo.DebugInfo;
import org.jboss.tools.openshift.core.odo.JSonParser;
import org.jboss.tools.openshift.core.odo.KubernetesLabels;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.OperatorCRD;
import org.jboss.tools.openshift.core.odo.ServiceDeserializer;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.ServiceTemplatesDeserializer;
import org.jboss.tools.openshift.core.odo.Storage;
import org.jboss.tools.openshift.core.odo.StoragesDeserializer;
import org.jboss.tools.openshift.core.odo.URL;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.odo.ExecHelper.ExecResult;

public class OdoCli implements Odo {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory());
  
  private static final String METADATA_FIELD = "metadata";
  private static final String NAME_FIELD = "name";
  private static final String NAMESPACE_FIELD = "namespace";
  private static final String SPEC_FIELD = "spec";
  
  /**
   * Home sub folder for the plugin
   */
  public static final String PLUGIN_FOLDER = ".odo";

  private final String command;
  private Map<String, String> envVars;
  private final KubernetesClient client;
  private String namespace;
  
  private final AtomicBoolean swaggerLoaded = new AtomicBoolean();

  private JSonParser swagger;

  
  private static String buildHttpProxy(IProxyData data) {
    StringBuilder builder = new StringBuilder();
    
    builder.append("http://");
    if (data.isRequiresAuthentication()) {
      builder.append(data.getUserId()).append(':').append(data.getPassword()).append('@');
    }
    builder.append(data.getHost()).append(':').append(data.getPort());
    return builder.toString();
  }
  
  private IProxyData getProxyData(URI uri) {
    IProxyData data = null;
    IProxyService proxyService = OpenShiftUIActivator.getDefault().getProxyService();
    if (proxyService != null) {
      IProxyData[] datas = proxyService.select(uri);
      if (datas != null && datas.length > 0) {
        data = datas[0];
      }
    }
    return data;
  }
  
  protected Config getConfig() {
    ConfigBuilder builder = new ConfigBuilder();
    try {
      IProxyData data = getProxyData(new URI(builder.getMasterUrl()));
      if (data != null) {
        if (data.isRequiresAuthentication()) {
          builder.withProxyUsername(data.getUserId());
          builder.withProxyPassword(data.getPassword());
        }
        builder.withHttpsProxy("http://" + data.getHost() + ':' + data.getPort());
      }
    } catch (URISyntaxException e) {
      OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
    }
    return builder.build();
  }
  
  protected Map<String, String> buildEnvVars() {
    final String HTTP_PROXY = "HTTP_PROXY";
    final String HTTPS_PROXY = "HTTPS_PROXY";
    final String ALL_PROXY = "ALL_PROXY";
    final Set<String> proxyEnvironmentVariables = new HashSet<>(Arrays.asList(HTTP_PROXY, HTTPS_PROXY, ALL_PROXY));
    Map<String, String> vars = new HashMap<>();
    vars.put("ODO_DISABLE_TELEMETRY", "true");
    try {
      IProxyData data = getProxyData(getMasterUrl().toURI());
      if (data != null) {
          final String envVarValue = buildHttpProxy(data);
          proxyEnvironmentVariables.forEach(envVarName -> {
            vars.put(envVarName, envVarValue);
            vars.put(envVarName.toLowerCase(), envVarValue);
          });
      }
    } catch (URISyntaxException e) {
      OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
    }
    return vars;
  }

  OdoCli(String command) {
    this.command = command;
    this.client = new DefaultKubernetesClient(getConfig());
    this.envVars = buildEnvVars();
    reportTelemetry();
  }
  
  private void reportTelemetry() {
    try {
      ClusterInfo info = ClusterHelper.getClusterInfo(client);
      UsageStats.getInstance().kubernetesVersion(info.getKubernetesVersion());
      UsageStats.getInstance().isOpenShift(info.isOpenshift());
      UsageStats.getInstance().openshiftVersion(info.getOpenshiftVersion());
    }catch (KubernetesClientException e) {
      // WARNING only as it can be no route to host errors and only impact telemetry info
      OpenShiftUIActivator.log(IStatus.WARNING, e.getLocalizedMessage(), e);
    }
  }


  private ObjectMapper configureObjectMapper(final JsonDeserializer deserializer) {
    final SimpleModule module = new SimpleModule();
    module.addDeserializer(List.class, deserializer);
    return JSON_MAPPER.registerModule(module);
  }

  @Override
  public List<String> getNamespaces() throws IOException {
    try {
      if (client.isAdaptable(OpenShiftClient.class)) {
        return client.adapt(OpenShiftClient.class).projects().list().getItems().stream().
            map(p -> p.getMetadata().getName()).collect(Collectors.toList());
      } else {
        return client.namespaces().list().getItems().stream().
            map(n -> n.getMetadata().getName()).collect(Collectors.toList());
      }
    } catch (KubernetesClientException e) {
      throw new IOException(e);
    }
  }
  
  protected String validateNamespace(String ns) {
    if (StringUtils.isEmpty(ns)) {
      ns = "default";
    }
    try {
      if (client.isAdaptable(OpenShiftClient.class)) {
        client.adapt(OpenShiftClient.class).projects().withName(ns).get();
      } else {
        client.namespaces().withName(ns).get();
      }
    } catch (KubernetesClientException e) {
      ns = "";
      if (client.isAdaptable(OpenShiftClient.class)) {
        List<Project> projects = client.adapt(OpenShiftClient.class).projects().list().getItems();
        if (!projects.isEmpty()) {
          ns = projects.get(0).getMetadata().getNamespace();
        }
      } else {
        List<Namespace> namespaces = client.namespaces().list().getItems();
        if (!namespaces.isEmpty()) {
          ns = namespaces.get(0).getMetadata().getNamespace();
        }
      }
    }
    return ns;
  }
  
  @Override
  public String getNamespace() {
    if (namespace == null) {
      namespace = validateNamespace(client.getNamespace());
    }
    return "".equals(namespace)?null:namespace;
  }


  private static String execute(File workingDirectory, String command, Map<String, String> envs, String ...args) throws IOException {
    ExecResult result = ExecHelper.execute(command, workingDirectory, envs, args);
    try (BufferedReader reader = new BufferedReader(new StringReader(result.getStdOut()))) {
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

  private static String execute(String command, Map<String, String> envs, String ...args) throws IOException {
    return execute(new File(HOME_FOLDER), command, envs, args);
  }

  @Override
  public void describeApplication(String project, String application) throws IOException {
    try {
      ExecHelper.executeWithTerminal(envVars, command, "app", "describe", application, "--project", project);
      UsageStats.getInstance().odoCommand("app describe", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("app describe", false);
      throw e;
    }
  }

  @Override
  public void deleteApplication(String project, String application) throws IOException {
    try {
      execute(command, envVars, "app", "delete", application, "-f", "--project", project);
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
      ExecHelper.executeWithTerminal(new File(context), false, envVars, command, "push");
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
        ExecHelper.executeWithTerminal(new File(context), envVars, command, "describe");
      } else {
        ExecHelper.executeWithTerminal(envVars, command, "describe", "--project", project, "--app", application, component);
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
      ExecHelper.executeWithTerminal(new File(context), false, envVars, command, "watch");
      UsageStats.getInstance().odoCommand("watch", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("watch", false);
      throw e;
    }
  }

  @Override
  public void createComponentLocal(String project, String application, String componentType, String componentVersion, String registryName, String component, String source, String devfile, String starter, boolean push) throws IOException {
    try {
      List<String> args = new ArrayList<>();
      args.add(command);
      args.add("create");
      if (StringUtils.isNotBlank(devfile)) {
        args.add("--devfile");
        args.add(devfile);
      } else if (StringUtils.isNotBlank(componentVersion)) {
          args.add(componentType + ":" + componentVersion);
      } else {
        if (StringUtils.isNotBlank(starter)) {
          args.add("--starter=" + starter);
        }
        args.add(componentType);
        args.add("--registry");
        args.add(registryName);
      }
      args.add(component);
      args.add("--project");
      args.add(project);
      args.add("--app");
      args.add(application);
      if (StringUtils.isNotBlank(componentVersion)) {
          args.add("--s2i");
      }
      if (push) {
          args.add("--now");
      }
      ExecHelper.executeWithTerminal(new File(source), envVars, args.toArray(new String[0]));
      UsageStats.getInstance().odoCommand("create", true);
      String componentStatPrefix = StringUtils.isNotBlank(componentVersion) ? "s2i:" : "devfile:";
      UsageStats.getInstance().createComponent(componentStatPrefix + componentType, true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("create", false);
      String componentStatPrefix = StringUtils.isNotBlank(componentVersion) ? "s2i:" : "devfile:";
      UsageStats.getInstance().createComponent(componentStatPrefix + componentType, false);
      throw e;
    }
  }

  @Override
  public void createComponentGit(String project, String application, String context, String componentType, String componentVersion, String component, String source, String reference, boolean push) throws IOException {
    try {
      if (StringUtils.isNotBlank(reference)) {
        if (push) {
          ExecHelper.executeWithTerminal(new File(context), envVars, command, "create", componentType + ':' + componentVersion, component,
                  "--git", source, "--ref", reference, "--project", project, "--app", application, "--now");
        } else {
          ExecHelper.executeWithTerminal(new File(context), envVars, command, "create", componentType + ':' + componentVersion, component,
                  "--git", source, "--ref", reference, "--project", project, "--app", application);
        }
      } else {
        if (push) {
          ExecHelper.executeWithTerminal(new File(context), envVars, command, "create", componentType + ':' + componentVersion, component,
                  "--git", source, "--project", project, "--app", application, "--now");
        } else {
          ExecHelper.executeWithTerminal(new File(context), envVars, command, "create", componentType + ':' + componentVersion, component,
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
        ExecHelper.executeWithTerminal(new File(context), envVars, command, "create", componentType + ':' + componentVersion, component,
                "--binary", source, "--project", project, "--app", application, "--now");
      } else {
        ExecHelper.executeWithTerminal(new File(context), envVars, command, "create", componentType + ':' + componentVersion, component,
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
  
  private CustomResourceDefinitionContext toCustomResourceDefinitionContext(OperatorCRD crd) {
    String group = crd.getName().substring(crd.getName().indexOf('.') + 1);
    String plural = crd.getName().substring(0, crd.getName().indexOf('.'));
    return new CustomResourceDefinitionContext.Builder().withName(crd.getName()).withGroup(group)
        .withScope("Namespaced").withKind(crd.getKind()).withPlural(plural).withVersion(crd.getVersion()).build();
  }

  private CustomResourceDefinitionContext toCustomResourceDefinitionContext(org.jboss.tools.openshift.core.odo.Service service) {
    String version = service.getApiVersion().substring(service.getApiVersion().indexOf('/') + 1);
    String group = service.getApiVersion().substring(0, service.getApiVersion().indexOf('/'));
    return new CustomResourceDefinitionContext.Builder().withName(service.getKind().toLowerCase() + "s." + group)
        .withGroup(group).withScope("Namespaced").withKind(service.getKind())
        .withPlural(service.getKind().toLowerCase() + "s").withVersion(version).build();
  }
  
  private void updatePayload(JsonNode node, JsonNode spec, String project, String service) {
    ((ObjectNode) node.get(METADATA_FIELD)).set(NAME_FIELD, JSON_MAPPER.getNodeFactory().textNode(service));
    ((ObjectNode) node.get(METADATA_FIELD)).set(NAMESPACE_FIELD, JSON_MAPPER.getNodeFactory().textNode(project));
    if (spec != null) {
      ((ObjectNode)node).set(SPEC_FIELD, spec);
    }
  }  

  @Override
  public void createService(String project, String application, ServiceTemplate serviceTemplate, OperatorCRD serviceCRD, String service,
      ObjectNode spec, boolean wait) throws IOException {
    try {
      CustomResourceDefinitionContext context = toCustomResourceDefinitionContext(serviceCRD);
      ObjectNode payload = serviceCRD.getSample().deepCopy();
      updatePayload(payload, spec, project, service);
      client.customResource(context).create(project, JSON_MAPPER.writeValueAsString(payload));
      UsageStats.getInstance().odoCommand("service create", true);
      UsageStats.getInstance().createService(serviceTemplate.getName(), true);
    } catch (KubernetesClientException e) {
        UsageStats.getInstance().odoCommand("service create", false);
        UsageStats.getInstance().createService(serviceTemplate.getName(), false);
        throw new IOException(e.getLocalizedMessage(), e);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("service create", false);
      UsageStats.getInstance().createService(serviceTemplate.getName(), false);
      throw e;
    }
  }


  @Override
  public String getServiceTemplate(String project, String application, String service) throws IOException {
    throw new IOException("Not implemented by odo yet");
  }

  @Override
  public void deleteService(String project, String application, org.jboss.tools.openshift.core.odo.Service service) throws IOException {
    try {
      CustomResourceDefinitionContext context = toCustomResourceDefinitionContext(service);
      client.customResource(context).delete(project, service.getName());
      UsageStats.getInstance().odoCommand("service delete", true);
    } catch (KubernetesClientException e) {
      UsageStats.getInstance().odoCommand("service delete", false);
      throw e;
    }
  }

  @Override
  public List<ComponentType> getComponentTypes() throws IOException {
      try {
        List<ComponentType> componentTypes = configureObjectMapper(new ComponentTypesDeserializer()).readValue(
                execute(command, envVars, "catalog", "list", "components", "-o", "json"),
                new TypeReference<List<ComponentType>>() {});
        UsageStats.getInstance().odoCommand("catalog list components", true);
        return componentTypes;
      } catch (IOException e) {
        UsageStats.getInstance().odoCommand("catalog list components", false);
        throw e;
      }
  }
  
  @Override
  public List<DevfileComponentType> getComponentTypes(String registryName) throws IOException {
    return getComponentTypes().stream().
        filter(type -> type instanceof DevfileComponentType).
        map(type -> (DevfileComponentType)type).
        filter(type -> registryName.equals(type.getDevfileRegistry().getName())).
        collect(Collectors.toList());
  }
  
  private ComponentTypeInfo parseComponentTypeInfo(String json, String registryName) throws IOException {
    JSonParser parser = new JSonParser(JSON_MAPPER.readTree(json));
    return parser.parseComponentTypeInfo(registryName);
  }
  
  @Override
  public ComponentTypeInfo getComponentTypeInfo(String componentType, String registryName) throws IOException {
    return parseComponentTypeInfo(execute(command, envVars, "catalog", "describe", "component", componentType, "-o", "json"), registryName);
  }
  
  private void loadSwagger() {
    try {
        Request req = new Request.Builder().get().url(new java.net.URL(client.getMasterUrl(), "/openapi/v2")).build();
        Response response = client.adapt(OkHttpClient.class).newCall(req).execute();
        if (response.isSuccessful()) {
            swagger = new JSonParser(new ObjectMapper().readTree(response.body().charStream()));
        }
    } catch (IOException e) {
        OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
    }
}

private ObjectNode findSchema(String crd) {
    try {
        if (swaggerLoaded.compareAndSet(false, true)) {
            loadSwagger();
        }
        if (swagger != null) {
            return swagger.findSchema("/apis/" + crd);
        }
    } catch (IOException e) {}
    return null;
}


  @Override
  public List<ServiceTemplate> getServiceTemplates() throws IOException {
    try {
      List<ServiceTemplate> serviceTemplates = configureObjectMapper(new ServiceTemplatesDeserializer(this::findSchema)).readValue(
              execute(command, envVars, "catalog", "list", "services", "-o", "json"),
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
      ExecHelper.executeWithTerminal(envVars, command, "catalog", "describe", "service", template);
      UsageStats.getInstance().odoCommand("catalog describe service", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("catalog describe service", false);
      throw e;
    }
  }

  @Override
  public List<Integer> getServicePorts(String project, String application, String component) {
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
        List<URL> urls = parseURLs(execute(new File(context), command, envVars, "url", "list", "-o", "json"));
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
  
  private ComponentInfo parseComponentInfo(String json, ComponentKind kind) throws IOException {
    JSonParser parser = new JSonParser(JSON_MAPPER.readTree(json));
    return parser.parseComponentInfo(kind);
}

  @Override
  public ComponentInfo getComponentInfo(String project, String application, String component, String path, ComponentKind kind) throws IOException {
    return parseComponentInfo(execute(new File(path), command, envVars, "describe", "-o", "json"), kind);
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
      ExecHelper.executeWithTerminal(new File(context), envVars, args.toArray(new String[args.size()]));
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
      execute(new File(context), command, envVars, "url", "delete", "-f", name);
      UsageStats.getInstance().odoCommand("url delete", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("url delete", false);
      throw e;
    }
  }

  private void undeployComponent(String project, String application, String context, String component, boolean deleteConfig, ComponentKind kind) throws IOException {
    try {
      List<String> args = new ArrayList<>();
      args.add("delete");
      args.add("-f");
      if (deleteConfig) {
          args.add("-a");
      }
      if (context != null) {
          execute(new File(context), command, envVars, args.toArray(new String[0]));
      } else {
          args.add("--project");
          args.add(project);
          args.add("--app");
          args.add(application);
          args.add(component);
          execute(command, envVars, args.toArray(new String[0]));
      }
      UsageStats.getInstance().odoCommand("delete", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("delete", false);
      throw e;
    }
}

  @Override
  public void undeployComponent(String project, String application, String context, String component, ComponentKind kind) throws IOException {
    undeployComponent(project, application, context, component, false, kind);
  }

  @Override
  public void deleteComponent(String project, String application, String context, String component, ComponentKind kind) throws IOException {
    undeployComponent(project, application, context, component, context != null, kind);
  }

  @Override
  public void follow(String project, String application, String context, String component) throws IOException {
    try {
      ExecHelper.executeWithTerminal(new File(context!=null?context:HOME_FOLDER), false, envVars, command, "log", "-f");
      UsageStats.getInstance().odoCommand("log", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("log", false);
      throw e;
    }
  }

  @Override
  public void log(String project, String application, String context, String component) throws IOException {
    try {
      ExecHelper.executeWithTerminal(new File(context!=null?context:HOME_FOLDER), envVars, command, "log");
      UsageStats.getInstance().odoCommand("log", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("log", false);
      throw e;
    }
  }

  @Override
  public void createProject(String project) throws IOException {
    try {
      execute(command, envVars, "project", "create", "-w", project);
      UsageStats.getInstance().odoCommand("project create", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("project create", false);
      throw e;
    }
  }

  @Override
  public void deleteProject(String project) throws IOException {
    try {
      execute(command, envVars, "project", "delete", project, "-f");
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
        execute(command, envVars, "login", url, "-u", userName, "-p", String.valueOf(password), " --insecure-skip-tls-verify");
      } else {
        execute(command, envVars, "login", url, "-t", token, " --insecure-skip-tls-verify");
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
      execute(command, envVars, "logout");
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
      List<Application> applications = parseApplications(execute(command, envVars, "app", "list", "--project", project, "-o", "json"));
      UsageStats.getInstance().odoCommand("app list", true);
      return applications;
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("app list", false);
      throw e;
    }
  }

  @Override
  public List<Component> getComponents(String project, String application) throws IOException {
    return configureObjectMapper(new ComponentDeserializer()).readValue(
        execute(command, envVars, "list", "--app", application, "--project", project, "-o", "json"),
        new TypeReference<List<Component>>() {
        });
  }

  @Override
  public List<org.jboss.tools.openshift.core.odo.Service> getServices(String project, String application) throws IOException {
    try {
      return configureObjectMapper(new ServiceDeserializer()).readValue(
          execute(command, envVars, "service", "list", "--app", application, "--project", project, "-o", "json"),
          new TypeReference<List<org.jboss.tools.openshift.core.odo.Service>>() {
          });
    } catch (IOException e) {
      // https://github.com/openshift/odo/issues/5010
      if (e.getMessage().contains("\"no operator backed services found in namespace:")) {
        return Collections.emptyList();
      }
      throw e;
    }
  }

  protected LabelSelector getLabelSelector(String application, String component) {
    return new LabelSelectorBuilder().addToMatchLabels(KubernetesLabels.APP_LABEL, application)
      .addToMatchLabels(KubernetesLabels.COMPONENT_NAME_LABEL, component)
      .build();
  }

  @Override
  public List<Storage> getStorages(String project, String application, String context, String component) throws IOException {
    if (context != null) {
      return configureObjectMapper(new StoragesDeserializer()).readValue(
              execute(new File(context), command, envVars, "storage", "list", "-o", "json"),
              new TypeReference<List<Storage>>() {});
    } else {
      return client.persistentVolumeClaims().inNamespace(project).withLabelSelector(getLabelSelector(application, component)).list().getItems()
                           .stream().filter(pvc -> pvc.getMetadata().getLabels().containsKey(KubernetesLabels.STORAGE_NAME_LABEL)).
                                   map(pvc -> Storage.of(Storage.getStorageName(pvc))).collect(Collectors.toList());    }
  }

  @Override
  public void listComponents() throws IOException {
    try {
      ExecHelper.executeWithTerminal(envVars, command, "catalog", "list", "components");
      UsageStats.getInstance().odoCommand("catalog list components", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("catalog list components", false);
      throw e;
    }
  }

  @Override
  public void listServices() throws IOException {
    try {
      ExecHelper.executeWithTerminal(envVars, command,"catalog", "list", "services");
      UsageStats.getInstance().odoCommand("catalog list services", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("catalog list services", false);
      throw e;
    }
  }

  @Override
  public void about() throws IOException {
    try {
      ExecHelper.executeWithTerminal(envVars, command, "version");
      UsageStats.getInstance().odoCommand("version", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("version", false);
      throw e;
    }
  }

  @Override
  public void createStorage(String project, String application, String context, String component, String name, String mountPath, String storageSize) throws IOException {
    try {
      execute(new File(context), command, envVars, "storage", "create", name, "--path", mountPath, "--size", storageSize);
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
      execute(new File(context), command, envVars, "storage", "delete", storage, "-f");
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
        execute(new File(context), command, envVars, "link", source, "--port", port.toString(), "--wait");
      } else {
        execute(new File(context), command, envVars, "link", source, "--wait");
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
      ExecHelper.execute(command, new File(component), "push", "--debug");
      ExecHelper.executeWithTerminal(new File(component), false, envVars, command, "debug", "port-forward", "--local-port", port.toString());
      UsageStats.getInstance().odoCommand("debug port-forward", true);
    } catch (IOException e) {
      UsageStats.getInstance().odoCommand("debug port-forward", false);
      throw e;
    }
  }
  
  @Override
  public DebugInfo debugInfo(String project, String application, String context, String component) throws IOException {
    try {
      ExecResult result = ExecHelper.execute(command, new File(context), envVars, "debug", "info", "-o", "json");
      JSonParser parser = new JSonParser(JSON_MAPPER.readTree(result.getStdOut()));
      return parser.parseDebugInfo();
    } catch (IOException e) {
      if (e.getMessage().contains("debug is not running")) {
        return new DebugInfo(DebugStatus.NOT_RUNNING);
      }
      throw e;
    }
  }

  @Override
  public java.net.URL getMasterUrl() {
    return client.getMasterUrl();
  }

  @Override
  public String consoleURL() throws IOException {
    try {
      VersionInfo version = client.adapt(OpenShiftClient.class).getVersion();
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
            execute(new File(path), command, envVars, "list", "--path", ".", "-o", "json"),
            new TypeReference<List<ComponentDescriptor>>() {});
  }
  
  @Override
  public List<DevfileRegistry> listDevfileRegistries() throws IOException {
    return configureObjectMapper(new DevfileRegistriesDeserializer()).readValue(
        execute(command, envVars, "registry" , "list", "-o", "json"),
        new TypeReference<List<DevfileRegistry>>() {});
  }

  @Override
  public void createDevfileRegistry(String name, String url, boolean secure) throws IOException {
    if (secure) {
      execute(command, envVars, "registry", "add", name, url, "--token", "true");
    } else {
      execute(command, envVars, "registry", "add", name, url);
    }
  }

  @Override
  public void deleteDevfileRegistry(String name) throws IOException {
    execute(command, envVars, "registry", "delete", "-f", name);
  }
}
