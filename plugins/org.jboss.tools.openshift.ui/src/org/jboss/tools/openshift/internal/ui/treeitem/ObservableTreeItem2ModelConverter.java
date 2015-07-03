package org.jboss.tools.openshift.internal.ui.treeitem;

import org.eclipse.core.databinding.conversion.Converter;

public class ObservableTreeItem2ModelConverter extends Converter {
	
	public ObservableTreeItem2ModelConverter() {
		this(Object.class);
	}

	public ObservableTreeItem2ModelConverter(Class<?> toType) {
		super(ObservableTreeItem.class, toType);
	}

	@Override
	public Object convert(Object fromObject) {
		if (!(fromObject instanceof ObservableTreeItem)) {
			return fromObject;
		} else {
			return ((ObservableTreeItem) fromObject).getModel();
		}
	}
}