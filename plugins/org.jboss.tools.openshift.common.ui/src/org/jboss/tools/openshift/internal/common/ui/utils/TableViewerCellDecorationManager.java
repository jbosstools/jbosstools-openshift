/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * A manager that controls image based decorations for cells in a table viewer.
 * 
 * @author Andre Dietisheim
 */
public class TableViewerCellDecorationManager {

	private Image image;
	private Table table;
	private Map<ViewerCell, TableEditor> decorationByCell = new HashMap<ViewerCell, TableEditor>();

	/**
	 * @param table
	 *            the table to handle cell decorations for
	 * @param image
	 *            the image to show as decoration
	 */
	public TableViewerCellDecorationManager(Image image, Table table) {
		this.image = image;
		this.table = table;
	}

	public void toggle(boolean show, ViewerCell cell) {
		if (show) {
			show(cell);
		} else {
			hide(cell);
		}
	}
	
	public void show(ViewerCell cell) {
		TableEditor editor = decorationByCell.get(cell);
		if (editor == null) {
			Control decoration = createDecoration(image, table);
			editor = createTableEditor(image, table);
			decorationByCell.put(cell, editor);
			editor.setEditor(decoration, (TableItem) cell.getItem(), cell.getColumnIndex());
		}
	}

	public void hide(ViewerCell cell) {
		TableEditor editor = decorationByCell.get(cell);
		if (editor != null) {
			hide(editor);
			decorationByCell.remove(cell);
		}
	}

	private void hide(TableEditor editor) {
		Control decoration = editor.getEditor();
		if (decoration != null) {
			decoration.setVisible(false);
			decoration.dispose();
		}
		editor.setEditor(null);
		editor.dispose();
	}

	public void hideAll() {
		for (TableEditor decoration : decorationByCell.values()) {
			hide(decoration);
		}
		decorationByCell.clear();
	}
	
	private Control createDecoration(Image image, Table table) {
		Label validationDecoration = new Label(table, SWT.None);
		validationDecoration.setImage(image);
		return validationDecoration;
	}

	private TableEditor createTableEditor(Image image, Table table) {
		TableEditor tableEditor = new TableEditor(table);
		tableEditor.grabHorizontal = false;
		tableEditor.grabVertical = false;
		Rectangle bounds = image.getBounds();
		tableEditor.minimumHeight = bounds.height;
		tableEditor.minimumWidth = bounds.width;
		tableEditor.verticalAlignment = SWT.BEGINNING;
		tableEditor.horizontalAlignment = SWT.LEFT;

		return tableEditor;
	}
}
