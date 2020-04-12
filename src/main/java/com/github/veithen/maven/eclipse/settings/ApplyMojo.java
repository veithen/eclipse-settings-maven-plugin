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
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name="apply", defaultPhase=LifecyclePhase.INITIALIZE)
public class ApplyMojo extends AbstractMojo {
    @Component
    private BuildContext buildContext;
    
    @Parameter(property="project.basedir", readonly=true)
    private File basedir;
    
    @Parameter(required=true)
    private Bundle[] bundles;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        File settingsDir = new File(basedir, ".settings");
        settingsDir.mkdirs();
        for (Bundle bundle : bundles) {
            File prefsFile = new File(settingsDir, bundle.getId() + ".prefs");
            Properties prefs = new Properties();
            if (prefsFile.exists()) {
                try (InputStream in = new FileInputStream(prefsFile)) {
                    prefs.load(in);
                } catch (IOException ex) {
                    throw new MojoFailureException(String.format("Failed to read properties from %s", prefsFile), ex);
                }
            }
            boolean updated = false;
            for (Map.Entry<String, String> entry : bundle.getSettings().entrySet()) {
                Object oldValue = prefs.put(entry.getKey(), entry.getValue());
                updated |= !entry.getValue().equals(oldValue);
            }
            if (updated) {
                log.info("Applying settings for bundle " + bundle.getId());
                try (OutputStream out = buildContext.newFileOutputStream(prefsFile)) {
                    prefs.store(out, null);
                } catch (IOException ex) {
                    throw new MojoFailureException(String.format("Failed to write properties to %s", prefsFile), ex);
                }
            }
        }
    }
}
