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
import org.watto.ge.plugin.archive.Plugin_WAD_WADH_3;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WAD_WADH_3_DB extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_WAD_WADH_3_DB() {
    super("WAD_WADH_3_DB", "NHL 2K3 DB Table");
    setExtensions("db");

    setGames("NHL 2K3");
    setPlatforms("PS2");
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
      if (plugin instanceof Plugin_WAD_WADH_3) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String filename = fm.getFile().getName().toLowerCase();
      if (filename.equals("feplayer.db") || filename.equals("feplayer_aux.db")) {
        rating += 5;
      }
      else {
        rating = 0;
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
        int lineSize = 98;

        int numKeys = (int) (arcSize / lineSize);
        FieldValidator.checkNumFiles(numKeys);

        WSTableColumn[] columns = new WSTableColumn[6];
        columns[0] = new WSTableColumn("Original First", 'f', String.class, false, true, 0, 0); // hidden column 0,0
        columns[1] = new WSTableColumn("Edited First", 'g', String.class, true, true);
        columns[2] = new WSTableColumn("Original Last", 'l', String.class, false, true, 0, 0); // hidden column 0,0
        columns[3] = new WSTableColumn("Edited Last", 'm', String.class, true, true);
        columns[4] = new WSTableColumn("ID", 'i', Integer.class, false, true, 0, 0); // hidden column 0,0
        columns[5] = new WSTableColumn("Metadata", 'd', byte[].class, false, true, 0, 0); // hidden column 0,0

        Object[][] data = new Object[numKeys][6];
        for (int k = 0; k < numKeys; k++) {
          // 16 - First Name
          String firstName = fm.readNullString(16);

          // 16 - Last Name
          String lastName = fm.readNullString(16);

          // 4 - Unknown ID
          int id = fm.readInt();

          // 62 - Other DB Columns
          byte[] otherDB = fm.readBytes(62);

          data[k] = new Object[] { firstName, firstName, lastName, lastName, id, otherDB };
        }

        PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);
        return preview;

      }
      else if (filename.equals("feplayer_aux.db")) {
        int lineSize = 140;

        int numKeys = (int) (arcSize / lineSize);
        FieldValidator.checkNumFiles(numKeys);

        WSTableColumn[] columns = new WSTableColumn[3];
        columns[0] = new WSTableColumn("Original", 'o', String.class, false, true, 0, 0); // hidden column 0,0
        columns[1] = new WSTableColumn("Edited", 'e', String.class, true, true);
        columns[2] = new WSTableColumn("Metadata", 'd', byte[].class, false, true, 0, 0); // hidden column 0,0

        Object[][] data = new Object[numKeys][6];
        for (int k = 0; k < numKeys; k++) {
          // 36 - Text
          String text = fm.readNullString(36);

          // 104 - Other DB Columns
          byte[] otherDB = fm.readBytes(104);

          data[k] = new Object[] { text, text, otherDB };
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

          // 4 - Unknown ID
          int id = (Integer) currentRow[4];

          // 62 - Other DB Columns
          byte[] otherDB = (byte[]) currentRow[5];

          fm.writeString(firstName);
          for (int p = 0; p < firstRemaining; p++) {
            fm.writeByte(0);
          }

          fm.writeString(lastName);
          for (int p = 0; p < lastRemaining; p++) {
            fm.writeByte(0);
          }

          fm.writeInt(id);

          fm.writeBytes(otherDB);
        }

      }
      else if (filename.equals("feplayer_aux.db")) {
        for (int k = 0; k < numRows; k++) {
          Object[] currentRow = data[k];

          // 36 - Text
          String text = ((String) currentRow[1]);
          if (text.length() > 36) {
            text = text.substring(0, 36);
          }
          int textRemaining = 36 - text.length();

          // 104 - Other DB Columns
          byte[] otherDB = (byte[]) currentRow[2];

          fm.writeString(text);
          for (int p = 0; p < textRemaining; p++) {
            fm.writeByte(0);
          }

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