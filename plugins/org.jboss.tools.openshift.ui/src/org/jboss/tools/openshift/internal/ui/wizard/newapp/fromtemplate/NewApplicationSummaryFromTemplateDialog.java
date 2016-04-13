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
package org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.ICellToolTipProvider;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryContentProvider;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryLabelProvider;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;
import org.jboss.tools.openshift.internal.ui.webhooks.WebHooksDialog;
import org.jboss.tools.openshift.internal.ui.webhooks.WebhookUtil;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateParameterViewerUtils.ParameterNameViewerComparator;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;

/**
 * 
 * @author jeff.cantrill
 * @author Andre Dietisheim
 * @author Fred Bricon
 */
public class NewApplicationSummaryFromTemplateDialog extends ResourceSummaryDialog {

	private static final int COPIED_NOTIFICATION_SHOW_DURATION = 2*1000;
	
	private CreateApplicationFromTemplateJob job;

	public NewApplicationSummaryFromTemplateDialog(Shell parentShell, CreateApplicationFromTemplateJob job, String message) {
		super(parentShell, job.getResources(),  "Create Application Summary", message,  new ResourceSummaryLabelProvider(), new ResourceSummaryContentProvider());
		this.job = job;
	}

	@Override
	protected void createAreaAfterResourceSummary(Composite parent) {
		
		Composite area = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(area);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(area);
		
		final Collection<IBuildConfig> buildConfigs = findBuildConfigsWithWebHooks();
		if (!buildConfigs.isEmpty()) {
			Link webHooksLink = new Link(area, SWT.NONE);
			webHooksLink.setText("Click <a>here</a> to display the webhooks available to automatically trigger builds.");
			GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(webHooksLink);
			webHooksLink.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					WebHooksDialog dialog = new WebHooksDialog(getParentShell(), buildConfigs);
					dialog.open();
				}
			});
		}

		if(job.getParameters().isEmpty()) {
			return;
		}

		Label lblParams = new Label(area, SWT.WRAP);
		lblParams.setText("Please make note of the following parameters which may include values required to administer your resources:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.TOP).hint(100, SWT.DEFAULT).grab(true, false).applyTo(lblParams);

		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).equalWidth(false).applyTo(container);
		
		Composite parameters = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults()
			.hint(100, 200)
			.grab(true, true)
			.applyTo(parameters);

		TableViewer viewer = createTable(parameters);
		viewer.setInput(job.getParameters());

		Button copyToClipboard = new Button(container, SWT.PUSH);
		copyToClipboard.setImage(OpenShiftImages.COPY_TO_CLIPBOARD_IMG);
		copyToClipboard.setToolTipText("Copy parameters to clipboard");
		copyToClipboard.addSelectionListener(onClickCopyButton(lblParams));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(false, false).applyTo(copyToClipboard);
	}
	
	private SelectionAdapter onClickCopyButton(final Control control) {
	  return new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
	    	List<IParameter> params = new ArrayList<>(job.getParameters());
	    	Collections.sort(params, new Comparator<IParameter>() {

				@Override
				public int compare(IParameter p1, IParameter p2) {
					return p1.getName().compareTo(p2.getName());
				}
			});
	    	String text = getAsString(params);
	        copyToClipBoard(control, text, "Parameters copied to clipboard");
	    }
	  };
	}

	private static String getAsString(Collection<IParameter> parameters) {
	  StringBuilder content = new StringBuilder();
	  for (IParameter param : parameters) {
	    content.append(getAsString(param)).append("\r\n");
	  }
	  return content.toString();
	}

	private static String getAsString(IParameter param) {
	  StringBuilder content = new StringBuilder(param.getName());
	  content.append(": ").append(param.getValue());
	  return content.toString();
	}
	 
	private void copyToClipBoard(Control control, String text, String notification) {
	  copyToClipBoard(text);
	  notifyCopied(control, notification);
	}

	private void notifyCopied(Control control, String notification) {
	  DefaultToolTip copiedNotification = new DefaultToolTip(control, ToolTip.NO_RECREATE, true);
	  copiedNotification.setText(notification);
	  copiedNotification.setHideDelay(COPIED_NOTIFICATION_SHOW_DURATION);
	  copiedNotification.show(control.getLocation());
	  copiedNotification.deactivate();
	}

	private void copyToClipBoard(String text) {
	  Clipboard clipboard = new Clipboard(Display.getCurrent());
	  Object[] data = new Object[] { text };
	  Transfer[] dataTypes = new Transfer[] { TextTransfer.getInstance() };
	  clipboard.setContents(data, dataTypes);
	  clipboard.dispose();
	}
 
	private Collection<IBuildConfig> findBuildConfigsWithWebHooks() {
		Set<IBuildConfig> buildConfigs = new LinkedHashSet<>();
		for (IResource r : job.getResources()) {
			if (r instanceof IBuildConfig && !WebhookUtil.getWebHooks((IBuildConfig)r).isEmpty()) {
				buildConfigs.add((IBuildConfig)r);
			}
		}
		return buildConfigs;
	}

	public TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		ICellToolTipProvider<IParameter> cellToolTipProvider = new ICellToolTipProvider<IParameter>() {

			@Override
			public String getToolTipText(IParameter object) {
				return object.getDescription();
			}

			@Override
			public int getToolTipDisplayDelayTime(IParameter object) {
				return 0;
			}
		};
		TableViewer viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IParameter>() {

					@Override
					public String getValue(IParameter variable) {
						return variable.getName();
					}})
					.cellToolTipProvider(cellToolTipProvider)
					.name("Name")
					.align(SWT.LEFT).weight(2).minWidth(100)
					.buildColumn()
				.column(new IColumnLabelProvider<IParameter>() {

					@Override
					public String getValue(IParameter parameter) {
						return TemplateParameterViewerUtils.getValueLabel(parameter);
					}})
					.cellToolTipProvider(cellToolTipProvider)
					.name("Value")
					.align(SWT.LEFT).weight(2).minWidth(100)
					.buildColumn()
				.buildViewer();
		viewer.setComparator(new ParameterNameViewerComparator());

		viewer.addDoubleClickListener(onDoubleClick(table));
		return viewer;
	}

	 private IDoubleClickListener onDoubleClick(final Control control) {
	    return new IDoubleClickListener() {
	      @Override
	      public void doubleClick(DoubleClickEvent event) {
	        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
	        IParameter param = (IParameter) selection.getFirstElement();
	        if (param != null) {
	        	String text = param.getValue();
	        	if (StringUtils.isNotBlank(text)) {
	        		String notification = param.getName() + " value copied to clipboard";
	        		copyToClipBoard(control, text, notification);
	        	}
	        }
	      }
	    };
	}
}
