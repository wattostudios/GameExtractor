/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GTC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GTC() {

    super("GTC", "GTC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Drome Racers");
    setExtensions("gtc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("md2", "MD2 Model", FileType.TYPE_MODEL),
        new FileType("pc texture", "PC Texture Image", FileType.TYPE_IMAGE),
        new FileType("xbx texture", "XBox Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("cmu", "ifl"); // LOWER CASE

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

      if (fm.readLong() == 0) {
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
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same Unreal header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressArchive(FileManipulator fm) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        if (decompFile.length() == 0) {
          // empty file (ie wasn't decompressed properly before)
          decompFile.delete();
        }
        else {
          // we've already decompressed this file before - open and return it
          return new FileManipulator(decompFile, false);
        }
      }

      // need to open the Compress.inf file and read the compression details from it
      File compressFile = new File(pathOnly + File.separatorChar + "Compress.inf");
      if (!compressFile.exists()) {
        compressFile = new File(pathOnly + File.separatorChar + "COMPRESS.INF");
      }
      if (!compressFile.exists()) {
        compressFile = new File(pathOnly + File.separatorChar + "compress.inf");
      }
      if (!compressFile.exists()) {
        // Compress.inf file not found
        return fm;
      }

      long arcSize = fm.getLength();

      int numBlocks = (int) (compressFile.length() / 12);
      FieldValidator.checkNumFiles(numBlocks);

      long[] blockOffsets = new long[numBlocks];
      long[] blockLengths = new long[numBlocks];

      FileManipulator compressFM = new FileManipulator(compressFile, false);

      for (int i = 0; i < numBlocks; i++) {
        // 4 - Compressed Block Offset
        int offset = compressFM.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Block Length (not including padding)
        int length = compressFM.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Compressed Block Length (including padding)
        int paddedLength = compressFM.readInt();
        FieldValidator.checkLength(paddedLength, arcSize);

        int nullSize = paddedLength - length;
        FieldValidator.checkLength(nullSize);
        offset += nullSize; // to skip the nulls at the beginning of the compressed block

        blockOffsets[i] = offset;
        blockLengths[i] = length;
      }

      compressFM.close();

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      fm.seek(0); // return to the start, ready for decompression

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      // Do the decompression
      // Ref: https://github.com/JrMasterModelBuilder/UNGTC/blob/master/ungtc.cpp
      int maxDecompBlockSize = 131072;
      for (int i = 0; i < numBlocks; i++) {
        //System.out.println(i + " of " + numBlocks);

        long blockOffset = blockOffsets[i];
        long blockLength = blockLengths[i];
        //long endOffset = blockOffset + blockLength;

        fm.relativeSeek(blockOffset);

        byte[] compBytes = fm.readBytes((int) blockLength);

        // Check if this block is uncompressed.
        if (blockLength == maxDecompBlockSize) {
          //This block is uncompressed, copy as is.
          decompFM.writeBytes(compBytes);
          //System.out.println("Raw " + maxDecompBlockSize + " bytes");
        }
        else {

          // buffer for holding the decompressed blocks, so we can just around and copy data within it
          byte[] decompBytes = new byte[maxDecompBlockSize];

          boolean back_ref = false;
          int single = 0;
          int back_ref_size = 0;
          int back_ref_jump = 0;

          int compPosi = 0;
          int decompPosi = 0;

          do {
            //Copy the command bits and move past.
            int command = ByteConverter.unsign(compBytes[compPosi])
                | (ByteConverter.unsign(compBytes[compPosi + 1]) << 8)
                | (ByteConverter.unsign(compBytes[compPosi + 2]) << 16)
                | (ByteConverter.unsign(compBytes[compPosi + 3]) << 24);
            compPosi += 4;

            //Read the bits from right to left, but only until the entire block or file is read.
            for (int j = 0; j < 32 && compPosi < blockLength && decompPosi < maxDecompBlockSize; ++j) {
              //Get the boolean value of the bit.
              boolean bit = ((command >> j & 1) == 1);

              //Check if already in a backref.
              if (back_ref) {
                //Check if we have detected what kind of backref yet.
                if (single != 0) {
                  //We are in a single byte backref, we need to read the size.

                  //Add the value of the bit if set.
                  if (bit) {
                    //This addition logic only works because we never go above 2 bits
                    back_ref_size += single;
                  }

                  //If we have read the last bit, copy the bytes and exit the backref.
                  if (--single == 0) {
                    //We know how big the backref is now, copy the data from the decompressed data from the specified backref jump.
                    for (int k = 0; k < back_ref_size; ++k) {
                      //decompPosi += copy_data(decompBytes, decompBytes, 1, back_ref_jump);
                      decompBytes[decompPosi] = decompBytes[decompPosi + back_ref_jump]; // Note: back_ref_jump is negative
                      decompPosi++;
                    }

                    back_ref = false;
                  }
                }
                else {
                  //We have not yet determined what kind of backref this is.
                  if (bit) {
                    //A self-contained multi-byte, read it now and continue on.

                    //Read the first 3 bits.
                    back_ref_size = ByteConverter.unsign(compBytes[compPosi]) & 0x07;

                    //Read the block offset.
                    back_ref_jump = -8192 + (((ByteConverter.unsign(compBytes[compPosi]) & 0xF8) >> 3)
                        | (ByteConverter.unsign(compBytes[compPosi + 1]) << 5));

                    compPosi += 2;

                    //If all the bits are 0, this is a 3 byte block, else it is a 2 byte block.
                    if (back_ref_size == 0) {
                      //Read the 7 bits that are the backref block size.
                      back_ref_size = ByteConverter.unsign(compBytes[compPosi]) & 0x7F;

                      //Check the last 1 bit to see if the backref starts 2x further back.
                      if ((ByteConverter.unsign(compBytes[compPosi]) & 0x80) == 0x80) {
                        back_ref_jump -= 8192;
                      }

                      compPosi++;

                      //If the backref size is still 0, read the next 2 bytes to get the size.
                      if (back_ref_size == 0) {
                        back_ref_size = ((ByteConverter.unsign(compBytes[compPosi]))
                            | (ByteConverter.unsign(compBytes[compPosi + 1]) << 8));
                        compPosi += 2;
                      }
                      else {
                        //The minimum size of a backref is 2, so add 2 to the total.
                        back_ref_size += 2;
                      }
                    }
                    else {
                      //The minimum size of a backref is 2, so add 2 to the total.
                      back_ref_size += 2;
                    }

                    //Copy the data and more ahead.
                    for (int k = 0; k < back_ref_size; ++k) {
                      //decompPosi += copy_data(decompBytes,decompBytes, 1, back_ref_jump);
                      decompBytes[decompPosi] = decompBytes[decompPosi + back_ref_jump]; // Note: back_ref_jump is negative
                      decompPosi++;
                    }

                    back_ref = false;
                  }
                  else {
                    //This is a single byte backref, we need to read the next 2 bits.
                    single = 2;
                    //The minimum single backref size is 2.
                    back_ref_size = 2;

                    //We do not know how large the backref block is yet, but we must read where, in case the size runs into the next command block.
                    back_ref_jump = -256 + ByteConverter.unsign(compBytes[compPosi]);

                    compPosi++;
                  }
                }
              }
              else {
                //Not currently in a backref, check if literal or declaring a backref.
                if (bit) {
                  //Backref, setup to read what kind from the next bit(s).
                  back_ref = true;
                  single = 0;
                }
                else {
                  //Literal byte, copy it.
                  //decompPosi += copy_data(compBytes, decompBytes, 1);
                  decompBytes[decompPosi] = compBytes[compPosi];
                  decompPosi++;

                  compPosi++;
                }
              }
            }
          }
          while (compPosi < blockLength && decompPosi < maxDecompBlockSize); // -3 because the "command" in the do-while reads the next 4 bytes

          // write out the decompressed bytes
          decompFM.writeBytes(decompBytes);
          //System.out.println("Decompressed " + decompPosi + " bytes");
        }
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(0);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
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

      String pathOnly = FilenameSplitter.getDirectory(path);

      FileManipulator fm = new FileManipulator(path, false);

      FileManipulator decompFM = decompressArchive(fm);
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(0); // go to the start of the decompressed file

        path = fm.getFile(); // So the resources are stored against the decompressed file
      }

      long arcSize = fm.getLength();

      // need to open the FileList.Inf file and read the file details from it
      File detailsFile = new File(pathOnly + File.separatorChar + "FileList.Inf");
      if (!detailsFile.exists()) {
        detailsFile = new File(pathOnly + File.separatorChar + "FileList.inf");
      }
      if (!detailsFile.exists()) {
        detailsFile = new File(pathOnly + File.separatorChar + "FILELIST.INF");
      }
      if (!detailsFile.exists()) {
        detailsFile = new File(pathOnly + File.separatorChar + "filelist.inf");
      }
      if (!detailsFile.exists()) {
        // FileList.Inf file not found
        return null;
      }

      int numFiles = (int) (detailsFile.length() / 136);
      FieldValidator.checkNumFiles(numFiles);

      FileManipulator detailsFM = new FileManipulator(detailsFile, false);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 128 - Filename (null terminated, filled with nulls)
        String filename = detailsFM.readNullString(128);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        int offset = detailsFM.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = detailsFM.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      detailsFM.close();

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
