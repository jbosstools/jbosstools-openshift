package org.jboss.tools.openshift.internal.ui.treeitem;

import org.eclipse.core.databinding.conversion.Converter;

public class Model2ObservableTreeItemConverter extends Converter {

	private IModelFactory factory;

	public Model2ObservableTreeItemConverter(IModelFactory factory) {
		super(Object.class, ObservableTreeItem.class);
		this.factory = factory;
	}

	@Override
	public Object convert(Object fromObject) {
		ObservableTreeItem item = new ObservableTreeItem(fromObject, factory);
		if (item != null) {
			return item;
		} 
		return fromObject;
	}
}
