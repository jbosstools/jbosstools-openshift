/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

public interface IDialogProvider {

	public static final int ERROR = 1;
	public static final int INFORMATION = 2;
	public static final int QUESTION = 3;
	public static final int WARNING = 4;
	public static final int CONFIRM = 5;

	static final String YES_LABEL = "Yes";
	static final int YES_ID = 2;
	static final String NO_LABEL = "No";
	static final int NO_ID = 3;

	void warn(String title, String message, String preferencesKey);

	int message(String title, int type, String message, Consumer<String> callback,
			LinkedHashMap<String, Integer> buttonLabelToIdMap, int defaultButton, String preferencesKey);

	public void preferencePage(String page);
}
