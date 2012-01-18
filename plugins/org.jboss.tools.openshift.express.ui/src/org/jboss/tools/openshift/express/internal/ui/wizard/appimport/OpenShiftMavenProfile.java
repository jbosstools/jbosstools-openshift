/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.appimport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author André Dietisheim
 */
public class OpenShiftMavenProfile {

	private static final String POM_FILENAME = "pom.xml";

	private static final String ID_OPENSHIFT = "openshift";
	private static final String ELEMENT_PROJECT = "project";
	private static final String ELEMENT_PROFILES = "profiles";
	private static final String ELEMENT_PROFILE = "profile";
	private static final String ELEMENT_ID = "id";

	private static final String OPENSHIFT_PROFILE =
			"<profile>\n"
					+ "<!-- When built in OpenShift the 'openshift' profile will be used when invoking mvn. -->\n"
					+ "<!-- Use this profile for any OpenShift specific customization your app will need. -->\n"
					+ "<!-- By default that is to put the resulting archive into the 'deployments' folder. -->\n"
					+ "<!-- http://maven.apache.org/guides/mini/guide-building-for-different-environments.html -->\n"
					+ "<id>openshift</id>\n"
					+ "<build>\n"
					+ "   <finalName>{0}</finalName>\n"
					+ "   <plugins>\n"
					+ "     <plugin>\n"
					+ "       <artifactId>maven-war-plugin</artifactId>\n"
					+ "       <version>2.1.1</version>\n"
					+ "       <configuration>\n"
					+ "         <outputDirectory>deployments</outputDirectory>\n"
					+ "         <warName>ROOT</warName>\n"
					+ "       </configuration>\n"
					+ "     </plugin>\n"
					+ "   </plugins>\n"
					+ " </build>\n"
					+ "</profile>\n";

	private IFile pomFile;
	private String pluginId;
	private Document document;

	public OpenShiftMavenProfile(IProject project, String pluginId) {
		this(project.getFile(POM_FILENAME), pluginId);
	}

	/**
	 * Creates an openshift profile that will allow you to deal with the
	 * openshift profile in a given pom.
	 * 
	 * @param pomFile
	 * @param pluginId
	 */
	public OpenShiftMavenProfile(IFile pomFile, String pluginId) {
		this.pomFile = pomFile;
		this.pluginId = pluginId;
	}

	/**
	 * Checks the pom (that was given at constructin time) for presence of the
	 * OpenShift profile. Returns <code>true</code> if this pom has the
	 * OpenShift profile, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if the pom has the openshift profile.
	 * @throws CoreException
	 */
	public boolean existsInPom() throws CoreException {
		if (!exists(pomFile)) {
			return false;
		}

		Element openShiftProfileElement = getOpenShiftProfileElement(getDocument());
		return openShiftProfileElement != null;
	}

	/**
	 * Adds the openshift profile to the pom this is instance is bound to.
	 * Returns <code>true</code> if it was added, <code>false</code> otherwise.
	 * 
	 * @return true if the profile was added to the pom this instance is bound
	 *         to.
	 * @throws CoreException
	 */
	public boolean addToPom(String finalName) throws CoreException {
		try {
			if (existsInPom()) {
				return false;
			}
			Document document = getDocument();
			Element profilesElement = getOrCreateProfilesElement(document);
			Node profileNode = document.importNode(createOpenShiftProfileElement(finalName), true);
			profilesElement.appendChild(profileNode);
			return true;
		} catch (SAXException e) {
			throw new CoreException(createStatus(e));
		} catch (IOException e) {
			throw new CoreException(createStatus(e));
		} catch (ParserConfigurationException e) {
			throw new CoreException(createStatus(e));
		}
	}

