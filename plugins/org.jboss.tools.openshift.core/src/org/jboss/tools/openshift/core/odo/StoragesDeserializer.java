/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StoragesDeserializer extends StdNodeBasedDeserializer<List<Storage>> {
    /**
	 * 
	 */
	private static final String NAME_FIELD = "name";
		/**
	 * 
	 */
	private static final String METADATA_FIELD = "metadata";
		/**
	 * 
	 */
	private static final String ITEMS_FIELD = "items";

		public StoragesDeserializer() {
        super(TypeFactory.defaultInstance().constructCollectionType(List.class, Storage.class));
    }
    @Override
    public List<Storage> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
        List<Storage> result = new ArrayList<>();
        JsonNode items = root.get(ITEMS_FIELD);
        if (items != null) {
            for (Iterator<JsonNode> it = items.iterator(); it.hasNext(); ) {
                JsonNode item = it.next();
                result.add(Storage.of(getName(item)));
            }
        }
        return result;
    }

    private String getName(JsonNode item) {
        if (item.has(METADATA_FIELD) && item.get(METADATA_FIELD).has(NAME_FIELD)) {
            return item.get(METADATA_FIELD).get(NAME_FIELD).asText();
        } else {
            return "";
        }
    }
}
