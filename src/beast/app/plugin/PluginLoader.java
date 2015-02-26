/*
 * PluginLoader.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.app.plugin;

import beast.util.FileHelpers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Arman Bilge
 */
public class PluginLoader {

    public static final ClassLoader PLUGIN_CLASS_LOADER;
    private static final Set<Plugin> LOADED_PLUGINS;
    private static final Logger LOGGER = Logger.getLogger("beast.app.plugin");

    static {

        final List<File> jars = new ArrayList<>();
        for (final File folder : getPluginFolders())
            jars.addAll(Arrays.asList(folder.listFiles(p -> !p.isDirectory() && p.getAbsolutePath().endsWith(".jar"))));

        PLUGIN_CLASS_LOADER = new URLClassLoader(jars.stream().map(j -> {
            try {
                return j.toURI().toURL();
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }).toArray(URL[]::new), ClassLoader.getSystemClassLoader());

        LOADED_PLUGINS = Collections.unmodifiableSet(jars.stream().map(j -> {
            final String name = j.getName();
            return loadPlugin(name.substring(0, name.length() - 4));
        }).filter(p -> p != null).collect(Collectors.<Plugin>toSet()));

    }

    public static Set<Plugin> getPlugins() {
        return LOADED_PLUGINS;
    }

    private static Set<File> getPluginFolders() {
        final Set<File> folders = new LinkedHashSet<>(2);
        {
            final String folder = java.lang.System.getProperty("beast.plugins.dir");
            if (folder != null) {
                final File f = new File(folder);
                if (f.exists())
                    folders.add(new File(folder));
            }
        }
        {
            final File folder = FileHelpers.getFile("plugins");
            if (folder.exists())
                folders.add(folder);
        }
        return Collections.unmodifiableSet(folders);
    }

    private static Plugin loadPlugin(final String pluginName) {
        try {
            LOGGER.info("Loading plugin " + pluginName);
            return (Plugin) PLUGIN_CLASS_LOADER.loadClass(pluginName).newInstance();
        } catch (final Exception ex) {
            LOGGER.severe(ex.toString());
        }
        return null;
    }

}
