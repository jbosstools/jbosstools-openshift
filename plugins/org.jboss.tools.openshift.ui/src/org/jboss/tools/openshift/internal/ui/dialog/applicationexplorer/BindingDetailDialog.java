package org.jboss.tools.openshift.internal.ui.dialog.applicationexplorer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.openshift.core.odo.Binding;

public class BindingDetailDialog extends Dialog {

	private Text bindingName;
	private Text serviceName;

	private ListViewer environmentVariables;
	private Binding binding;

	public BindingDetailDialog(Shell parentShell, Binding binding) {
		super(parentShell);
		this.binding = binding;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		Label bindingNameLabel = new Label(container, SWT.None);
		bindingNameLabel.setText("Binding name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(bindingNameLabel);

		bindingName = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(bindingName);

		Label serviceNameLabel = new Label(container, SWT.None);
		serviceNameLabel.setText("Bound service:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(serviceNameLabel);

		serviceName = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(serviceName);

		Label environmentVariablesLabel = new Label(container, SWT.None);
		environmentVariablesLabel.setText("Environment variables:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(environmentVariablesLabel);

		environmentVariables = new ListViewer(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		environmentVariables.setContentProvider(new ArrayContentProvider());
		environmentVariables.setLabelProvider(new LabelProvider());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true)
				.applyTo(environmentVariables.getControl());

		return container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Binding");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	@Override
	public void create() {
		super.create();
		bindingName.setText(binding.getName());
		serviceName.setText(binding.getService().getName());
		environmentVariables.setInput(binding.getEnvironmentVariables());
	}

}