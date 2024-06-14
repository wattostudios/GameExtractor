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
import org.watto.component.WSPluginException;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DBF_STATICDATABASE;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DBF_STATICDATABASE_TABLE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DBF_STATICDATABASE_TABLE() {
    super("DBF_STATICDATABASE_TABLE", "NHL 2000 Table");
    setExtensions("table");

    setGames("NHL 2000");
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
      if (plugin instanceof Plugin_DBF_STATICDATABASE) {
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

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  String openedFile = null;

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      openedFile = null;

      long arcSize = fm.getLength();

      String filename = fm.getFile().getName().toLowerCase();
      if (filename.endsWith("04.table")) { // NHL2K000.DBF
        try {
          int lineSize = 48;

          int numKeys = (int) (arcSize / lineSize);
          FieldValidator.checkNumFiles(numKeys);

          if (arcSize % lineSize != 0) { // not the right file
            //return null;
            throw new WSPluginException("Not the right format");
          }

          openedFile = "04_48";

          WSTableColumn[] columns = new WSTableColumn[4];
          columns[0] = new WSTableColumn("Original Country", 'o', String.class, false, true, 0, 0); // hidden column 0,0
          columns[1] = new WSTableColumn("Country", 'e', String.class, true, true);
          columns[2] = new WSTableColumn("Hash", 'h', Integer.class, false, true, 0, 0); // hidden column 0,0
          columns[3] = new WSTableColumn("ID", 'i', Integer.class, true, true);

          Object[][] data = new Object[numKeys][4];
          for (int k = 0; k < numKeys; k++) {
            // 4 - Unknown Hash
            int hash = fm.readInt();

            // 4 - Unknown ID
            int id = fm.readInt();

            // 40 - Country Name
            String name = fm.readNullString(40);
            FieldValidator.checkFilenameLength(name);

            data[k] = new Object[] { name, name, hash, id };
          }

          PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);
          return preview;
        }
        catch (Throwable t) {
          // leave it, try another one further on
        }
      }

      if (filename.endsWith("01.table")) { // NHL2K000.DBF
        try {
          int lineSize = 120;

          int numKeys = (int) (arcSize / lineSize);
          FieldValidator.checkNumFiles(numKeys);

          if (arcSize % lineSize != 0) { // not the right file
            //return null;
            throw new WSPluginException("Not the right format");
          }

          openedFile = "01_120";

          WSTableColumn[] columns = new WSTableColumn[12];
          columns[0] = new WSTableColumn("Original Team", 'a', String.class, false, true, 0, 0); // hidden column 0,0
          columns[1] = new WSTableColumn("Team", 'b', String.class, true, true);
          columns[2] = new WSTableColumn("Original Code", 'c', String.class, false, true, 0, 0); // hidden column 0,0
          columns[3] = new WSTableColumn("Code", 'd', String.class, true, true);
          columns[4] = new WSTableColumn("Original City", 'e', String.class, false, true, 0, 0); // hidden column 0,0
          columns[5] = new WSTableColumn("City", 'f', String.class, true, true);
          columns[6] = new WSTableColumn("Original Venue", 'g', String.class, false, true, 0, 0); // hidden column 0,0
          columns[7] = new WSTableColumn("Venue", 'h', String.class, true, true);
          columns[8] = new WSTableColumn("Original Country", 'i', String.class, false, true, 0, 0); // hidden column 0,0
          columns[9] = new WSTableColumn("Country", 'j', String.class, true, true);
          columns[10] = new WSTableColumn("Hash", 'k', Integer.class, false, true, 0, 0); // hidden column 0,0
          columns[11] = new WSTableColumn("ID", 'l', Integer.class, true, true);

          Object[][] data = new Object[numKeys][12];
          for (int k = 0; k < numKeys; k++) {

            // 4 - Unknown ID
            int id = fm.readInt();

            // 4 - Unknown Hash
            int hash = fm.readInt();

            // 28 - Team Name
            String team = fm.readNullString(28);
            FieldValidator.checkFilenameLength(team);

            // 8 - Team Code
            String code = fm.readNullString(8);
            FieldValidator.checkFilenameLength(code);

            // 22 - City
            String city = fm.readNullString(22);
            FieldValidator.checkFilenameLength(city);

            // 34 - Home Venue
            String venue = fm.readNullString(34);
            FieldValidator.checkFilenameLength(venue);

            // 20 - Country
            String country = fm.readNullString(20);
            FieldValidator.checkFilenameLength(country);

            data[k] = new Object[] { team, team, code, code, city, city, venue, venue, country, country, hash, id };
          }

          PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);
          return preview;
        }
        catch (Throwable t) {
          // leave it, try another one further on
        }
      }

      if (filename.endsWith("02.table")) { // NHL2K000.DBF
        try {
          int lineSize = 44;

          int numKeys = (int) (arcSize / lineSize);
          FieldValidator.checkNumFiles(numKeys);

          if (arcSize % lineSize != 0) { // not the right file
            //return null;
            throw new WSPluginException("Not the right format");
          }

          openedFile = "02_44";

          WSTableColumn[] columns = new WSTableColumn[7];
          columns[0] = new WSTableColumn("Original First", 'o', String.class, false, true, 0, 0); // hidden column 0,0
          columns[1] = new WSTableColumn("First", 'e', String.class, true, true);
          columns[2] = new WSTableColumn("Original Last", 'l', String.class, false, true, 0, 0); // hidden column 0,0
          columns[3] = new WSTableColumn("Last", 'k', String.class, true, true);
          columns[4] = new WSTableColumn("Hash 1", 'h', Integer.class, false, true, 0, 0); // hidden column 0,0
          columns[5] = new WSTableColumn("Hash 2", 'j', Integer.class, false, true, 0, 0); // hidden column 0,0
          columns[6] = new WSTableColumn("ID", 'i', Integer.class, true, true);

          Object[][] data = new Object[numKeys][7];
          for (int k = 0; k < numKeys; k++) {
            // 4 - Unknown Hash 1
            int hash1 = fm.readInt();

            // 4 - Unknown Hash 2
            int hash2 = fm.readInt();

            // 4 - Unknown ID
            int id = fm.readInt();

            // 16 - First Name
            String firstName = fm.readNullString(16);
            FieldValidator.checkFilenameLength(firstName);

            // 16 - Last Name
            String lastName = fm.readNullString(16);
            FieldValidator.checkFilenameLength(lastName);

            data[k] = new Object[] { firstName, firstName, lastName, lastName, hash1, hash2, id };
          }

          PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);
          return preview;
        }
        catch (Throwable t) {
          // leave it, try another one further on
        }
      }

      if (filename.endsWith("01.table")) { // NHL2K001.DBF
        try {
          int lineSize = 44;

          int numKeys = (int) (arcSize / lineSize);
          FieldValidator.checkNumFiles(numKeys);

          if (arcSize % lineSize != 0) { // not the right file
            //return null;
            throw new WSPluginException("Not the right format");
          }

          openedFile = "02_44"; // same format, so leave this even though it should actually be "01_44"

          WSTableColumn[] columns = new WSTableColumn[7];
          columns[0] = new WSTableColumn("Original First", 'o', String.class, false, true, 0, 0); // hidden column 0,0
          columns[1] = new WSTableColumn("First", 'e', String.class, true, true);
          columns[2] = new WSTableColumn("Original Last", 'l', String.class, false, true, 0, 0); // hidden column 0,0
          columns[3] = new WSTableColumn("Last", 'k', String.class, true, true);
          columns[4] = new WSTableColumn("Hash 1", 'h', Integer.class, false, true, 0, 0); // hidden column 0,0
          columns[5] = new WSTableColumn("Hash 2", 'j', Integer.class, false, true, 0, 0); // hidden column 0,0
          columns[6] = new WSTableColumn("ID", 'i', Integer.class, true, true);

          Object[][] data = new Object[numKeys][7];
          for (int k = 0; k < numKeys; k++) {
            // 4 - Unknown Hash 1
            int hash1 = fm.readInt();

            // 4 - Unknown Hash 2
            int hash2 = fm.readInt();

            // 4 - Unknown ID
            int id = fm.readInt();

            // 16 - First Name
            String firstName = fm.readNullString(16);

            // 16 - Last Name
            String lastName = fm.readNullString(16);

            if (hash1 == 0 && hash2 == 0 && id == 0) {
              // valid empty line
            }
            else {
              FieldValidator.checkFilenameLength(firstName);
              FieldValidator.checkFilenameLength(lastName);
            }

            data[k] = new Object[] { firstName, firstName, lastName, lastName, hash1, hash2, id };
          }

          PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);
          return preview;
        }
        catch (Throwable t) {
          // leave it, try another one further on
        }
      }

      if (filename.endsWith("03.table")) { // NHL2K001.DBF
        try {
          int lineSize = 48;

          int numKeys = (int) (arcSize / lineSize);
          FieldValidator.checkNumFiles(numKeys);

          if (arcSize % lineSize != 0) { // not the right file
            //return null;
            throw new WSPluginException("Not the right format");
          }

          openedFile = "04_48"; // same format, so leave this even though it should actually be "03_48"

          WSTableColumn[] columns = new WSTableColumn[4];
          columns[0] = new WSTableColumn("Original Country", 'o', String.class, false, true, 0, 0); // hidden column 0,0
          columns[1] = new WSTableColumn("Country", 'e', String.class, true, true);
          columns[2] = new WSTableColumn("Hash", 'h', Integer.class, false, true, 0, 0); // hidden column 0,0
          columns[3] = new WSTableColumn("ID", 'i', Integer.class, true, true);

          Object[][] data = new Object[numKeys][4];
          for (int k = 0; k < numKeys; k++) {
            // 4 - Unknown Hash
            int hash = fm.readInt();

            // 4 - Unknown ID
            int id = fm.readInt();

            // 40 - Country Name
            String name = fm.readNullString(40);

            if (hash == 0 && id == 0) {
              // valid empty line
            }
            else {
              FieldValidator.checkFilenameLength(name);
            }

            data[k] = new Object[] { name, name, hash, id };
          }

          PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);
          return preview;
        }
        catch (Throwable t) {
          // leave it, try another one further on
        }
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

      if (openedFile.equals("04_48")) {

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
      }
      else if (openedFile.equals("01_120")) {

        for (int k = 0; k < numRows; k++) {
          Object[] currentRow = data[k];

          // 4 - Unknown ID
          int id = (Integer) currentRow[11];
          fm.writeInt(id);

          // 4 - Unknown Hash
          int hash = (Integer) currentRow[10];
          fm.writeInt(hash);

          // 28 - Team Name
          String name = ((String) currentRow[1]);
          if (name.length() > 28) {
            name = name.substring(0, 28);
          }
          int nameRemaining = 28 - name.length();

          fm.writeString(name);
          for (int p = 0; p < nameRemaining; p++) {
            fm.writeByte(0);
          }

          // 8 - Team Code
          name = ((String) currentRow[3]);
          if (name.length() > 8) {
            name = name.substring(0, 8);
          }
          nameRemaining = 8 - name.length();

          fm.writeString(name);
          for (int p = 0; p < nameRemaining; p++) {
            fm.writeByte(0);
          }

          // 22 - City
          name = ((String) currentRow[5]);
          if (name.length() > 22) {
            name = name.substring(0, 22);
          }
          nameRemaining = 22 - name.length();

          fm.writeString(name);
          for (int p = 0; p < nameRemaining; p++) {
            fm.writeByte(0);
          }

          // 34 - Home Venue
          name = ((String) currentRow[7]);
          if (name.length() > 34) {
            name = name.substring(0, 34);
          }
          nameRemaining = 34 - name.length();

          fm.writeString(name);
          for (int p = 0; p < nameRemaining; p++) {
            fm.writeByte(0);
          }

          // 20 - Country
          name = ((String) currentRow[9]);
          if (name.length() > 20) {
            name = name.substring(0, 20);
          }
          nameRemaining = 20 - name.length();

          fm.writeString(name);
          for (int p = 0; p < nameRemaining; p++) {
            fm.writeByte(0);
          }

        }
      }
      else if (openedFile.equals("02_44")) {

        for (int k = 0; k < numRows; k++) {
          Object[] currentRow = data[k];

          // 4 - Unknown Hash 1
          int hash = (Integer) currentRow[4];
          fm.writeInt(hash);

          // 4 - Unknown Hash 2
          hash = (Integer) currentRow[5];
          fm.writeInt(hash);

          // 4 - Unknown ID
          int id = (Integer) currentRow[6];
          fm.writeInt(id);

          // 16 - First Name
          String name = ((String) currentRow[1]);
          if (name.length() > 16) {
            name = name.substring(0, 16);
          }
          int nameRemaining = 16 - name.length();

          fm.writeString(name);
          for (int p = 0; p < nameRemaining; p++) {
            fm.writeByte(0);
          }

          // 16 - Last Name
          name = ((String) currentRow[3]);
          if (name.length() > 16) {
            name = name.substring(0, 16);
          }
          nameRemaining = 16 - name.length();

          fm.writeString(name);
          for (int p = 0; p < nameRemaining; p++) {
            fm.writeByte(0);
          }
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