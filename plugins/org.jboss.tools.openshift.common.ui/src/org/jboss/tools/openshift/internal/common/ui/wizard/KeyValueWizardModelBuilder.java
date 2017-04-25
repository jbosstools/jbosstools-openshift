/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.wizard;

import org.eclipse.core.databinding.validation.IValidator;

/**
 * A builder class to facilitate creating the model for a
 * key/value pair dialog
 * @author jeff.cantrill
 *
 * @param <T extends IKeyValueItem> A type that is being edited
 */
public class KeyValueWizardModelBuilder<T extends IKeyValueItem> {
	
	private String title;
	private String description;
	private IValidator keyAfterConvertValidator;
	private String groupLabel;
	private IValidator valueAfterConvertValidator;
	private String key;
	private String value;
	private String keyLabel = "Name";
	private String valueLabel = "Value";
	private String keyDescription;
	private String valueDescription;
	private String windowTitle = "";
	private boolean keyEditable = true;

	public KeyValueWizardModelBuilder() {
		this(null);
	}
	
	public KeyValueWizardModelBuilder(T  label) {
		if (label == null) {
			return;
		}
		this.key = label.getKey();
		this.value = label.getValue();
	}
	
	public  IKeyValueWizardModel<T> build(){
		KeyValueWizardModel<T> model = new KeyValueWizardModel<>(
				windowTitle,
				title,
				keyLabel, 
				valueLabel,
				description,
				groupLabel,
				keyAfterConvertValidator,
				valueAfterConvertValidator,
				keyEditable);
		model.setKey(key);
		model.setValue(value);
		model.setKeyDescription(keyDescription);
		model.setValueDescription(valueDescription);
		return model;
	}		
	public KeyValueWizardModelBuilder<T> editableKey(boolean editable){
		this.keyEditable  = editable;
		return this;
	}
	public KeyValueWizardModelBuilder<T>  keyLabel(String label) {
		this.keyLabel = label;
		return this;
	}
	
	public KeyValueWizardModelBuilder<T>  valueLabel(String label) {
		this.valueLabel = label;
		return this;
	}
	
	public KeyValueWizardModelBuilder<T>  keyDescription(String keyDescription) {
		this.keyDescription = keyDescription;
		return this;
	}
	
	public KeyValueWizardModelBuilder<T>  valueDescription(String valueDescription) {
		this.valueDescription = valueDescription;
		return this;
	}
	
	public KeyValueWizardModelBuilder<T>  title(String title) {
		this.title = title;
		return this;
	}
	
	public KeyValueWizardModelBuilder<T> description(String description) {
		this.description = description;
		return this;
	}

	public KeyValueWizardModelBuilder<T>  keyAfterConvertValidator(IValidator validator) {
		this.keyAfterConvertValidator = validator;
		return this;
	}	

	public KeyValueWizardModelBuilder<T> valueAfterConvertValidator(IValidator validator) {
		this.valueAfterConvertValidator = validator;
		return this;
	}

	public KeyValueWizardModelBuilder<T> groupLabel(String groupLabel) {
		this.groupLabel = groupLabel;
		return this;
	}
	
	public KeyValueWizardModelBuilder<T> windowTitle(String windowTitle) {
		this.windowTitle = windowTitle;
		return this;
	}
}
