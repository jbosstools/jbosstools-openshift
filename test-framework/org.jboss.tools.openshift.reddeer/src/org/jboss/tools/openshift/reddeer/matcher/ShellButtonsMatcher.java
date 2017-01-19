/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Matches the buttons in a shell for the given labels.
 * 
 * @author adietish@redhat.com
 *
 */
public class ShellButtonsMatcher extends BaseMatcher<String> {

	private Collection<String> buttonLabels;

	public ShellButtonsMatcher(Collection<String> buttonLabels) {
		this.buttonLabels = buttonLabels;
	}

	@Override
	public boolean matches(Object item) {
		if (!(item instanceof Shell)) {
			return false;
		}
		final Shell shell = (Shell) item;
		final boolean[] result = new boolean[] { false };
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				result[0] = findButtons(shell.getChildren(), buttonLabels).isEmpty();
			}
		});
		return result[0];
	}

	private Collection<String> findButtons(Control[] controls, Collection<String> buttonLabels) {
		for (Control control : controls) {
			if (buttonLabels.isEmpty()) {
				return buttonLabels;
			}

			if (control instanceof Composite) {
				buttonLabels = findButtons(((Composite) control).getChildren(), buttonLabels);
			}
			if (control instanceof org.eclipse.swt.widgets.Button) {
				buttonLabels = matchLabels(buttonLabels, (org.eclipse.swt.widgets.Button) control);
			}
		}
		return buttonLabels;
	}

	private List<String> matchLabels(Collection<String> labels, org.eclipse.swt.widgets.Button button) {
		List<String> labelsNotFound = new ArrayList<>();
		for (String label : labels) {
			String buttonLabel = button.getText().replace("&", "");
			if (!buttonLabel.equals(label)) {
				labelsNotFound.add(label);
			}
		}
		return labelsNotFound;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(NLS.bind("shell contains button with labeled {0}", buttonLabels));
	}
}
