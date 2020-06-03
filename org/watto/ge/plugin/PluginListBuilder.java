/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.component.WSPlugin;
import org.watto.component.WSPluginGroup;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;

public class PluginListBuilder {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static PluginList[] convertPluginsToExtensionList(WSPlugin[] plugins) {
    if (plugins.length == 0) {
      return new PluginList[0];
    }

    Hashtable<String, PluginList> pluginList = new Hashtable<String, PluginList>(plugins.length);

    for (int i = 0; i < plugins.length; i++) {
      ArchivePlugin arcPlugin = (ArchivePlugin) plugins[i];
      if (arcPlugin == null) {
        continue;
      }
      String name = arcPlugin.getName();
      String[] exts = arcPlugin.getExtensions();

      for (int e = 0; e < exts.length; e++) {
        String desc = "*." + exts[e] + " (" + name + ")";
        pluginList.put(desc, new PluginList(desc, arcPlugin));
      }
    }

    Collection<PluginList> values = pluginList.values();
    PluginList[] list = values.toArray(new PluginList[0]);

    if (Settings.getBoolean("SortPluginLists")) {
      Arrays.sort(list);
    }

    return list;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static PluginList[] convertPluginsToGameList(WSPlugin[] plugins) {
    if (plugins.length == 0) {
      return new PluginList[0];
    }

    Hashtable<String, PluginList> pluginList = new Hashtable<String, PluginList>(plugins.length);

    for (int i = 0; i < plugins.length; i++) {
      ArchivePlugin arcPlugin = (ArchivePlugin) plugins[i];

      if (arcPlugin == null) {
        continue;
      }
      String[] games = arcPlugin.getGames();

      String ext = " (" + arcPlugin.getName() + ")";

      for (int g = 0; g < games.length; g++) {
        String desc = games[g] + ext;
        pluginList.put(desc, new PluginList(desc, arcPlugin));
      }
    }

    Collection<PluginList> values = pluginList.values();
    PluginList[] list = values.toArray(new PluginList[0]);

    if (Settings.getBoolean("SortPluginLists")) {
      Arrays.sort(list);
    }

    return list;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static PluginList[] convertPluginsToList(WSPlugin[] plugins) {
    if (plugins.length == 0) {
      return new PluginList[0];
    }

    PluginList[] list = new PluginList[plugins.length];

    for (int i = 0; i < plugins.length; i++) {
      ArchivePlugin arcPlugin = (ArchivePlugin) plugins[i];
      if (arcPlugin == null) {
        continue;
      }
      String name = arcPlugin.getName();
      list[i] = new PluginList(name, arcPlugin);
    }

    if (Settings.getBoolean("SortPluginLists")) {
      Arrays.sort(list);
    }

    return list;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static ScannerPlugin[] getEnabledScanners() {
    WSPlugin[] plugins = WSPluginManager.getGroup("Scanner").getPlugins();

    if (plugins == null || plugins.length == 0) {
      return new ScannerPlugin[0];
    }

    ScannerPlugin[] allowed = new ScannerPlugin[plugins.length];
    int numAllowed = 0;

    for (int i = 0; i < plugins.length; i++) {
      ScannerPlugin plugin = (ScannerPlugin) plugins[i];
      if (Settings.getBoolean("Scanner_" + plugin.getCode())) {
        allowed[numAllowed] = plugin;
        numAllowed++;
      }
    }

    if (numAllowed == allowed.length) {
      return allowed;
    }
    else {
      ScannerPlugin[] temp = new ScannerPlugin[numAllowed];
      System.arraycopy(allowed, 0, temp, 0, numAllowed);
      return temp;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static PluginList[] getPluginList() {
    WSPlugin[] plugins = WSPluginManager.getGroup("Archive").getPlugins();

    if (plugins == null || plugins.length == 0) {
      plugins = new ArchivePlugin[0];
    }

    return getPluginList(plugins);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static PluginList[] getPluginList(WSPlugin[] plugins) {
    if (plugins.length == 0) {
      return new PluginList[0];
    }

    PluginList[] list;
    String displayType = Settings.get("PluginListDisplayType");

    if (displayType.equals("Game")) {
      list = convertPluginsToGameList(plugins);
    }
    else if (displayType.equals("Extension")) {
      list = convertPluginsToExtensionList(plugins);
    }
    else {
      list = convertPluginsToList(plugins);
    }

    return list;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static PluginList[] getWritePluginList() {
    ArchivePlugin[] arcPlugins = getWritePlugins();
    return getPluginList(arcPlugins);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static ArchivePlugin[] getWritePlugins() {
    WSPlugin[] plugins = WSPluginManager.getGroup("Archive").getPlugins();

    if (plugins == null || plugins.length == 0) {
      plugins = new ArchivePlugin[0];
    }

    ArchivePlugin[] writePlugins = new ArchivePlugin[plugins.length];
    int numWritable = 0;

    // If the plugin can do renames or replaces, add it into the writeArchive list.
    // But if the archive has been modified irreversably, don't allow it.
    // Irreversable is, for example, add or remove files in replace-only or rename-only archives
    ArchivePlugin readPlugin = Archive.getReadPlugin();
    if (readPlugin != null) {

      // Also grabs all implicitReplacing plugins
      if (!readPlugin.canWrite() && (readPlugin.canReplace() || readPlugin.canRename())) {
        writePlugins[numWritable] = readPlugin;
        numWritable++;
      }
    }

    for (int i = 0; i < plugins.length; i++) {
      ArchivePlugin arcPlugin = (ArchivePlugin) plugins[i];
      if (arcPlugin.canWrite()) {
        writePlugins[numWritable] = arcPlugin;
        numWritable++;
      }
    }

    if (numWritable == writePlugins.length) {
      return writePlugins;
    }

    ArchivePlugin[] resizedWrite = new ArchivePlugin[numWritable];
    System.arraycopy(writePlugins, 0, resizedWrite, 0, numWritable);

    return resizedWrite;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static ViewerPlugin[] getWriteViewers(PreviewPanel panel) {
    WSPluginGroup pluginGroup = WSPluginManager.getGroup("Viewer");
    if (pluginGroup == null) {
      return new ViewerPlugin[0];
    }
    WSPlugin[] plugins = pluginGroup.getPlugins();

    ViewerPlugin[] writers = new ViewerPlugin[plugins.length];
    int numWriters = 0;

    for (int i = 0; i < plugins.length; i++) {
      ViewerPlugin plugin = (ViewerPlugin) plugins[i];
      if (plugin.canWrite(panel)) {
        writers[numWriters] = plugin;
        numWriters++;
      }
    }

    if (numWriters <= 0) {
      return new ViewerPlugin[0];
    }
    else if (numWriters < writers.length) {
      // resize the array
      ViewerPlugin[] temp = writers;
      writers = new ViewerPlugin[numWriters];
      System.arraycopy(temp, 0, writers, 0, numWriters);
    }

    return writers;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PluginListBuilder() {
  }

}