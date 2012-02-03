package org.jboss.tools.openshift.express.internal.ui.viewer.property;

import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.ui.views.properties.tabbed.AbstractTypeMapper;

public class TypeMapper extends AbstractTypeMapper {

	@SuppressWarnings("rawtypes")
	@Override
	public Class mapType(Object object) {
		if (object instanceof TreeNode) {
			return ((TreeNode) object).getValue().getClass();
		}
		return super.mapType(object);
	}

}
