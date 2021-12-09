/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.explorer;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.navigator.CommonNavigator;

public class BaseOpenShiftExplorerView extends CommonNavigator {

	@Override
	public void initListeners(TreeViewer viewer) {
		super.initListeners(viewer);
		Tree tree = viewer.getTree();
		if (tree != null && !tree.isDisposed()) {
			new LinkMouseListener(tree);
		}
	}

	static class LinkMouseListener extends MouseAdapter implements MouseMoveListener {
		Tree tree;

		LinkMouseListener(Tree tree) {
			this.tree = tree;
			tree.addMouseListener(this);
			tree.addMouseMoveListener(this);
		}

		boolean isLink = false;

		@Override
		public void mouseMove(MouseEvent e) {
			if (tree.isDisposed()) {
				return;
			}
			ILink link = getLink(e);
			if (isLink != (link != null)) {
				isLink = (link != null);
				Cursor cursor = isLink ? Display.getDefault().getSystemCursor(SWT.CURSOR_HAND) : null;
				tree.setCursor(cursor);
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			if (e.button != 1) {
				return;
			}
			final ILink link = getLink(e);
			if (link != null) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (tree.isDisposed()) {
							return;
						}
						tree.setCursor(null);
						isLink = false;
						link.execute();
					}
				});
			}
		}

		ILink getLink(MouseEvent e) {
			if (e.getSource() instanceof Tree) {
				Tree sourceTree = (Tree) e.getSource();
				TreeItem t = sourceTree.getItem(new Point(e.x, e.y));
				Object o = t == null ? null : t.getData();
				return o instanceof ILink ? (ILink) o : null;
			}
			return null;
		}
	}
}
