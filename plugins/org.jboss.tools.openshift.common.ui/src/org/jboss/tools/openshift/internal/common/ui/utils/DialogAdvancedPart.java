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
package org.jboss.tools.openshift.internal.common.ui.utils;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public abstract class DialogAdvancedPart {
	protected Button advancedButton;
	protected DialogChildVisibilityAdapter advancedSectionVisibilityAdapter;

	public DialogAdvancedPart() {
	}

	public final void createAdvancedGroup(Composite parent, int numColumns) {
		// advanced button
		this.advancedButton = new Button(parent, SWT.NONE);
		advancedButton.setText(getAdvancedButtonLabel(false));
		GridDataFactory.fillDefaults()
				.align(SWT.BEGINNING, SWT.CENTER).span(numColumns, 1).applyTo(advancedButton);

		// advanced composite
		Composite advancedComposite = new Composite(parent, SWT.NONE);
		GridData advancedCompositeGridData = GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).span(numColumns, 1).create();
		advancedComposite.setLayoutData(advancedCompositeGridData);
		adjustAdvancedCompositeLayout(GridLayoutFactory.fillDefaults()).applyTo(advancedComposite);
		
		this.advancedSectionVisibilityAdapter = new DialogChildVisibilityAdapter(advancedComposite, false);
		advancedButton.addSelectionListener(onAdvancedClicked());
		
		createAdvancedContent(advancedComposite);
	}

	protected GridLayoutFactory adjustAdvancedCompositeLayout(GridLayoutFactory gridLayoutFactory) {
		return gridLayoutFactory;
	}

	private SelectionListener onAdvancedClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				showAdvancedSection(!advancedSectionVisibilityAdapter.isVisible());
			}
		};
	}

	public void showAdvancedSection(boolean visible) {
		advancedSectionVisibilityAdapter.setVisible(visible);
		advancedButton.setText(getAdvancedButtonLabel(visible));
	}

	protected String getAdvancedButtonLabel(boolean visible) {
		if (visible) {
			return " << Advanced ";
		} else {
			return " Advanced >> ";
		}
	}

	protected abstract void createAdvancedContent(Composite advancedComposite);
}
