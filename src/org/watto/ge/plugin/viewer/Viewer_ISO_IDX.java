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
import org.watto.ge.plugin.archive.Plugin_ISO;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ISO_IDX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ISO_IDX() {
    super("ISO_IDX", "Harvester IDX Table");
    setExtensions("idx");

    setGames("Harvester");
    setPlatforms("PC");
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
      if (plugin instanceof Plugin_ISO) {
        rating += 5;
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

      int numKeys = Archive.getMaxFiles();
      int realNumKeys = 0;

      String[] texts = new String[numKeys];

      byte xorKey = (byte) 170;

      String text = "";

      while (arcSize > 0) {

        byte currentChar = fm.readByte();
        arcSize--;
        if (currentChar == 13) {
          currentChar = fm.readByte();
          arcSize--;
          if (currentChar == 10) {
            // found the new line
            texts[realNumKeys] = text;
            realNumKeys++;

            text = "";
            continue;
          }

          // premature - not a new line
          text += (char) (13 ^ xorKey);

        }

        text += (char) (currentChar ^ xorKey);

        if (arcSize <= 0) {
          if (text.length() > 0) {
            texts[realNumKeys] = text;
            realNumKeys++;

            text = "";
            continue;
          }
        }

      }

      Object[][] data = new Object[realNumKeys / 2][3];

      int currentPos = 0;
      for (int i = 0; i < realNumKeys; i += 2) {
        int key = Integer.parseInt(texts[i]);
        String value = texts[i + 1];
        data[currentPos] = new Object[] { key, value, value };
        currentPos++;
      }

      WSTableColumn[] columns = new WSTableColumn[3];
      columns[0] = new WSTableColumn("Key", 'n', Integer.class, false, true, 0, 0); // hidden column 0,0
      columns[1] = new WSTableColumn("Original", 'o', String.class, false, true, 0, 0); // hidden column 0,0
      columns[2] = new WSTableColumn("Edited", 'e', String.class, true, true);

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

      byte xorKey = (byte) 170;

      for (int i = 0; i < numRows; i++) {
        // write the key
        String key = "" + (Integer) data[i][0];
        byte[] keyBytes = key.getBytes();
        int numBytes = keyBytes.length;

        for (int t = 0; t < numBytes; t++) {
          fm.writeByte(keyBytes[t] ^ xorKey);
        }

        fm.writeByte(13);
        fm.writeByte(10);

        // write the text
        String editedText = (String) data[i][2];
        byte[] textBytes = editedText.getBytes();
        numBytes = textBytes.length;

        for (int t = 0; t < numBytes; t++) {
          fm.writeByte(textBytes[t] ^ xorKey);
        }

        fm.writeByte(13);
        fm.writeByte(10);
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