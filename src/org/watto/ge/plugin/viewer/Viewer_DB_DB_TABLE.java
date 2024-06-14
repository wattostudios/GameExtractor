/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Table;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DB_DB;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DB_DB_TABLE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DB_DB_TABLE() {
    super("DB_DB_TABLE", "NHL 10 Table");
    setExtensions("table");

    setGames("NHL 10");
    setPlatforms("PS3");
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
      if (plugin instanceof Plugin_DB_DB) {
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

      fm.skip(20);

      // 2 - Number of Records?
      short numRecords1 = ShortConverter.changeFormat(fm.readShort());

      // 2 - Number of Records?
      short numRecords2 = ShortConverter.changeFormat(fm.readShort());

      if (numRecords1 == numRecords2) {
        rating += 5;
      }

      // 4 - Unknown (65535)
      if (IntConverter.changeFormat(fm.readInt()) == 65535) {
        rating += 5;
      }

      // 4 - Number of Fields (LITTLE)
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
  @SuppressWarnings("rawtypes")
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      // 4 - Hash?
      // 4 - Unknown (2)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      fm.skip(20);

      // 2 - Number of Records?
      short numKeys = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkNumFiles(numKeys);

      // 2 - Number of Records?
      // 4 - Unknown (65535)
      fm.skip(6);

      // 4 - Number of Fields (LITTLE)
      int numFields = fm.readInt();
      FieldValidator.checkNumFiles(numFields);

      // 4 - null
      // 4 - Hash?
      fm.skip(8);

      WSTableColumn[] columns = new WSTableColumn[numFields];

      int[] types = new int[numFields];
      int[] offsets = new int[numFields];
      int[] depths = new int[numFields];

      int lineSize = 0;
      for (int i = 0; i < numFields; i++) {
        Class typeClass = Integer.class;

        // 4 - Field Type (3=Integer, 0=String)
        int type = IntConverter.changeFormat(fm.readInt());
        types[i] = type;
        if (type == 0) {
          typeClass = String.class;
        }
        else if (type == 3) {
          // already an Integer
        }
        else {
          ErrorLogger.log("[Viewer_DB_DB_TABLE] Unknown field type: " + type);
        }

        // 4 - Data Offset for the Field in the Record
        int offset = IntConverter.changeFormat(fm.readInt());
        offsets[i] = offset;

        // 4 - Field Code
        String name = fm.readString(4);

        // 4 - Depth (Bits per Entry)
        int depth = IntConverter.changeFormat(fm.readInt());
        depths[i] = depth;

        columns[i] = new WSTableColumn(name, (char) (i + 65), typeClass, true, true);

        int endPos = offset + depth;
        if (endPos > lineSize) {
          lineSize = endPos;
        }
      }

      // work out the amount of padding at the end of each line
      int paddingBits = ArchivePlugin.calculatePadding(lineSize, 32);
      lineSize += paddingBits;

      Object[][] data = new Object[numKeys][numFields];

      for (int k = 0; k < numKeys; k++) {

        // read the line (including padding at the end)
        boolean[] bits = new boolean[lineSize];
        for (int b = 0; b < lineSize; b += 8) {
          boolean[] bit8 = fm.readBits();
          System.arraycopy(bit8, 0, bits, b, 8);
        }

        for (int f = 0; f < numFields; f++) {
          int type = types[f];
          int depth = depths[f];

          if (type == 0) {
            // String
            String text = "";

            // read blocks of 8 bits and convert to a character
            int startPos = offsets[f];
            int endPos = startPos + depth;

            for (int r = startPos; r < endPos; r += 8) {
              int value = 0;
              for (int b = 0; b < 8; b++) {
                boolean bit = bits[r + b];
                if (bit) {
                  value += (1 << (7 - b)); // BIG ENDIAN
                }
              }

              if (value != 0) {
                text += (char) value;
              }
            }

            data[k][f] = text;
          }
          else if (type == 3) {
            // Integer
            int value = 0;

            int startPos = offsets[f];
            int endPos = startPos + depth;
            for (int b = startPos; b < endPos; b++) {
              boolean bit = bits[b];
              if (bit) {
                value += (1 << ((endPos - 1) - b)); // BIG ENDIAN
              }
            }

            data[k][f] = value;
          }
        }

      }

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

      for (int k = 0; k < numRows; k++) {
        Object[] currentRow = data[k];

        // 4 - Unknown Hash
        int hash = (Integer) currentRow[2];
        fm.writeInt(hash);

        // 4 - Unknown ID
        int id = (Integer) currentRow[3];
        fm.writeInt(id);

        // 40 - Country Name
        String name = ((String) currentRow[1]);
        if (name.length() > 40) {
          name = name.substring(0, 40);
        }
        int nameRemaining = 40 - name.length();

        fm.writeString(name);
        for (int p = 0; p < nameRemaining; p++) {
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