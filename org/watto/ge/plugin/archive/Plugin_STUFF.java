
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.task.TaskProgressManager;
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
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_STUFF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_STUFF() {

    super("STUFF", "STUFF");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("stuff");
    setGames("Eve Online");
    setPlatforms("PC");

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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Length (not including null)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 1 - null Filename Terminator
        fm.skip(1);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, 0, length, length);

        TaskProgressManager.setValue(i);
      }

      // Go through and set the offsets.
      // Also checks for compression.
      long offset = fm.getOffset();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        resource.setOffset(offset);

        long length = resource.getLength();
        if (length <= 0) {
          continue;
        }

        // check compression
        if (fm.readString(1).equals("x")) {
          resource.setExporter(exporter);
        }

        fm.skip(length - 1);

        offset += length;
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        String name = resource.getName();

        // 4 - File Length
        offsets[i] = fm.getOffset();
        fm.writeInt((int) decompLength);

        // 4 - Filename Length (not including null)
        fm.writeInt(name.length());

        // X - Filename
        // 1 - null Filename Terminator
        fm.writeNullString(resource.getName());
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      //write(resources, fm);

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      long[] compressedLengths = write(exporter, resources, fm);

      // Archive Footer

      // 4 - null
      fm.writeInt(0);

      // 12 - Format Description ("EmbedFs 1.0" + null)
      fm.writeString("EmbedFs 1.0");
      fm.writeByte(0);

      // now go back and write the compressed file sizes in the directory
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // 4 - File Length
        fm.writeInt((int) compressedLengths[i]);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}