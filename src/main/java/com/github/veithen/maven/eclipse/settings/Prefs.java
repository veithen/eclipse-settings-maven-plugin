/*-
 * #%L
 * Eclipse Settings Maven Plugin
 * %%
 * Copyright (C) 2020 - 2023 Andreas Veithen
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

final class Prefs {
    private Prefs() {}

    static void store(Properties prefs, OutputStream out) throws IOException {
        StringWriter sw = new StringWriter();
        prefs.store(sw, null);
        List<String> lines =
                new BufferedReader(new StringReader(sw.toString()))
                        .lines()
                        .filter(s -> !s.startsWith("#"))
                        .collect(Collectors.toList());
        Collections.sort(lines);
        OutputStreamWriter writer = new OutputStreamWriter(out, "8859_1");
        for (String line : lines) {
            writer.write(line);
            writer.write('\n');
        }
        writer.flush();
    }
}
