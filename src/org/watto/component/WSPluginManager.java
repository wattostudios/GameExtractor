////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.component;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.datatype.PluginPrefix;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 * Manages dynamic <code>WSPlugin</code>s that can be loaded from directories and ZIP files.
 ***********************************************************************************************/
public class WSPluginManager {

  /** The plugin groups **/
  static Hashtable<String, WSPluginGroup> groups = new Hashtable<String, WSPluginGroup>();

  /***********************************************************************************************
   * Creates a <code>WSPluginGroup</code> for the given <code>WSPlugin</code> <code>type</code>
   * @param type the <code>WSPlugin</code> type
   ***********************************************************************************************/
  public static void addGroup(String type) {
    groups.put(type, new WSPluginGroup(type));
  }

  /***********************************************************************************************
   * Loads a <code>WSPlugin</code> using the <code>ClassLoader</code> and assigns it to the
   * <code>group</code> defined by the <code>WSPlugin</code> <code>getType()</code>
   * @param pluginName the name of the <code>WSPlugin</code> <code>class</code>
   * @param classLoader the ClassLoader to use for loading the <code>class</code>
   ***********************************************************************************************/
  public static void addPlugin(String pluginName, ClassLoader classLoader) {
    try {
      WSPlugin plugin = (WSPlugin) classLoader.loadClass(pluginName).newInstance();
      addPlugin(plugin);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Loads a <code>WSPlugin</code> using the <code>ClassLoader</code> and assigns it to the
   * <code>group</code> defined by the <code>WSPlugin</code> <code>getType()</code>
   * @param pluginName the name of the <code>WSPlugin</code> <code>class</code>
   * @param pluginType the name of the <code>WSPlugin</code> <code>type</code>
   * @param classLoader the ClassLoader to use for loading the <code>class</code>
   ***********************************************************************************************/
  public static void addPlugin(String pluginName, String pluginType, ClassLoader classLoader) {
    try {
      WSPlugin plugin = (WSPlugin) classLoader.loadClass(pluginName).newInstance();

      if (plugin != null) {
        plugin.setType(pluginType);
      }

      addPlugin(plugin);
    }
    catch (Throwable t) {
      ErrorLogger.log("Load Plugin failed for " + pluginName + " of type " + pluginType);
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Adds a <code>WSPlugin</code> to the <code>group</code> defined by the <code>WSPlugin</code>
   * <code>getType()</code>
   * @param plugin the <code>WSPlugin</code> to add to a group
   ***********************************************************************************************/
  public static void addPlugin(WSPlugin plugin) {
    try {
      String pluginType = plugin.getType();
      WSPluginGroup group = groups.get(pluginType);
      if (group == null) {
        addGroup(pluginType);
        group = groups.get(pluginType);
      }
      group.addPlugin(plugin);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Gets the <code>WSPluginGroup</code> for the given <code>WSPlugin</code> <code>type</code>
   * @param type the <code>WSPlugin</code> type
   * @return the <code>WSPluginGroup</code> with the given <code>type</code>
   ***********************************************************************************************/
  public static WSPluginGroup getGroup(String type) {
    return groups.get(type);
  }

  /***********************************************************************************************
   * Gets the number of <code>groups</code>
   * @return the number of <code>groups</code>
   ***********************************************************************************************/
  public static int getGroupCount() {
    return groups.size();
  }

  /***********************************************************************************************
   * Gets the <code>groups</code> as a <code>WSPluginGroup</code>[]
   * @return the <code>groups</code> as a <code>WSPluginGroup</code>[]
   ***********************************************************************************************/
  public static WSPluginGroup[] getGroups() {
    int groupCount = groups.size();

    Enumeration<WSPluginGroup> groupEnumeration = groups.elements();

    WSPluginGroup[] groupArray = new WSPluginGroup[groupCount];
    for (int i = 0; i < groupCount; i++) {
      groupArray[i] = groupEnumeration.nextElement();
    }

    return groupArray;
  }

  /***********************************************************************************************
   * Gets the <code>WSPlugin</code> for the given <code>type</code> and <code>code</code>
   * @param type the <code>WSPlugin</code> type
   * @param code the code of the <code>WSPlugin</code>
   * @return the <code>WSPlugin</code> with the given <code>type</code> and <code>code</code>
   ***********************************************************************************************/
  public static WSPlugin getPlugin(String type, String code) {
    WSPluginGroup group = groups.get(type);
    if (group == null) {
      return null;
    }
    return group.getPlugin(code);
  }

  /***********************************************************************************************
   * Loads the <code>WSPlugin</code> defined by the <code>pluginFile</code> <code>class</code>
   * @param pluginFile a <code>class</code> <code>File</code> of a <code>WSPlugin</code>
   ***********************************************************************************************/
  public static void loadPlugin(File pluginFile) {
    try {
      URL classURL = new URL("file:" + pluginFile.getParent() + "/");
      URLClassLoader cl = URLClassLoader.newInstance(new URL[] { classURL });

      String name = pluginFile.getName();
      if (name.length() <= 6 || name.indexOf(".class") < 0) {
        return;
      }
      name = name.substring(0, name.length() - 6);
      addPlugin(name, cl);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Loads a list of directories and ZIP files from the <code>File</code> referenced in the
   * <i>PluginPreferencesFile</i> <code>Settings</code>, and scans them for
   * <code>WSPlugin</code>s to load
   * @throws FileNotFoundException The <code>File</code> referenced by the
   *         <i>PluginPreferencesFile</i> <code>Settings</code> could not be found.
   ***********************************************************************************************/
  public static void loadPlugins() throws FileNotFoundException {

    File pluginPreferences = new File(Settings.getString("PluginPathsFile"));

    if (!pluginPreferences.exists()) {
      throw new FileNotFoundException("The plugin preferences file at " + pluginPreferences.getAbsolutePath() + " could not be found.");
    }

    loadPlugins(pluginPreferences);

  }

  /***********************************************************************************************
   * Loads a list of directories and ZIP files from the <code>pluginPreferences</code> and scans
   * them for <code>WSPlugin</code>s to load
   * @param pluginPreferences an XML-format <code>File</code> that lists directories and ZIP
   *        files
   ***********************************************************************************************/
  public static void loadPlugins(File pluginPreferences) {
    try {

      XMLNode root = XMLReader.read(pluginPreferences);

      // load the prefixes
      XMLNode prefixesNode = root.getChild("prefixes");

      int prefixCount = prefixesNode.getChildCount();
      PluginPrefix[] prefixes = new PluginPrefix[prefixCount];

      for (int i = 0; i < prefixCount; i++) {
        XMLNode prefixNode = prefixesNode.getChild(i);
        String prefixName = prefixNode.getContent();
        String prefixType = prefixNode.getAttribute("type");

        prefixes[i] = new PluginPrefix(prefixName, prefixType);
      }

      // load the plugins
      XMLNode locationsNode = root.getChild("locations");
      int locationCount = locationsNode.getChildCount();

      for (int i = 0; i < locationCount; i++) {
        XMLNode locationNode = locationsNode.getChild(i);

        String locationName = locationNode.getContent();
        File location = new File(new File(locationName).getAbsolutePath());

        if (!location.exists()) {
          // Lets try swapping the / and \ characters, in case we're on Unix
          locationName = locationName.replace('\\', '/');
          location = new File(new File(locationName).getAbsolutePath());

          if (!location.exists()) {
            ErrorLogger.log("[WSPluginManager] Plugin location " + locationName + " could not be found");
            continue;
          }
        }

        String locationType = locationNode.getAttribute("type");
        if (locationType.equals("zip")) {
          scanZip(location, prefixes);
        }
        else if (locationType.equals("file")) {
          loadPlugin(location);
        }
        else if (locationType.equals("directory")) {

          // traverse tag
          String tag = locationNode.getAttribute("traverse");

          boolean traverse = false;
          if (tag != null && tag.equals("true")) {
            traverse = true;
          }

          // package tag
          tag = locationNode.getAttribute("package");

          String packageName = "";
          if (packageName != null) {
            packageName = tag;
          }

          // pathIsPackage tag
          tag = locationNode.getAttribute("pathIsPackage");

          boolean pathIsPackage = false;
          if (tag != null && tag.equals("true")) {
            pathIsPackage = true;
          }

          scanDirectory(location, packageName, traverse, pathIsPackage, prefixes);

        }
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Scans all <code>File</code>s in a <code>directory</code> for <code>WSPlugin</code>
   * <code>class</code>es and loads them
   * @param directory the directory to scan for <code>WSPlugin</code> <code>class</code>es
   * @param traverse <b>true</b> to scan this <code>directory</code> and any
   *        sub-directories<br />
   *        <b>false</b> to only scan this <code>directory</code>
   ***********************************************************************************************/
  public static void scanDirectory(File directory, String packagePrefix, boolean traverse, boolean pathIsPackage) {
    scanDirectory(directory, packagePrefix, traverse, pathIsPackage, null);
  }

  /***********************************************************************************************
   * Scans all <code>File</code>s in a <code>directory</code> for <code>WSPlugin</code>
   * <code>class</code>es and loads them
   * @param directory the directory to scan for <code>WSPlugin</code> <code>class</code>es
   * @param traverse <b>true</b> to scan this <code>directory</code> and any
   *        sub-directories<br />
   *        <b>false</b> to only scan this <code>directory</code>
   * @param prefixes the prefixes that determine whether a <code>File</code> is a
   *        <code>WSPlugin</code> or not
   ***********************************************************************************************/
  public static void scanDirectory(File directory, String packagePrefix, boolean traverse, boolean pathIsPackage, PluginPrefix[] prefixes) {
    try {

      if (!directory.exists()) {
        throw new FileNotFoundException("The directory " + directory.getAbsolutePath() + " does not exist.");
      }

      File[] files = directory.listFiles();

      URL classURL = new URL("file:" + directory.getAbsolutePath() + "/");
      URLClassLoader cl = URLClassLoader.newInstance(new URL[] { classURL });

      for (int i = 0; i < files.length; i++) {
        File file = files[i];

        if (file.isDirectory() && traverse) {
          String packageName = packagePrefix;
          if (pathIsPackage) {
            packageName += "." + file.getName();
          }
          scanDirectory(file, packageName, traverse, pathIsPackage, prefixes);
          continue;
        }

        String name = file.getName();
        if (name.length() <= 6 || name.indexOf(".class") < 0) {
          continue;
        }

        name = name.substring(0, name.length() - 6);

        // check that the filename has a valid prefix

        if (prefixes != null) {
          int prefixCount = prefixes.length;
          for (int j = 0; j < prefixCount; j++) {
            if (name.startsWith(prefixes[j].getPrefix())) {
              if (packagePrefix == null) {
                addPlugin(name, prefixes[j].getType(), cl);
              }
              else {
                addPlugin(packagePrefix + "." + name, prefixes[j].getType(), cl);
              }

              continue;
            }
          }
        }
        else {
          // no prefix check required
          addPlugin(packagePrefix + "." + name, cl);
        }

      }

    }
    catch (Throwable t) {
      //ErrorLogger.log(t);
      if (Settings.getBoolean("DebugMode")) {
        ErrorLogger.log(t.getMessage());
      }
    }
  }

  /***********************************************************************************************
   * Scans a <code>zip</code> <code>File</code> for <code>WSPlugin</code> <code>class</code>es
   * and loads them
   * @param zip the zip <code>File</code> to scan for <code>WSPlugin</code> <code>class</code>es
   ***********************************************************************************************/
  public static void scanZip(File zip) {
    scanZip(zip, null);
  }

  /***********************************************************************************************
   * Scans a <code>zip</code> <code>File</code> for <code>WSPlugin</code> <code>class</code>es
   * and loads them
   * @param zip the zip <code>File</code> to scan for <code>WSPlugin</code> <code>class</code>es
   * @param prefixes the prefixes that determine whether a <code>File</code> is a
   *        <code>WSPlugin</code> or not
   ***********************************************************************************************/
  public static void scanZip(File zip, PluginPrefix[] prefixes) {
    try {

      if (!zip.exists()) {
        throw new FileNotFoundException("The zip file " + zip.getAbsolutePath() + " does not exist.");
      }

      ZipFile zipFile = new ZipFile(zip);
      Enumeration<? extends ZipEntry> files = zipFile.entries();

      ClassLoader cl = ClassLoader.getSystemClassLoader();

      while (files.hasMoreElements()) {

        String name = ((ZipEntry) files.nextElement()).getName();
        if (name.length() <= 6 || name.indexOf(".class") < 0) {
          continue;
        }

        name = name.substring(0, name.length() - 6);

        // Correct the Java Package --> Replace all "/" characters with "."
        name = name.replaceAll("/", ".");

        //ErrorLogger.log("trying to load " + name + " from the jar file");

        // check that the filename has a valid prefix
        if (prefixes != null) {
          int prefixCount = prefixes.length;
          for (int j = 0; j < prefixCount; j++) {
            if (name.indexOf(prefixes[j].getPrefix()) >= 0) {
              //ErrorLogger.log("Loading Plugin " + name + " from the jar file");
              addPlugin(name, prefixes[j].getType(), cl);
              continue;
            }
          }
        }
        else {
          // no prefix check required
          addPlugin(name, cl);
        }

      }

      zipFile.close();

    }
    catch (Throwable t) {
      //ErrorLogger.log(t);
      if (Settings.getBoolean("DebugMode")) {
        ErrorLogger.log(t.getMessage());
      }
    }
  }

  /***********************************************************************************************
   * Constructor
   ***********************************************************************************************/
  public WSPluginManager() {
  }
}