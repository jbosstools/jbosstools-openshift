package org.jboss.tools.openshift.internal.ui.treeitem;

import java.util.List;

public interface IModelFactory {
	public <T> List<T> createChildren(Object model);
}
