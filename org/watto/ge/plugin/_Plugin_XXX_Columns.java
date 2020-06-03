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

package org.watto.ge.plugin;

import java.io.File;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.resource.Resource_Property;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class _Plugin_XXX_Columns extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public _Plugin_XXX_Columns() {

    super("XXX", "XXX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("");
    setExtensions(""); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

  }

  /**
   **********************************************************************************************
   * Gets a blank resource of this type, for use when adding resources
   **********************************************************************************************
   **/
  @Override
  public Resource getBlankResource(File file, String name) {
    return new _Resource_XXX(file, name);
  }

  /**
   **********************************************************************************************
   * Gets all the columns
   **********************************************************************************************
   **/
  @Override
  public WSTableColumn[] getColumns() {

    // used codes: a,c,C,d,D,E,F,i,I,N,O,P,r,R,S,z,Z
    //                                          code,languageCode,class,editable,sortable
    WSTableColumn idColumn = new WSTableColumn("ID", 'I', Integer.class, true, true);
    WSTableColumn crcColumn = new WSTableColumn("CRC", 'X', String.class, false, true);
    WSTableColumn timeColumn = new WSTableColumn("Time", 'T', String.class, false, true);

    return getDefaultColumnsWithAppended(idColumn, crcColumn, timeColumn);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Object getColumnValue(Resource text, char code) {
    if (text instanceof _Resource_XXX) {
      _Resource_XXX resource = (_Resource_XXX) text;

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
      if (fm.readString(4).equals("")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (BIGF)
      // 4 - Archive Size
      fm.skip(8);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 8 - Language Name
      // 4 - Language Version
      String langName = fm.readString(8);
      int langVersion = fm.readInt();

      Resource_Property[] properties = new Resource_Property[2];
      properties[0] = new Resource_Property("Language Name", langName);
      properties[1] = new Resource_Property("Version", "" + langVersion);
      setProperties(properties);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      int realNumFiles = numFiles;
      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

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
      if (text instanceof _Resource_XXX) {
        _Resource_XXX resource = (_Resource_XXX) text;

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
   * Sets up the default properties
   **********************************************************************************************
   **/
  @Override
  public void setDefaultProperties(boolean force) {
    if (force || properties.length == 0) {
      properties = new Resource_Property[2];
      properties[0] = new Resource_Property("Language Name");
      properties[1] = new Resource_Property("Version");
    }
  }

}
