
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.resource.Resource_GTR_GMOTORMAS;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GTR_GMOTORMAS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GTR_GMOTORMAS() {

    super("GTR_GMOTORMAS", "GTR_GMOTORMAS");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("BMW M3 Challenge",
        "GTR: FIA GT Racing",
        "GT Legends",
        "GTR2",
        "Race");
    setExtensions("gtr", "gtl", "bmw");
    setPlatforms("PC");

  }

  /**
   **********************************************************************************************
   * Gets a blank resource of this type, for use when adding resources
   **********************************************************************************************
   **/
  @Override
  public Resource getBlankResource(File file, String name) {
    return new Resource_GTR_GMOTORMAS(file, name);
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
    columns[numColumns] = new WSTableColumn("Type ID", 'I', Integer.class, true, true);

    return columns;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object getColumnValue(Resource text, char code) {
    if (text instanceof Resource_GTR_GMOTORMAS) {
      Resource_GTR_GMOTORMAS resource = (Resource_GTR_GMOTORMAS) text;

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
      if (fm.readString(11).equals("GMOTORMAS10")) {
        rating += 50;
      }

      // 5 - null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      fm.skip(1);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Length Of File Data
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 11 - Header (GMOTORMAS10)
      // 5 - null
      fm.skip(16);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Length of File Data
      fm.skip(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int relOffset = 24 + (numFiles * 256);
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Type ID (18=BMP, 20=TGA)
        int typeID = fm.readInt();

        // 4 - File Offset (relative to the end of the directory)
        long offset = relOffset + fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 240 - Filename (null)
        String filename = fm.readNullString(240);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_GTR_GMOTORMAS(path, filename, offset, length, decompLength, exporter, typeID);

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
      if (text instanceof Resource_GTR_GMOTORMAS) {
        Resource_GTR_GMOTORMAS resource = (Resource_GTR_GMOTORMAS) text;

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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 12 - Header (GMOTORMAS10 + null)
      fm.writeString("GMOTORMAS10" + (byte) 0);

      // 4 - null
      fm.writeInt(0);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 4 - File Data Length
      fm.writeInt((int) filesSize);

      // WE WANT TO WRITE THE FILES FIRST, SO WE GET THEIR COMPRESSED SIZES
      int relOffset = 24 + (numFiles * 256);
      fm.setLength(relOffset);
      fm.seek(relOffset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      long[] compLengths = write(exporter, resources, fm);

      fm.seek(24);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        int typeID = 0;
        if (resource instanceof Resource_GTR_GMOTORMAS) {
          typeID = ((Resource_GTR_GMOTORMAS) resource).getID();
        }

        // 4 - File Type ID (17=*.gmt, 18=Image (*.bmp), 23/55/567=Image (*.dds), 31=Audio (*.psh/*.gfx/*.vsh))
        fm.writeInt(typeID);

        // 4 - File Offset (relative to the start of the file data)
        fm.writeInt((int) offset);

        // 4 - Decompressed File Length
        fm.writeInt((int) decompLength);

        // 4 - Compressed File Length
        fm.writeInt((int) compLengths[i]);

        // 240 - Filename (null)
        fm.writeNullString(resource.getName(), 240);

        offset += decompLength;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}