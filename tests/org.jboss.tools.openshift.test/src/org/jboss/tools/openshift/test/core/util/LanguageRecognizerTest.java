/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import com.redhat.devtools.alizer.api.Language;
import com.redhat.devtools.alizer.api.LanguageRecognizer;
import com.redhat.devtools.alizer.api.RecognizerFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * 
 */

public class LanguageRecognizerTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	@Test
	public void checkLanguageRecognizerOnEmptyFolder() throws IOException {
		LanguageRecognizer recognize = new RecognizerFactory().createLanguageRecognizer();
		List<Language> languages = recognize.analyze(folder.newFolder().getAbsolutePath());
		assertTrue(languages.isEmpty());
	}
}
