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
import org.watto.ge.plugin.archive.Plugin_DAT_DAT;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_DAT_DAT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_DAT_DAT() {
    super("DAT_DAT_DAT", "ESPN NHL Hockey NAMES.DAT Table");
    setExtensions("dat");

    setGames("ESPN NHL Hockey");
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
      if (plugin instanceof Plugin_DAT_DAT) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (fm.getFile().getName().equalsIgnoreCase("names.dat")) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 2 - Number of Names
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      // 2 - Number of Names
      short numKeys = fm.readShort();
      FieldValidator.checkNumFiles(numKeys);

      int[] offsets = new int[numKeys];
      for (int k = 0; k < numKeys; k++) {
        // 2 - Name Offset
        int offset = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkOffset(offset, arcSize);
        offsets[k] = offset;
      }

      Object[][] data = new Object[numKeys][2];
      for (int k = 0; k < numKeys; k++) {
        fm.relativeSeek(offsets[k]);
        // X - Name
        // 1 - null Name Terminator
        String name = fm.readNullString();

        data[k] = new Object[] { name, name };
      }

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

      // 2 - Number of Names
      fm.writeShort(numRows);

      // Offsets
      int offset = 2 + (numRows * 2);
      for (int k = 0; k < numRows; k++) {
        Object[] currentRow = data[k];

        // 2 - Name Offset
        fm.writeShort(offset);

        String name = (String) currentRow[1];
        offset += name.length() + 1;
      }

      // Texts
      for (int k = 0; k < numRows; k++) {
        Object[] currentRow = data[k];

        // X - Name
        // 1 - null Name Terminator

        String text = (String) currentRow[1];

        fm.writeString(text);
        fm.writeByte(0);

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