/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage.ImageStreamApplicationSource;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateApplicationSource;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IProjectTemplateList;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.image.ITagReference;
import com.openshift.restclient.model.template.ITemplate;

/**
 * @author Andre Dietisheim
 * @author Jeff Maury
 */
public class ApplicationSourceTreeItems implements IModelFactory , ICommonAttributes{

	private static final String BUILDER_TAG = "builder";
	public static final ApplicationSourceTreeItems INSTANCE = new ApplicationSourceTreeItems();
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<T> createChildren(Object parent) {
		if (parent instanceof Connection) {
			return (List<T>) ((Connection) parent).getResources(ResourceKind.PROJECT);
		} else if (parent instanceof IProject) {
			IProject project = (IProject) parent;
			Collection appSources = loadTemplates(project);
			appSources.addAll(loadImageStreams(project));
			return (List<T>) new ArrayList<>(appSources);
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("rawtypes")
	private Collection<IApplicationSource> loadImageStreams(IProject project) {
		Connection conn = ConnectionsRegistryUtil.getConnectionFor(project);
		Collection<IImageStream> streams = conn.getResources(ResourceKind.IMAGE_STREAM, project.getNamespace());
		try {
            streams.addAll(conn.getResources(ResourceKind.IMAGE_STREAM, (String) conn.getClusterNamespace()));
        } catch (OpenShiftException e) {
            OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
        }
		
		Collection<IApplicationSource> sources = new ArrayList<>();
		for (IImageStream is : streams) {
			List<ITagReference> tags = is.getTags().stream().filter(t->
				t.isAnnotatedWith(OpenShiftAPIAnnotations.TAGS) 
				&& ArrayUtils.contains(t.getAnnotation(OpenShiftAPIAnnotations.TAGS).split(","),BUILDER_TAG)
			).collect(Collectors.toList());
			if(!tags.isEmpty()) {
				tags.forEach(t->sources.add(new ImageStreamApplicationSource(is, t)));
			}
		}
		return sources;
	}

	public List<ObservableTreeItem> create(Collection<?> openShiftObjects) {
		if (openShiftObjects == null) {
			return Collections.emptyList();
		}
		List<ObservableTreeItem> items = new ArrayList<>();
		for (Object openShiftObject : openShiftObjects) {
			ObservableTreeItem item = create(openShiftObject);
			if (item != null) {
				items.add(item);
			}
		}
		return items;
	}

	@Override
	public ObservableTreeItem create(Object object) {
		return new ObservableTreeItem(object, this);
	}
	
	@SuppressWarnings("rawtypes")
	private Collection<IApplicationSource> loadTemplates(IProject project){
		return project.accept(new CapabilityVisitor<IProjectTemplateList,  Collection<IApplicationSource>>() {

			@Override
			public  Collection<IApplicationSource> visit(IProjectTemplateList capability) {
				Collection<ITemplate> templates = capability.getTemplates();
				templates.addAll(capability.getCommonTemplates());
				return templates.stream().map(t->new TemplateApplicationSource(t)).collect(Collectors.toList());
			}
		}, Collections.emptyList());
	}
}
