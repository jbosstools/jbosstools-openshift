/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.requirement;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.tools.common.reddeer.utils.StackTraceUtils;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.condition.core.NamedResourceExist;
import org.jboss.tools.openshift.reddeer.condition.core.ResourceExists;
import org.jboss.tools.openshift.reddeer.condition.core.ServicePodsExist;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.utils.v3.OpenShift3NativeResourceUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.resources.IProjectTemplateProcessing;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.template.ITemplate;

/**
 * A requirement that makes sure a given service, it's replication controller(s) and pod(s) exist. If they
 * dont, it will create them.
 * 
 * @author adietish@redhat.com
 */
public class OpenShiftServiceRequirement implements Requirement<RequiredService> {

	private static final Logger LOGGER = new Logger(OpenShiftServiceRequirement.class);
	private static final TimePeriod TIMEPERIOD_WAIT_FOR_BUILD = TimePeriod.getCustom(30 * 60); // 30mins

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RequiredService {
		/** a connection url as specified in {@link ConnectionURL}. ex. https://adietish@10.1.2.2.:8443*/
		String connectionURL() default StringUtils.EMPTY;
		/** the name of the project that the service (specified below) should exist in */
		String project() default StringUtils.EMPTY;
		/** the name of the service that should exist, triggering the use of the template (specified below) otherwise */
		String service();
		/** the name of a template, URL of a remote template, or path to a local template file that should be used to create the service if it doesn't exist */
		String template();
		/** whether the resources created by the requirement should be automatically deleted after test class, default false */
		boolean cleanup() default false;
		/** whether wait for resource build, default true */
		boolean waitForBuild() default true;
	}

	private RequiredService serviceSpec;
	private Connection connection;
	private IService service;
	private ITemplate template;

	@Override
	public void fulfill() {
		this.connection = ConnectionUtils.getConnectionOrDefault(serviceSpec.connectionURL());
		assertNotNull(NLS.bind("No connection for {0} exists", serviceSpec.connectionURL()), connection);
		final String projectName = TestUtils.getValueOrDefault(serviceSpec.project(), DatastoreOS3.TEST_PROJECT);
		assertTrue(NLS.bind("No project {0} exists on server {1}", projectName, connection.getHost()), 
				OpenShift3NativeResourceUtils.hasProject(projectName, connection));
		final String serviceName = serviceSpec.service();
		final String templateName = serviceSpec.template();
		
		IResourceFactory factory = new ClientBuilder(connection.getHost()).build().getResourceFactory();
		try {
			// try if template comes from a URL
			URL url = new URL(templateName);
			template = factory.create(url.openStream());
		} catch (MalformedURLException ex) {
			// template is not a URL, try a path to local file instead
			if (new File(templateName).exists()) {
				try {	
					template = factory.create(new FileInputStream(templateName));
				} catch (FileNotFoundException e) {
					throw new RedDeerException("Unable to read local template", e);
				}
			} else {
				// it is not an external template
				template = connection.getResource(
						ResourceKind.TEMPLATE, OpenShiftResources.OPENSHIFT_PROJECT, templateName);
			}
		} catch (IOException ex) {
			throw new RedDeerException("Unable to read template from URL", ex);
		}
		
		
		assertNotNull(template);
		this.service = getOrCreateService(projectName, serviceName, template);
		if (serviceSpec.waitForBuild()) {
			waitForResources(serviceName, projectName, service);
			waitForUI(serviceName, projectName);
		}
	}

	private void waitForResources(final String serviceName, final String projectName, final IService service) {
		new WaitUntil(
				new ServicePodsExist(serviceName, projectName, connection)
				, TimePeriod.VERY_LONG);

		new WaitUntil(
				new ResourceExists(ResourceKind.REPLICATION_CONTROLLER, new BaseMatcher<List<IResource>>() {

					@Override
					public boolean matches(Object item) {
						if (!(item instanceof List)) {
							return false;
						}
						@SuppressWarnings("unchecked")
						List<IReplicationController> resources = (List<IReplicationController>) item;
						if (resources.isEmpty()) {
							return false;
						}
						return ResourceUtils.getReplicationControllerFor(service, resources) != null;
					}

					@Override
					public void describeTo(Description description) {
					}
				}
				, projectName, connection),
				TIMEPERIOD_WAIT_FOR_BUILD);
	}

