
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.WSPluginException;
import org.watto.datatype.Archive;
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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SPK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SPK_2() {

    super("SPK_2", "SPK_2");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("Quest For Glory 5");
    setExtensions("spk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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

      // go to the archive footer
      fm.seek(arcSize - 10);

      // 4 - Directory Length
      long dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - File Data Length
      long fileDataLength = fm.readInt();
      FieldValidator.checkLength(fileDataLength, arcSize);

      long relOffset = arcSize - dirLength - fileDataLength - 22;

      long dirOffset = arcSize - dirLength - 22;
      FieldValidator.checkOffset(dirOffset, arcSize);
      fm.seek(dirOffset);

      long endOfDirectory = arcSize - 22;

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirOffset);

      // Loop through directory
      int realNumFiles = 0;
      boolean recalcOffsets = false;
      while (fm.getOffset() < endOfDirectory) {

        // 2 - Header (PK)
        // 4 - Entry Type (1639169 = Directory Entry)
        // 2 - Unknown (10)
        // 4 - null
        // 8 - Checksum
        fm.skip(20);

        // 4 - Compressed File Size
        long length = fm.readInt();

        // 4 - Decompressed File Size
        long decompressedLength = fm.readInt();

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        if (filenameLength != 0) {
          // if the filename length is zero, we will search through to find the next file
          FieldValidator.checkFilenameLength(filenameLength);
        }
        else {

          //recalcOffsets = true;
          //relOffset = 24200;//24242;
          // THIS IS BECAUSE ONE OF THE RUSSIAN FILES DOESN'T READ WELL!!!
          return readManually(path, fm, endOfDirectory);
        }

        // minus the PK header from the file
        //length -= (30 + filenameLength);
        FieldValidator.checkLength(length);

        //decompressedLength -= (30 + filenameLength);
        FieldValidator.checkLength(decompressedLength);

        // 6 - null
        // 4 - Unknown (32)
        fm.skip(10);

        // 4 - File Offset (add the RelOffset, and skip over the PK header from the file)
        long offset = 0;
        if (recalcOffsets) {
          offset = fm.readInt();
        }
        else {
          offset = fm.readInt() + relOffset + 66 + filenameLength;
        }
        FieldValidator.checkOffset(offset, arcSize);

        // X - Filename
        String filename = "";
        if (filenameLength != 0) {
          filename = fm.readString(filenameLength);
        }
        else {
          // search through and find the next PK header
          long thisOffset = fm.getOffset();
          String tempFilename = fm.readString(256);
          int pkOffset = tempFilename.indexOf("PK");
          if (pkOffset > 0) {
            filename = tempFilename.substring(0, pkOffset);
            fm.relativeSeek(thisOffset + pkOffset);
          }
          else {
            return null;
          }
        }

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompressedLength);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      resources = resizeResources(resources, realNumFiles);

      /*
      if (recalcOffsets) {
        relOffset = dirOffset;// + 43;
      
        for (int i = realNumFiles - 1; i >= 0; i--) {
          // working backwards
          Resource currentResource = resources[i];
          relOffset -= currentResource.getLength();
          currentResource.setOffset(relOffset);
      
          relOffset -= (66 + currentResource.getNameLength());
        }
      }
      */

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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculate the length of the header section, and copy it
      long oldArcSize = src.getLength();
      src.seek(oldArcSize - 10);

      long oldDirLength = src.readInt();
      FieldValidator.checkLength(oldDirLength, oldArcSize);

      long oldFileDataLength = src.readInt();
      FieldValidator.checkLength(oldFileDataLength, oldArcSize);

      long headerLength = oldArcSize - oldDirLength - oldFileDataLength - 22;
      src.seek(0);

      // X - Header Data
      fm.writeBytes(src.readBytes((int) headerLength));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      TaskProgressManager.setMaximum(numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String filename = resource.getName();
        int filenameLength = filename.length();
        long length = resource.getDecompressedLength();// + 30 + filenameLength;
        // 2 - Header (PK)
        // 4 - Entry Type (1283 = File Entry)
        // 4 - null
        // 8 - Checksum
        fm.writeBytes(src.readBytes(18));

        // 4 - Compressed File Size
        fm.writeInt((int) length);
        int oldLength = src.readInt();

        // 4 - Decompressed File Size
        fm.writeInt((int) length);
        src.skip(4);

        // 2 - Filename Length
        fm.writeShort((short) filenameLength);
        short oldFilenameLength = src.readShort();

        // 2 - Extra Header Length (36)
        fm.writeBytes(src.readBytes(2));

        // X - Filename
        fm.writeString(filename);
        src.skip(oldFilenameLength);

        // 36 - Unknown
        fm.writeBytes(src.readBytes(36));

        // X - File Data
        write(resource, fm);
        src.skip(oldLength);// - 30 - oldFilenameLength);
        TaskProgressManager.setValue(i);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      long dirLength = 0;
      long fileDataLength = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String filename = resource.getName();
        int filenameLength = filename.length();
        long length = resource.getDecompressedLength();

        // 2 - Header (PK)
        // 4 - Entry Type (1639169 = Directory Entry)
        // 2 - Unknown (10)
        // 4 - null
        // 8 - Checksum
        fm.writeBytes(src.readBytes(20));

        // 4 - Compressed File Size
        // 4 - Decompressed File Size
        fm.writeInt((int) length);
        fm.writeInt((int) length);
        src.skip(8);

        // 4 - Filename Length
        fm.writeInt(filenameLength);
        int oldFilenameLength = src.readInt();

        // 6 - null
        // 4 - Unknown (32)
        fm.writeBytes(src.readBytes(10));

        // 4 - File Offset
        fm.writeInt((int) offset);
        src.skip(4);

        // X - Filename
        fm.writeString(filename);
        src.skip(oldFilenameLength);

        length += 66 + filenameLength;

        offset += length;
        fileDataLength += length;
        dirLength += 46 + filenameLength;
      }

      // Write Footer Data

      // 2 - Header (PK)
      // 4 - Entry Type (1797 = Footer Entry)
      // 2 - null
      // 4 - Unknown
      fm.writeBytes(src.readBytes(12));

      // 4 - Directory Length
      fm.writeInt((int) dirLength);
      src.skip(4);

      // 4 - File Data Length
      fm.writeInt((int) fileDataLength);
      src.skip(4);

      // 2 - null
      fm.writeBytes(src.readBytes(2));

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource[] readManually(File path, FileManipulator fm, long endOfDirectory) {
    try {

      // first, scan through the beginning of the file, to find the first offset
      fm.seek(0);
      String header = fm.readString(40000);
      int firstFile = header.indexOf("PK");
      if (firstFile <= 0) {
        return null;
      }
      fm.relativeSeek(firstFile);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(endOfDirectory);

      int realNumFiles = 0;
      while (fm.getOffset() < endOfDirectory) {
        // 2 - Header (PK)
        if (!fm.readString(2).equals("PK")) {
          // read through a little more, see if we can find the next entry
          long thisOffset = fm.getOffset();
          long bytesToAddToPrevious = thisOffset;
          String headerFinder = fm.readString(10000);
          int pkPos = headerFinder.indexOf("PK");
          //System.out.println("skipping forward a few bytes: " + pkPos + " to new offset " + thisOffset);
          if (pkPos > 0) {
            thisOffset += pkPos + 2;
          }
          else {
            // try to find the beginning of the filename, and move backwards
            pkPos = headerFinder.indexOf("GRA");
            if (pkPos < 80) {
              thisOffset += (pkPos - 28);
            }
          }
          fm.seek(thisOffset);

          // we had to skip some bytes (forward or back), so adjust the size of te previous file to suit
          bytesToAddToPrevious = (thisOffset - bytesToAddToPrevious);

          if (realNumFiles != 0) {
            Resource previousResource = resources[realNumFiles - 1];
            long length = previousResource.getLength() + bytesToAddToPrevious;
            previousResource.setLength(length);
            previousResource.setDecompressedLength(length);
          }
        }

        // 4 - Entry Type (1311747 = File Entry)
        int entryType = fm.readInt();
        if (entryType == 1311747 || entryType == 656643 || entryType == 160287384 || entryType == 2112503309) {
          // File Entry

          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          fm.skip(12);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 2 - Filename Length
          short filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // 2 - Extra Length
          short extraLength = fm.readShort();
          FieldValidator.checkFilenameLength(extraLength);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - Extra Data
          fm.skip(extraLength);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else if (entryType == 513 || entryType == 1639169) {
          // Directory Entry

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.skip(22);

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // 10 - null
          // 4 - File Offset (points to PK for this file in the directory)
          fm.skip(14);

          // X - Filename
          fm.skip(filenameLength);

        }
        else if (entryType == 656387) {
          // Directory Entry (Short)

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.skip(20);

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          fm.skip(filenameLength);

        }
        else if (entryType == 1541) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          fm.skip(16);
        }
        else {
          // bad header
          String errorMessage = "[SPK_2]: Manual read: Unknown entry type " + entryType + " at offset " + (fm.getOffset() - 6);
          if (realNumFiles >= 5) {
            // we found a number of files, so lets just return them, it might be a "prematurely-short" archive.
            ErrorLogger.log(errorMessage);
            break;
          }
          else {
            throw new WSPluginException(errorMessage);
          }
        }

      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
