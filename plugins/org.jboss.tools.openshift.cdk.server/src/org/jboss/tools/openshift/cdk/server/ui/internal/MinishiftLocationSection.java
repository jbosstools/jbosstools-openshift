/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.cdk.server.ui.internal;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader.MinishiftVersions;

public class MinishiftLocationSection extends AbstractLocationSection {

	private static String SECTION_TITLE = "CDK Details";
	private static String LABEL_STRING = "Minishift Binary: ";
	private static String COMMAND_NAME = "Modify Minishift Location";
	private static String LOC_ATTR = CDK3Server.MINISHIFT_FILE;

	private Combo hypervisorCombo;

	private Job longValidation;
	private MinishiftVersions minishiftVersionProps = null;

	Text msHomeText;
	Button msHomeBrowse;
	ControlDecoration msHomeDecor;
	SelectionListener msHomeSelListener;
	ModifyListener msHomeModListener;

	public MinishiftLocationSection() {
		super(SECTION_TITLE, LABEL_STRING, COMMAND_NAME, LOC_ATTR);
	}

	@Override
	protected void fillUI(FormToolkit toolkit, Composite composite) {
		createHypervisorWidgets(toolkit, composite);
		createLocationWidgets(toolkit, composite);
		createMinishiftHomeWidgets(toolkit, composite);
	}

	protected void createHypervisorWidgets(FormToolkit toolkit, Composite composite) {
		toolkit.createLabel(composite, "Hypervisor:");
		hypervisorCombo = new Combo(composite, SWT.READ_ONLY);
		hypervisorCombo.setLayoutData(GridDataFactory.defaultsFor(hypervisorCombo).span(4, 1).create());
		hypervisorCombo.setItems(CDK3Server.getHypervisors());
	}

	protected void createMinishiftHomeWidgets(FormToolkit toolkit, Composite composite) {
		toolkit.createLabel(composite, "Minishift Home:");
		msHomeText = toolkit.createText(composite, "", SWT.SINGLE | SWT.BORDER);
		msHomeBrowse = toolkit.createButton(composite, "Browse...", SWT.PUSH);

		msHomeText.setLayoutData(GridDataFactory.defaultsFor(msHomeText).span(3, 1).minSize(150, SWT.DEFAULT).create());

		msHomeDecor = new ControlDecoration(msHomeText, SWT.TOP | SWT.LEFT);
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		Image img = fieldDecoration.getImage();
		msHomeDecor.setImage(img);
		msHomeDecor.hide(); // hiding it initially
	}

	@Override
	protected void setDefaultValues() {
		// set initial values
		super.setDefaultValues();
		String hyp = server.getAttribute(CDK3Server.PROP_HYPERVISOR, CDK3Server.getHypervisors()[0]);
		int ind = Arrays.asList(CDK3Server.getHypervisors()).indexOf(hyp);
		if (ind != -1) {
			hypervisorCombo.select(ind);
		}
		String defMSHome = new Path(System.getProperty("user.home")).append(".minishift").toOSString();
		String msHome = server.getAttribute(CDK3Server.MINISHIFT_HOME, defMSHome);
		msHomeText.setText(msHome);
	}

	@Override
	protected void addListeners() {
		super.addListeners();
		SelectionListener sl = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				execute(new SetHypervisorPropertyCommand(server, this));
			}
		};
		hypervisorCombo.addSelectionListener(sl);

		msHomeSelListener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseClicked(msHomeText, FOLDER);
				validate();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		msHomeBrowse.addSelectionListener(msHomeSelListener);

		msHomeModListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				execute(new SetMinishiftHomePropertyCommand(server));
			}
		};
		msHomeText.addModifyListener(msHomeModListener);
		msHomeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});

	}

	public class SetMinishiftHomePropertyCommand
			extends org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyTextCommand {
		public SetMinishiftHomePropertyCommand(IServerWorkingCopy server) {
			super(server, "Change Minishift Home", msHomeText, msHomeText.getText(), CDK3Server.MINISHIFT_HOME,
					msHomeModListener);
		}
	}

	public class SetHypervisorPropertyCommand
			extends org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyComboCommand {
		public SetHypervisorPropertyCommand(IServerWorkingCopy server, SelectionListener sl) {
			super(server, "Change hypervisor", hypervisorCombo,
					hypervisorCombo.getItem(hypervisorCombo.getSelectionIndex()), CDK3Server.PROP_HYPERVISOR, sl);
		}
	}

	@Override
	protected void locationBrowseClicked() {
		browseClicked(getLocationText(), FILE);
	}

	@Override
	protected void validate() {
		kickValidationJob();
	}

	protected void validate2() {
		String s = getErrorString();
		if (s == null) {
			txtDecorator.hide();
		} else {
			txtDecorator.setDescriptionText(s);
			txtDecorator.show();
		}
	}

	private synchronized void kickValidationJob() {
		if (longValidation != null) {
			longValidation.cancel();
		}
		Text t = getLocationText();
		if (t == null || t.isDisposed()) {
			return;
		}
		File f = new File(t.getText());
		if (!f.exists() || !f.canExecute()) {
			validate2();
			return;
		}
		String homeDir = t.getText();

		longValidation = new Job("Validate minishift location") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				minishiftVersionProps = MinishiftVersionLoader.getVersionProperties(homeDir);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						validate2();
					}
				});
				return Status.OK_STATUS;
			}
		};
		longValidation.setSystem(true);
		longValidation.schedule(750);
	}

	protected String getErrorString() {
		// Subclass override
		Text t = getLocationText();
		if (t != null && !t.isDisposed()) {
			String v = t.getText();
			File f = new File(v);
			if (!f.exists()) {
				return "File " + v + " does not exist.";
			} else if (!f.canExecute()) {
				return "File " + v + " is not executable.";
			} else if (minishiftVersionProps == null) {
				return "Unknown error when checking minishift version: " + v;
			} else if (!minishiftVersionProps.isValid()) {
				String err = minishiftVersionProps.getError();
				if (err == null) {
					err = "Unknown error while checking minishift version";
				}
				return err;
			} else {
				String versionCompatError = isVersionCompatible(minishiftVersionProps);
				if (versionCompatError != null)
					return versionCompatError;
			}
		}
		return null;
	}

	protected String isVersionCompatible(MinishiftVersions versions) {
		String cdkVers = versions.getCDKVersion();
		if (cdkVers == null) {
			return "Cannot determine CDK version.";
		}
		if (CDK3Server.matchesCDK3(cdkVers)) {
			return null;
		}
		return "CDK version " + cdkVers + " is not compatible with this server adapter.";
	}

	@Override
	public IStatus[] getSaveStatus() {
		String err = getErrorString();
		if (err != null) {
			return new Status[] { new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, err) };
		}
		return new IStatus[] { Status.OK_STATUS };
	}

}