	private Element createOpenShiftProfileElement(String finalName) throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		String openShiftProfile = MessageFormat.format(OPENSHIFT_PROFILE, finalName);
		Document document = documentBuilder.parse(new ByteArrayInputStream(openShiftProfile.getBytes()));
		return document.getDocumentElement();
	}

	private Element getOrCreateProfilesElement(Document document) throws CoreException {
		Element profilesElement = getProfilesElement(document);
		if (profilesElement == null) {
			profilesElement = createProfilesElement(document);
		}
		return profilesElement;
	}

	private Element createProfilesElement(Document document) throws CoreException {
		Element profilesElement;
		profilesElement = document.createElement(ELEMENT_PROFILES);
		Element projectElement = getProjectElement(document);
		if (projectElement == null) {
			throw new CoreException(
					createStatus(NLS.bind("Could not find <project> tag in pom {0}", pomFile.toString())));
		}
		projectElement.appendChild(profilesElement);
		return profilesElement;
	}

	private boolean exists(IFile file) {
		return file != null
				&& file.exists();
	}

	private Element getProfilesElement(Document document) {
		return getFirstElement(ELEMENT_PROFILES, document);
	}

	private Element getProjectElement(Document document) {
		return getFirstElement(ELEMENT_PROJECT, document);
	}

	private Element getOpenShiftProfileElement(Document document) {
		return getOpenShiftProfileElement(getProfilesElement(document));
	}

	private Element getOpenShiftProfileElement(Element element) {
		Element openshiftProfile = getFirstElementByMatcher(ELEMENT_PROFILE, new IMatcher() {

			@Override
			public boolean isMatch(Element element) {
				if (element == null) {
					return false;
				}

				Element idElement = getFirstElement(ELEMENT_ID, element);
				if (idElement == null) {
					return false;
				}
				if (idElement.hasChildNodes()) {
					return ID_OPENSHIFT.equals(idElement.getFirstChild().getTextContent());
				}
				return false;
			}
		}, element);
		return openshiftProfile;
	}

	private Element getFirstElement(String elementName, Document document) {
		NodeList elements = document.getElementsByTagName(elementName);
		if (elements != null
				&& elements.getLength() > 0) {
			return (Element) elements.item(0);
		}
		return null;
	}

	protected Element getFirstElement(String elementName, Element element) {
		NodeList children = element.getElementsByTagName(elementName);
		if (children == null
				|| children.getLength() == 0) {
			return null;
		}
		return (Element) children.item(0);
	}

	protected Element getFirstElementByMatcher(String elementName, IMatcher matcher, Element element) {
		if (element == null) {
			return null;
		}

		NodeList children = element.getElementsByTagName(elementName);
		if (children == null
				|| children.getLength() == 0) {
			return null;
		}
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element) children.item(i);
			if (matcher.isMatch(child)) {
				return child;
			}
		}
		return null;
	}

	private Document getDocument() throws CoreException {
		try {
			if (document == null) {
				DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				this.document = documentBuilder.parse(pomFile.getContents());
			}
			return document;
		} catch (ParserConfigurationException e) {
			throw new CoreException(createStatus(e));
		} catch (SAXException e) {
			throw new CoreException(createStatus(e));
		} catch (IOException e) {
			throw new CoreException(createStatus(e));
		}
	}

	private interface IMatcher {
		public boolean isMatch(Element element);
	}

	private IStatus createStatus(Throwable e) {
		return new Status(IStatus.ERROR, pluginId, e.getMessage(), e);
	}

	private IStatus createStatus(String message) {
		return new Status(IStatus.ERROR, pluginId, message);
	}

	public IFile savePom() throws CoreException {
		Writer writer = null;
		try {
			writer = new StringWriter();
			createTransformer().transform(new DOMSource(getDocument()), new StreamResult(writer));
			pomFile.setContents(new ByteArrayInputStream(writer.toString().getBytes()), IResource.FORCE, null);
			return pomFile;
		} catch (TransformerConfigurationException e) {
			throw new CoreException(createStatus(e));
		} catch (TransformerException e) {
			throw new CoreException(createStatus(e));
		} finally {
			safeClose(writer);
		}
	}

	private Transformer createTransformer()
			throws TransformerFactoryConfigurationError, TransformerConfigurationException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute("indent-number", new Integer(4));

		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		return transformer;
	}

	private void safeClose(Writer writer) {
		if (writer == null) {
			return;
		}
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// ignore;
		}
	}

	public static boolean isMavenProject(IProject project) {
		return project.getFile(POM_FILENAME).exists();
	}
}
