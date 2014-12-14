/*
 * PluginLoader.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
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
import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PluginLoader {	 

		public static File getPluginFolder() {
			String pluginFolderFromProperty = null;
			try {
				pluginFolderFromProperty =  java.lang.System.getProperty("beast.plugins.dir");
			} catch (Exception ex) {
                //
			}
			if (pluginFolderFromProperty != null) {
				return new File(pluginFolderFromProperty);
			}
			final String PLUGIN_FOLDER = "plugins";
			final File PLUGIN_FILE = new File(PLUGIN_FOLDER);
			return PLUGIN_FILE;

	   }

	public static List<String> getAvailablePlugins() {
		File pluginFile = PluginLoader.getPluginFolder();
		return getAvailablePlugins(pluginFile);
	}

	   public static List<String> getAvailablePlugins(File pluginFile){

	       List<String> plugins = new ArrayList<String> ();

	       Logger.getLogger("beast.app.plugin").info("Looking for plugins in " + pluginFile.getAbsolutePath());

           File[] classFolderFiles = pluginFile.listFiles(new FileFilter() {
	           public boolean accept(File pathname) {
	               String name = pathname.getName();
	               if(!pathname.isDirectory() || name.endsWith("CVS") || name.endsWith(".classes"))
	                   return false;
	               File[] directoryContents = pathname.listFiles(new FileFilter() {
	                   public boolean accept(File pathname) {
	                       String name = pathname.getName();
	                       return name.endsWith(".jar");
	                   }
	               });
	               return directoryContents.length != 0;
	           }
	       });

	       if (classFolderFiles != null) {
	           for (File folder : classFolderFiles) {
	               plugins.add(folder.getName());
	           }
	       }

	       File[] pluginJarFiles = pluginFile.listFiles(new FileFilter() {
	           public boolean accept(File pathname) {
	               return !pathname.isDirectory() && pathname.getAbsolutePath().endsWith(".jar");
	           }
	       });

	       if (pluginJarFiles != null) {
	           for (File jarFile : pluginJarFiles) {
	               String name = jarFile.getName();
	               name =name.substring(0, name.length()- 4);
	               if(! plugins.contains(name))
	                   plugins.add(name);
	           }
	       }

	       return plugins;
	   }

	  public static Plugin loadPlugin(final String pluginName/*, boolean pluginEnabled*/) {
          //the class loader must still be assigned if the plugin isnt enabled so
	      //documents from that plugin can still be displayed.
          final String loggerName = "beast.app.plugin";
          Logger.getLogger(loggerName).info("Loading plugin " + pluginName);
	      File pluginDir = PluginLoader.getPluginFolder();
	      File file = new File(pluginDir, pluginName);

	      try {
	          URL[] urls;
	          if (!file.exists()) {
	        	  Logger.getLogger(loggerName).info("Loading jar file");
	              file = new File(pluginDir, pluginName + ".jar");
	              urls = new URL[]{file.toURI().toURL()};
	          }
	          else {
	              File classFiles = new File(pluginDir, "classes");
	              final boolean classesExist = classFiles.exists();

	              File[] files = file.listFiles(new FileFilter() {
	                  public boolean accept(File pathname) {
	                      String name = pathname.getName();
	                      if(!name.endsWith(".jar")) return false;
	                      name = name.substring(0, name.length()- 4);
	                      return !(name.equals(pluginName) && classesExist);
	                  }
	              });
	              if(files == null) files = new File[0];

	              int length = files.length+1;
	              if(classesExist) length ++;

	              urls = new URL[length];
	              int count = 0;
	              if( classesExist ) {
	                  urls[ count ++ ] = classFiles.toURI().toURL();
	              }
	              urls[ count ++ ] = file.toURI().toURL();

                  Logger.getLogger(loggerName).info("Adding " + file + " to class path");

	              for (File jarFile : files) {
	                  urls[count++] = jarFile.toURI().toURL();
	              }
	          }
	          final URLClassLoader classLoader = new URLClassLoader(urls);

	          for (URL url : classLoader.getURLs()) {
	        	  Logger.getLogger(loggerName).info("URL from loader: " + url.toString() + "\n");
	          }

	          final Class myClass = classLoader.loadClass(pluginName);

	          final Object plugin = myClass.newInstance();

              // isn't that covered by the cast failing?
	          if (!(plugin instanceof Plugin)){
	              throw new Exception("Class should be " + Plugin.class.getName());
	          }
	          return (Plugin)plugin;

	      } catch (Exception e) {
	    	  Logger.getLogger(loggerName).severe(e.getMessage());
	      }
	      return null;
	  }
}
