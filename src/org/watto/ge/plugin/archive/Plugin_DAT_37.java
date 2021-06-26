
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.task.TaskProgressManager;
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
import org.watto.ge.plugin.resource.Resource_DAT_37;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_37 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_37() {

    super("DAT_37", "DAT_37");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Apache Havoc");
    setExtensions("dat");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

  }

  /**
   **********************************************************************************************
   * Gets a blank resource of this type, for use when adding resources
   **********************************************************************************************
   **/
  @Override
  public Resource getBlankResource(File file, String name) {
    return new Resource_DAT_37(file, name);
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
    if (text instanceof Resource_DAT_37) {
      Resource_DAT_37 resource = (Resource_DAT_37) text;

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

      getDirectoryFile(fm.getFile(), "hdr");
      rating += 25;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "hdr");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID
        int fileID = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Length (including null terminator)
        int filenameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (starts with "..\")
        String filename = fm.readString(filenameLength);
        if (filename.length() >= 3 && filename.substring(0, 3).equals("..\\")) {
          filename = filename.substring(3);
        }
        FieldValidator.checkFilename(filename);

        // 1 - null Filename Terminator
        fm.skip(1);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource_DAT_37(path, filename, offset, length, fileID);

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
      if (text instanceof Resource_DAT_37) {
        Resource_DAT_37 resource = (Resource_DAT_37) text;

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
  @SuppressWarnings("unused")
  @Override
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // WRITE THE FILE THAT CONTAINS THE DIRECTORY
      File dirPath = getDirectoryFile(path, "hdr", false);
      FileManipulator fm = new FileManipulator(dirPath, true);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long archiveSize = 16;
      long directorySize = 0;
      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
        directorySize += 8 + resources[i].getNameLength() + 1;
      }
      archiveSize += filesSize + directorySize;

      // Write Header Data

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        String name = resource.getName();
        int fileID = 0;

        if (resource instanceof Resource_DAT_37) {
          fileID = ((Resource_DAT_37) resource).getID();
        }

        // 4 - File ID
        fm.writeInt(fileID);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        // 4 - Filename Length (including null terminator)
        fm.writeInt(name.length() + 1);

        // X - Filename
        // 1 - null Filename Terminator
        fm.writeNullString(name);

        offset += decompLength;
      }

      fm.close();

      // WRITE THE FILE THAT CONTAINS THE DATA
      fm = new FileManipulator(path, true);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();
      //long[] compressedLengths = write(exporter,resources,fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
