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

package org.watto.ge.helper;

import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/

public class FileListFilter {

  /**
   **********************************************************************************************
   Filters a list of Resources
   @return the filtered list of resources
   **********************************************************************************************
   **/
  @SuppressWarnings("rawtypes")
  public static Resource[] filterResources(Resource[] resources) {

    if (SingletonManager.has("FileListFilterValue")) {
      try {
        String filterValue = (String) SingletonManager.get("FileListFilterValue");
        WSTableColumn[] filterColumns = (WSTableColumn[]) SingletonManager.get("FileListFilterColumns");

        int numColumns = filterColumns.length;

        boolean isNumber = false;
        long searchValNumber = -1;
        try {
          searchValNumber = Long.parseLong(filterValue);
          isNumber = true;
        }
        catch (Throwable t) {
        }

        boolean isBoolean = false;
        boolean searchValBoolean = true;
        if (filterValue.equals("true")) {
          isBoolean = true;
          searchValBoolean = true;
        }
        else if (filterValue.equals("false")) {
          isBoolean = true;
          searchValBoolean = false;
        }

        boolean regexSearch = false;

        String searchValString = filterValue;
        if (Settings.getBoolean("SearchWildcardConversion")) {
          searchValString = filterValue.replace("*", "(.*)");
          regexSearch = true;
        }

        if (Settings.getBoolean("SearchRegExConversion")) {
          regexSearch = true;
        }

        // determine the starting position
        int numFiles = resources.length;
        ArchivePlugin readPlugin = Archive.getReadPlugin();

        // search for the files 
        int numFound = 0;
        Resource[] filteredResources = new Resource[numFiles];

        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[i];
          if (resource != null) {
            for (int c = 0; c < numColumns; c++) {
              WSTableColumn column = filterColumns[c];
              Class type = column.getType();
              char columnChar = column.getCharCode();

              boolean found = false;
              if (type == String.class) {
                if (regexSearch) { // regex
                  try {
                    found = ((String) readPlugin.getColumnValue(resource, columnChar)).matches(searchValString);
                  }
                  catch (Throwable t) {
                  }
                }
                else { // literal
                  found = (((String) readPlugin.getColumnValue(resource, columnChar)).indexOf(searchValString) >= 0);
                }
              }
              else if (isNumber && type == Long.class) {
                found = (((Long) readPlugin.getColumnValue(resource, columnChar)).longValue() == searchValNumber);
              }
              else if (isBoolean && type == Boolean.class) {
                found = (((Boolean) readPlugin.getColumnValue(resource, columnChar)).booleanValue() == searchValBoolean);
              }

              if (found) {
                filteredResources[numFound] = resource;
                numFound++;
                break; // stop searching the remaining columns - begin searching for the next file
              }

            }
          }
        }

        if (numFound == numFiles) {
          return resources;
        }
        else {
          resources = new Resource[numFound];
          System.arraycopy(filteredResources, 0, resources, 0, numFound);
          return resources;
        }

      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }

    return resources;
  }
}
