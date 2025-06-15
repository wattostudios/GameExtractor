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

import java.io.File;

import org.watto.ErrorLogger;
import org.watto.component.WSPlugin;
import org.watto.component.WSPluginGroup;
import org.watto.component.WSPluginManager;
import org.watto.io.FileManipulator;

public class PluginFinder {

  /**
  **********************************************************************************************
  does not do sorting!
  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  public static RatedPlugin[] findPlugins(File path, Class pluginType) {
    FileManipulator fm = new FileManipulator(path, false);
    return findPlugins(fm, pluginType);
  }

  /**
  **********************************************************************************************
  does not do sorting!
  **********************************************************************************************
  **/
  @SuppressWarnings({ "rawtypes", "unused" })
  public static RatedPlugin[] findPlugins(FileManipulator fm, Class pluginType) {
    try {
      WSPlugin[] plugins = new ArchivePlugin[0];

      // should we only use plugins with rating > 25%
      boolean checkRating = false;

      if (pluginType == ArchivePlugin.class) {
        plugins = WSPluginManager.getGroup("Archive").getPlugins();
        checkRating = true;
      }
      else if (pluginType == ViewerPlugin.class) {
        WSPluginGroup group = WSPluginManager.getGroup("Viewer");
        if (group != null) {
          plugins = group.getPlugins();
        }
        else {
          plugins = new WSPlugin[0];
        }
        checkRating = true;
      }

      RatedPlugin[] results = new RatedPlugin[plugins.length];
      int zeroPos = plugins.length - 1;
      int startPos = 0;

      if (pluginType == ArchivePlugin.class) {
        for (int i = 0; i < results.length; i++) {
          //if (plugins[i] == null){
          //  zeroPos
          //  continue();
          //  }

          fm.seek(0);
          //System.out.println("Trying plugin " + plugins[i]);
          int rating = ((ArchivePlugin) plugins[i]).getMatchRating(fm);
          if (checkRating && rating < 25) {
            // failed plugin
            zeroPos--;
          }
          else {
            // successful plugin
            results[startPos] = new RatedPlugin(plugins[i], rating);
            startPos++;
          }
        }
      }

      else if (pluginType == ViewerPlugin.class) {
        for (int i = 0; i < results.length; i++) {
          fm.seek(0);
          //System.out.println("Trying plugin " + plugins[i]);
          int rating = ((ViewerPlugin) plugins[i]).getMatchRating(fm);
          if (checkRating && rating < 25) {
            // failed plugin
            zeroPos--;
          }
          else {
            // successful plugin
            results[startPos] = new RatedPlugin(plugins[i], rating);
            startPos++;
          }
        }
      }

      fm.close();

      // resize the results array
      RatedPlugin[] temp = results;
      results = new RatedPlugin[startPos];
      System.arraycopy(temp, 0, results, 0, startPos);

      return results;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PluginFinder() {
  }

}