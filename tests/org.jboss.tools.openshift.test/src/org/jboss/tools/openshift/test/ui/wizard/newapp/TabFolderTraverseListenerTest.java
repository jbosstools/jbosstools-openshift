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
package org.jboss.tools.openshift.test.ui.wizard.newapp;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.TabFolderTraverseListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TabFolderTraverseListenerTest {
	static final int TAB_COUNT = 3;
	TestWindow window;

	@Before
	public void before() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		window = new TestWindow(shell, TAB_COUNT);
		window.open();
	}

	@After
	public void after() {
		if(window != null) {
			window.close();
			window = null;
		}
	}

	@Test
	public void testTabFolderTraverseListener() {
		for (int i = 0; i < window.items.length; i++) {
			checkTab(i);
		}
	}

	/**
	 * For the tab with the given index sends traverse event 
	 * to the focus control and checks that
	 * 1. After traverse on the tab, the first focusable control gets focus.
	 * 2. After traverse on child controls before the last one, the next child control gets focus.
	 * 3. After traverse on the last child, the next sibling of the tab folder gets focus.
	 * @param index
	 */
	private void checkTab(int index) {
		window.tabFolder.forceFocus();
		window.tabFolder.setSelection(index);
		TestItem item = window.items[index];
		fireTraverse(window.tabFolder);
		checkFocus(item.controls[0]);
		for (int i = 0; i < item.controls.length - 1; i++) {
			fireTraverse(item.controls[i]);
			checkFocus(item.controls[i + 1]);
		}
		fireTraverse(item.controls[item.controls.length - 1]);
		checkFocus(window.next);
	}

	private void fireTraverse(Control control) {
		window.getShell().forceActive();
		control.notifyListeners(SWT.Traverse, createTraverseEvent());
	}

	private void checkFocus(Control expected) {
		Assert.assertTrue(expected == window.listener.lastFocused);
//		Assert.assertTrue(expected.isFocusControl());
	}

	private Event createTraverseEvent() {
		Event event = new Event();
		event.detail = SWT.TRAVERSE_TAB_NEXT;
		return event;
	}

	class TestWindow extends Window {
		TabFolder tabFolder;
		TestableTabFolderTraverseListener listener;
		
		TestItem[] items;
		
		//A control to be focused after all child controls of the selected tab are traversed. 
		Control next;

		protected TestWindow(Shell shellProvider, int tabCount) {
			super(shellProvider);
			items = new TestItem[tabCount];
		}

		@Override
		protected Control createContents(Composite parent) {
			Composite root = new Composite(parent, SWT.NONE);

			tabFolder = new TabFolder(root, SWT.NONE);

			listener = new TestableTabFolderTraverseListener(tabFolder);
			
			for (int i = 0; i < items.length; i++) {
				items[i] = new TestItem();
				items[i].create(tabFolder, listener);
			}
			next = new Text(root, SWT.NONE);
			return root;
		}
	}

	class TestItem {
		TabItem tabItem;
		Composite itemRoot;
		Control[] controls = new Control[5];

		public void create(TabFolder tabFolder, TabFolderTraverseListener listener) {
			tabItem = new TabItem(tabFolder, SWT.NONE);
			tabItem.setText("Tab " + tabFolder.getItemCount());
			
			Composite parent = new Composite(tabFolder, SWT.NONE);
			tabItem.setControl(parent);
			
			for (int i = 0; i < controls.length - 1; i++) {
				Label label = new Label(parent, SWT.NONE); //Label will not get focus.
				label.setText("Input " + i);
				controls[i] = new Text(parent, SWT.NONE);
				Text disabledText = new Text(parent, SWT.NONE); //Disabled text will not get focus.
				disabledText.setEnabled(false);
			}
			controls[controls.length - 1] = new Button(parent, SWT.NONE);
			
			listener.bindTabControls(tabFolder.getItemCount() - 1, controls);
		}
	}

	/**
	 * Assertion control.isFocusControl() can fail if something during test grabs the focus.
	 * Let us just check that method setFocus() was called with the right control.
	 */
	class TestableTabFolderTraverseListener extends TabFolderTraverseListener {
		Control lastFocused;
		public TestableTabFolderTraverseListener(TabFolder tabFolder) {
			super(tabFolder);
		}

		@Override
		protected boolean setFocus(Control control) {
			super.setFocus(control);
			lastFocused = control;
			return true;
		}

	}
}
