package org.jboss.tools.openshift.internal.ui.treeitem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	public <T> List<T> convert(List<ObservableTreeItem> items) {
		if (items == null
				|| items.size() == 0) {
			return Collections.emptyList();
		}
		ArrayList<T> models = new ArrayList<T>();
		for (ObservableTreeItem item : items) {
			models.add((T) item.getModel());
		}
		return models;
	}

}