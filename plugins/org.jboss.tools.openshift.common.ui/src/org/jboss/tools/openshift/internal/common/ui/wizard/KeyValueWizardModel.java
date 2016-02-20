/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.wizard;

import org.eclipse.core.databinding.validation.IValidator;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

/**
 * Wizard that lists the environment variables and edit, add, remove them.
 * 
 * @author jeff.cantrill
 * 
 * @param <T>  An IKeyValueItem type
 */
public class KeyValueWizardModel<T extends IKeyValueItem> extends ObservableUIPojo implements IKeyValueWizardModel<T>{

	private String key;
	private String value;
	private String description;
	private String keyDescription;
	private String valueDescription;
	private String groupLabel;
	private IValidator keyAfterConvertValidator;
	private IValidator valueAfterConvertValidator;
	private String keyLabel;
	private String valueLabel;
	private String title;
	private String windowTitle;
	private boolean keyEditable;

	public KeyValueWizardModel(String windowTitle, 
			String title, String keyLabel, String valueLabel, String description, String groupLabel, 
			IValidator keyAfterConvertValidator, 
			IValidator valueAfterConvertValidator, boolean keyEditable) {
		this.windowTitle = windowTitle;
		this.title = title;
		this.keyLabel = keyLabel;
		this.valueLabel = valueLabel;
		this.description = description;
		this.groupLabel = groupLabel;
		this.keyAfterConvertValidator = keyAfterConvertValidator;
		this.valueAfterConvertValidator = valueAfterConvertValidator;
		this.keyEditable = keyEditable;
	}

	@Override
	public String getWindowTitle() {
		return windowTitle;
	}



	@Override
	public String getTitle() {
		return title;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getKeyDescription() {
		return keyDescription;
	}

	public void setKeyDescription(String s) {
		keyDescription = s;
	}

	@Override
	public String getValueDescription() {
		return valueDescription;
	}
	
	public void setValueDescription(String s) {
		valueDescription = s;
	}

	@Override
	public String getGroupLabel() {
		return groupLabel;
	}
	
	@Override
	public IValidator getKeyAfterConvertValidator() {
		return keyAfterConvertValidator;
	}

	@Override
	public IValidator getValueAfterConvertValidator() {
		return valueAfterConvertValidator;
	}

	@Override
	public String getKeyLabel() {
		return keyLabel;
	}

	@Override
	public String getValueLabel() {
		return valueLabel;
	}
	
	@Override
	public String getKey() {
		return this.key;
	}
	
	@Override
	public void setKey(String key) {
		firePropertyChange(PROPERTY_KEY, this.key, this.key = key);
	}

	@Override
	public void setValue(String value) {
		firePropertyChange(PROPERTY_VALUE, this.value, this.value = value);
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public boolean isKeyEditable() {
		return keyEditable;
	}

}
