/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.v3;

import java.util.List;

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.BackButton;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.button.NextButton;
import org.jboss.reddeer.swt.impl.button.NoButton;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.tab.DefaultTabItem;
import org.jboss.reddeer.swt.impl.table.DefaultTable;
import org.jboss.reddeer.swt.impl.text.DefaultText;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.reddeer.swt.impl.tree.DefaultTreeItem;
import org.jboss.tools.openshift.reddeer.condition.TreeIsAvailable;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * Creator allows to create a new OpenShift 3 application in simplified way. It's required to provide mostly
 * only server, username and project together with template name or location on a file system.
 * 
 * @author mlabuda@redhat.com
 */
public class TemplatesCreator {

	private NewOpenShift3ApplicationWizard wizard;
	
	/**
	 * Initiates a new template creator to create a new OpenShift 3 application based on template.
	 * Wizard is opened from OpenShift explorer view.
	 * 
	 * @param server OpenShift 3 server
	 * @param username user name
	 * @param project project
	 */
	public TemplatesCreator() {
		this(false);
	}

	/**
	 * Initiates a new template creator to create a new OpenShift 3 application based on template.
	 * Wizard is opened from OpenShift explorer if openFromShellMenu is false. Otherwise wizard is 
	 * opened via shell menu.
	 * 
	 * @param server OpenShift 3 server
	 * @param username user name
	 * @param project project
	 * @param openFromShellMenu open wizard from shell menu, if false, opens it from OpenShift explorer
	 */
	public TemplatesCreator(boolean openFromShellMenu) {
		this(null, openFromShellMenu);
	}

	public TemplatesCreator(String project, boolean openFromShellMenu) {
		wizard = new NewOpenShift3ApplicationWizard();
		
		if (openFromShellMenu) {
			wizard.openWizardFromShellMenu(project);
		} else {
			wizard.openWizardFromExplorer(project);
		}
	}

	/**
	 * Creates new OpenShift 3 application from a local template.
	 * 
	 * @param templatePath path on local file system to a template
	 */
	public void createOpenShiftApplicationBasedOnLocalTemplate(String templatePath) {
		createOpenShiftApplicationBasedOnTemplate(false, null, templatePath, null, (TemplateParameter[]) null);
	}

	/**
	 * Creates new OpenShift 3 application from a local template with specific labels.
	 * 
	 * @param templatePath path on local file system to a template
	 * @param labels OpenShift labels
	 */
	public void createOpenShiftApplicationBasedOnLocalTemplate(String templatePath, List<Label> labels) {
		createOpenShiftApplicationBasedOnTemplate(false, null, templatePath, labels, (TemplateParameter[]) null);
	}
	
	/**
	 * Creates new OpenShift 3 application from a local template with specific parameters.
	 * 
	 * @param templatePath path on local file system to a template
	 * @param parameters template parameters
	 */
	public void createOpenShiftApplicationBasedOnLocalTemplate(String templatePath, TemplateParameter... parameters) {
		createOpenShiftApplicationBasedOnTemplate(false, null, templatePath, null, parameters);
	}
	
	/**
	 * Creates new OpenShift 3 application from a local template with specific labels and template parameters.
	 * 
	 * @param templatePath path on local file system to a template
	 * @param labels OpenShift labels
	 * @param parameters template parameters
	 */
	public void createOpenShiftApplicationBasedOnLocalTemplate(String templatePath, List<Label> labels,
			TemplateParameter... parameters) {
		createOpenShiftApplicationBasedOnTemplate(false, null, templatePath, labels, parameters);
	}
	
	/**
	 * Creates new OpenShift 3 application from a server template with matching name.
	 * 
	 * @param templateName name of a server template
	 */
	public void createOpenShiftApplicationBasedOnServerTemplate(String templateName) {
		createOpenShiftApplicationBasedOnTemplate(true, templateName, null, null, (TemplateParameter[]) null);
	}
	
	/**
	 * Creates new OpenShift 3 application from a server template with matching name.
	 * 
	 * @param templateName name of a server template
	 */
	public void createOpenShiftApplicationBasedOnServerTemplate(String templateName, boolean importProject) {
		createOpenShiftApplicationBasedOnTemplate(importProject, true, templateName, null, null, (TemplateParameter[]) null);
	}

	/**
	 * Creates new OpenShift 3 application from a server template with matching name and specific template parameters.
	 * 
	 * @param templateName name of a server template
	 * @param parameters template parameters
	 */
	public void createOpenShiftApplicationBasedOnServerTemplate(String templateName, TemplateParameter... parameters) {
		createOpenShiftApplicationBasedOnTemplate(true, templateName, null, null, parameters);
	}
	
