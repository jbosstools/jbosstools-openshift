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
package org.jboss.tools.openshift.internal.ui.odo;

import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.HOME_FOLDER;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP3_CONFIG_NAMESPACE;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP3_WEBCONSOLE_CONFIG_MAP_NAME;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP3_WEBCONSOLE_YAML_FILE_NAME;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP4_CONFIG_NAMESPACE;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP4_CONSOLE_PUBLIC_CONFIG_MAP_NAME;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.OCP4_CONSOLE_URL_KEY_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalServiceOutputStreamMonitorListener;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.ComponentDescriptorsDeserializer;
import org.jboss.tools.openshift.core.odo.ComponentDeserializer;
import org.jboss.tools.openshift.core.odo.ComponentFeature;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.ComponentKind;
import org.jboss.tools.openshift.core.odo.ComponentMetadata;
import org.jboss.tools.openshift.core.odo.ComponentMetadatasDeserializer;
import org.jboss.tools.openshift.core.odo.ComponentTypeInfo;
import org.jboss.tools.openshift.core.odo.ComponentTypesDeserializer;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.DevfileRegistriesDeserializer;
import org.jboss.tools.openshift.core.odo.DevfileRegistry;
import org.jboss.tools.openshift.core.odo.JSonParser;
import org.jboss.tools.openshift.core.odo.KubernetesLabels;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.OperatorCRD;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.ServiceTemplatesDeserializer;
import org.jboss.tools.openshift.core.odo.URL;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.odo.ExecHelper.ExecResult;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
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

	/*
	 * Map of process launched for feature (dev, debug,...) related. Key is
	 * component name value is map index by the feature and value is the process
	 * handler
	 */
	private Map<String, Map<ComponentFeature, ProcessHandle>> componentFeatureProcesses = new HashMap<>();

	/*
	 * Map of process launched for log activity. Key is component name value is list
	 * with 2 process handler index 0 is dev index 1 is deploy
	 */
	private Map<String, List<ProcessHandle>> componentLogProcesses = new HashMap<>();

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
		computeTelemetrySettings();
		reportTelemetry();
	}

	private void reportTelemetry() {
		try {
			ClusterInfo info = ClusterHelper.getClusterInfo(client);
			UsageStats.getInstance().kubernetesVersion(info.getKubernetesVersion());
			UsageStats.getInstance().isOpenShift(info.isOpenshift());
			UsageStats.getInstance().openshiftVersion(info.getOpenshiftVersion());
		} catch (KubernetesClientException e) {
			// WARNING only as it can be no route to host errors and only impact telemetry
			// info
			OpenShiftUIActivator.log(IStatus.WARNING, e.getLocalizedMessage(), e);
		}
	}

	private void computeTelemetrySettings() {
		if (UsageStats.getInstance().isTrackingEnabled()) {
			this.envVars.put("ODO_TRACKING_CONSENT", "yes");
			this.envVars.put("TELEMETRY_CALLER", "jbosstools");
		} else {
			this.envVars.put("ODO_TRACKING_CONSENT", "no");
		}
	}

	private ObjectMapper configureObjectMapper(final StdNodeBasedDeserializer<? extends List<?>> deserializer) {
		final SimpleModule module = new SimpleModule();
		module.addDeserializer(List.class, deserializer);
		return JSON_MAPPER.copy().registerModule(module);
	}

	@Override
	public boolean isOpenShift() {
		return ClusterHelper.getClusterInfo(client).isOpenshift();
	}

	@Override
	public void migrateComponent(String context, String name) throws IOException {
		client.apps().deployments().withLabel(KubernetesLabels.COMPONENT_NAME_LABEL, name).delete();
	}

	@Override
	public List<String> getNamespaces() throws IOException {
		try {
			if (isOpenShift()) {
				return client.adapt(OpenShiftClient.class).projects().list().getItems().stream()
						.map(p -> p.getMetadata().getName()).collect(Collectors.toList());
			}
			return client.namespaces().list().getItems().stream().map(n -> n.getMetadata().getName())
					.collect(Collectors.toList());
		} catch (KubernetesClientException e) {
			throw new IOException(e);
		}
	}

	protected String validateNamespace(String ns) {
		if (StringUtils.isEmpty(ns)) {
			ns = "default";
		}
		try {
			if (isOpenShift()) {
				client.adapt(OpenShiftClient.class).projects().withName(ns).get();
			} else {
				client.namespaces().withName(ns).get();
			}
		} catch (KubernetesClientException e) {
			ns = "";
			if (isOpenShift()) {
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
		return "".equals(namespace) ? null : namespace;
	}

	private static String execute(File workingDirectory, String command, Map<String, String> envs, String... args)
			throws IOException {
		ExecResult result = ExecHelper.execute(command, workingDirectory, envs, args);
		try (BufferedReader reader = new BufferedReader(new StringReader(result.getStdOut()))) {
			BinaryOperator<String> reducer = new BinaryOperator<String>() {
				private boolean notificationFound = false;

				@Override
				public String apply(String s, String s2) {
					if (s2.startsWith("---")) {
						notificationFound = true;
					}
					return notificationFound ? s : s + s2 + "\n";
				}
			};
			return reader.lines().reduce("", reducer);
		}
	}

	private static String execute(String command, Map<String, String> envs, String... args) throws IOException {
		return execute(new File(HOME_FOLDER), command, envs, args);
	}

	@Override
	public void start(String project, String context, String component, ComponentFeature feature,
			Consumer<Boolean> callback) throws IOException {
		if (feature.getPeer() != null) {
			stop(project, context, component, feature.getPeer(), callback);
		}
		Map<ComponentFeature, ProcessHandle> componentMap = componentFeatureProcesses.computeIfAbsent(component,
				name -> new HashMap<>());
		ProcessHandle handler = componentMap.get(feature);
		if (handler == null) {
			List<String> args = new ArrayList<>();
			args.add(command);
			args.addAll(feature.getStartArgs());

			TerminalOutputMonitorListener listener = new TerminalOutputMonitorListener(componentMap, callback, feature);

			long pid = ExecHelper.executeWithTerminal(createWorkingDirectory(context), envVars, listener,
					args.toArray(new String[args.size()]));
			Optional<ProcessHandle> processHandle = ProcessHandle.of(pid);
			if (processHandle.isPresent()) {
				componentMap.put(feature, processHandle.get());
				CompletableFuture<ProcessHandle> onProcessExit = processHandle.get().onExit();
				onProcessExit.thenAccept(p -> componentMap.remove(feature));
			}
		}
	}

	private void stopHandler(ProcessHandle handler) {
		handler.destroy();
	}

	@Override
	public void stop(String project, String context, String component, ComponentFeature feature, Consumer<Boolean> callback) throws IOException {
		Map<ComponentFeature, ProcessHandle> componentMap = componentFeatureProcesses.computeIfAbsent(component,
				name -> new HashMap<>());
		ProcessHandle handler = componentMap.remove(feature);
		if (handler != null) {
			stopHandler(handler);
			if (!feature.getStopArgs().isEmpty()) {
				List<String> args = new ArrayList<>();
				args.add(command);
				args.addAll(feature.getStopArgs());
				execute(createWorkingDirectory(context), command, envVars,
						feature.getStopArgs().toArray(new String[feature.getStopArgs().size()]));
			}
			handler.onExit().thenAccept(t -> callback.accept(Boolean.TRUE));
		}
	}

	@Override
	public boolean isStarted(String project, String context, String component, ComponentFeature feature)
			throws IOException {
		Map<ComponentFeature, ProcessHandle> componentMap = componentFeatureProcesses.computeIfAbsent(component,
				name -> new HashMap<>());
		return componentMap.containsKey(feature);
	}

	private File createWorkingDirectory(String context) {
		File workingDirectory = null;
		if (context != null) {
			workingDirectory = new File(context);
		}
		return workingDirectory;
	}

	@Override
	public void describeComponent(String project, String context, String component) throws IOException {
		try {
			ExecHelper.executeWithTerminal(createWorkingDirectory(context), envVars, command, "describe", "component");
			UsageStats.getInstance().odoCommand("describe", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("describe", false);
			throw e;
		}
	}

	@Override
	public List<ComponentMetadata> analyze(String path) throws IOException {
		return configureObjectMapper(new ComponentMetadatasDeserializer()).readValue(
				execute(new File(path), command, envVars, "analyze", "-o", "json"),
				new TypeReference<List<ComponentMetadata>>() {});
	}

	@Override
	public void createComponent(String project, String componentType, String registryName, String component,
			String source, String devfile, String starter) throws IOException {
		try {
			List<String> args = new ArrayList<>();
			args.add("init");
			if (StringUtils.isNotBlank(devfile)) {
				args.add("--devfile-path");
				args.add(devfile);
			} else {
				if (StringUtils.isNotBlank(starter)) {
					args.add("--starter");
					args.add(starter);
				}
				args.add("--devfile");
				args.add(componentType);
				args.add("--devfile-registry");
				args.add(registryName);
			}
			args.add("--name");
			args.add(component);
			execute(new File(source), command, envVars, args.toArray(new String[0]));
			UsageStats.getInstance().odoCommand("create", true);
			UsageStats.getInstance().createComponent("devfile:" + componentType, true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("create", false);
			UsageStats.getInstance().createComponent("devfile:" + componentType, false);
			throw e;
		}
	}

	/**
	 * ensure that $HOME/.odo/config.yaml file exists so thar we can use service
	 * related commands.
	 */
	private void ensureDefaultOdoConfigFileExists() {
		Path dir = Paths.get(HOME_FOLDER, PLUGIN_FOLDER);
		Path config = dir.resolve("config.yaml");
		try {
			if (!Files.exists(dir)) {
				Files.createDirectories(dir);
			}
			if (!Files.exists(config)) {
				Files.createFile(config);
			}
		} catch (IOException e) {
			OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
		}
	}

	private CustomResourceDefinitionContext toCustomResourceDefinitionContext(OperatorCRD crd) {
		String group = crd.getName().substring(crd.getName().indexOf('.') + 1);
		String plural = crd.getName().substring(0, crd.getName().indexOf('.'));
		return new CustomResourceDefinitionContext.Builder().withName(crd.getName()).withGroup(group)
				.withScope("Namespaced").withKind(crd.getKind()).withPlural(plural).withVersion(crd.getVersion())
				.build();
	}

	private CustomResourceDefinitionContext toCustomResourceDefinitionContext(
			org.jboss.tools.openshift.core.odo.Service service) {
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
			((ObjectNode) node).set(SPEC_FIELD, spec);
		}
	}

	@Override
	public void createService(String project, ServiceTemplate serviceTemplate, OperatorCRD serviceCRD, String service,
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
	public String getServiceTemplate(String project, String service) throws IOException {
		throw new IOException("Not implemented by odo yet");
	}

	@Override
	public void deleteService(String project, org.jboss.tools.openshift.core.odo.Service service) throws IOException {
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
	public List<DevfileComponentType> getComponentTypes() throws IOException {
		try {
			List<DevfileComponentType> componentTypes = configureObjectMapper(new ComponentTypesDeserializer())
					.readValue(execute(command, envVars, "registry", "list", "-o", "json"),
							new TypeReference<List<DevfileComponentType>>() {});
			UsageStats.getInstance().odoCommand("catalog list components", true);
			return componentTypes;
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("catalog list components", false);
			throw e;
		}
	}

	@Override
	public List<DevfileComponentType> getComponentTypes(String registryName) throws IOException {
		return getComponentTypes().stream().filter(type -> registryName.equals(type.getDevfileRegistry().getName()))
				.collect(Collectors.toList());
	}

	@Override
	public ComponentTypeInfo getComponentTypeInfo(String componentType, String registryName) throws IOException {
		String json = execute(command, envVars, "registry", "list", "--devfile-registry", registryName, "--devfile",
				componentType, "-o", "json");
		JSonParser parser = new JSonParser(JSON_MAPPER.readTree(json));
		return parser.parseComponentTypeInfo();
	}

	private void loadSwagger() {
		try {
			Request req = new Request.Builder().get().url(new java.net.URL(client.getMasterUrl(), "/openapi/v2"))
					.build();
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
		} catch (IOException e) {
		}
		return null;
	}

	@Override
	public List<ServiceTemplate> getServiceTemplates() throws IOException {
		try {
			List<ServiceTemplate> serviceTemplates = configureObjectMapper(
					new ServiceTemplatesDeserializer(this::findSchema))
					.readValue(execute(command, envVars, "catalog", "list", "services", "-o", "json"),
							new TypeReference<List<ServiceTemplate>>() {});
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
	public List<URL> listURLs(String project, String context, String component) throws IOException {
		try {
			if (context != null) {
				List<URL> urls = parseURLs(execute(createWorkingDirectory(context), command, envVars, "describe",
						"component", "-o", "json"));
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

	private List<URL> parseURLs(String json) throws IOException {
		JSonParser parser = new JSonParser(JSON_MAPPER.readTree(json));
		return parser.parseURLS();
	}

	private ComponentInfo parseComponentInfo(String json, ComponentKind kind) throws IOException {
		JSonParser parser = new JSonParser(JSON_MAPPER.readTree(json));
		return parser.parseDescribeComponentInfo(kind);
	}

	@Override
	public ComponentInfo getComponentInfo(String project, String component, String path, ComponentKind kind)
			throws IOException {
		if (path != null) {
			return parseComponentInfo(
					execute(createWorkingDirectory(path), command, envVars, "describe", "component", "-o", "json"),
					kind);
		} else {
			return parseComponentInfo(execute(command, envVars, "describe", "component", "--namespace", project,
					"--name", component, "-o", "json"), kind);
		}
	}

	@Override
	public void deleteURL(String project, String context, String component, String name) throws IOException {
		try {
			execute(new File(context), command, envVars, "url", "delete", "-f", name);
			UsageStats.getInstance().odoCommand("url delete", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("url delete", false);
			throw e;
		}
	}

	/*
	 * We should emulate oc delete all -l app.kubernetes.io/component=comp_name but
	 * as the Kubernetes client does not allow to retrieve all APIGroups we reduce
	 * the scope to: - Deployment - Service - Route - BuildConfig - ImageStreams
	 */
	private void deleteDeployment(String project, String deployment) throws IOException {
		try {
			client.apps().deployments().inNamespace(project).withName(deployment)
					.withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
			client.services().inNamespace(project).withLabel(KubernetesLabels.COMPONENT_LABEL, deployment).list()
					.getItems().forEach(service -> client.services().withName(service.getMetadata().getName())
							.withPropagationPolicy(DeletionPropagation.BACKGROUND).delete());
			if (isOpenShift()) {
				OpenShiftClient oclient = client.adapt(OpenShiftClient.class);
				oclient.routes().inNamespace(project).withLabelIn(KubernetesLabels.COMPONENT_LABEL, deployment).list()
						.getItems().forEach(route -> oclient.routes().withName(route.getMetadata().getName())
								.withPropagationPolicy(DeletionPropagation.BACKGROUND).delete());
				oclient.buildConfigs().inNamespace(project).withLabel(KubernetesLabels.COMPONENT_LABEL, deployment)
						.list().getItems().forEach(bc -> oclient.buildConfigs().withName(bc.getMetadata().getName())
								.withPropagationPolicy(DeletionPropagation.BACKGROUND).delete());
				oclient.imageStreams().inNamespace(project).withLabel(KubernetesLabels.COMPONENT_LABEL, deployment)
						.list().getItems().forEach(is -> oclient.imageStreams().withName(is.getMetadata().getName())
								.withPropagationPolicy(DeletionPropagation.BACKGROUND).delete());
			}
		} catch (KubernetesClientException e) {
			throw new IOException(e.getLocalizedMessage(), e);
		}
	}

	private void undeployComponent(String project, String context, String component, boolean deleteConfig,
			ComponentKind kind) throws IOException {
		try {
			if (kind != ComponentKind.OTHER) {
				List<String> args = new ArrayList<>();
				args.add("delete");
				args.add("component");
				args.add("--files");
				args.add("-f");
				if (context != null) {
					File dir = createWorkingDirectory(context);
					try {
						execute(dir, command, envVars, args.toArray(new String[0]));
					} catch (IOException e) {
						OpenShiftUIActivator.log(IStatus.WARNING, e.getLocalizedMessage(), e);
					}
				} else {
					args.add("--namespace");
					args.add(project);
					args.add("--name");
					args.add(component);
					execute(command, envVars, args.toArray(new String[0]));
				}
			} else {
				deleteDeployment(project, component);
			}
			UsageStats.getInstance().odoCommand("delete", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("delete", false);
			throw e;
		}
	}

	@Override
	public void deleteComponent(String project, String context, String component, ComponentKind kind)
			throws IOException {
		undeployComponent(project, context, component, true, kind);
	}

	private void doLog(String context, String component, boolean follow, boolean deploy) throws IOException {
		List<ProcessHandle> handlers = componentLogProcesses.computeIfAbsent(component,
				name -> Arrays.asList(new ProcessHandle[2]));
		int index = deploy ? 1 : 0;
		ProcessHandle handler = handlers.get(index);
		if (handler == null) {
			List<String> args = new ArrayList<>();
			args.add(command);
			args.add("logs");
			if (deploy) {
				args.add("--deploy");
			} else {
				args.add("--dev");
			}
			if (follow) {
				args.add("--follow");
			}
			long pid = ExecHelper.executeWithTerminal(createWorkingDirectory(context), envVars,
					args.toArray(new String[args.size()]));

			Optional<ProcessHandle> processHandle = ProcessHandle.of(pid);
			if (processHandle.isPresent()) {
				handlers.set(index, processHandle.get());
				CompletableFuture<ProcessHandle> onProcessExit = processHandle.get().onExit();
				onProcessExit.thenAccept(p -> handlers.set(index, null));
			}
		}
	}

	@Override
	public boolean isLogRunning(String context, String component, boolean deploy) {
		return componentLogProcesses.computeIfAbsent(component, name -> Arrays.asList(new ProcessHandle[2]))
				.get(deploy ? 1 : 0) != null;
	}

	@Override
	public void follow(String project, String context, String component, boolean deploy) throws IOException {
		try {
			doLog(context, component, true, deploy);
			UsageStats.getInstance().odoCommand("log", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("log", false);
			throw e;
		}
	}

	@Override
	public void log(String project, String context, String component, boolean deploy) throws IOException {
		try {
			doLog(context, component, false, deploy);
			UsageStats.getInstance().odoCommand("log", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("log", false);
			throw e;
		}
	}

	@Override
	public void createProject(String project) throws IOException {
		try {
			execute(command, envVars, "create", "namespace", project, "-w");
			UsageStats.getInstance().odoCommand("project create", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("project create", false);
			throw e;
		}
	}

	@Override
	public void deleteProject(String project) throws IOException {
		try {
			execute(command, envVars, "delete", "namespace", project, "-f", "-w");
			if (project.equals(namespace)) {
				namespace = null;
			}
			UsageStats.getInstance().odoCommand("project delete", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("project delete", false);
			throw e;
		}
	}

	@Override
	public void login(String url, String userName, char[] password, char[] token) throws IOException {
		UsageStats.getInstance().login();
		try {
			if (userName != null && !userName.isEmpty()) {
				execute(command, envVars, "login", url, "-u", userName, "-p", String.valueOf(password),
						" --insecure-skip-tls-verify");
			} else {
				execute(command, envVars, "login", url, "-t", String.valueOf(token), " --insecure-skip-tls-verify");
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

	@Override
	public List<Component> getComponents(String project) throws IOException {
		return configureObjectMapper(new ComponentDeserializer()).readValue(
				execute(command, envVars, "list", "--namespace", project, "-o", "json"),
				new TypeReference<List<Component>>() {
				});
	}

	@Override
	public List<org.jboss.tools.openshift.core.odo.Service> getServices(String project) throws IOException {
//		try {
//			return configureObjectMapper(new ServiceDeserializer())
//					.readValue(
//							execute(command, envVars, "service", "list", "--app", application, "--project", project,
//									"-o", "json"),
//							new TypeReference<List<org.jboss.tools.openshift.core.odo.Service>>() {
//							});
//		} catch (IOException e) {
//			// https://github.com/openshift/odo/issues/5010
//			if (e.getMessage().contains("\"no operator backed services found in namespace:")) {
//				return Collections.emptyList();
//			}
//			throw e;
//		}
		return Collections.emptyList();
	}

	protected LabelSelector getLabelSelector(String application, String component) {
		return new LabelSelectorBuilder().addToMatchLabels(KubernetesLabels.APP_LABEL, application)
				.addToMatchLabels(KubernetesLabels.COMPONENT_NAME_LABEL, component).build();
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
			ExecHelper.executeWithTerminal(envVars, command, "catalog", "list", "services");
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
	public void link(String project, String component, String context, String target) throws IOException {
		UsageStats.getInstance().link();
		try {
			execute(new File(context), command, envVars, "link", target);
			UsageStats.getInstance().odoCommand("link", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("link", false);
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
			if (isOpenShift()) {
				VersionInfo version = client.adapt(OpenShiftClient.class).getVersion();
				if (version == null || "4".equals(version.getMajor())) { // assuming null version is version 4
					ConfigMap configMap = client.configMaps().inNamespace(OCP4_CONFIG_NAMESPACE)
							.withName(OCP4_CONSOLE_PUBLIC_CONFIG_MAP_NAME).get();
					if (configMap != null) {
						return configMap.getData().get(OCP4_CONSOLE_URL_KEY_NAME);
					}
				} else if ("3".equals(version.getMajor())) {
					ConfigMap configMap = client.configMaps().inNamespace(OCP3_CONFIG_NAMESPACE)
							.withName(OCP3_WEBCONSOLE_CONFIG_MAP_NAME).get();
					String yaml = configMap.getData().get(OCP3_WEBCONSOLE_YAML_FILE_NAME);
					return JSON_MAPPER.readTree(yaml).path("clusterInfo").path("consolePublicURL").asText();
				}
			}
			// test if minikube is used and dashboard is installed
			if (!client.namespaces().list().getItems().stream()
					.filter(ns -> "kubernetes-dashboard".equals(ns.getMetadata().getName()))
					.collect(Collectors.toList()).isEmpty()) {
				// https://<master-ip>:<apiserver-port>/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
				return getMasterUrl() + "console";
			}
			return getMasterUrl() + "console";
		} catch (KubernetesClientException e) {
			return getMasterUrl().toExternalForm();
		}
	}

	@Override
	public List<ComponentDescriptor> discover(String path) throws IOException {
		return configureObjectMapper(new ComponentDescriptorsDeserializer(new File(path).getAbsolutePath())).readValue(
				execute(createWorkingDirectory(path), command, envVars, "list", "-o", "json"),
				new TypeReference<List<ComponentDescriptor>>() {
				});
	}

	@Override
	public List<DevfileRegistry> listDevfileRegistries() throws IOException {
		return configureObjectMapper(new DevfileRegistriesDeserializer()).readValue(
				execute(command, envVars, "preference", "view", "-o", "json"),
				new TypeReference<List<DevfileRegistry>>() {
				});
	}

	@Override
	public void createDevfileRegistry(String name, String url, String token) throws IOException {
		try {
			if (StringUtils.isNotBlank(token)) {
				execute(command, envVars, "preference", "add", "registry", name, url, "--token", token);
			} else {
				execute(command, envVars, "preference", "add", "registry", name, url);
			}
			UsageStats.getInstance().odoCommand("devfile registry create", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("devfile registry create", false);
			throw e;
		}
	}

	@Override
	public void deleteDevfileRegistry(String name) throws IOException {
		try {
			execute(command, envVars, "preference", "remove", "registry", "-f", name);
			UsageStats.getInstance().odoCommand("devfile registry delete", true);
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand("devfile registry delete", false);
			throw e;
		}
	}

	private class TerminalOutputMonitorListener implements ITerminalServiceOutputStreamMonitorListener {

		private Consumer<Boolean> callback;

		private boolean callBackCalled = false;

		private Map<ComponentFeature, ProcessHandle> componentMap;

		private ComponentFeature feature;

		private StringBuffer content = new StringBuffer();

		public TerminalOutputMonitorListener(Map<ComponentFeature, ProcessHandle> componentMap,
				Consumer<Boolean> callback, ComponentFeature feature) {
			this.callback = callback;
			this.componentMap = componentMap;
			this.feature = feature;
		}

		@Override
		public void onContentReadFromStream(byte[] byteBuffer, int bytesRead) {
			if (callback != null && !callBackCalled) {
				String text = new String(byteBuffer, 0, bytesRead, Charset.defaultCharset());
				content.append(text);
				if (content.toString().contains(feature.getOutput())) {
					callback.accept(true);
					callBackCalled = true;
				}
			}
		}

	}
}
