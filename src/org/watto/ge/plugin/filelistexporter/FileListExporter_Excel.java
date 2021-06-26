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

public class FileListExporter_Excel extends FileListExporterPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public FileListExporter_Excel() {
    super("xls", "Excel Spreadsheet");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(WSTableColumn[] columns, FileManipulator fm) {
    try {
      Resource[] resources = Archive.getResources();

      //String archiveName = "";
      //if (Archive.getBasePath() != null) {
      //  archiveName = Archive.getBasePath().getName();
      //}

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin == null) {
        fm.writeString("This archive does not have a plugin");
        return;
      }

      //String archivePlugin = "";
      //archivePlugin = readPlugin.getName();

      // table columns
      int numColumns = columns.length;
      for (int i = 0; i < numColumns; i++) {
        fm.writeString(columns[i].getName() + "\t");
      }
      fm.writeString("\n\n");

      // resources
      for (int i = 0; i < resources.length; i++) {
        for (int j = 0; j < numColumns; j++) {
          fm.writeString(readPlugin.getColumnValue(resources[i], columns[j].getCharCode()) + "\t");
        }
        fm.writeString("\n");
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}