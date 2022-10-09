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
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WAD_WADH_3_ENG extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_WAD_WADH_3_ENG() {
    super("WAD_WADH_3_ENG", "NHL 2K3 ENG Table");
    setExtensions("eng");

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

      // 4 - Number of Keys
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
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
      int numKeys = fm.readInt();
      FieldValidator.checkNumFiles(numKeys);

      int textOffset = (numKeys * 16) + 4;
      fm.relativeSeek(textOffset);

      int textLength = (int) (arcSize - textOffset);
      byte[] textBytes = fm.readBytes(textLength);
      FileManipulator textFM = new FileManipulator(new ByteBuffer(textBytes));

      fm.relativeSeek(4);

      Object[][] data = new Object[numKeys][5];
      for (int k = 0; k < numKeys; k++) {
        // 4 - Hash
        int hash = fm.readInt();

        // 4 - Unknown (3)
        int unknownID = fm.readInt();

        // 4 - Text Offset
        int offset = fm.readInt() - textOffset;
        FieldValidator.checkOffset(offset, textLength);

        // 4 - Text Length (including null terminator and padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, textLength);

        int codeValue = 0;

        textFM.relativeSeek(offset);

        if (length > 4) {
          codeValue = textFM.readInt();
          length -= 4;
        }
        String text = textFM.readNullString(length);

        data[k] = new Object[] { hash, unknownID, codeValue, text, text };
      }

      textFM.close();

      WSTableColumn[] columns = new WSTableColumn[5];
      columns[0] = new WSTableColumn("Hash", 'h', Integer.class, false, true, 0, 0); // hidden column 0,0
      columns[1] = new WSTableColumn("Type ID", 'i', Integer.class, false, true, 0, 0); // hidden column 0,0
      columns[2] = new WSTableColumn("Code Value", 'c', Integer.class, false, true, 0, 0); // hidden column 0,0
      columns[3] = new WSTableColumn("Original", 'o', String.class, false, true, 0, 0); // hidden column 0,0
      columns[4] = new WSTableColumn("Edited", 'e', String.class, true, true);

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

      // 4 - Number of Texts
      fm.writeInt(numRows);

      int textOffset = 4 + (numRows * 16);

      // Directory
      for (int k = 0; k < numRows; k++) {
        Object[] currentRow = data[k];

        // 4 - Hash
        fm.writeInt((Integer) currentRow[0]);

        // 4 - Unknown (3)
        fm.writeInt((Integer) currentRow[1]);

        // 4 - Text Offset
        fm.writeInt(textOffset);

        // 4 - Text Length (including null terminator and padding)
        String text = (String) currentRow[4];
        int length = text.length() + 1 + 4; // +1 for null terminator, +4 for the code value
        length += ArchivePlugin.calculatePadding(length, 4);

        fm.writeInt(length);
        textOffset += length;
      }

      // Texts
      for (int k = 0; k < numRows; k++) {
        Object[] currentRow = data[k];

        // 4 - Code Value?
        fm.writeInt((Integer) currentRow[2]);

        // X - Text String
        // 1 - null Text String terminator
        // 0-3 - null Padding to a multiple of 4 bytes

        String text = (String) currentRow[4];
        int length = text.length() + 1; // +1 for null terminator
        int padding = ArchivePlugin.calculatePadding(length, 4);

        fm.writeString(text);
        fm.writeByte(0);

        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
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