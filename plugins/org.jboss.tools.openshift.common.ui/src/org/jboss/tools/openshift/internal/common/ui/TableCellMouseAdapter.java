/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Kabanovich
 * @author Andre Dietisheim
 *
 */
public class TableCellMouseAdapter implements MouseListener {

	private int column;

	public TableCellMouseAdapter(int column) {
		this.column = column;
	}

	@Override
	public void mouseUp(MouseEvent event) {
		if (isWithinCell(column, event)) {
			mouseUpCell(event);
		}
	}

	@Override
	public void mouseDown(MouseEvent event) {
		if (isWithinCell(column, event)) {
			mouseDownCell(event);
		}
	}

	@Override
	public void mouseDoubleClick(MouseEvent event) {
		if (isWithinCell(column, event)) {
			mouseDoubleClickCell(event);
		}
	}

	private boolean isWithinCell(int column, MouseEvent event) {
		Point pt = new Point(event.x, event.y);
		if (!(event.widget instanceof Table)) {
			return false;
		}
		TableItem item = ((Table) event.widget).getItem(pt);
		return item != null 
				&& item.getBounds(column) != null
				&& item.getBounds(column).contains(pt);
	}

	public void mouseUpCell(MouseEvent event) { //intended to be overriden by the extensions
	}
	
	public void mouseDownCell(MouseEvent event) { //intended to be overriden by the extensions
	}

	public void mouseDoubleClickCell(MouseEvent event) { //intended to be overriden by the extensions
	}
}
