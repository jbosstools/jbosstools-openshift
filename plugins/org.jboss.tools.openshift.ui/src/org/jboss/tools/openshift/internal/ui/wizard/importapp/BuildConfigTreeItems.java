package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.conversion.Converter;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;

public class BuildConfigTreeItems {

	public static class ConnectionTreeItem extends ObservableTreeItem {

		ConnectionTreeItem(Connection model) {
			super(model);
		}

		@Override
		protected List<? extends Object> loadChildren() {
			if (!(getModel() instanceof Connection)) {
				return Collections.emptyList();
			}
			return ((Connection) getModel()).getResources(ResourceKind.PROJECT);
		}

		@Override
		protected ObservableTreeItem createChildItem(Object model) {
			return new ProjectTreeItem(model);
		}

	}
	
	public static class ProjectTreeItem extends ObservableTreeItem {

		ProjectTreeItem(Object model) {
			super(model);
		}

		@Override
		protected List<? extends Object> loadChildren() {
			if (!(getModel() instanceof IProject)) {
				return Collections.emptyList();
			}
			return ((IProject) getModel()).getResources(ResourceKind.BUILD_CONFIG);
		}

		@Override
		protected ObservableTreeItem createChildItem(Object model) {
			return new BuildConfigTreeItem(model);
		}

	}
	
	public static class BuildConfigTreeItem extends ObservableTreeItem {

		BuildConfigTreeItem(Object model) {
			super(model);
		}

		@Override
		protected ObservableTreeItem createChildItem(Object model) {
			return null;
		}

	}
	
	public static class Model2ObservableTreeItemConverter extends Converter {
		
		Model2ObservableTreeItemConverter() {
			super(Object.class, ObservableTreeItem.class);
		}

		@Override
		public Object convert(Object fromObject) {
				if (fromObject instanceof IProject) {
					return new ProjectTreeItem(fromObject);
				} else if (fromObject instanceof IBuildConfig) {
					return new BuildConfigTreeItem(fromObject);
				} else {
					return fromObject;
				}
		}
	}
	
}
