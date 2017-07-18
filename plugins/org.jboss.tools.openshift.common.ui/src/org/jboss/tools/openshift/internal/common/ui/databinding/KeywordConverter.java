/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.conversion.Converter;

/**
 * A converted used to match keyword at end of value.
 * 
 * @author Jeff MAURY
 *
 */
public class KeywordConverter extends Converter {

    private String[] values;
    private boolean remove;

    /**
     * Constructor.
     * 
     * @param values the allowed end values
     * @param remove if true extract remove the matched keyword (beginning of value) if false extract it
     */
    public KeywordConverter(String[] values, boolean remove) {
        super(String.class, String.class);
        this.values = values;
        this.remove = remove;
    }

    @Override
    public Object convert(Object fromObject) {
        String str = (String) fromObject;
        for(String s : values) {
            if (str.endsWith(s)) {
                if (remove) {
                    str = str.substring(0, str.length() - s.length());
                } else {
                    str = s;
                }
                break;
            }
        }
        return str;
    }
}