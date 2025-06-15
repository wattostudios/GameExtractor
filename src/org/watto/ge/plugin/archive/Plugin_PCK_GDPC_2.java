/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PCK_GDPC_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PCK_GDPC_2() {

    super("PCK_GDPC_2", "PCK_GDPC_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Buckshot Roulette");
    setExtensions("pck"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("gst2", "Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("import", "remap", "shader", "tres", "tscn", "po", "gd", "gdshader"); // LOWER CASE

    //setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("GDPC")) {
        rating += 50;
      }

      fm.skip(92);

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

      // 4 - Header (GDPC)
      // 4 - Unknown (2)
      // 4 - Unknown (4)
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - null
      // 4 - Hash?
      // 68 - null
      fm.skip(96);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length (including padding)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        // 0-3 - null Padding to a multiple of 4 bytes
        String filename = fm.readNullString(filenameLength);

        if (filename.startsWith("res://.")) {
          filename = filename.substring(7);
        }
        else if (filename.startsWith("res://")) {
          filename = filename.substring(6);
        }

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 16 - Hash?
        // 4 - null
        fm.skip(20);

        /*
        String extension = FilenameSplitter.getExtension(filename);
        if (extension.equals("stex")) {
          //offset += 32;
          //length -= 32;
        }
        */

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // 8 - Hash?
      fm.skip(8);

      // Now set the offsets according to this location
      long dataOffset = fm.getOffset();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        resource.setOffset(resource.getOffset() + dataOffset);
      }

      // now analyse individual files

      fm.getBuffer().setBufferSize(512);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String extension = resource.getExtension();

        // This needs more investigation - it returns a raw vorbis packet, no OGG headers or anything... may need to be re-wrapped? 
        /*
        if (extension.equals("oggvorbisstr")) {
          long offset = resource.getOffset();
        
          fm.seek(offset);
        
          // 4 - Header (RSRC)
          // 8 - null
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(24);
        
          // 4 - Description Length (including null)
          int length = fm.readInt();
          FieldValidator.checkFilenameLength(length);
        
          // X - Description
          // 1 - null Desciption Terminator
          fm.skip(length);
        
          // 64 - null
          fm.skip(64);
        
          // 4 - Number of Names
          int numNames = fm.readInt();
          FieldValidator.checkNumFiles(numNames);
        
          // for each name
          for (int n = 0; n < numNames; n++) {
            //   4 - Name Length (including null)
            length = fm.readInt();
            FieldValidator.checkFilenameLength(length);
            //   X - Name
            //   1 - null Name Terminator
            fm.skip(length);
          }
        
          // 4 - null
          fm.skip(4);
        
          // 4 - Number of Local Names
          numNames = fm.readInt();
          FieldValidator.checkNumFiles(numNames);
        
          int streamOffset = 0;
        
          // for each local name
          for (int n = 0; n < numNames; n++) {
            //   4 - Local Name Length (including null)
            length = fm.readInt();
            FieldValidator.checkFilenameLength(length);
            //   X - Local Name
            //   1 - null Local Name Terminator
            String localName = fm.readNullString(length);
        
            // 8 - Data Offset
            if (localName.contains("OggPacketSequence")) {
              streamOffset = (int) (offset + fm.readInt());
              fm.skip(4);
            }
            else {
              fm.skip(8);
            }
        
          }
        
          if (streamOffset == 0) {
            continue; // don't worry about it, go to the next file
          }
        
          int skipLength = (int) (streamOffset - fm.getOffset());
          FieldValidator.checkLength(skipLength);
          fm.skip(skipLength);
        
          // 4 - Description Length (including null)
          length = fm.readInt();
          FieldValidator.checkFilenameLength(length);
        
          // X - Description
          // 1 - null Description Terminator
          fm.skip(length);
        
          // 4 - Unknown (4)
          // 4 - Unknown (2)
          // 4 - Unknown (30)
          // 4 - Unknown
          // 4 - Unknown (30)
          // 4 - Unknown (1)
          // 4 - Unknown (31)
          // 4 - Unknown (30)
          fm.skip(32);
        
          // X - File Data (Vorbis)
          long headerLength = fm.getOffset() - offset;
          length = (int) (resource.getLength() - headerLength);
        
          offset = fm.getOffset();
        
          resource.setOffset(offset);
          resource.setLength(length);
        
        }
        */
        if (extension.equals("ctex")) {
          long offset = resource.getOffset();

          fm.seek(offset);
          String mainHeader = fm.readString(4);

          fm.skip(60); // 4 already read from the above

          if (fm.readString(4).equals("WEBP")) {
            offset += 56;
            int length = (int) resource.getLength() - 56;

            resource.setOffset(offset);
            resource.setLength(length);
            resource.setDecompressedLength(length);

            String newName = resource.getName() + ".webp";
            resource.setName(newName);
            resource.setOriginalName(newName);

            resource.forceNotAdded(true);
          }
          else if (mainHeader.equals("GST2")) {
            String newName = resource.getName() + ".gst2";
            resource.setName(newName);
            resource.setOriginalName(newName);

            resource.forceNotAdded(true);
          }
        }
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
