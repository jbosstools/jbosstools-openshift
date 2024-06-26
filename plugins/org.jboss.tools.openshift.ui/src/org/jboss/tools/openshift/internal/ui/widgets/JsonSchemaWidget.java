/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.widgets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A widget whose content is driven by JSON schema. The widget is initially
 * created empty but then calling init function will create the sub widgets
 * according to the schema.
 *
 * {@link #init(ObjectNode, JsonNode)} {@link #init(ObjectNode)}
 */
public class JsonSchemaWidget extends Composite {
	private final class ControlConsumer implements Consumer<Boolean> {
		private Control control;
		
		@Override
		public void accept(Boolean t) {
			control.setVisible(t);
			((GridData)control.getLayoutData()).exclude = !t;
		}

		void setControl(Control control) {
			this.control = control;
		}
	}

	private static final String PROPERTIES = "properties";
	private static final String REQUIRED = "required";
	private static final String TYPE = "type";
	private static final String ENUM = "enum";
	private static final String DESCRIPTION = "description";
	private static final String DISPLAY_NAME = "displayName";

	private static final String TYPE_PROPERTY = JsonSchemaWidget.class.getName() + ".type";
	private static final String NAME_PROPERTY = JsonSchemaWidget.class.getName() + ".name";
	private static final String REQUIRED_PROPERTY = JsonSchemaWidget.class.getName() + ".required";
	
	private static final Image PLUS_IMG = OpenShiftImages.PLUS_IMG;
	private static final Image CHEVRON_DOWN_IMG = OpenShiftImages.CHEVRON_DOWN_IMG;

	private final ScrolledComposite container; 
	
	public JsonSchemaWidget(Composite parent, int style, ScrolledComposite container) {
		super(parent, style);
		this.container = container;
		GridLayout gridLayout = new GridLayout(1, false);
		setLayout(gridLayout);
	}

	private static boolean isRequired(String name, ArrayNode required) {
		if (required != null) {
			for (JsonNode el : required) {
				if (el.asText().equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	private static String normalize(String s) {
		StringBuilder builder = new StringBuilder();
		builder.append(Character.toUpperCase(s.charAt(0)));
		for (int i = 1; i < s.length(); ++i) {
			if (Character.isUpperCase(s.charAt(i))) {
				builder.append(' ');
			}
			builder.append(s.charAt(i));
		}
		return builder.toString();
	}

	private static String getDisplayName(String name, JsonNode node) {
		return node.has(DISPLAY_NAME) ? node.get(DISPLAY_NAME).asText() : normalize(name);
	}

	private Label createNameLabel(Composite parent, String name, JsonNode node) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(getDisplayName(name, node));
		if (node.has(DESCRIPTION)) {
			label.setToolTipText(node.get(DESCRIPTION).asText());
		}
		return label;
	}

	private void createArrayItemWidget(String name, JsonNode node, JsonNode def, Composite panel) {
		Composite itemPanel = new Composite(panel, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(itemPanel);
		Button removeButton = new Button(itemPanel, SWT.NONE);
		removeButton.setText("Remove " + name);
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			itemPanel.dispose();
			resize();
		}));

		ArrayNode required = node.has(REQUIRED) && node.get(REQUIRED).isArray() ? node.withArray(REQUIRED) : null;
		createWidget(null, node.get("items"), def, isRequired(name, required), itemPanel);
		// panel.revalidate();
	}

	private Composite getHeaderPanel(Composite parent, String name, Consumer<Boolean> sub) {
		Composite header = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 20).applyTo(header);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(header);

		Label label = new Label(header, SWT.NONE);
		label.setText(name);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
		Label sign = new Label(header, SWT.NONE);
		sign.setImage(PLUS_IMG);
		sign.setToolTipText("Click to expand");
		sign.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (sign.getImage() == PLUS_IMG) {
					sign.setImage(CHEVRON_DOWN_IMG);
					sign.setToolTipText("Click to collapse");
					sub.accept(true);
				} else {
					sign.setImage(PLUS_IMG);
					sign.setToolTipText("Click to expand");
					sub.accept(false);
				}
				JsonSchemaWidget.this.resize();
			}
			
		});
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(sign);

		return header;
	}

	private void setMetadata(Widget comp, String name, String type, boolean required) {
		comp.setData(TYPE_PROPERTY, type);
		if (name != null) {
			comp.setData(NAME_PROPERTY, name);
		}
		comp.setData(REQUIRED_PROPERTY, required);
	}

	private void createWidget(String name, JsonNode node, JsonNode def, boolean required, Composite parent) {
		switch (node.get(TYPE).asText()) {
		case "string":
		case "integer":
		case "number":
			if (name != null) {
				Label label = createNameLabel(parent, name, node);
				GridDataFactory.fillDefaults().applyTo(label);
			}
			Control field;
			if (node.has(ENUM) && node.get(ENUM).isArray()) {
				field = new Combo(parent, SWT.NONE);
				for (JsonNode child : node.get(ENUM)) {
					((Combo) field).add(child.asText());
				}
				if (def != null && def.has(name)) {
					((Combo) field).setText(def.get(name).asText());
				}
				if (def != null) {
					if (name != null && def.has(name)) {
						((Combo) field).setText(def.get(name).asText());
					} else if (name == null) {
						((Combo) field).setText(def.asText());
					}
				}
			} else {
				field = new Text(parent, SWT.NONE);
				if (def != null) {
					if (name != null && def.has(name)) {
						((Text) field).setText(def.get(name).asText());
					} else if (name == null) {
						((Text) field).setText(def.asText());
					}
				}
			}
			GridDataFactory.fillDefaults().applyTo(field);
			setMetadata(field, name, node.get(TYPE).asText(), required);
			break;
		case "boolean":
			Label label = null;
			if (name != null) {
				label = createNameLabel(parent, name, node);
				GridDataFactory.fillDefaults().applyTo(label);
			}
			Button checkbox = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(checkbox);
			if (def != null && def.has(name)) {
				checkbox.setSelection(def.get(name).asBoolean());
			}
			setMetadata(checkbox, name, node.get(TYPE).asText(), required);
			break;
		case "object":
			var acc = new ControlConsumer();
			if (name != null) {
				getHeaderPanel(parent, getDisplayName(name, node), acc);
			}
			JsonSchemaWidget sub = new JsonSchemaWidget(parent, SWT.NONE, container);
			GridDataFactory.fillDefaults().exclude(true).applyTo(sub);
			sub.setVisible(false);
			acc.setControl(sub);
			sub.init((ObjectNode) node, def != null && name != null ? def.get(name) : def);
			setMetadata(sub, name, node.get(TYPE).asText(), required);
			break;
		case "array":
			if (node.has("items")) {
				var acc1 = new ControlConsumer();
				if (name != null) {
					getHeaderPanel(parent, getDisplayName(name, node), acc1);
				}
				Composite panel = new Composite(parent, SWT.NONE);
				acc1.setControl(panel);
				GridLayoutFactory.fillDefaults().applyTo(panel);
				GridDataFactory.fillDefaults()
					.grab(true, false)
					.exclude(true)
					.applyTo(panel);
				panel.setVisible(false);
				Button button = new Button(panel, SWT.NONE);
				button.setText("Add " + getDisplayName(name, node));
				button.addSelectionListener(
						SelectionListener.widgetSelectedAdapter(e -> {
							createArrayItemWidget(name, node, null, panel);
							resize();
						}));
				setMetadata(panel, name, node.get(TYPE).asText(), required);
				if (def != null && def.has(name) && def.get(name).isArray()) {
					for (JsonNode item : def.withArray(name)) {
						createArrayItemWidget(name, node, item, panel);
					}
				}
			}
		}
	}

	/**
	 * Reset the content for this widget from the schema and default values.
	 *
	 * @param schema the object representing the JSON schema
	 * @param def    the object with default values
	 */
	public void init(ObjectNode schema, JsonNode def) {
		for (Control c : getChildren()) {
			c.dispose();
		}
		if (schema.has(PROPERTIES) && schema.get(PROPERTIES).isObject()) {
			ObjectNode properties = (ObjectNode) schema.get(PROPERTIES);
			ArrayNode required = schema.has(REQUIRED) && schema.get(REQUIRED).isArray() ? schema.withArray(REQUIRED)
					: null;
			for (Iterator<Map.Entry<String, JsonNode>> it = properties.fields(); it.hasNext();) {
				Map.Entry<String, JsonNode> entry = it.next();
				String name = entry.getKey();
				JsonNode node = entry.getValue();
				if (node.isObject() && node.has(TYPE)) {
					createWidget(name, node, def, isRequired(name, required), this);
				}
			}
		}
		if (container == getParent()) {
			resize();
		}
	}

	/**
	 * Reset the content for this widget from the schema.
	 *
	 * @param schema the object representing the JSON schema
	 */
	public void init(ObjectNode schema) {
		init(schema, null);
	}

	private void dump(Composite container, JsonNode node) {
		for (Control comp : container.getChildren()) {
			if (comp.getData(TYPE_PROPERTY) != null) {
				String name = (String) comp.getData(NAME_PROPERTY);
				String type = (String) comp.getData(TYPE_PROPERTY);
				if ("array".equals(type)) {
					ArrayNode array = ((ObjectNode) node).arrayNode();
					dump((Composite) comp, array);
					if (!array.isEmpty()) {
						if (name == null) {
							((ArrayNode) node).add(array);
						} else {
							((ObjectNode) node).set(name, array);
						}
					}
				} else if ("object".equals(type)) {
					ObjectNode sub = ((ContainerNode) node).objectNode();
					dump((Composite) comp, sub);
					if (!sub.isEmpty()) {
						if (name == null) {
							((ArrayNode) node).add(sub);
						} else {
							((ObjectNode) node).set(name, sub);
						}
					}
				} else if ("boolean".equals(type)) {
					boolean val = ((Button) comp).getSelection();
					if (val || (boolean) comp.getData(REQUIRED_PROPERTY)) {
						if (name == null) {
							((ArrayNode) node).add(val);
						} else {
							((ObjectNode) node).put(name, val);
						}
					}
				} else {
					String val = (comp instanceof Text) ? ((Text) comp).getText() : (String) ((Combo) comp).getText();
					if (val != null && val.length() > 0) {
						if (name == null) {
							((ArrayNode) node).add(val);
						} else if ("string".equals(type)) {
							((ObjectNode) node).put(name, val);
						} else if ("integer".equals(type)) {
							((ObjectNode) node).put(name, Integer.parseInt(val));
						} else {
							((ObjectNode) node).put(name, Double.parseDouble(val));
						}
					}
				}
			} else if (comp instanceof Composite) {
				dump((Composite) comp, node);
			}
		}
	}

	/**
	 * Dump the content of the widget in a JSON object.
	 *
	 * @param node the JSON object to dump into
	 */
	public void dump(JsonNode node) {
		dump(this, node);
	}
	
	private void resize() {
		container.layout(true, true);
		container.setMinSize(container.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public static void main (String [] args) throws IOException {
		//Display display = new Display ();
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		shell.setText("Snippet 1");
		GridLayoutFactory.fillDefaults().applyTo(shell);
		shell.setSize(400, 400);
		
        String kafkaSchema = Files.readString(Paths.get("kafka-schema.json"));
        ObjectNode schemaNode = new ObjectMapper().readValue(kafkaSchema, ObjectNode.class);
        String kafka = Files.readString(Paths.get("kafka.yaml"));
        ObjectNode sampleNode = new ObjectMapper(new YAMLFactory()).readValue(kafka, ObjectNode.class);

        ScrolledComposite scrolled = new ScrolledComposite(shell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(scrolled);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);
        final JsonSchemaWidget jsonSchemaWidget = new JsonSchemaWidget(scrolled, SWT.NONE, scrolled);
        jsonSchemaWidget.init(schemaNode, sampleNode);
        scrolled.setContent(jsonSchemaWidget);
        scrolled.setMinSize(jsonSchemaWidget.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
	}
}
