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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 */
public class EditDomainWizardPage extends NewDomainWizardPage {

	public EditDomainWizardPage(DomainWizardModel model, IWizard wizard) {
		super("OpenShift Domain Name", "Please provide a new name for your OpenShift domain", model, wizard);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		super.doCreateControls(parent, dbc);

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
									new WebUIDomainPageNotFoundDialog(getShell(), getModel().getOriginWebUIDomainPageUrl()).open();
								}
								return Status.OK_STATUS;
							}
						})
						.schedule();
			}
		};
	}

	private class GetUrlJob extends Job {

		private String webUIDomainPageUrl;

		public GetUrlJob() {
			super("Get Domain Web UI URL");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			this.webUIDomainPageUrl = getModel().getWebUIDomainPageUrl();
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
					"Could not find the web page in the web console where you can edit your domain members.\n"
							+ "To get there manually, please log into the web console and open the page that shows the details of your domain.\n"
							+ (webUIDomainPageUrl != null ? "The url normally looks as follows:" : ""),
					MessageDialog.ERROR,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			this.webUIDomainPageUrl = webUIDomainPageUrl;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite container = new Composite(parent, SWT.None);
			GridLayoutFactory.fillDefaults().applyTo(container);
			if (webUIDomainPageUrl != null) {
				Link link = new Link(container, SWT.NONE);
				link.setText("<a>" + webUIDomainPageUrl + "</a>");
				link.addSelectionListener(onLinkClicked(webUIDomainPageUrl));
				GridDataFactory.fillDefaults().indent(60, 0).applyTo(link);
			}
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
