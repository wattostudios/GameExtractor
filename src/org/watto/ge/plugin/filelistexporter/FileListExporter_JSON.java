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

package org.watto.ge.plugin.filelistexporter;

import org.watto.ErrorLogger;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.FileListExporterPlugin;
import org.watto.io.FileManipulator;

public class FileListExporter_JSON extends FileListExporterPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public FileListExporter_JSON() {
    super("json", "JSON Document");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(WSTableColumn[] columns, FileManipulator fm) {
    try {
      Resource[] resources = Archive.getResources();

      String archiveName = "";
      if (Archive.getBasePath() != null) {
        archiveName = Archive.getBasePath().getName();
      }

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin == null) {
        fm.writeString("This archive does not have a plugin");
        return;
      }

      String archivePlugin = "";
      archivePlugin = readPlugin.getName();

      // header
      fm.writeString("{ \"filelist\" : {\n" +
          "  \"program\" : {\n" +
          "    \"name\" : \"GAME EXTRACTOR\",\n" +
          "    \"description\" : \"Extensible Game Archive Editor\",\n" +
          "    \"website\" : \"http://www.watto.org\"\n" +
          "  },\n" +
          "  \"archive\" : {\n" +
          "    \"name\" : \"" + archiveName + "\",\n" +
          "    \"plugin\" : \"" + archivePlugin + "\",\n" +
          "    \"fileCount\" : \"" + resources.length + "\"\n" +
          "  },\n" +
          "  \"files\" : [\n" +
          "");

      // resources
      int numColumns = columns.length;
      for (int i = 0; i < resources.length; i++) {
        fm.writeString("    {\n");
        fm.writeString("    \"file\" : {\n");
        for (int j = 0; j < numColumns; j++) {
          String columnName = columns[j].getName();

          // removing incompatable XML characters
          try {
            columnName = columnName.replace('(', ' ');
            columnName = columnName.replace(')', ' ');
            columnName = columnName.replaceAll(" ", "");
          }
          catch (Throwable t) {
          }

          fm.writeString("      \"" + columnName + "\" : \"" + readPlugin.getColumnValue(resources[i], columns[j].getCharCode()) + "\"");
          if (j + 1 < numColumns) {
            fm.writeString(",\n");
          }
          else {
            fm.writeString("\n");
          }
        }
        fm.writeString("      }\n");
        if (i + 1 < resources.length) {
          fm.writeString("    },\n");
        }
        else {
          fm.writeString("    }\n");
        }
      }

      // footer
      fm.writeString("]}}");

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}