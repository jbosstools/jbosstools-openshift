/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.internal.core.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCBinaryValidator {
	
	/*
	 * The regular expression use to match line with version of the oc executable
	 */
	private static final Pattern OC_VERSION_LINE_PATTERN = Pattern.compile("oc (v.*)");
	
	private static final String OC_MINIMUM_VERSION_FOR_RSYNC = "v1.1.1";

	private String path;
	
	public OCBinaryValidator(String path) {
		this.path = path;
	}

	
	/**
	 * Returns the version of the OC binary by running the version command.
	 * The returned string is formatted as vx.y.z.
	 * 
	 * @return the string version
	 */
	public String getVersion() {
        String version = null;
		try {
            ProcessBuilder builder = new ProcessBuilder(path, "version");
            Process process = builder.start();
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while ((version == null) && ((line = reader.readLine()) != null)) {
                    Matcher matcher = OC_VERSION_LINE_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        version = matcher.group(1);
                    }
                }
            } 
        } catch (IOException e) {}
        return version;
	}
	
	/**
	 * Checks if the oc binary is compatible for rsync publishing.
	 * 
	 * @return true if the oc binary is compatible
	 * @see https://issues.jboss.org/browse/JBIDE-21307
	 * @see https://github.com/openshift/origin/issues/6109
	 */
	public boolean isCompatibleForPublishing() {
	    String version = getVersion();
	    return version != null && version.compareTo(OC_MINIMUM_VERSION_FOR_RSYNC) >= 0;
	}
}
