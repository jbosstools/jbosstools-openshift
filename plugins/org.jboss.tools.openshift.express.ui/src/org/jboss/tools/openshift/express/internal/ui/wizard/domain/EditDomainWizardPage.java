/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author André Dietisheim
 */
public class EditDomainWizardPage extends AbstractOpenShiftWizardPage {

	private EditDomainWizardModel pageModel;

	public EditDomainWizardPage(String title, String description, EditDomainWizardModel model, IWizard wizard) {
		super(title, description, "", wizard);
		this.pageModel = model;
	}

	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(parent);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(2).applyTo(parent);

		// domain name
		Label namespaceLabel = new Label(parent, SWT.NONE);
		namespaceLabel.setText(OpenshiftUIMessages.DomainName);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(namespaceLabel);
		Text namespaceText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(namespaceText);
		ISWTObservableValue namespaceTextObservable =
				WidgetProperties.text(SWT.Modify).observe(namespaceText);
		NamespaceValidator namespaceValidator = new NamespaceValidator(namespaceTextObservable);
		dbc.addValidationStatusProvider(namespaceValidator);
		ControlDecorationSupport.create(namespaceValidator, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		IObservableValue namespaceModelObservable =
				BeanProperties.value(EditDomainWizardModel.PROPERTY_DOMAIN_ID).observe(pageModel);
		ValueBindingBuilder
				.bind(namespaceTextObservable)
				.to(namespaceModelObservable)
				.in(dbc);

		// edit domain members
		Link editMembersLink = new Link(parent, SWT.NONE);
		editMembersLink.setText("<a>Edit domain members</a>");
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.LEFT, SWT.CENTER).applyTo(editMembersLink);
		editMembersLink.addSelectionListener(onEditMembers());
	}

	private SelectionListener onEditMembers() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final GetUrlJob getURLJob = new GetUrlJob();
				new JobChainBuilder(getURLJob)
						.runWhenSuccessfullyDone(new UIJob("Open Browser to Edit domain members") {

							@Override
							public IStatus runInUIThread(IProgressMonitor monitor) {
								String webUIDomainPage = getURLJob.getWebUIDomainPageUrl();
								if (webUIDomainPage != null) {
									new BrowserUtility().checkedCreateExternalBrowser(webUIDomainPage,
											OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
								} else {
									new WebUIDomainPageNotFoundDialog(getShell(), pageModel.getOriginWebUIDomainPageUrl()).open();
								}
								return Status.OK_STATUS;
							}
						})
						.schedule();
			}
		};
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}

	class NamespaceValidator extends MultiValidator {

		private final ISWTObservableValue domainNameObservable;

		public NamespaceValidator(ISWTObservableValue domainNameObservable) {
			this.domainNameObservable = domainNameObservable;
		}

		@Override
		protected IStatus validate() {
			final String domainName = (String) domainNameObservable.getValue();
			if (pageModel.isCurrentDomainId(domainName)) {
				return ValidationStatus.cancel(getDescription());
			}
			if (domainName.isEmpty()) {
				return ValidationStatus.cancel(
						OpenshiftUIMessages.EnterDomainName);
			}
			if (!StringUtils.isAlphaNumeric(domainName)) {
				return ValidationStatus.error(
						OpenshiftUIMessages.DomainNameMayHaveLettersAndDigits);
			}
			if (domainName.length() > 16) {
				return ValidationStatus.error(
						OpenshiftUIMessages.DomainNameMaximumLength);
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(domainNameObservable);
			return targets;
		}
	}

	private class GetUrlJob extends Job {

		private String webUIDomainPageUrl;

		public GetUrlJob() {
			super("Get Domain Web UI URL");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			this.webUIDomainPageUrl = pageModel.getWebUIDomainPageUrl();
			return Status.OK_STATUS;
		}

		public String getWebUIDomainPageUrl() {
			return webUIDomainPageUrl;
		}
	}

	private class WebUIDomainPageNotFoundDialog extends MessageDialog {

		private String webUIDomainPageUrl;

		public WebUIDomainPageNotFoundDialog(Shell parentShell, String webUIDomainPageUrl) {
			super(
					parentShell,
					"Could not find web console",
					null,
					"We could not find the web page in the web console where you can edit your domain members.\n"
							+ "To get there manually, please log into the web console and open the page that shows the details of your domain.\n"
							+ "The url normally looks as follows:",
					MessageDialog.ERROR,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			this.webUIDomainPageUrl = webUIDomainPageUrl;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite container = new Composite(parent, SWT.None);
			GridLayoutFactory.fillDefaults().applyTo(container);
			Link link = new Link(container, SWT.NONE);
			link.setText("<a>" + webUIDomainPageUrl + "</a>");
			link.addSelectionListener(onLinkClicked(webUIDomainPageUrl));
			GridDataFactory.fillDefaults().indent(60, 0).applyTo(link);
			return container;
		}

		private SelectionListener onLinkClicked(final String webUIDomainPageUrl) {
			return new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					new BrowserUtility().checkedCreateExternalBrowser(webUIDomainPageUrl,
							OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
				}

			};
		}

	}

}
