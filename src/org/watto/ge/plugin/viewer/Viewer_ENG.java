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
public class Viewer_ENG extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ENG() {
    super("ENG", "NHL 2000 ENG Table");
    setExtensions("eng", "cze", "fin", "fra", "ger", "swe");

    setGames("NHL 2000");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
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

      // 4 - Number of Keys
      if (FieldValidator.checkNumFiles(fm.readInt() / 4)) {
        rating += 5;
      }

      try {
        String parent = fm.getFile().getParent();
        if (parent.equalsIgnoreCase("REQUIRED") || parent.equalsIgnoreCase("GUI")) {
          rating += 5;
        }
      }
      catch (Throwable t) {
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

      // 4 - Number of Texts
      int numKeys = fm.readInt() / 4;
      FieldValidator.checkNumFiles(numKeys);

      if (numKeys < 10) { // early exit, to try to combat incorrect reading
        return null;
      }

      fm.relativeSeek(0);

      int[] offsets = new int[numKeys];
      for (int k = 0; k < numKeys; k++) {
        // 4 - Text Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[k] = offset;
      }

      Object[][] data = new Object[numKeys][2];
      for (int k = 0; k < numKeys; k++) {
        fm.relativeSeek(offsets[k]);

        // X - Text
        // 1 - null Text Terminator
        String text = fm.readNullString();

        data[k] = new Object[] { text, text };
      }

      fm.close();

      WSTableColumn[] columns = new WSTableColumn[2];
      columns[0] = new WSTableColumn("Original", 'o', String.class, false, true, 0, 0); // hidden column 0,0
      columns[1] = new WSTableColumn("Edited", 'e', String.class, true, true);

      PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);

      return preview;

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
  public void write(PreviewPanel panel, FileManipulator fm) {
    try {
      // should only be triggered from Table panels
      if (!(panel instanceof PreviewPanel_Table)) {
        return;
      }
      PreviewPanel_Table previewPanel = (PreviewPanel_Table) panel;

      // Get the table data from the preview (which are the edited ones), so we know which ones have been changed, and can put that data
      // into the "fm" as we read through and replicate the "originalFM"
      Object[][] data = previewPanel.getData();
      if (data == null) {
        return;
      }

      int numRows = data.length;

      int textOffset = numRows * 4;

      // Directory
      for (int k = 0; k < numRows; k++) {
        Object[] currentRow = data[k];

        // 4 - Text Offset
        fm.writeInt(textOffset);

        String text = (String) currentRow[1];
        int length = text.length() + 1; // +1 for null terminator

        textOffset += length;
      }

      // Texts
      for (int k = 0; k < numRows; k++) {
        Object[] currentRow = data[k];

        // X - Text String
        // 1 - null Text String terminator
        String text = (String) currentRow[1];
        fm.writeString(text);
        fm.writeByte(0);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}