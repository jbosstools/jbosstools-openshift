package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IProjectTemplateList;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.template.ITemplate;

public class TemplateTreeItems implements IModelFactory {

	public static final TemplateTreeItems INSTANCE = new TemplateTreeItems();
	
	@SuppressWarnings("unchecked")
	public <T> List<T> createChildren(Object parent) {
		if (parent instanceof Connection) {
			return (List<T>) ((Connection) parent).getResources(ResourceKind.PROJECT);
		} else if (parent instanceof IProject) {
			IProject project = (IProject) parent;
			Collection<ITemplate> templates = project.accept(new CapabilityVisitor<IProjectTemplateList,  Collection<ITemplate>>() {

				@Override
				public  Collection<ITemplate> visit(IProjectTemplateList capability) {
					Collection<ITemplate> templates = capability.getTemplates();
					templates.addAll(capability.getCommonTemplates());
					return templates;
				}
			}, Collections.<ITemplate> emptyList());
			return (List<T>) new ArrayList<ITemplate>(templates);
		}
		return Collections.emptyList();
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

	public ObservableTreeItem create(Object object) {
		return new ObservableTreeItem(object, this);
	}
}
