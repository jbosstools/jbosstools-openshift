/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import com.openshift.restclient.images.DockerImageURI;

import static org.jboss.tools.openshift.internal.ui.validator.ServiceNameValidator.MAXLENGTH;

/**
 * 
 * A converter for the docker image name to Openshift resource mapping.
 * Will replace '_' by '-', skip other forbidden characters and trucate the
 * result to 24.
 * 
 * @author Jeff Maury
 *
 */
public class DockerImage2OpenshiftResourceConverter {

    public String convert(DockerImageURI dockerImage) {
        String name = dockerImage.getName();
        StringBuilder builder = new StringBuilder(name.length());
        boolean lastSpecialChar = true;
        for (int i = 0; i < name.length() && builder.length() < MAXLENGTH; ++i) {
            char c = name.charAt(i);
            if (((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))) {
                if ((c >= '0') && (c <= '9') && (builder.length() == 0)) {
                    /*Openshift does not allow resource beginning with numeric*/
                    builder.append('a');
                }
                builder.append(c);
                lastSpecialChar = false;
            } else if (((c == '_') || (c == '-')) && !lastSpecialChar) {
                builder.append('-');
                lastSpecialChar = true;
            }
        }
        if ((builder.length() > 0) && lastSpecialChar) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

}
