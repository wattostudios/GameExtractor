/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.ErrorLogger;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.ge.plugin.exporter.Exporter_Oodle;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.Exporter_ZStd;
import org.watto.ge.plugin.exporter.HeaderSkipExporterWrapper;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FIFAMOD_FIFATOOL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FIFAMOD_FIFATOOL() {

    super("FIFAMOD_FIFATOOL", "FIFAMOD_FIFATOOL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Frosty Tool Suite",
        "FIFA Mod Manager");
    setExtensions("fifamod"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("fifa_tex", "FIFA Texture Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readString(8).equals("FIFATOOL")) {
        rating += 50;
      }

      fm.skip(21);

      long arcSize = fm.getLength();

      // 8 - Details Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      ExporterPlugin exporterOodle = Exporter_Oodle.getInstance();
      ExporterPlugin exporterZstd = Exporter_ZStd.getInstance();
      ExporterPlugin exporterLZ4 = Exporter_LZ4.getInstance();
      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - HEADER (FIFATOOL)
      fm.skip(8);

      // 4 - Version? (11)
      int version = fm.readInt();

      // 1 - Unknown (1)
      fm.skip(1);

      // 4 - Strings Data Length
      int stringsLength = fm.readInt();

      // 4 - Unknown
      // 8 - Hash?
      fm.skip(12);

      // 8 - Details Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      if (version >= 16/*28*/) {
        fm.seek(stringsLength + 12);
      }
      else {

        // 1 - Game Name Length (6)
        int gameNameLength = fm.readByte();
        FieldValidator.checkPositive(gameNameLength);

        // X - Game Name (FIFA20 / FIFA21)
        fm.skip(gameNameLength);

        // 2 - Unknown
        // 2 - Unknown (24 for version 28, 19 for version 16)
        fm.skip(4);

        // 1 - Descriptive Name Length
        int nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Descriptive Name
        fm.skip(nameLength);

        // 1 - Short/Code Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Short/Code Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        if (nameLength == 127) {
          // not actually valid - empty (Version 28)
          nameLength = 0;
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Content Type Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Content Type Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Version Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Version Name
        fm.skip(nameLength);

        // 1 - Replacing Details Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Replacing Details Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Website/Twitter Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Website/Twitter Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        // 1 - Unknown Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Unknown Name
        fm.skip(nameLength);

        if (version >= 28) {
          // 2 - null
          fm.skip(2);
        }

        // 8 - Hash?
        fm.skip(8);

        if (version >= 28) {
          // 1 - Unknown (28)
          // 4 - null
          fm.skip(5);
        }

        // 4 - Unknown
        fm.skip(4);
      }

      long filenameOffset = fm.getOffset();

      fm.relativeSeek(dirOffset);

      long dataOffset = dirOffset + (numFiles * 16);

      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 8 - File Offset (relative to the start of the File Data)
        long offset = fm.readLong() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;
      }

      fm.relativeSeek(filenameOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] names = new String[numFiles];

      int realNumFiles = 0;
      if (version >= 28) {
        // Read Directory - version 28

        // numFiles is inaccurate in the header
        numFiles *= 2;

        resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);
        names = new String[numFiles];

        for (int i = 0; i < numFiles; i++) {
          if (fm.getOffset() >= dirOffset) {
            break; // END OF DIRECTORY
          }

          // 1 - Type Flag (0/1/2)
          int typeFlag = fm.readByte();

          // 4 - File ID (incremental from 0)
          int fileID = fm.readInt();

          if (fileID == -1) {
            i--; // don't want this to count as a filename
            continue;
          }

          // 1 - Filename Length
          //int filenameLength = ByteConverter.unsign(fm.readByte());
          int filenameLength = fm.readByte();
          if (filenameLength < 0) {
            filenameLength = ((fm.readByte() << 7) | filenameLength & 127);
          }

          // X - Filename
          String filename = fm.readString(filenameLength);
          //System.out.println(filename);
          //System.out.println(fm.getOffset() + "\t" + fileID + "\t" + filename);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offsets[fileID], lengths[fileID]);
          TaskProgressManager.setValue(i);

          //System.out.println(typeFlag + "\t" + filename + "\t" + fm.getOffset());

          int otherFilenameLength = 0;

          boolean postSkip = true;

          if (fileID != -1) {
            FieldValidator.checkFilename(filename);

            // 4 - Unknown
            // 16 - Hash?
            // 8 - Original File Length?
            // 4 - Unknown
            // 1 - null
            fm.skip(33);

            // 1 - Other Filename Length
            otherFilenameLength = fm.readByte();
            if (otherFilenameLength < 0) {
              otherFilenameLength = ((fm.readByte() << 7) | otherFilenameLength & 127);
            }

            // X - Filename
            String otherFilename = fm.readString(otherFilenameLength);

            if (otherFilenameLength > 0) {
              // this is a blob with a real filename stored for it, rather than being referenced by a different file entry

              if (otherFilename.startsWith("legacy;")) {
                otherFilename = otherFilename.substring(7);
              }

              resources[i].setName(otherFilename);
              resources[i].setOriginalName(otherFilename);
            }

            /*
            if (otherFilenameLength > 0) {
              // 4 - null
              fm.skip(4);
            }
            */

            // 2 - Unknown Count (0/1/2/3)
            int unknownCount = fm.readShort();
            FieldValidator.checkPositive(unknownCount);

            // 2 - null
            fm.skip(2);

            if (unknownCount == 0) {
              // 2 - Unknown Count (0/1/2/3)
              unknownCount = fm.readShort();
              FieldValidator.checkPositive(unknownCount);

              // 2 - null
              fm.skip(2);

              postSkip = false;
            }

            // for each (unknownCount)
            //   4 - Unknown
            fm.skip(unknownCount * 4);

            if (typeFlag == 0 || typeFlag == 1) {
              // nothing else
            }
            else if (typeFlag == 2) {
              // 4 - null
              // 12 - Hash?
              // 4 - Unknown (16)
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              fm.skip(32);
            }

            else if (typeFlag == 3) {
              // 8 - null
              // 8 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              fm.skip(24);
            }
            else {
              ErrorLogger.log("[FIFAMOD_FIFATOOL] Unknown Type Flag: " + typeFlag);
            }
          }

          if (postSkip) {
            // 4 - Unknown
            fm.skip(4);
          }

          if (fileID != -1) {
            names[realNumFiles] = filename;
            TaskProgressManager.setValue(realNumFiles);
            realNumFiles++;
          }
          else {
            i--; // to make it loop again - don't want to count this iteration, it was a padding one
          }
        }

      }
      else {
        // Read Directory - any other (older) version

        for (int i = 0; i < numFiles; i++) {
          // 1 - Type Flag (0/1/2)
          int typeFlag = fm.readByte();

          // 4 - File ID (incremental from 0)
          int fileID = fm.readInt();

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          //System.out.println(filename);

          if (fileID != -1) {
            FieldValidator.checkFilename(filename);

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offsets[fileID], lengths[fileID]);
            TaskProgressManager.setValue(i);

            // 20 - Hash?
            // 8 - Original File Length?
            // 4 - Unknown
            // 2 - null
            fm.skip(34);

            // 2 - Unknown Count (0/1/2/3)
            int unknownCount = fm.readShort();
            FieldValidator.checkPositive(unknownCount);

            // 2 - null
            fm.skip(2);

            // for each (unknownCount)
            //   4 - Unknown
            fm.skip(unknownCount * 4);

            if (typeFlag == 0 || typeFlag == 1) {
              // nothing else
            }
            else if (typeFlag == 2) {
              // 4 - null
              // 12 - Hash?
              // 4 - Unknown (16)
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              fm.skip(32);
            }

            else if (typeFlag == 3) {
              // 8 - null
              // 8 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              fm.skip(24);
            }
            else {
              ErrorLogger.log("[FIFAMOD_FIFATOOL] Unknown Type Flag: " + typeFlag);
            }
          }

          // 4 - Unknown (0/-1)
          fm.skip(4);

          if (fileID != -1) {
            names[realNumFiles] = filename;
            TaskProgressManager.setValue(realNumFiles);
            realNumFiles++;
          }
          else {
            i--; // to make it loop again - don't want to count this iteration, it was a padding one
          }
        }
      }

      // Now, for each file that has the first 4 bytes = 1024, this is a BLOB for a real file earlier.
      // Read each file of ~152 bytes in size - at offset 56-60 or 52-56, the HEX should match the HEX at the end of a BLOB filename
      fm.getBuffer().setBufferSize(64);

      Resource[] realResources = new Resource[numFiles];
      realNumFiles = 0;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource == null) {
          continue; // this was a BLOB that we've already consumed
        }

        int length = (int) resource.getLength();

        if (length > 64 && length < 200) {
          // maybe just a pointer to a blob

          fm.relativeSeek(resource.getOffset());
          fm.skip(40);

          if (fm.readInt() == 0) {
            fm.skip(12);
          }
          else {
            fm.skip(8);
          }

          String hexString = fm.readHex(4).toString().toLowerCase();
          //System.out.println("hex: " + hexString + " for file " + resource.getName());

          // see if there's a file with this name
          boolean matchFound = false;
          for (int j = 0; j < numFiles; j++) {
            Resource compareResource = resources[j];
            if (compareResource == null) {
              continue; // already used BLOB
            }

            String filename = compareResource.getName().toLowerCase();
            if (filename.endsWith(hexString)) {
              // found the match
              matchFound = true;

              //System.out.println("matched with " + filename);

              // I think these are all images? set a filename on them
              filename = resource.getName() + ".fifa_tex";

              //if (filename.contains("banner_11")) {
              //  System.out.println("here");
              //}

              resource.setName(filename);
              resource.setOriginalName(filename);
              resource.addProperty("ImageDataOffset", (int) resource.getLength());
              resource.addProperty("ResourceBlobFilename", compareResource.getName());

              // now lets read the compression details
              fm.relativeSeek(compareResource.getOffset());

              // change this file so that it contains the metadata followed by the real data
              int maxBlocks = (int) (compareResource.getLength() / 512) + 20;
              int numBlocks = 0;
              long totalLength = resource.getLength();
              long totalDecompLength = resource.getDecompressedLength();

              long[] blockOffsets = new long[maxBlocks];
              long[] blockLengths = new long[maxBlocks];
              long[] blockDecompLengths = new long[maxBlocks];
              ExporterPlugin[] blockExporters = new ExporterPlugin[maxBlocks];

              // set the first block as the metadata
              blockOffsets[0] = resource.getOffset();
              blockLengths[0] = resource.getLength();
              blockDecompLengths[0] = resource.getDecompressedLength();
              blockExporters[0] = exporterDefault;
              numBlocks++;

              long endOffset = fm.getOffset() + compareResource.getLength();
              while (fm.getOffset() < endOffset) {
                // 1 - Need Dictionary
                fm.skip(1);

                // 3 - Decomp Block Length
                int blockDecompLength = IntConverter.convertBig(new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() });

                // 2 - Flags
                int flags = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

                // 2 - Comp Block Length
                int blockLength = ((flags & 15) << 16) | ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

                if (numBlocks == 1 && blockLength < 100) {
                  // not really a compressed file
                  //System.out.println(numBlocks + "\t" + fm.getOffset() + "\t" + blockLength + "\t" + blockDecompLength);

                  blockOffsets[1] = compareResource.getOffset();
                  blockLengths[1] = compareResource.getLength();
                  blockDecompLengths[1] = compareResource.getDecompressedLength();
                  blockExporters[1] = exporterDefault;
                  numBlocks++;

                  totalLength = resource.getLength() + compareResource.getLength();
                  totalDecompLength = resource.getDecompressedLength() + compareResource.getDecompressedLength();

                  break;
                }

                int compressionType = (flags >> 8);
                if ((compressionType & 0x10) == 0x10) {
                  blockExporters[numBlocks] = exporterOodle;
                }
                else if ((compressionType & 0xf) == 0xf) {
                  blockExporters[numBlocks] = exporterZstd;
                }
                else if ((compressionType & 0x9) == 0x9) {
                  blockExporters[numBlocks] = exporterLZ4;
                }
                else if (compressionType == 0) {
                  blockExporters[numBlocks] = exporterDefault;
                }
                else {
                  blockExporters[numBlocks] = exporterZLib;
                }

                blockOffsets[numBlocks] = fm.getOffset();
                blockLengths[numBlocks] = blockLength;
                blockDecompLengths[numBlocks] = blockDecompLength;

                numBlocks++;

                fm.skip(blockLength);

                totalLength += blockLength;
                totalDecompLength += blockDecompLength;
              }

              if (numBlocks < maxBlocks) {
                long[] oldOffsets = blockOffsets;
                blockOffsets = new long[numBlocks];
                System.arraycopy(oldOffsets, 0, blockOffsets, 0, numBlocks);

                long[] oldLengths = blockLengths;
                blockLengths = new long[numBlocks];
                System.arraycopy(oldLengths, 0, blockLengths, 0, numBlocks);

                long[] oldDecompLengths = blockDecompLengths;
                blockDecompLengths = new long[numBlocks];
                System.arraycopy(oldDecompLengths, 0, blockDecompLengths, 0, numBlocks);

                ExporterPlugin[] oldExporters = blockExporters;
                blockExporters = new ExporterPlugin[numBlocks];
                System.arraycopy(oldExporters, 0, blockExporters, 0, numBlocks);
              }

              BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);
              resource.setExporter(blockExporter);

              resource.setLength(totalLength);
              resource.setDecompressedLength(totalDecompLength);

              // keep the combined resource
              realResources[realNumFiles] = resource;
              realNumFiles++;

              // flag the blob resource as having already been used
              resources[j] = null;

              break; // already found a match - stop checking other files
            }
          }

          if (!matchFound) {
            realResources[realNumFiles] = resource;
            realNumFiles++;
          }
        }
        else {
          try {
            // now lets read the compression details of this single file
            fm.relativeSeek(resource.getOffset());

            // change this file so that it contains the metadata followed by the real data
            int maxBlocks = (int) (resource.getLength() / 512) + 20;
            int numBlocks = 0;
            long totalLength = 0;//resource.getLength();
            long totalDecompLength = 0;//resource.getDecompressedLength();

            long[] blockOffsets = new long[maxBlocks];
            long[] blockLengths = new long[maxBlocks];
            long[] blockDecompLengths = new long[maxBlocks];
            ExporterPlugin[] blockExporters = new ExporterPlugin[maxBlocks];

            long endOffset = fm.getOffset() + resource.getLength();
            while (fm.getOffset() < endOffset) {
              // 1 - Need Dictionary
              fm.skip(1);

              // 3 - Decomp Block Length
              int blockDecompLength = IntConverter.convertBig(new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() });

              // 2 - Flags
              int flags = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

              // 2 - Comp Block Length
              int blockLength = ((flags & 15) << 16) | ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));
              //System.out.println(blockDecompLength + "\t" + blockLength);
              if (blockLength > resource.getLength() || blockLength < 0) {// || (blockDecompLength / 30 > blockLength)) { // 30x compression ratio must be wrong!!
                // not a compressed file
                blockOffsets = new long[] { resource.getOffset() };
                blockLengths = new long[] { resource.getLength() };
                blockDecompLengths = new long[] { resource.getDecompressedLength() };
                blockExporters = new ExporterPlugin[] { exporterDefault };

                numBlocks = 1;
                maxBlocks = 1;

                totalLength = resource.getLength();
                totalDecompLength = resource.getDecompressedLength();

                break;
              }

              int compressionType = (flags >> 8);
              if ((compressionType & 0x10) == 0x10) {
                blockExporters[numBlocks] = exporterOodle;
              }
              else if ((compressionType & 0xf) == 0xf) {
                blockExporters[numBlocks] = exporterZstd;
              }
              else if ((compressionType & 0x9) == 0x9) {
                blockExporters[numBlocks] = exporterLZ4;
              }
              else if (compressionType == 0) {
                blockExporters[numBlocks] = exporterDefault;
              }
              else {
                blockExporters[numBlocks] = exporterZLib;
              }

              blockOffsets[numBlocks] = fm.getOffset();
              blockLengths[numBlocks] = blockLength;
              blockDecompLengths[numBlocks] = blockDecompLength;

              numBlocks++;

              fm.skip(blockLength);

              totalLength += blockLength;
              totalDecompLength += blockDecompLength;
            }

            if (numBlocks < maxBlocks) {
              long[] oldOffsets = blockOffsets;
              blockOffsets = new long[numBlocks];
              System.arraycopy(oldOffsets, 0, blockOffsets, 0, numBlocks);

              long[] oldLengths = blockLengths;
              blockLengths = new long[numBlocks];
              System.arraycopy(oldLengths, 0, blockLengths, 0, numBlocks);

              long[] oldDecompLengths = blockDecompLengths;
              blockDecompLengths = new long[numBlocks];
              System.arraycopy(oldDecompLengths, 0, blockDecompLengths, 0, numBlocks);

              ExporterPlugin[] oldExporters = blockExporters;
              blockExporters = new ExporterPlugin[numBlocks];
              System.arraycopy(oldExporters, 0, blockExporters, 0, numBlocks);
            }

            BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);
            resource.setExporter(blockExporter);

            resource.setLength(totalLength);
            resource.setDecompressedLength(totalDecompLength);

            realResources[realNumFiles] = resource;
            realNumFiles++;
          }
          catch (Throwable t) {
            ErrorLogger.log(t);
          }
        }

      }

      resources = resizeResources(realResources, realNumFiles);

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

    String extension = resource.getExtension();
    if (extension != null && !extension.equals("")) {
      return extension;
    }

    if (headerInt1 == -148156712) {
      resource.setExporter(Exporter_Default.getInstance());
      resource.setDecompressedLength(resource.getLength());
      return "png";
    }
    else if (headerInt3 == 5784133) {
      // EBX file - if we strip off the 3-field header, it can be imported into FIFA Editor as a raw file
      //return "ebx";
      ExporterPlugin exporter = resource.getExporter();
      if (!(exporter instanceof HeaderSkipExporterWrapper)) {
        resource.setExporter(new HeaderSkipExporterWrapper(exporter, 12));
      }
      return "ebx.bin";
    }
    else if (resource.getName().endsWith("_mesh")) {
      return "fbx";
    }

    return null;
  }

}