	/**
	 * Waits for the service and replication controller to appear in the UI,
	 * possibly refreshes the project. This shouldnt be required but turns out
	 * that the UI takes extensive amount of time to notice the new resources.
	 * We therefore make sure they are present in UI before considering this
	 * requirement as fullfilled and possibly refresh the project (in ui) to
	 * force it to appear. This shouldnt be necessary, I consider this as workaround.
	 * 
	 * @param projectName
	 * @param serviceName
	 */
	private void waitForUI(final String serviceName, final String projectName) {
		// wait for service to appear in UI
		new WaitUntil(
				new AbstractWaitCondition() {

					@Override
					public boolean test() {
						OpenShiftExplorerView explorer = new OpenShiftExplorerView();
						explorer.open();
						OpenShift3Connection os3Connection = explorer.getOpenShift3Connection(connection);
						assertThat(os3Connection, not(nullValue()));
						OpenShiftProject os3Project = os3Connection.getProject(projectName);
						assertThat(os3Project, not(nullValue()));
						boolean serviceExists = false;
						try {
							serviceExists =  os3Project.getService(serviceName) != null;
						} catch (RedDeerException e) {
							// catched intentionnally
							System.err.println(e);
						}
						/*
						 * WORKAROUND: UI takes extensive amount of time to notice resource changes
						 * -> refresh tree to force it to see changes
						 */
						if (!serviceExists) {
							os3Project.refresh();
						}
						return serviceExists;
					}}
				, TimePeriod.VERY_LONG);

		// wait for replication controller to appear in UI
		List<IReplicationController> rcs = connection.getResources(ResourceKind.REPLICATION_CONTROLLER, service.getNamespace());
		IReplicationController serviceRc = ResourceUtils.getReplicationControllerFor(service, rcs);
		assertThat(serviceRc, not(nullValue()));
		new WaitUntil(
				new OpenShiftResourceExists(Resource.DEPLOYMENT, containsString(serviceRc.getName()), ResourceState.UNSPECIFIED, projectName)
				, TimePeriod.VERY_LONG);
	}

	private IService getOrCreateService(String projectName, String serviceName, ITemplate template) {
		IService service = OpenShift3NativeResourceUtils.safeGetResource(
				ResourceKind.SERVICE, serviceName, projectName, connection);
		if (service == null) {
			service = createService(serviceName, template, projectName, connection);
		}
		return service;
	}

	private IService createService(String serviceName, ITemplate template, String projectName, Connection connection) {
		
		LOGGER.debug(NLS.bind("Creating service in project {0} on server {1} using template {2}",
				new Object[] { projectName, connection.getHost(), template.getName() }));
		IProject project = OpenShift3NativeResourceUtils.getProject(projectName, connection);
		assertNotNull(project);

		CreateApplicationFromTemplateJob job = new CreateApplicationFromTemplateJob(project, template);
		job.schedule();

		new WaitWhile(
				new JobIsRunning(new Matcher[] { CoreMatchers.sameInstance(job) }), 
				TimePeriod.LONG);
		new WaitUntil(
				new NamedResourceExist(ResourceKind.SERVICE, serviceName, projectName, connection),
				TimePeriod.VERY_LONG);

		return connection.getResource(ResourceKind.SERVICE, projectName, serviceName);
	}

	@Override
	public void setDeclaration(RequiredService serviceSpec) {
		this.serviceSpec = serviceSpec;
	}

	@Override
	public void cleanUp() {
		if (serviceSpec.cleanup()) {
			String projectName = TestUtils.getValueOrDefault(serviceSpec.project(), DatastoreOS3.TEST_PROJECT);
			final IProject project = OpenShift3NativeResourceUtils.getProject(projectName, connection);
			
			IProjectTemplateProcessing capability = project.getCapability(IProjectTemplateProcessing.class);
			ITemplate processed = capability.process(template);			
			
			for(IResource resource : processed.getObjects()) {
				IResource res = connection.getResource(resource.getKind(), projectName, resource.getName());
				try {
					connection.deleteResource(res);
				} catch (OpenShiftException ex) {
					LOGGER.error("Unable to remove " + res.getKind() + " named " + res.getName());
					LOGGER.error(StackTraceUtils.stackTraceToString(ex));
				}
			}

			cleanResources(connection, ResourceKind.BUILD, project, template);
			cleanResources(connection, ResourceKind.REPLICATION_CONTROLLER, project, template);
			cleanResources(connection, ResourceKind.POD, project, template);
			
			new WaitWhile(new AbstractWaitCondition() {				
				
				@Override
				public boolean test() {
					for (IResource resource : project.getResources(ResourceKind.POD)) {
						if (resource.getName().startsWith(template.getName())) {
							return true;
						}
					}
					return false;
				}
				
				@Override
				public String description() {
					return "at least one application pod is running";
				}
				
			}, TimePeriod.LONG);
			new OpenShiftExplorerView().getOpenShift3Connection(connection).refresh();
		}
	}

	public IService getService() {
		return service;
	}
	
	public IReplicationController getReplicationController() {
		if (service == null
				|| connection == null) {
			return null;
		}
		List<IReplicationController> rcs = connection.getResources(ResourceKind.REPLICATION_CONTROLLER, service.getNamespace());
		IReplicationController rc = ResourceUtils.getReplicationControllerFor(service, rcs);
		return rc;
	}
	
	private void cleanResources(Connection connection, String kind, IProject project, ITemplate template) {
		for (IResource resource : project.getResources(kind)) {
			if (resource.getName().startsWith(template.getName())) {
				try {
					connection.deleteResource(resource);
				} catch (OpenShiftException ex) {
					LOGGER.error("Unable to remove " + resource.getKind() + " named " + resource.getName());
					LOGGER.error(StackTraceUtils.stackTraceToString(ex));
				}
			}
		}
	}

	@Override
	public RequiredService getDeclaration() {
		return this.serviceSpec;
	}
}
