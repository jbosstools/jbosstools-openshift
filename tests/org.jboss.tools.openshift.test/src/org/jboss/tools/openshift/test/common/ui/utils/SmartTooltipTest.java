/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.jboss.tools.openshift.test.common.ui.utils;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.openshift.internal.common.ui.utils.SmartTooltip;
import org.junit.Assert;
import org.junit.Test;

public class SmartTooltipTest {
	static final String SHORT_TEXT = "short text";
	static final String LONG_TEXT = "long text long text long text long text long text";
	static final String CUSTOM_TOOLTIP = "my custom tooltip";

	@Test
	public void testSmartTooltip() {
		TestWindow window = new TestWindow(null);
		window.create();
		window.getShell().setSize(200, 100);
		Text text = window.text;
		SmartTooltip tooltip = SmartTooltip.get(text);
		Assert.assertNotNull(tooltip);
		
		try {
			window.open();
			Assert.assertNull(text.getToolTipText());

			//1. Test that on short text there is no tooltip.
			text.setText(SHORT_TEXT);
			Assert.assertNull(text.getToolTipText());

			//2. Test that on long text there is tooltip.
			text.setText(LONG_TEXT);
			Assert.assertEquals(LONG_TEXT, text.getToolTipText());
			
			//3. Test that if custom tooltip is assigned, it is respected.
			tooltip.setToolTip(CUSTOM_TOOLTIP);
			Assert.assertEquals(LONG_TEXT + "\n" + CUSTOM_TOOLTIP, text.getToolTipText());

			//4. Test that if window is made wide enough, only custom tooltip remains.
			window.getShell().setSize(800, 100);
			Assert.assertEquals(CUSTOM_TOOLTIP, text.getToolTipText());

			//5. Test that if window is made narrow again, smart tooltip returns.
			window.getShell().setSize(200, 100);
			Assert.assertEquals(LONG_TEXT + "\n" + CUSTOM_TOOLTIP, text.getToolTipText());

			//6. Test that if smart tooltip is disabled, only custom tooltip remains.
			tooltip.setEnabled(false);
			Assert.assertEquals(CUSTOM_TOOLTIP, text.getToolTipText());

			//7. Test that if smart tooltip is disabled, it appears.
			tooltip.setEnabled(true);
			Assert.assertEquals(LONG_TEXT + "\n" + CUSTOM_TOOLTIP, text.getToolTipText());
		} finally {
			window.close();
		}
	}

	class TestWindow extends Window {
		Text text;

		protected TestWindow(Shell shell) {
			super(shell);
		}
		
		protected Control createContents(Composite parent) {
			Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(container);
			GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(container);
			text = new Text(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER);
			new SmartTooltip(text);
			return container;
		}
	}
}
