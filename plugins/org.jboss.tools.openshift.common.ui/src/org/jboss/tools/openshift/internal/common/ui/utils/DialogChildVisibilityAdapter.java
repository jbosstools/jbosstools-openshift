/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * An adapter that toggles the dialog height when showing/hiding a given child control.
 * 
 * 
 * @author Andre Dietisheim
 */
public class DialogChildVisibilityAdapter {

	private boolean visible;
	private final Composite composite;
	private final GridData gridData;
	private final Shell shell;
	private int invisibleChildShellHeight;
	private boolean resizing;
	
	public DialogChildVisibilityAdapter(Composite child, boolean visible) {
		Assert.isTrue(child != null && !child.isDisposed());
		this.composite = child;
		Object layoutData = child.getLayoutData();
		Assert.isTrue(layoutData instanceof GridData, "only supports GridLayout");
		this.gridData = (GridData) layoutData;
		gridData.exclude = !visible;
		Assert.isTrue(child.getShell() != null	&& !child.getShell().isDisposed());
		this.shell = child.getShell();
		shell.addControlListener(onShellResized(shell));
		this.invisibleChildShellHeight = computeChildHeight(visible, child, shell);
		this.visible = visible;
		child.setVisible(visible);
	}
	
	private ControlListener onShellResized(final Shell shell) {
		final ControlListener listener = new ControlAdapter() {

			@Override
			public void controlResized(ControlEvent e) {
				if (!resizing) {
					DialogChildVisibilityAdapter.this.invisibleChildShellHeight = shell.getSize().y;
				}
			}

		};
		
		shell.addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				shell.removeControlListener(listener);
			}
		});
		return listener;
	}

	private int computeChildHeight(boolean visible, Composite child, Shell shell) {
		Point size = shell.getSize();
		if (visible) {
			Point childSize = child.computeSize(child.getSize().x, SWT.DEFAULT);
			size.y = size.y - childSize.y ;
		} 
		return size.y;
	}

	public void setVisible(boolean visible) {
		if (this.visible == visible) {
			return;
		}
		toggle();
	}
	
	public boolean toggle() {
		this.resizing = true;
		this.visible = !visible;
		composite.setVisible(visible);
		gridData.exclude = !visible;
		int newShellHeight= computeShellHeight(shell.getSize());
		shell.setSize(shell.getSize().x, newShellHeight);
		shell.layout(true, true);
		this.resizing = false;
		return visible;
	}

	protected int computeShellHeight(Point shellSize) {
		if (visible) {
			return shell.computeSize(shellSize.x, SWT.DEFAULT, true).y;
		} else {
			return new Point(shellSize.x, invisibleChildShellHeight).y;
		}
	}

	public boolean isVisible() {
		return visible;
	}
	
}
