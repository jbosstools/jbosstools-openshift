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

import org.eclipse.core.databinding.conversion.Converter;

public class Model2ObservableTreeItemConverter extends Converter {

	private IModelFactory factory;

	public Model2ObservableTreeItemConverter(IModelFactory factory) {
		super(Object.class, ObservableTreeItem.class);
		this.factory = factory;
	}

	@Override
	public Object convert(Object fromObject) {
		return new ObservableTreeItem(fromObject, factory);
	}
}
