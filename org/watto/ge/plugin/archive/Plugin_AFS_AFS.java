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
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AFS_AFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AFS_AFS() {

    super("AFS_AFS", "ADX Audio Package");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("50 Cent: Bulletproof",
        "Crazy Taxi",
        "DragonBall Z: Budokai 3",
        "Leisure Suit Larry: Manga Cum Laude",
        "Mortal Kombat: Shaolin Monks",
        "Phantasy Star Online",
        "Pro Evolution Soccer 5",
        "Resident Evil 4",
        "Silent Hill 3",
        "Pro Evolution Soccer 2008",
        "Pro Evolution Soccer 2009",
        "Sonic Adventure 2");

    setPlatforms("PC", "PS2");

    setExtensions("afs");

    setFileTypes("adx", "ADX Audio",
        "ahx", "AHX Audio");

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

      String ext = FilenameSplitter.getExtension(fm.getFile()).toLowerCase();
      if (ext.equalsIgnoreCase("afs_archive")) {
        // an internal archive type - doesn't have a header
        rating += 25;
      }
      else {
        // Header
        if (fm.readString(4).equals("AFS" + (char) 0)) {
          rating += 50;
        }
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      FileManipulator fm = new FileManipulator(path, false);

      String ext = FilenameSplitter.getExtension(fm.getFile()).toLowerCase();
      if (ext.equalsIgnoreCase("afs_archive")) {
        //
        // An internal archive type
        //

        // 4 - Number Of Files
        int numFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles);

        // 4 - Unknown (8)
        fm.skip(4);

        Resource[] resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        long arcSize = fm.getLength();

        for (int i = 0; i < numFiles; i++) {
          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown (-16)
          fm.skip(4);

          String filename = Resource.generateFilename(i);

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }

        fm.close();

        return resources;
      }
      else {
        //
        // A normal "AFS"+null archive
        //

        // 4 - Header (AFS + null)
        fm.skip(4);

        // 4 - Number Of Files
        int numFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles);

        long arcSize = fm.getLength();

        Resource[] resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles * 2);

        // Loop through directory
        int realNumFiles = 0;
        for (int i = 0; i < numFiles; i++) {
          // 4 - Data Offset
          long offsetPointerLocation = fm.getOffset();
          long offsetPointerLength = 4;

          long offset = fm.readInt();
          if (offset == arcSize) {
            // the last file is the archive size
            numFiles--;
            resources = resizeResources(resources, numFiles);
          }
          else {
            FieldValidator.checkOffset(offset, arcSize);

            // 4 - File Size
            long lengthPointerLocation = fm.getOffset();
            long lengthPointerLength = 4;

            long length = fm.readInt();
            FieldValidator.checkLength(length, arcSize + 1);

            String filename = Resource.generateFilename(realNumFiles);

            if (length != 0 && length != arcSize) {
              //path,id,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);
              realNumFiles++;

              TaskProgressManager.setValue(i);
            }
          }
        }

        // go to the filename directory
        int filenameDirOffset = fm.readInt();
        FieldValidator.checkOffset(filenameDirOffset);

        if (realNumFiles > 0 && filenameDirOffset == resources[realNumFiles - 1].getOffset()) {
          // no filenames, so just return from here
        }
        else {

          fm.skip(4);

          if (fm.readInt() == 0) {
            fm.seek(filenameDirOffset);

            for (int i = 0; i < realNumFiles; i++) {
              // 32 - Filename (null)
              String filename = fm.readNullString(32);
              if (filename.length() == 0) {
                resources[i].setName(Resource.generateFilename(i));
              }
              else {
                resources[i].setName(filename);
              }

              if (filename.equals("AFS")) {
                i = realNumFiles;
              }

              // 4 - File Type ID
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 4 - Junk (matches each value in the directory, including offsets and sizes, in order)
              fm.skip(16);

              TaskProgressManager.setValue(i + numFiles);
            }

            if (resources[0].getName().equals("AFS")) {
              // no real filenames were stored
              for (int i = 0; i < realNumFiles; i++) {
                resources[i].setName(Resource.generateFilename(i));
              }
            }
          }
          else {
            // no real filenames were stored
            for (int i = 0; i < realNumFiles; i++) {
              resources[i].setName(Resource.generateFilename(i));
            }
          }
        }

        fm.getBuffer().setBufferSize(32); // quick reads

        // Now go to each file, see if we can work out what type they are, and if compression is used
        ExporterPlugin exporter = Exporter_ZLib.getInstance();
        for (int i = 0; i < realNumFiles; i++) {
          Resource resource = resources[i];

          long offset = resource.getOffset();

          fm.seek(offset);

          short fileType = fm.readShort();
          fm.skip(2);

          int headerInt = fm.readInt();

          if (headerInt == 1398362949) {
            // Pro Evolution Soccer 2008/2009 compressed file header...

            int compLength = fm.readInt();
            FieldValidator.checkLength(compLength, arcSize);

            int decompLength = fm.readInt();
            if (decompLength == 0) {
              // not compressed
            }
            else {
              // compressed
              FieldValidator.checkLength(decompLength);
              resource.setDecompressedLength(decompLength);
              resource.setExporter(exporter);
            }

            resource.setLength(compLength);
            resource.setOffset(offset + 16);

            if (fileType == 256) {
              resource.setName(resource.getName() + ".afs_archive");
            }
            else if (fileType == 0) {
              resource.setName(resource.getName() + ".afs_file");
            }
          }
          else {
            String extension = resource.getExtension();
            if (extension == null || extension.length() <= 0) {
              resource.setName(resource.getName() + ".adx"); // ADX audio file
            }
          }

          resource.setOriginalName(resource.getName()); // so it doesn't think it's been renamed

          /*
          int compLength = fm.readInt();
          int decompLength = fm.readInt();
          
          
          if (headerInt == 512) {
          resource.setName(resource.getName() + ".imgarc");
          }
          else if (headerInt == 67072) {
          // Language File (sometimes)
          resource.setName(resource.getName() + ".67072");
          
          FieldValidator.checkLength(compLength, arcSize);
          FieldValidator.checkLength(decompLength);
          
          resource.setLength(compLength);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporter);
          resource.setOffset(offset + 32);
          }
          else if (headerInt == 69120) {
          resource.setName(resource.getName() + ".wkm");
          }
          else if (headerInt == 1536) {
          resource.setName(resource.getName() + ".unk1536");
          }
          else if (headerInt == 1413763169) {
          resource.setName(resource.getName() + ".tdp");
          }
          else if (headerInt == 65792) {
          resource.setName(resource.getName() + ".unk65792");
          }
          else if (headerInt == 65536) {
          resource.setName(resource.getName() + ".unk65536");
          }
          */
        }

        fm.close();

        resources = resizeResources(resources, realNumFiles);

        return resources;
      }

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

}
