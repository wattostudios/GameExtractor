/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Table;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DB extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DB() {
    super("DB", "NHL 2K (Dreamcast) DB Table");
    setExtensions("db");

    setGames("NHL 2K");
    setPlatforms("Dreamcast");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canEdit(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Table) {
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String filename = fm.getFile().getName().toLowerCase();
      if (filename.equals("feplayer.db")) {
        rating += 5;
      }
      else {
        rating = 0;
      }

      if (fm.getLength() % 60 == 0) {
        rating += 5;
      }
      else {
        return 0;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      String filename = fm.getFile().getName().toLowerCase();
      if (filename.equals("feplayer.db")) {
        int lineSize = 60;

        int numKeys = (int) (arcSize / lineSize);
        FieldValidator.checkNumFiles(numKeys);

        WSTableColumn[] columns = new WSTableColumn[6];
        columns[0] = new WSTableColumn("Original First", 'f', String.class, false, true, 0, 0); // hidden column 0,0
        columns[1] = new WSTableColumn("Edited First", 'g', String.class, true, true);
        columns[2] = new WSTableColumn("Original Last", 'l', String.class, false, true, 0, 0); // hidden column 0,0
        columns[3] = new WSTableColumn("Edited Last", 'm', String.class, true, true);
        columns[4] = new WSTableColumn("ID", 'i', Short.class, false, true, 0, 0); // hidden column 0,0
        columns[5] = new WSTableColumn("Metadata", 'd', byte[].class, false, true, 0, 0); // hidden column 0,0

        Object[][] data = new Object[numKeys][6];
        for (int k = 0; k < numKeys; k++) {
          // 16 - First Name
          String firstName = fm.readNullString(16);

          // 16 - Last Name
          String lastName = fm.readNullString(16);

          // 2 - Unknown ID
          short id = fm.readShort();

          // 26 - Other DB Columns
          byte[] otherDB = fm.readBytes(26);

          data[k] = new Object[] { firstName, firstName, lastName, lastName, id, otherDB };
        }

        PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);
        return preview;

      }

      return null;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void edit(FileManipulator originalFM, PreviewPanel preview, FileManipulator fm) {
    // We can't write from scratch, but we can Edit the old file

    try {
      // should only be triggered from Table panels
      if (!(preview instanceof PreviewPanel_Table)) {
        return;
      }
      PreviewPanel_Table previewPanel = (PreviewPanel_Table) preview;

      // Get the table data from the preview (which are the edited ones), so we know which ones have been changed, and can put that data
      // into the "fm" as we read through and replicate the "originalFM"
      Object[][] data = previewPanel.getData();
      if (data == null) {
        return;
      }

      int numRows = data.length;

      String filename = fm.getFile().getName().toLowerCase();
      if (filename.equals("feplayer.db")) {

        for (int k = 0; k < numRows; k++) {
          Object[] currentRow = data[k];

          // 16 - First Name
          String firstName = ((String) currentRow[1]);
          if (firstName.length() > 16) {
            firstName = firstName.substring(0, 16);
          }
          int firstRemaining = 16 - firstName.length();

          // 16 - Last Name
          String lastName = ((String) currentRow[3]);
          if (lastName.length() > 16) {
            lastName = lastName.substring(0, 16);
          }
          int lastRemaining = 16 - lastName.length();

          // 2 - Unknown ID
          short id = (Short) currentRow[4];

          // 26 - Other DB Columns
          byte[] otherDB = (byte[]) currentRow[5];

          fm.writeString(firstName);
          for (int p = 0; p < firstRemaining; p++) {
            fm.writeByte(0);
          }

          fm.writeString(lastName);
          for (int p = 0; p < lastRemaining; p++) {
            fm.writeByte(0);
          }

          fm.writeShort(id);

          fm.writeBytes(otherDB);
        }

      }

      fm.close();
      originalFM.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel panel, FileManipulator destination) {

  }

}