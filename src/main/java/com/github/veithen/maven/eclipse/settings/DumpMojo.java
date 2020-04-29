/*-
 * #%L
 * Eclipse Settings Maven Plugin
 * %%
 * Copyright (C) 2020 Andreas Veithen
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.veithen.maven.eclipse.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Mojo(name="dump")
public class DumpMojo extends AbstractMojo {
    private static String PREFS_SUFFIX = ".prefs";

    @Parameter(property="project.basedir", readonly=true)
    private File basedir;

    private static void appendSimpleElement(Element parent, String name, String text) {
        Document document = parent.getOwnerDocument();
        Element element = document.createElement(name);
        Node child;
        if (text.contains("<")) {
            child = document.createCDATASection(text);
        } else {
            child = document.createTextNode(text);
        }
        element.appendChild(child);
        parent.appendChild(element);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File[] files = new File(basedir, ".settings").listFiles(f -> f.getName().endsWith(PREFS_SUFFIX));
        if (files == null) {
            return;
        }
        Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            throw new Error(ex);
        }
        Element configuration = document.createElement("configuration");
        document.appendChild(configuration);
        for (File file : files) {
            Properties prefs = new Properties();
            try (InputStream in = new FileInputStream(file)) {
                prefs.load(in);
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to read preferences from " + file, ex);
            }
            Element bundle = document.createElement("bundle");
            configuration.appendChild(bundle);
            String fileName = file.getName();
            appendSimpleElement(bundle, "symbolicName", fileName.substring(0, fileName.length()-PREFS_SUFFIX.length()));
            Element properties = document.createElement("properties");
            bundle.appendChild(properties);
            for (Map.Entry<Object,Object> entry : prefs.entrySet()) {
                String name = (String)entry.getKey();
                if (name.equals("eclipse.preferences.version")) {
                    continue;
                }
                Element property = document.createElement("property");
                properties.appendChild(property);
                appendSimpleElement(property, "name", name);
                appendSimpleElement(property, "value", ((String)entry.getValue()).replaceAll("\\$", "\\$\\$"));
            }
        }
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(configuration), new StreamResult(System.out));
        } catch (TransformerException ex) {
            throw new Error(ex);
        }
    }
}
