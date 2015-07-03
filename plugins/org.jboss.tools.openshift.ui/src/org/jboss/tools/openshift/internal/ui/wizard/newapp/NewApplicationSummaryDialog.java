/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryContentProvider;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryLabelProvider;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class NewApplicationSummaryDialog extends ResourceSummaryDialog {

	private CreateApplicationFromTemplateJob job;

	public NewApplicationSummaryDialog(Shell parentShell, CreateApplicationFromTemplateJob job, String message) {
		super(parentShell, job.getResources(),  "Create Application Summary", message,  new ResourceSummaryLabelProvider(), new ResourceSummaryContentProvider());
		this.job = job;
	}

	@Override
	protected void createAreaAfterResourceSummary(Composite parent) {
		if(job.getParameters().size() == 0) {
			return;
		}
		
		Composite area = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(area);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(area);
		
		Label lblParams = new Label(area, SWT.NONE);
		lblParams.setText("Please make note of the following parameters which may \ninclude values required to administer your resources:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(lblParams);
		
		Composite parameters = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults()
			.hint(100, 250)
			.grab(true, true)
			.applyTo(parameters);
		
		TableViewer viewer = TemplateParametersPage.createTable(parameters);
		viewer.setInput(job.getParameters());
	}
	
	
}
