/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.widget;

import org.eclipse.swt.widgets.Control;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.core.lookup.ShellLookup;
import org.jboss.reddeer.core.matcher.AndMatcher;
import org.jboss.reddeer.core.matcher.WithTextMatcher;
import org.jboss.reddeer.core.reference.ReferencedComposite;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.shell.AbstractShell;

public class ShellWithButton extends AbstractShell {
	
	public ShellWithButton(String title, String buttonLabel) {
		super(lookForShellWithButton(title, buttonLabel));
		setFocus();
	}
	
	private static org.eclipse.swt.widgets.Shell lookForShellWithButton(
			final String title, final String buttonLabel) {
		
		Matcher<String> titleMatcher = new WithTextMatcher(title);
		Matcher<String> buttonMatcher = new BaseMatcher<String>() {
			@Override
			public boolean matches(Object obj) {
				if (obj instanceof Control) {
					final Control control = (Control) obj;
					ReferencedComposite ref = new ReferencedComposite() {
						@Override
						public Control getControl() {
							return control;
						}
					};
					try {
						new PushButton(ref, buttonLabel);
						return true;
					} catch (CoreLayerException e) {
						// ok, this control doesn't contain the button
					}
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("containing button '" + buttonLabel + "'");
			}
		};
		@SuppressWarnings("unchecked")
		Matcher<String> matcher = new AndMatcher(titleMatcher, buttonMatcher);
		return ShellLookup.getInstance().getShell(matcher);
	}
}
