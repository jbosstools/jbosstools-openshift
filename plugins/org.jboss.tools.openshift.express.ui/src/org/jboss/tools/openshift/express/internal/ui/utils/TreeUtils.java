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
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author André Dietisheim
 */
public class TreeUtils {

	/**
	 * Creates a tree editor for the given cell with the given control.
	 * <p>
	 * A tree editor puts a control over a given cell in a tree. Furthermore it puts text into the cell so that the
	 * column's large enough so that the control has enough room to overlay itself over it
	 * 
	 * @param cell the cell to put the control to
	 * @param control the control to put to the given cell
	 * @param cellText the text that's put into the table cell (overlayed by the editor), that only enlarges the
	 *            column to have enough room for the editor
	 * 
	 * @return the tree editor
	 * 
	 * @see ViewerCell
	 * @see TreeEditor
	 */
	public static TreeEditor createTreeEditor( Control control, String cellText, ViewerCell cell )
	{
		Assert.isTrue(cell.getControl() instanceof Tree);
		
		Tree tree = ( Tree ) cell.getControl();
		TreeEditor treeEditor = new TreeEditor( tree );
		initializeTreeEditor( treeEditor, control, cellText, cell );
		return treeEditor;
	}

	/**
	 * Initializes a given tree editor for a given viewer cell with a given (editor-)control.
	 * 
	 * @param treeEditor the tree editor that shall get initialized
	 * @param control the control that shall be positioned by the tree editor
	 * @param cellText the text that will get displayed in the cell (only used to make sure, the cell has the required size)
	 * @param cell the cell the table editor shall be positioned to.
	 * 
	 * @see TreeEditor
	 * @see ViewerCell
	 */
	public static void initializeTreeEditor( TreeEditor treeEditor, Control control, String cellText, ViewerCell cell )
	{
		treeEditor.grabHorizontal = true;
		treeEditor.grabVertical = true;
		treeEditor.horizontalAlignment = SWT.FILL;
		treeEditor.verticalAlignment = SWT.FILL;
		TreeItem treeItem = ( TreeItem ) cell.getItem();
		treeEditor.setEditor( control, treeItem, cell.getColumnIndex() );
		// ensure cell is as large as space needed for link
		//cell.setText( " " + cellText + " ");
	}

	/**
	 * Sets the height of the rows in a given table. The height might only be increased (compared to the standard
	 * height). Decreasing it below the default height has no effect.
	 * 
	 * @param height the height in pixels
	 * @param table the table
	 */
	public static void setRowHeight( final int height, Tree tree )
	{
		tree.addListener( SWT.MeasureItem, new Listener()
		{
			public void handleEvent( Event event )
			{
				event.height = height;
			}
		} );
	}
}