	/**
	 * Creates new OpenShift 3 application from a server template with matching name and specific template parameters.
	 * 
	 * @param templateName name of a server template
	 * @param label list of OpenShift labels
	 */
	public void createOpenShiftApplicationBasedOnServerTemplate(String templateName, List<Label> labels) {
		createOpenShiftApplicationBasedOnTemplate(true, templateName, null, labels, (TemplateParameter[]) null);
	}
	
	/**
	 * Creates new OpenShift 3 application from a server template with matching name and specific labels
	 * and template parameters.
	 * @param <T>
	 * 
	 * @param templateName name of a server template
	 * @param labels list of OpenShift labels
	 * @param parameters template parameters
	 */
	public void createOpenShiftApplicationBasedOnServerTemplate(String templateName, List<Label> labels,
			TemplateParameter... parameters) {
		createOpenShiftApplicationBasedOnTemplate(true, templateName, null, labels, parameters);
	}

	private void createOpenShiftApplicationBasedOnTemplate(boolean serverTemplate, String templateName, 
			final String templateLocalPath, List<Label> labels, TemplateParameter... parameters) {
		createOpenShiftApplicationBasedOnTemplate(true, serverTemplate, templateName, templateLocalPath, labels, parameters);
	}

	private void createOpenShiftApplicationBasedOnTemplate(boolean importProject, boolean serverTemplate, String templateName, 
			final String templateLocalPath, List<Label> labels, TemplateParameter... parameters) {
		if (serverTemplate) {
			selectServerTemplate(templateName);
		} else {
			selectLocalTemplate(templateLocalPath);
		}		
		
		new WaitUntil(new WidgetIsEnabled(new NextButton()), TimePeriod.NORMAL);
		new NextButton().click();

		new WaitUntil(new WidgetIsEnabled(new BackButton()), TimePeriod.LONG);
		if (parameters != null && parameters.length != 0) {
			setTemplateParameters(parameters);
		}
		new NextButton().click();

		new WaitWhile(new WidgetIsEnabled(new NextButton()), TimePeriod.LONG);
		if (labels != null && labels.size() != 0) {
			createOpenShiftLabels(labels);
		}
		
		new FinishButton().click();
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.APPLICATION_SUMMARY), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.APPLICATION_SUMMARY);
		new OkButton().click();
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));

		executeImport(importProject);
	}

	private void selectLocalTemplate(final String templateLocalPath) {
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		
		new WaitWhile(new TreeIsAvailable());
		
		new LabeledText(OpenShiftLabel.TextLabels.SELECT_LOCAL_TEMPLATE).setText(templateLocalPath);
			
	}

	private void selectServerTemplate(String templateName) {
		new DefaultTabItem(OpenShiftLabel.TextLabels.SERVER_TEMPLATE).activate();
		
		new WaitUntil(new TreeIsAvailable());
		
		new DefaultTree().selectItems(new DefaultTreeItem(templateName));
	}

	private void setTemplateParameters(TemplateParameter[] parameters) {
		for (TemplateParameter parameter: parameters) {
				new DefaultTable().select(parameter.getName());
				
				new WaitUntil(new WidgetIsEnabled(new PushButton(OpenShiftLabel.Button.EDIT)));
				
				new PushButton(OpenShiftLabel.Button.EDIT).click();
				
				new DefaultShell(OpenShiftLabel.Shell.EDIT_TEMPLATE_PARAMETER);
				new DefaultText().setText(parameter.getValue());
				
				new WaitUntil(new WidgetIsEnabled(new OkButton()));
				
				new OkButton().click();
				
				new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.EDIT_TEMPLATE_PARAMETER));

				new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		}
	}

	private void createOpenShiftLabels(List<Label> labels) {
		for (Label label: labels) {
			new PushButton(OpenShiftLabel.Button.ADD).click();
			
			new DefaultShell(OpenShiftLabel.Shell.RESOURCE_LABEL);
			new LabeledText(OpenShiftLabel.TextLabels.LABEL).setText(label.getName());
			new LabeledText(OpenShiftLabel.TextLabels.VALUE).setText(label.getValue());
			
			new WaitUntil(new WidgetIsEnabled(new OkButton()));
			new OkButton().click();
			
			new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.RESOURCE_LABEL));
			
			new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		}
	}
	
	private void executeImport(boolean importProject) {
		new DefaultShell(OpenShiftLabel.Shell.IMPORT_APPLICATION);
		if (!importProject) {
			new CancelButton().click();
		} else {
			new FinishButton().click();
			try {
				new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.CHEATSHEET), TimePeriod.LONG);
				
				new DefaultShell(OpenShiftLabel.Shell.CHEATSHEET);
				new CheckBox(0).click();
				new NoButton().click();
				
				new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.CHEATSHEET));
			} catch (WaitTimeoutExpiredException ex) {
				// do nothing if cheat sheet is not provided
			}

			new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
			new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		}
	}
}
