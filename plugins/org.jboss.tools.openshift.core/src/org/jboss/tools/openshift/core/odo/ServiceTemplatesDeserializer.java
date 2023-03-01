/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.CRDDescription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersionList;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SpecDescriptor;

public class ServiceTemplatesDeserializer  {

    private static final String ALM_EXAMPLES_FIELD = "alm-examples";
    private static final String VERSION_FIELD = "version";
    private static final String KIND_FIELD = "kind";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String API_VERSION_FIELD = "apiVersion";
    private static final String SPEC_DESCRIPTORS_FIELD = "specDescriptors";
    private static final String PATH_FIELD = "path";
    private static final String DESCRIPTORS_FIELD = "x-descriptors";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Function<String, ObjectNode> schemaMapper;
    private final List<GenericKubernetesResource> bindableKinds;

    public ServiceTemplatesDeserializer(Function<String, ObjectNode> schemaMapper,
                                        List<GenericKubernetesResource> bindableKinds) {
        this.schemaMapper = schemaMapper;
        this.bindableKinds = bindableKinds;
    }

    private List<OperatorCRDSpecDescriptor> getSpecDescriptors(CRDDescription node) {
        List<OperatorCRDSpecDescriptor> descriptors = new ArrayList<>();
        if (node.getSpecDescriptors() !=null) {
            for(SpecDescriptor descriptor : node.getSpecDescriptors()) {
                if (descriptor.getPath() != null) {
                    String displayName = descriptor.getDisplayName()!=null?descriptor.getDisplayName():"";
                    String description = descriptor.getDescription()!=null?descriptor.getDescription():"";
                    List<String> descs = descriptor.getXDescriptors()!=null?descriptor.getXDescriptors():Collections.emptyList();
                    descriptors.add(new OperatorCRDSpecDescriptor() {
                        @Override
                        public String getPath() {
                            return descriptor.getPath();
                        }

                        @Override
                        public String getDisplayName() {
                            return displayName;
                        }

                        @Override
                        public String getDescription() {
                            return description;
                        }

                        @Override
                        public List<String> getDescriptors() {
                            return descs;
                        }
                    });
                }
            }
        }
        return descriptors;
    }

    private OperatorCRD getOperatorCRD(CRDDescription crd, ArrayNode samples, List<OperatorCRDSpecDescriptor> descriptors) {
        return new OperatorCRD() {
            private ObjectNode sample;
            private ObjectNode schema;

            @Override
            public String getName() {
                return crd.getName();
            }

            @Override
            public String getVersion() {
                return crd.getVersion();
            }

            @Override
            public String getKind() {
                return crd.getKind();
            }

            @Override
            public String getDisplayName() {
                return crd.getDisplayName() != null ? crd.getDisplayName() : getName();
            }

            @Override
            public String getDescription() {
                return crd.getDescription() != null ? crd.getDescription() : getName();
            }

            @Override
            public ObjectNode getSample() {
                if (sample == null && samples != null) {
                    sample = selectSample(samples, this);
                }
                return sample;
            }

            @Override
            public ObjectNode getSchema() {
                if (schema == null) {
                    schema = schemaMapper.apply(getCRDPrefix(this) + "/namespaces/{namespace}/" + getCRDSuffix(this));
                    if (schema != null) {
                        schema = SchemaHelper.getAnnotatedSchema(schema, getSpecDescriptors());
                    }
                }
                return schema;
            }

            @Override
            public List<OperatorCRDSpecDescriptor> getSpecDescriptors() {
                return descriptors;
            }
        };
    }

    private boolean isBindable(CRDDescription crd) {
        String apiVersion = crd.getName().substring(crd.getName().indexOf('.') + 1) + '/' + crd.getVersion();
        return bindableKinds.stream().anyMatch(bk -> bk.getKind().equals(crd.getKind()) && bk.getApiVersion().equals(apiVersion));
    }

    public ServiceTemplate fromPOJO(ClusterServiceVersion csv) {
        try {
            String name = csv.getMetadata().getName();
            String displayName = csv.getSpec().getDisplayName()!=null?csv.getSpec().getDisplayName():name;
            ArrayNode samples = null;
            if (csv.getMetadata().getAnnotations() != null && csv.getMetadata().getAnnotations()
                    .containsKey(ALM_EXAMPLES_FIELD)) {
                samples = (ArrayNode) MAPPER.readTree(csv.getMetadata().getAnnotations().get(ALM_EXAMPLES_FIELD));
            }
            List<OperatorCRD> crds = new ArrayList<>();
            if (csv.getSpec().getCustomresourcedefinitions() != null &&
                    csv.getSpec().getCustomresourcedefinitions().getOwned() != null) {
                for(CRDDescription crd : csv.getSpec().getCustomresourcedefinitions().getOwned()) {
                    if (isBindable(crd)) {
                        List<OperatorCRDSpecDescriptor> descriptors = getSpecDescriptors(crd);
                        crds.add(getOperatorCRD(crd, samples, descriptors));
                    }
                }
            }
            if (!crds.isEmpty()) {
                return new ServiceTemplate() {
                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    public String getDisplayName() {
                        return displayName;
                    }

                    @Override
                    public List<OperatorCRD> getCRDs() {
                        return crds;
                    }
                };
            } else {
                return null;
            }
        } catch (JsonProcessingException e) {
            return null;
        }
    }


    private ObjectNode selectSample(ArrayNode samples, OperatorCRD crd) {
        String name = getCRDPrefix(crd);
        ObjectNode result = null;
        for(JsonNode node : samples) {
            if (node.has(API_VERSION_FIELD) && node.get(API_VERSION_FIELD).asText().equals(name) && node.has(KIND_FIELD) && node.get(KIND_FIELD).asText().equals(crd.getKind())) {
                result = (ObjectNode) node;
                break;
            }
        }
        return result;
    }

    private String getCRDPrefix(OperatorCRD crd) {
        String name = crd.getName();
        name = name.substring(name.indexOf('.') + 1) + '/' + crd.getVersion();
        return name;
    }

    private String getCRDSuffix(OperatorCRD crd) {
        String name = crd.getName();
        if (name.indexOf('.') != (-1)) {
            name = name.substring(0, name.indexOf('.'));
        }
        return name;
    }

    public List<ServiceTemplate> fromList(ClusterServiceVersionList list) {
        return list.getItems().stream().filter(csv -> csv.getStatus() != null &&
                "Succeeded".equals(csv.getStatus().getPhase()))
                .map(this::fromPOJO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}