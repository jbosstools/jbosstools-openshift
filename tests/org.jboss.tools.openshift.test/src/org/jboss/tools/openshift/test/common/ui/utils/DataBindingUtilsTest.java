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
package org.jboss.tools.openshift.test.common.ui.utils;

import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.junit.Assert;
import org.junit.Test;

public class DataBindingUtilsTest {

	@Test
	public void testAddDisposableListChangeListener() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Table table = new Table(shell, SWT.NONE);
		WritableList<Object> observable = new WritableList<>();
		ListChangeListener listener = new ListChangeListener();

		DataBindingUtils.addDisposableListChangeListener(listener, observable, table);
		observable.add(new Object());
		Assert.assertEquals("After the listener is added, it should listen to changes in the observable list", 1, listener.changes);

		table.dispose();
		observable.add(new Object());
		Assert.assertEquals("After the table is disposed, the listener should not listen to changes in the observable list", 1, listener.changes);
	}

	class ListChangeListener implements IListChangeListener<Object> {
		int changes = 0;

		@Override
		public void handleListChange(ListChangeEvent<? extends Object> event) {
			changes++;
		}
		
	}
}
