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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NoExt_6 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NoExt_6() {

    super("NoExt_6", "NoExt_6");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("NBA 2k18");
    setExtensions(""); // MUST BE LOWER CASE
    setPlatforms("XBox 360");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      if (new File(fm.getFile().getParent() + File.separatorChar + "0A").exists()) {
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
   
   **********************************************************************************************
   **/
  public File extractSplitFile(File archive1, File archive2, long offset, long length, int archiveNum) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = archive1;

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_splitfile" + archiveNum + ".temp");
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return decompFile;
      }

      if (!decompFile.createNewFile()) {
        // extract to a temporary location instead
        pathOnly = Settings.getString("TempDirectory");

        decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_splitfile" + archiveNum + ".temp");
        if (decompFile.exists()) {
          // we've already decompressed this file before - open and return it
          return decompFile;
        }
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      int firstFileCopyLength = (int) (archive1.length() - offset);
      int secondFileCopyLength = (int) (length - firstFileCopyLength);

      // copy the first file into it
      FileManipulator arcFM = new FileManipulator(archive1, false);
      arcFM.seek(offset);
      for (int i = 0; i < firstFileCopyLength; i++) {
        decompFM.writeByte(arcFM.readByte());
      }
      arcFM.close();

      // copy the second file into it
      arcFM = new FileManipulator(archive2, false);
      //arcFM.seek(offset); // starts at 0 on the second file
      for (int i = 0; i < secondFileCopyLength; i++) {
        decompFM.writeByte(arcFM.readByte());
      }
      arcFM.close();

      decompFM.close();

      return decompFile;
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

      ExporterPlugin exporterZlib = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      // Find the "0A" file, which contains the directory
      File dirFile = new File(path.getParent() + File.separatorChar + "0A");
      if (!dirFile.exists() || !dirFile.isFile()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(dirFile, false);

      // 4 - Unknown
      // 4 - Padding Multiple? (2048)
      fm.skip(8);

      // 4 - Number of Archive Files
      int numArchives = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkRange(numArchives, 1, 20);

      // 8 - Number of Files
      fm.skip(4);
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 5 - null
      fm.skip(5);

      File[] archiveFiles = new File[numArchives];
      long[] archiveLengths = new long[numArchives];
      long[] startOffsets = new long[numArchives];
      long totalLength = 0;
      String basePath = path.getParent() + File.separatorChar;

      // ARCHIVE DIRECTORY
      for (int i = 0; i < numArchives; i++) {
        // 8 - Archive File Length [<<3] (except the last archive file which has a smaller size here, but the archive file is padded larger)
        fm.skip(8);

        // 64 - Archive Filename
        String arcFilename = fm.readNullUnicodeString(32);

        File archiveFile = new File(basePath + arcFilename);
        if (!archiveFile.exists()) {
          ErrorLogger.log("[NoTex_6] Archive file " + arcFilename + " not found");
          return null;
        }
        archiveFiles[i] = archiveFile;
        archiveLengths[i] = archiveFile.length();
        startOffsets[i] = totalLength;
        totalLength += archiveFile.length();
      }

      // 3 - null
      fm.skip(3);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length (not including padding)
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, totalLength);

        // 4 - File Hash?
        fm.skip(4);

        // 4 - File Offset [*2048]
        long offset = ((long) IntConverter.changeFormat(fm.readInt())) * 2048;
        FieldValidator.checkOffset(offset, totalLength);

        // 4 - null
        fm.skip(4);

        // X - Filename (null)
        String filename = Resource.generateFilename(i);

        // work out what archive it's in
        File archiveFile = null;
        int archiveNum = 0;
        for (int a = numArchives - 1; a >= 0; a--) {
          if (startOffsets[a] < offset) {
            // found the archive
            archiveFile = archiveFiles[a];
            archiveNum = a;
            break;
          }
        }
        if (archiveFile == null) {
          ErrorLogger.log("[NoTex_6] No matching archive file for offset " + offset);
          return null;
        }

        // work out if the file is split across files or not
        offset -= startOffsets[archiveNum];
        if (offset + length > archiveLengths[archiveNum]) {
          //System.out.println("Split" + length);
          // Extract the split file out to a temporary location, and reference that here. We do it this way
          // so then we can run ZLib on it or other things without worrying about where the split is.
          TaskProgressManager.setMessage(Language.get("Progress_SplittingArchive"));
          archiveFile = extractSplitFile(archiveFiles[archiveNum], archiveFiles[archiveNum + 1], offset, length, archiveNum);
          TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive"));
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(archiveFile, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);

      }

      // Now lets read each file, work out what kind of file they are, whether they're compressed, etc.
      File currentArcFile = null;

      boolean debug = Settings.getBoolean("DebugMode");

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        File arcFile = resource.getSource();

        if (arcFile != currentArcFile) {
          fm.close();
          fm = new FileManipulator(arcFile, false, 256);// small quick reads, but should cover normal use cases
          currentArcFile = arcFile;
        }

        fm.relativeSeek(resource.getOffset());

        // 4 - Entry Type
        int entryType = IntConverter.changeFormat(fm.readInt());

        String extension = "." + entryType;
        if (entryType == -258453456) {
          // CDF Entry
          extension = ".cdf";
        }
        else if (entryType == -461827577) {
          // Sub Directory?
          extension = ".filelist";
        }
        else if (entryType == -12849260) {
          // Compressed File Data Entry
          if (debug) {
            extension = ".compFile";
          }

          long length = resource.getLength();

          // 4 - Compressed File Data Offset (relative to the start of this entry)
          int relOffset = IntConverter.changeFormat(fm.readInt()) - 8;
          FieldValidator.checkOffset(relOffset, length);
          length -= (relOffset + 8);

          relOffset += (int) fm.getOffset();

          int maxNumBlocks = 100; // guess

          fm.relativeSeek(relOffset);

          long arcSize = fm.getLength();

          long[] blockOffsets = new long[maxNumBlocks];
          long[] blockLengths = new long[maxNumBlocks];
          long[] blockDecompLengths = new long[maxNumBlocks];
          long totalDecompLength = 0;

          // for each compressed block
          int numBlocks = 0;
          for (int b = 0; b < maxNumBlocks; b++) {
            //   4 - Compression Header (ZLIB)
            String compHeader = fm.readString(4);
            if (!compHeader.equals("ZLIB")) {
              // EOF
              numBlocks = b;
              break;
            }

            //   4 - Decompressed Length
            int decompLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(decompLength);

            totalDecompLength += decompLength;

            //   4 - Compressed Length (including these header fields)
            int compLength = IntConverter.changeFormat(fm.readInt()) - 16;
            FieldValidator.checkLength(compLength, length);

            //   4 - Unknown (5/6)
            fm.skip(4);

            //   X - Compressed File Data (ZLib Compression)
            long blockOffset = fm.getOffset();
            FieldValidator.checkOffset(blockOffset, arcSize);
            fm.skip(compLength);

            blockOffsets[b] = blockOffset;
            blockLengths[b] = compLength;
            blockDecompLengths[b] = decompLength;
          }

          if (numBlocks < maxNumBlocks) {
            long[] tempArray = blockOffsets;
            blockOffsets = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockOffsets, 0, numBlocks);

            tempArray = blockLengths;
            blockLengths = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockLengths, 0, numBlocks);

            tempArray = blockDecompLengths;
            blockDecompLengths = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockDecompLengths, 0, numBlocks);
          }

          resource.setDecompressedLength(totalDecompLength);

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterZlib, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(blockExporter);

        }
        else if (entryType == -544881202) {
          // Raw File Data Entry
          extension = ".dram";
        }
        else if (entryType == 1514948930) {
          // Compressed File Data Entry (without headers)
          extension = ".image";

          long length = resource.getLength();

          int maxNumBlocks = 100; // guess

          long arcSize = fm.getLength();

          long[] blockOffsets = new long[maxNumBlocks];
          long[] blockLengths = new long[maxNumBlocks];
          long[] blockDecompLengths = new long[maxNumBlocks];
          long totalDecompLength = 0;

          // for each compressed block
          int numBlocks = 0;
          String compHeader = "ZLIB"; // we've read the first entry already
          for (int b = 0; b < maxNumBlocks; b++) {
            //   4 - Decompressed Length
            int decompLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(decompLength);

            totalDecompLength += decompLength;

            //   4 - Compressed Length (including these header fields)
            int compLength = IntConverter.changeFormat(fm.readInt()) - 16;
            FieldValidator.checkLength(compLength, length);

            //   4 - Unknown (5/6)
            fm.skip(4);

            //   X - Compressed File Data (ZLib Compression)
            long blockOffset = fm.getOffset();
            FieldValidator.checkOffset(blockOffset, arcSize);
            fm.skip(compLength);

            blockOffsets[b] = blockOffset;
            blockLengths[b] = compLength;
            blockDecompLengths[b] = decompLength;

            //   4 - Compression Header (ZLIB)
            compHeader = fm.readString(4);
            if (!compHeader.equals("ZLIB")) {
              // EOF
              numBlocks = b + 1;
              break;
            }
          }

          if (numBlocks < maxNumBlocks) {
            long[] tempArray = blockOffsets;
            blockOffsets = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockOffsets, 0, numBlocks);

            tempArray = blockLengths;
            blockLengths = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockLengths, 0, numBlocks);

            tempArray = blockDecompLengths;
            blockDecompLengths = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockDecompLengths, 0, numBlocks);
          }

          resource.setDecompressedLength(totalDecompLength);

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterZlib, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(blockExporter);
        }
        else if (entryType == 1) {
          // Compressed File Data Entry (Integrated Header)
          if (debug) {
            extension = ".compFileIntegrated";
          }

          long length = resource.getLength();

          int maxNumBlocks = 100; // guess

          long arcSize = fm.getLength();

          long[] blockOffsets = new long[maxNumBlocks];
          long[] blockLengths = new long[maxNumBlocks];
          long[] blockDecompLengths = new long[maxNumBlocks];
          long totalDecompLength = 0;

          // for each compressed block
          int numBlocks = 0;

          for (int b = 0; b < maxNumBlocks; b++) {
            // 4 - Unknown (1)
            // 4 - Unknown (5)
            // 4 - Unknown (2/5)
            // 4 - Unknown
            // 4 - Unknown
            // 4 - null
            // 4 - Unknown
            // 4 - Unknown (30)
            // 4 - Unknown
            // 4 - Unknown
            // 4 - null
            if (b == 0) {
              fm.skip(40); // NOTE: already read it for the first entry
            }
            else {
              fm.skip(44);
            }

            //   4 - Compression Header (ZLIB)
            String compHeader = fm.readString(4);
            if (!compHeader.equals("ZLIB")) {
              // EOF
              numBlocks = b;
              break;
            }

            //   4 - Decompressed Length
            int decompLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(decompLength);

            totalDecompLength += decompLength;

            //   4 - Compressed Length (including these header fields)
            int compLength = IntConverter.changeFormat(fm.readInt()) - 16;
            FieldValidator.checkLength(compLength, length);

            //   4 - Unknown (5/6)
            fm.skip(4);

            //   X - Compressed File Data (ZLib Compression)
            long blockOffset = fm.getOffset();
            FieldValidator.checkOffset(blockOffset, arcSize);
            fm.skip(compLength);

            blockOffsets[b] = blockOffset;
            blockLengths[b] = compLength;
            blockDecompLengths[b] = decompLength;
          }

          if (numBlocks < maxNumBlocks) {
            long[] tempArray = blockOffsets;
            blockOffsets = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockOffsets, 0, numBlocks);

            tempArray = blockLengths;
            blockLengths = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockLengths, 0, numBlocks);

            tempArray = blockDecompLengths;
            blockDecompLengths = new long[numBlocks];
            System.arraycopy(tempArray, 0, blockDecompLengths, 0, numBlocks);
          }

          resource.setDecompressedLength(totalDecompLength);

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterZlib, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(blockExporter);
        }

        String filename = resource.getName() + extension;
        resource.setName(filename);
        resource.setOriginalName(filename);

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
