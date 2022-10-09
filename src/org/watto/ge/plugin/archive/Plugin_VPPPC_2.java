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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.ge.plugin.exporter.Exporter_LZ4_Framed;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VPPPC_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VPPPC_2() {

    super("VPPPC_2", "VPPPC_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Saints Row: The Third: Remastered");
    setExtensions("vpp_pc", "str2"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("cte_xtbl", "xtbl", "vpkg", "vint_proj"); // LOWER CASE

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

      if (fm.readLong() == 27137739470l) {
        rating += 50;
      }

      fm.skip(8);

      if (fm.readLong() == 0) { // other vpp_pc files have NumFiles here, so this helps to differentiate
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
   The decompressed file contains the same header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressArchive(FileManipulator fm, long compArcLength, long decompArcLength) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(compArcLength); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_LZ4 exporter = Exporter_LZ4.getInstance();

      while (compArcLength > 0) {
        long currentOffset = fm.getOffset();

        // 8 - Compression Header (52596161184193518)
        long compressionHeader = fm.readLong();
        if (compressionHeader != 52596161184193518l) {

          // perhaps the compressed chunks are padded to 4096 bytes, rather than being consecutive
          int paddingSize = calculatePadding(currentOffset, 4096);
          if (paddingSize != 0) {
            fm.relativeSeek(currentOffset + paddingSize);
            compArcLength -= paddingSize;
            compressionHeader = fm.readLong();
          }

          if (compressionHeader != 52596161184193518l) {
            ErrorLogger.log("[VPPPC_2] Compression Header Mismatch: " + (fm.getOffset() - 8) + "\t" + compressionHeader);
          }
          else {
            // also pad the decompFM to 4096 bytes as well
            paddingSize = calculatePadding(decompFM.getLength(), 4096);
            for (int p = 0; p < paddingSize; p++) {
              decompFM.writeByte(0);
            }
          }
        }

        // 4 - Compressed Block Length (not including any of these headers)
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, compArcLength);

        // 4 - Decompressed Block Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength, decompArcLength);

        if (compLength == decompLength) {
          ErrorLogger.log("[VPPPC_2] Compressed Length = Decompressed Length: " + (fm.getOffset() - 16) + "\t" + compLength + "\t" + decompLength);
        }

        // X - Compressed Data
        compArcLength -= (compLength + 16);
        decompArcLength -= decompLength;

        byte[] compBytes = fm.readBytes(compLength);
        FileManipulator compFM = new FileManipulator(new ByteBuffer(compBytes));

        exporter.open(compFM, compLength, decompLength);

        while (exporter.available()) {
          decompFM.writeByte(exporter.read());
        }

        exporter.close();
        compFM.close();
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

      ExporterPlugin exporter = Exporter_LZ4_Framed.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Unknown (27137739470)
      // 336 - null
      fm.skip(344);

      // 8 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);
      fm.skip(4);

      // 8 - Archive Length
      fm.skip(8);

      // 8 - Details Directory Length
      long dirLength = fm.readLong();
      FieldValidator.checkLength(dirLength, arcSize);

      // 8 - Name Directory Length
      long nameDirLength = fm.readLong();
      FieldValidator.checkLength(nameDirLength, arcSize);

      // 8 - Decompressed File Data Length
      long dataLength = fm.readLong();

      // 8 - Compressed File Data Length (or -1 if the file data is not compressed)
      long compDataLength = fm.readLong();

      long dataOffset = arcSize - dataLength;

      boolean compressedArchive = false;
      if (compDataLength != -1) {
        dataOffset = arcSize - compDataLength;
        FieldValidator.checkOffset(dataOffset);

        fm.seek(dataOffset);

        FileManipulator decompFM = decompressArchive(fm, compDataLength, dataLength);
        if (decompFM != null) {
          // it was decompressed - don't want to read from the decompressed archive, but want to point to it.
          path = decompFM.getFile(); // So the resources are stored against the decompressed file
          //arcSize = path.length();
          arcSize = dataLength;
          decompFM.close();

          dataOffset = 0;
          compressedArchive = true;
        }
      }

      FieldValidator.checkLength(dataLength, arcSize);
      FieldValidator.checkOffset(dataOffset);

      long dirOffset = 4096;

      long nameDirOffset = dirOffset + (numFiles * 48);
      nameDirOffset += calculatePadding(nameDirOffset, 4096);

      fm.seek(nameDirOffset);
      byte[] nameBytes = fm.readBytes((int) nameDirLength);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - Filename Offset (relative to the start of the names directory)
        long filenameOffset = fm.readLong();
        FieldValidator.checkOffset(filenameOffset, nameDirLength);

        // 8 - Directory Name Offset (relative to the start of the names directory)
        long dirNameOffset = fm.readLong();
        FieldValidator.checkOffset(dirNameOffset, nameDirLength);

        String filename = "";
        if (dirNameOffset != 0) {
          nameFM.seek(dirNameOffset);
          filename = nameFM.readNullString();
        }

        nameFM.seek(filenameOffset);
        filename += nameFM.readNullString();

        if (filename.endsWith("_pc")) {
          filename = filename.substring(0, filename.length() - 3);
        }
        if (filename.startsWith("..\\")) {
          filename = filename.substring(3);
        }

        // 8 - File Data Offset (relative to the start of the File Data)
        long offset = fm.readLong() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize + 1); // to allow for files that are at the length of the archive

        // 8 - Decompressed File Length
        long decompLength = fm.readLong();
        FieldValidator.checkLength(decompLength);

        // 8 - Compressed File Length (or -1 if not compressed)
        long length = fm.readLong();
        if (length == -1) {
          length = decompLength;
        }
        FieldValidator.checkLength(length, arcSize);

        // 2 - Compression Flag (0=uncompressed, 1=compressed)
        short compressionFlag = fm.readShort();

        // 2 - Unknown (1)
        // 4 - null
        fm.skip(6);

        //path,name,offset,length,decompLength,exporter
        if (compressionFlag == 0) {
          if (compressedArchive) {
            length = decompLength; // the file has already been decompressed
          }
          Resource resource = new Resource(path, filename, offset, length);
          resource.forceNotAdded(true);
          resources[i] = resource;
        }
        else {
          Resource resource = new Resource(path, filename, offset, length, decompLength, exporter);
          resource.forceNotAdded(true);
          resources[i] = resource;
        }

        TaskProgressManager.setValue(i);
      }

      nameFM.close();
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
