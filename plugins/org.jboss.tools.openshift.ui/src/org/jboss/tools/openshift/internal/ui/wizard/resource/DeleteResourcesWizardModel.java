/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory.KeyValueFilter;
import org.jboss.tools.openshift.internal.core.job.DeleteResourcesJob;

import com.openshift.restclient.NotFoundException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.authorization.ResourceForbiddenException;
import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class DeleteResourcesWizardModel extends ObservablePojo {

	private static final String[] ALL_RESOURCE_KINDS = { 
			ResourceKind.BUILD,
			ResourceKind.BUILD_CONFIG,
			ResourceKind.DEPLOYMENT_CONFIG,
			ResourceKind.IMAGE_STREAM,
			ResourceKind.ROUTE,
			ResourceKind.TEMPLATE,
			ResourceKind.POD,
			ResourceKind.PVC,
			ResourceKind.PERSISTENT_VOLUME,
			ResourceKind.REPLICATION_CONTROLLER,
			ResourceKind.SERVICE,
			ResourceKind.SECRET,
			ResourceKind.CONFIG_MAP };

	public static final String PROP_LABEL_FILTER = "labelFilter";
	public static final String PROP_ALL_RESOURCES = "allResources";
	public static final String PROP_SELECTED_RESOURCES = "selectedResources";

	private Connection connection;
	private String namespace;

	private KeyValueFilter labelFilter;
	private List<IResource> allResources = new ArrayList<>();
	private List<IResource> selectedResources = new ArrayList<>();

	public DeleteResourcesWizardModel(Connection connection, String namespace) {
		this.connection = connection;
		this.namespace = namespace;
	}

	public void setLabelFilter(KeyValueFilter filter) {
		firePropertyChange(PROP_LABEL_FILTER, this.labelFilter, this.labelFilter = filter);
	}

	public KeyValueFilter getLabelFilter() {
		return this.labelFilter;
	}

	public void loadResources(IProgressMonitor monitor) {
		if (connection == null || StringUtils.isEmpty(namespace)) {
			return;
		}

		setAllResources(loadAllResources(monitor));
	}

	private List<IResource> loadAllResources(IProgressMonitor monitor) {
		return Arrays.stream(ALL_RESOURCE_KINDS)
				.flatMap(resourceKind -> safeLoadResources(resourceKind, namespace, connection, monitor).stream())
				.collect(Collectors.toList());
	}

	private Collection<IResource> safeLoadResources(String resourceKind, String namespace, Connection connection,
			IProgressMonitor monitor) {
		try {
			monitor.subTask(NLS.bind("Loading all {0} resources...", resourceKind));

			return connection.getResources(resourceKind, namespace);
		} catch (NotFoundException | ResourceForbiddenException e) {
			return Collections.emptyList();
		}
	}

	private void setAllResources(List<IResource> allResources) {
		List<IResource> old = new ArrayList<>(this.allResources);
		this.allResources.clear();
		this.allResources.addAll(allResources);

		firePropertyChange(PROP_ALL_RESOURCES, old, allResources);

		setSelectedResources(Collections.emptyList());
	}

	public List<IResource> getSelectedResources() {
		return selectedResources;
	}

	public void setSelectedResources(List<IResource> allResources) {
		List<IResource> old = new ArrayList<>(this.selectedResources);
		this.selectedResources.clear();
		this.selectedResources.addAll(allResources);

		firePropertyChange(PROP_SELECTED_RESOURCES, old, allResources);
	}

	public List<IResource> getAllResources() {
		return allResources;
	}

	/**
	 * Deletes the resources that are currently selected. Returns {@code true} if no
	 * error occurred, {@code false} otherwise
	 * 
	 * @return
	 */
	public void deleteSelectedResources() {
		deleteResources(new ArrayList<IResource>(selectedResources));
	}

	protected void deleteResources(List<IResource> resources) {
		if (CollectionUtils.isEmpty(resources)) {
			return;
		}

		new DeleteResourcesJob(resources).schedule();
	}
}
