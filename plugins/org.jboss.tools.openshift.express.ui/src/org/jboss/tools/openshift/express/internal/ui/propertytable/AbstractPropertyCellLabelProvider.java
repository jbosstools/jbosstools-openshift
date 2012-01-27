/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.propertytable;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public abstract class AbstractPropertyCellLabelProvider extends StyledCellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		if (cell.getElement() instanceof IProperty) {
			update((IProperty) cell.getElement(), cell);
		}
	}

	abstract protected void update(IProperty property, ViewerCell cell);
}
