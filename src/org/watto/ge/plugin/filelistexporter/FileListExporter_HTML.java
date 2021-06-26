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

public class FileListExporter_HTML extends FileListExporterPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileListExporter_HTML() {
    super("html", "HTML Webpage");
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
      fm.writeString("<html>\n<head>\n<title>Game Extractor File List - [" + archiveName + "]</title>\n</head>\n");
      fm.writeString("<body bgcolor='white'>\n<center>\n");
      fm.writeString("<font size='5' color='darkgreen' face='arial'>GAME EXTRACTOR<br>\n<font size='3'>Extensible Game Archive Editor<br>\n<a href='http://www.watto.org/extract'><font color='black'>http://www.watto.org/extract</font></a></font>\n</font><br><br>\n");
      fm.writeString("<font size='3' color='black' face='arial'>Archive name: " + archiveName + "<br>\nPlugin format: " + archivePlugin + "<br>\nNumber of files: " + resources.length + "<br><br>\n</font>\n\n");
      fm.writeString("<table border='2' cellpadding='4' cellspacing='0'>\n");

      // table columns
      int numColumns = columns.length;
      fm.writeString("<tr>");
      for (int i = 0; i < numColumns; i++) {
        fm.writeString("<td>" + columns[i].getName() + "</td>");
      }
      fm.writeString("</tr>\n");

      // resources
      for (int i = 0; i < resources.length; i++) {
        fm.writeString("<tr>");
        for (int j = 0; j < numColumns; j++) {
          fm.writeString("<td>" + readPlugin.getColumnValue(resources[i], columns[j].getCharCode()) + "</td>");
        }
        fm.writeString("</tr>\n");
      }

      // footer
      fm.writeString("</table>\n\n</center>\n</body>\n</html>");
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}