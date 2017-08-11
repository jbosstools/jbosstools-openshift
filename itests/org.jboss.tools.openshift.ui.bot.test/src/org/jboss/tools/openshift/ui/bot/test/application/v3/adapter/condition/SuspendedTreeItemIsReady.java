/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.condition;

import java.util.List;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.swt.api.TreeItem;

public class SuspendedTreeItemIsReady extends AbstractWaitCondition {
	
	private TreeItem jdkTreeItem;

	private TreeItem suspendedTreeItem;

	public SuspendedTreeItemIsReady(TreeItem jdkTreeItem) {
		this.jdkTreeItem = jdkTreeItem;
	}

	public TreeItem getSuspendedTreeItem() {
		return suspendedTreeItem;
	}

	@Override
	public boolean test() {
		List<TreeItem> frames = jdkTreeItem.getItems();
		for (TreeItem frame : frames) {
			try {
				if (frame.getText().contains("Suspended")) {
					suspendedTreeItem = frame;
					return true;
				}
			} catch (CoreLayerException e) {
				// most likely some widgetIsDisposed. Ignore.
				return false;
			}
		}
		return false;
	}

}
