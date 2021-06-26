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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.task.TaskProgressManager;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_000_FFIJ;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_000_FFIJ extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_000_FFIJ() {

    super("000_FFIJ", "000_FFIJ");

    //         read write replace rename
    setProperties(true, true, true, false);

    setGames("Afterlife");
    setExtensions("000");
    setPlatforms("PC");

  }

  /**
   **********************************************************************************************
   * Gets a blank resource of this type, for use when adding resources
   **********************************************************************************************
   **/
  @Override
  public Resource getBlankResource(File file, String name) {
    return new Resource_000_FFIJ(file, name);
  }

  /**
   **********************************************************************************************
   * Gets all the columns
   **********************************************************************************************
   **/
  @Override
  public WSTableColumn[] getColumns() {
    WSTableColumn[] baseColumns = super.getColumns();
    int numColumns = baseColumns.length;

    // copy the base columns into a new array
    WSTableColumn[] columns = new WSTableColumn[numColumns + 1];
    System.arraycopy(baseColumns, 0, columns, 0, numColumns);

    // add the additional columns...

    // used codes: a,c,C,d,D,E,F,i,I,N,O,P,r,R,S,z,Z
    //code,languageCode,class,editable,sortable
    columns[numColumns] = new WSTableColumn("ID", 'I', Integer.class, true, true);

    return columns;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Object getColumnValue(Resource text, char code) {
    if (text instanceof Resource_000_FFIJ) {
      Resource_000_FFIJ resource = (Resource_000_FFIJ) text;

      if (code == 'I') {
        return new Integer(resource.getID());
      }
    }

    return super.getColumnValue(text, code);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // Header
      if (fm.readString(4).equals("FFIJ")) {
        rating += 50;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Header 2
      if (fm.readString(4).equals("DAEH")) {
        rating += 5;
      }

      // Header Size (4)
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // Number Of Files
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Archive Header (FFIJ)
      // 4 - Archive Header Size (null)
      // 4 - Head Header (DAEH)
      // 4 - Header Section Size (4)
      fm.skip(16);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Offsets Header (TSFO)
      // 4 - Offsets Section Size
      fm.skip(8);

      int relOffset = (numFiles * 8) + 36;

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // 4 - ID Header (DICR)
      // 4 - ID Section Size
      fm.skip(8);

      int[] fileIDs = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID
        fileIDs[i] = fm.readInt();
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        int fileID = fileIDs[i];

        fm.seek(offset + 4);

        // 4 - File Length
        long length = fm.readInt() - 8;
        FieldValidator.checkLength(length, arcSize);

        offset += 8;

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_000_FFIJ(path, filename, offset, length, fileID);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

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
  public void setColumnValue(Resource text, char code, Object value) {
    try {
      if (text instanceof Resource_000_FFIJ) {
        Resource_000_FFIJ resource = (Resource_000_FFIJ) text;

        if (code == 'I') {
          resource.setID(((Integer) value).intValue());
          return;
        }
      }
    }
    catch (Throwable t) {
    }

    super.setColumnValue(text, code, value);
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // 4 - Archive Header (FFIJ)
      // 4 - Archive Header Size (null)
      fm.writeString("FFIJ");
      fm.writeInt(0);

      // 4 - Head Header (DAEH)
      // 4 - Header Section Size (4)
      // 4 - Number Of Files
      fm.writeString("DAEH");
      fm.writeInt(4);
      fm.writeInt(numFiles);

      // 4 - Offsets Header (TSFO)
      // 4 - Offsets Section Size
      fm.writeString("TSFO");
      fm.writeInt(numFiles * 4);

      // Write Directory
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (relative to the start of the file data)
        fm.writeInt((int) offset);

        offset += resources[i].getDecompressedLength() + 8; // +8 for the file header
      }

      // 4 - ID Header (DICR)
      // 4 - ID Section Size
      fm.writeString("DICR");
      fm.writeInt(numFiles * 4);

      // Write Directory
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // 4 - File ID
        if (resource instanceof Resource_000_FFIJ) {
          fm.writeInt(((Resource_000_FFIJ) resource).getID());
        }
        else {
          fm.writeInt(0);
        }
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // Write Files
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - Resource Header (CSER)
        fm.writeString("CSER");

        // 4 - File Length (including these 2 fields)
        fm.writeInt((int) decompLength + 8);

        // X - File Data
        write(resources[i], fm);

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
