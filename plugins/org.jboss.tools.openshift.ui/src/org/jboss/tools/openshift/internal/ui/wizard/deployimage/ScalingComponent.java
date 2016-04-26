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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class ScalingComponent {
	String label = "Replicas:";
	String tooltip = "Replicas are the number of copies of an image that will be scheduled to run on OpenShift";
	Spinner replicas;

	boolean inline = false;

	public ScalingComponent() {}

	/**
	 * When set to true, the component does not fill the available vertical area.
	 * 
	 * @param b
	 * @return
	 */
	public ScalingComponent inline(boolean b) {
		this.inline = b;
		return this;
	}

	public ScalingComponent create(Composite parent) {
		Composite scalingContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, inline ? SWT.FILL : SWT.CENTER)
			.grab(true, !inline)
			.applyTo(scalingContainer);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(6, 6).applyTo(scalingContainer);
		
		Label lblReplicas = new Label(scalingContainer, SWT.NONE);
		lblReplicas.setText(label);
		lblReplicas.setToolTipText(tooltip);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER)
			.applyTo(lblReplicas);
		
		replicas = new Spinner(scalingContainer, SWT.BORDER);
		replicas.setMinimum(1);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(replicas);
		return this;
	}

	public Spinner getSpinner() {
		return replicas;
	}
}
