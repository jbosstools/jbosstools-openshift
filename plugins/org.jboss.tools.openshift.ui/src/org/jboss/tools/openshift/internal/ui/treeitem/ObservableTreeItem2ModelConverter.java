/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.treeitem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.conversion.Converter;

/**
 * @author Andre Dietisheim
 */
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

	@SuppressWarnings("unchecked")
	public <T> List<T> convert(List<ObservableTreeItem> items) {
		if (items == null
				|| items.size() == 0) {
			return Collections.emptyList();
		}
		ArrayList<T> models = new ArrayList<>();
		for (ObservableTreeItem item : items) {
			models.add((T) item.getModel());
		}
		return models;
	}

}