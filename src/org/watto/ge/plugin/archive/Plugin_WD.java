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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WD() {

    super("WD", "WD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Frontline Attack: War Over Europe",
        "Earth 2150: The Moon Project",
        "Earth 2160",
        "Once Upon a Knight",
        "World War 3: Black Gold");
    setExtensions("wd");
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

      // ZLIB Header
      if (fm.readString(1).equals("x")) {
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("int") || extension.equalsIgnoreCase("ild") || extension.equalsIgnoreCase("dat") || extension.equalsIgnoreCase("aod") || extension.equalsIgnoreCase("sha") || extension.equalsIgnoreCase("con") || extension.equalsIgnoreCase("atd")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      fm.seek(arcSize - 4);

      // 4 - Directory Length [-4]
      int dirLength = fm.readInt();
      long dirOffset = arcSize - dirLength;
      dirLength -= 4;

      /*
      fm.close();
      
      // Decompress the directory
      File tempfile = new File("temp" + File.separator + "wd_directory_decompressed.dat");
      if (tempfile.exists()) {
        tempfile.delete();
      }
      FileManipulator extDir = new FileManipulator(tempfile, true);
      String dirTempName = extDir.getFilePath();
      Resource directory = new Resource(path, dirTempName, dirOffset, dirLength, dirLength * 20);
      
      exporter.extract(directory, extDir);
      
      extDir.close();
      
      // Now open the directory and read it
      fm = new FileManipulator(new File(dirTempName), false);
      */

      // X - Compressed Directory Data (ZLib)
      int dirDecompLength = dirLength * 20; // not sure how big the decompressed data actually is
      byte[] dirBytes = new byte[dirDecompLength];
      int decompWritePos = 0;
      fm.seek(dirOffset);
      exporter.open(fm, dirLength, dirDecompLength);

      for (int b = 0; b < dirDecompLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) exporter.read();
        }
        else {
          break;
        }
      }

      // resize to the actual decomp size
      byte[] oldBytes = dirBytes;
      dirBytes = new byte[decompWritePos];
      System.arraycopy(oldBytes, 0, dirBytes, 0, decompWritePos);

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      // 8 - Unknown
      fm.skip(8);

      // 2 - Number Of Files?
      fm.skip(2);
      int numFiles = Archive.getMaxFiles(4);
      //short numFiles = fm.readShort();
      //FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      String dirName = "";
      while (realNumFiles < numFiles && fm.getRemainingLength() > 0) {
        //for (int i=0;i<numFiles;i++){
        // 1 - Filename Length (NOT including the null terminator)
        int filenameLength = ByteConverter.unsign(fm.readByte());

        String filename = "";
        if (filenameLength == 73) {
          // Might be an ID field from the file before
          byte[] filenameBytes = fm.readBytes(3);
          if (filenameBytes[0] == 68 && filenameBytes[1] == 0) {
            // yep, an ID field, so skip it and read the filename again

            // 1 - Filename Length (NOT including the null terminator)
            filenameLength = ByteConverter.unsign(fm.readByte());
          }
          else {
            // not an ID field, it's part of the filename
            filename = StringConverter.convertLittle(filenameBytes);
            filenameLength -= 3;
          }
        }

        // X - Filename
        filename += fm.readString(filenameLength);

        //System.out.println(fm.getOffset() + "\t" + filename);

        // 1 - null Filename Terminator
        int filenameTerminator = ByteConverter.unsign(fm.readByte());
        if (filenameTerminator > filenameLength) {
          String extension = FilenameSplitter.getExtension(filename);
          if (extension.equalsIgnoreCase("msh") || extension.equalsIgnoreCase("lnd") || extension.equalsIgnoreCase("mis") || extension.equalsIgnoreCase("par") || extension.equalsIgnoreCase("int") || extension.equalsIgnoreCase("eco")) {
            // it's ok
          }
          else {
            // we actually just read the group, now we need to read the real filename

            // X - Filename
            filename = fm.readString(filenameTerminator);

            //System.out.println("OVERWRITTEN\t" + filename);

            // 1 - null Filename Terminator
            filenameTerminator = ByteConverter.unsign(fm.readByte());
          }
        }
        else if (filenameTerminator == 1) {
          // check if it's a 4-byte "1" field)
          long offset = fm.getOffset();
          if (fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 0) {
            // yep, now go back and start reading a filename again
            continue;
          }
          else {
            // nope, actually reading part of the file offset, so go back and read it again properly down below
            fm.seek(offset);
          }
        }

        // 4 - File Offset
        long offset = fm.readInt();
        if (offset <= 0 || offset > arcSize) {
          //directory

          // 3 - Unknown Padding (all 234's)
          // 4 - Unknown (P!P!)
          // 4 - Unknown (256)
          // 4 - null
          fm.skip(15);

          dirName = filename + "\\";

        }
        else {
          filename = dirName + filename;

          //System.out.println(realNumFiles + " Of " + numFiles);

          // 4 - Compressed File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();

          String extension = FilenameSplitter.getExtension(filename);
          if (extension.equalsIgnoreCase("msh")) {
            // 4 - (optional) "1" field
            int optionalField = fm.readInt();
            if (optionalField >= 1 && optionalField <= 10) {
              fm.skip(4);
            }
            // 16 - Other Mesh-related data
            fm.skip(12);
          }
          else if (extension.equalsIgnoreCase("lnd") || extension.equalsIgnoreCase("mis") || extension.equalsIgnoreCase("par")) {

            // 1 - Group Name Length
            int groupNameLength = ByteConverter.unsign(fm.readByte());

            // X - Group Name
            fm.skip(groupNameLength);

            // 4 - ID Field
            // 16 - Unknown
            fm.skip(20);
          }

          if (decompLength == length) {
            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }
          else {
            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          }

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;

        }
      }

      if (realNumFiles != numFiles) {
        resources = resizeResources(resources, realNumFiles);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
