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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.QuickBMSHelper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.BlockQuickBMSExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_BEGINAPPIDSTRING extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_BEGINAPPIDSTRING() {

    super("DAT_BEGINAPPIDSTRING", "DAT_BEGINAPPIDSTRING");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("LEGO Batman 2",
        "LEGO Batman 3",
        "LEGO Indiana Jones",
        "LEGO Lord of the Rings",
        "LEGO Pirates of the Caribbean",
        "LEGO The Hobbit");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ai2", "AI Level Path", FileType.TYPE_OTHER),
        new FileType("an3", "Animation", FileType.TYPE_OTHER),
        new FileType("ani", "Animation", FileType.TYPE_OTHER),
        new FileType("anm", "Animation", FileType.TYPE_OTHER),
        new FileType("cd", "Character Definition", FileType.TYPE_OTHER),
        new FileType("cft", "Font", FileType.TYPE_OTHER),
        new FileType("cu2", "Cutscene", FileType.TYPE_VIDEO),
        new FileType("fnt", "Font", FileType.TYPE_OTHER),
        new FileType("ft2", "Font", FileType.TYPE_OTHER),
        new FileType("git", "Git Options", FileType.TYPE_DOCUMENT),
        new FileType("giz", "Giz Obstacle", FileType.TYPE_OTHER),
        new FileType("gsc", "GSC Image Archive", FileType.TYPE_ARCHIVE),
        new FileType("par", "Part", FileType.TYPE_OTHER),
        new FileType("scp", "Script File", FileType.TYPE_DOCUMENT),
        new FileType("sf", "Script File", FileType.TYPE_DOCUMENT),
        new FileType("spl", "Spline", FileType.TYPE_OTHER),
        new FileType("sub", "Subtitles", FileType.TYPE_DOCUMENT),
        new FileType("subopt", "Subtitles", FileType.TYPE_DOCUMENT),
        new FileType("txm", "Minikit", FileType.TYPE_DOCUMENT),
        new FileType("txc", "Collectable", FileType.TYPE_DOCUMENT),
        new FileType("tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("fmv", "FMV Video", FileType.TYPE_VIDEO));

    setTextPreviewExtensions("ats", "git", "gix", "h", "scp"); // LOWER CASE

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

      long arcSize = fm.getLength();

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      if ((dirOffset & 0x80000000) == 0x80000000) {
        dirOffset = ((dirOffset ^ 0xffffffff) << 8) + 256;
      }
      if (FieldValidator.checkOffset(dirOffset, arcSize)) {
        rating += 5;
      }

      // 4 - Directory Length
      int dirLength = fm.readInt();
      if (FieldValidator.checkLength(dirLength, arcSize)) {
        rating += 5;
      }

      // additional check, to hopefully match more lego games
      if (dirOffset + dirLength == arcSize) {
        rating += 15; // should be pretty unique to lego games and not much else
      }

      // X - App ID String (BEGIN_APP_ID_STRINGPakDat v1.01END_APP_ID_STRING)
      if (fm.readString(19).equals("BEGIN_APP_ID_STRING")) {
        rating += 50;
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
  public FileManipulator decompressArchive(FileManipulator fm, long startOffset, long endOffset) {
    return fm;
    /* // NOT USED YET
    try {
      long arcSize = fm.getLength();
    
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
      String decompFilename = decompFile.getName();
    
      //File tempPath = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());
      //tempPath = FilenameChecker.correctFilename(tempPath); // removes funny characters etc.
    
      if (QuickBMSHelper.checkAndShowPopup() == null) {
        return fm; // don't have QuickBMS, and didn't install it, so return the currently-opened file
      }
    
      //
      // 1. Need to get all the compressed and decompressed sizes from the archive, then make a QuickBMS script to unpack it
      //
      fm.relativeSeek(startOffset);
    
      File scriptFile = new File(new File(new File(Settings.get("TempDirectory")).getAbsolutePath()) + File.separator + "ge_quickbms_extract_" + System.currentTimeMillis() + ".bms");
      scriptFile = FilenameChecker.correctFilename(scriptFile); // removes funny characters etc.
      FileManipulator scriptFM = new FileManipulator(scriptFile, true);
    
      scriptFM.writeString("append\n"); // turn on APPEND mode
    
      while (fm.getOffset() < endOffset) {
        // 4 - Compression Type (LZ2K)
        String compressionType = fm.readString(4);
    
        // 4 - Decompressed Block Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);
    
        // 4 - Compressed Block Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
    
        // X - Compressed File Data
        long offset = fm.getOffset();
        fm.skip(length);
    
        System.out.println(compressionType + "\t" + offset + "\t" + length + "\t" + decompLength);
    
        scriptFM.writeString("comtype " + compressionType + "\n");
        if (length == decompLength) {
          // raw data
          scriptFM.writeString("log \"" + decompFilename + "\" " + offset + " " + length + "\n");
        }
        else {
          // compressed data
          scriptFM.writeString("clog \"" + decompFilename + "\" " + offset + " " + length + " " + decompLength + "\n");
        }
    
        if (decompLength < 32768) {
          // this is the last block in this group, need to pad it out to a multiple or 512 bytes, then continue for the next group.
          //fm.skip(calculatePadding(fm.getOffset(), 512));
          endOffset = 0;
        }
      }
    
      scriptFM.writeString("append\n"); // turn off APPEND mode
    
      scriptFM.close();
    
      String scriptPath = scriptFile.getAbsolutePath();
    
      //
      // 2. Decompress the file, following the script
      //
    
      // get the QuickBMS Executable
      String quickbmsPath = QuickBMSHelper.getExternalLibraryPath();
      if (quickbmsPath == null) {
        // quickbms wasn't found
        return null;
      }
    
      // Build a script for doing the decompression
      if (scriptPath == null || !(new File(scriptPath).exists())) {
        // problem reading the script file
        return null;
      }
    
      ProcessBuilder pb = new ProcessBuilder(quickbmsPath, "-o", "-Q", scriptPath, fm.getFile().getAbsolutePath(), pathOnly);
    
      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_DecompressingFiles"));
      TaskProgressManager.setIndeterminate(true);
    
      // Start the task
      TaskProgressManager.startTask();
    
      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for QuickBMS to finish
    
      // Stop the task
      TaskProgressManager.stopTask();
    
      if (returnCode != 0) {
        // failed decompression
        ErrorLogger.log("[DAT_BEGINAPPIDSTRING] Archive decompression failed");
        return fm;
      }
    
      if (!decompFile.exists()) {
        // probably a decompression failure
        return fm;
      }
    
      FileManipulator decompFM = new FileManipulator(decompFile, false);
    
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
    */
  }

  /** older archives use name entries of length 8, newer ones are length 12. This is here to support Replacing to the right size. **/
  int nameEntrySize = 12;

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

      if (QuickBMSHelper.checkAndShowPopup() == null) {
        return null;
      }

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      //   4 - Directory Offset
      int dirOffsetRaw = fm.readInt();
      if ((dirOffsetRaw & 0x80000000) == 0x80000000) {
        dirOffsetRaw = ((dirOffsetRaw ^ 0xffffffff) << 8) + 256;
      }
      long dirOffset = IntConverter.unsign(dirOffsetRaw);
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      /*
      // 4 - Footer Length
      // X - App ID String (BEGIN_APP_ID_STRINGPakDat v1.01END_APP_ID_STRING)
      // 1 - null App ID String Terminator
      // X - Unknown
      // X - null Padding to offset 512
      
      fm.getBuffer().setBufferSize(12); // quick reading of block headers
      fm.seek(512);
      
      // 4 - Compression Type (LZ2K)
      String compressionType = fm.readString(4);
      if (compressionType.equals("LZ2K")) {
        fm.relativeSeek(512); // back to the start of the compression header
        FileManipulator decompFM = decompressArchive(fm, 512, footerOffset);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the start of the decompressed file
      
          path = fm.getFile(); // So the resources are stored against the decompressed file
        }
      }
      */

      // 4 - Unknown (-5)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset [*256] [+FileOffsetAdditionalValue]
        int offset = (fm.readInt() << 8);
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 3 - Unknown
        fm.skip(3);

        // 1 - File Offset Additional Value
        int additionalOffset = ByteConverter.unsign(fm.readByte());
        offset += additionalOffset;

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        if (length == decompLength) {
          // no compression
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          // lots of compressed blocks
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // 4 - Number of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // skip over the name directory to get the actual names first
      long nameDirOffset = fm.getOffset();
      fm.skip(numNames * 12);

      // 4 - Names Directory Length
      int nameDirLength = fm.readInt();

      nameEntrySize = 12;
      try {
        FieldValidator.checkLength(nameDirLength, arcSize);
      }
      catch (Throwable t) {
        // this was for name entries of length 12 (newer games - eg Hobbit, LotR).
        // try again for name entries of length 8 (older games - eg Indiana Jones).

        nameEntrySize = 8;

        fm.seek(nameDirOffset + (numNames * 8));

        // 4 - Names Directory Length
        nameDirLength = fm.readInt();
        FieldValidator.checkLength(nameDirLength, arcSize);
      }

      // read the names into memory
      byte[] nameBytes = fm.readBytes(nameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      // Now read the name CRC directory
      int[] crcs = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename CRC
        crcs[i] = fm.readInt();
      }

      // go back and process the names directory (and build the actual filenames)
      fm.seek(nameDirOffset);

      /*
      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // 2 - Next Name ID
        short nextID = fm.readShort();
      
        // 2 - Parent Name ID
        short parentID = fm.readShort();
      
        // 4 - Name Offset (relative to the start of the Names Directory)
        int nameOffset = fm.readInt();
        FieldValidator.checkOffset(nameOffset, nameDirLength);
      
        // 4 - Unknown
        fm.skip(4);
      
        // get the actual name
        nameFM.relativeSeek(nameOffset);
        String name = nameFM.readNullString();
      
        // work out the parents, set the directory name for this file
        String parentName = null;
        if (parentID != 0) {
          parentName = names[parentID];
          name = parentName + name;
        }
      
        if (nextID > 0) {
          // a folder
          name += "\\";
        }
      
        names[i] = name;
        System.out.println(name);
      }
      */

      // process the names directory, noting that the outer loop is for the number of FILES, and there's an inner loop as well. Complicated logic.
      String[] names = new String[numNames];
      int currentName = 0;
      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        short nextID = 999; // dummy to start the loop
        String fullPath = "";
        String name = "";

        while (nextID > 0) {
          // 2 - Next Name ID
          nextID = fm.readShort();

          // 2 - Previous Name ID
          short previousID = fm.readShort();

          // 4 - Name Offset (relative to the start of the Names Directory)
          int nameOffset = fm.readInt();
          if (nameOffset >= 0) { // want to allow negatives, just set their names to "". Want to still fail if the offset is too long
            FieldValidator.checkOffset(nameOffset, nameDirLength);
          }

          if (nameEntrySize == 12) {
            // 4 - Unknown
            fm.skip(4);
          }

          // get the actual name
          if (nameOffset >= 0) {
            nameFM.relativeSeek(nameOffset);
            name = nameFM.readNullString();
          }
          else {
            name = "";
          }

          // now do some processing for the parents
          //String fullPath = "";
          if (previousID != 0) {
            fullPath = names[previousID];
          }

          names[currentName] = fullPath;

          if (nextID > 0) { // a folder
            String tempParentName = names[previousID];
            if (!tempParentName.equals("")) {
              String oldName = "\\" + tempParentName + "\\";
              int oldNamePos = fullPath.lastIndexOf(oldName);
              if (oldNamePos >= 0) {
                fullPath = fullPath.substring(0, oldNamePos);
              }
            }
            if (!name.equals("")) {
              fullPath += name + "\\";
            }

          }
          currentName++;
        }

        String actualName = fullPath + name;

        // now work out the CRC of the name, so we know which file this name belongs to
        actualName = actualName.toUpperCase();
        int nameLength = actualName.length();

        int crc = 0x811c9dc5;
        for (int j = 0; j < nameLength; j++) {
          byte character = (byte) actualName.charAt(j);
          crc ^= character;
          crc *= 0x199933;
        }

        // now see if we can find the matching CRC
        int nameIndex = -1;
        for (int j = 0; j < numFiles; j++) {
          if (crc == crcs[j]) {
            // found it
            nameIndex = j;
            break;
          }
        }

        if (nameIndex == -1) {
          // not found
          ErrorLogger.log("[DAT_BEGINAPPIDSTRING]: Name CRC not found: " + crc);
        }
        else {
          Resource resource = resources[nameIndex];
          resource.setName(actualName);
          resource.setOriginalName(actualName);
        }

      }

      nameFM.close();

      // now for each compressed file, we need to find the compressed blocks
      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        Resource resource = resources[i];
        int length = (int) resource.getLength();
        int decompLength = (int) resource.getDecompressedLength();
        if (length != decompLength) {

          fm.seek(resource.getOffset());

          int numBlocks = (decompLength / 16384) + 1; // guess (older games use block size 16384, newer use 32768)
          int realNumBlocks = 0;

          String[] compressionTypes = new String[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockOffsets = new long[numBlocks];

          for (int b = 0; b < numBlocks; b++) {
            // 4 - Compression Type (LZ2K)
            String compressionType = fm.readString(4);
            compressionTypes[b] = compressionType;

            // 4 - Decompressed Block Length
            int blockDecompLength = fm.readInt();
            FieldValidator.checkLength(blockDecompLength);
            blockDecompLengths[b] = blockDecompLength;

            // 4 - Compressed Block Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, arcSize);
            blockLengths[b] = blockLength;

            // X - Compressed File Data
            long blockOffset = fm.getOffset();
            blockOffsets[b] = blockOffset;

            fm.skip(blockLength);

            realNumBlocks++;

            length -= (12 + blockLength);
            if (length <= 0) {
              // finished reading the compressed blocks - return
              break;
            }

          }

          // if we originally allocated too many blocks, resize the arrays
          if (realNumBlocks != numBlocks) {
            String[] oldCompressionTypes = compressionTypes;
            long[] oldBlockDecompLengths = blockDecompLengths;
            long[] oldBlockLengths = blockLengths;
            long[] oldBlockOffsets = blockOffsets;

            compressionTypes = new String[realNumBlocks];
            blockDecompLengths = new long[realNumBlocks];
            blockLengths = new long[realNumBlocks];
            blockOffsets = new long[realNumBlocks];

            System.arraycopy(oldCompressionTypes, 0, compressionTypes, 0, realNumBlocks);
            System.arraycopy(oldBlockDecompLengths, 0, blockDecompLengths, 0, realNumBlocks);
            System.arraycopy(oldBlockLengths, 0, blockLengths, 0, realNumBlocks);
            System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, realNumBlocks);
          }

          // This is a special exporter that will generate a script to append each block to the same single output file
          BlockQuickBMSExporterWrapper exportWrapper = new BlockQuickBMSExporterWrapper(compressionTypes, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(exportWrapper);
        }
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
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
    if (extension.equalsIgnoreCase("csv") || extension.equalsIgnoreCase("binary") || extension.equalsIgnoreCase("sf") || extension.equalsIgnoreCase("sub") || extension.equalsIgnoreCase("subopt") || extension.equalsIgnoreCase("txc") || extension.equalsIgnoreCase("txm")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Work out where the first file is, so we know how much to copy at the beginning
      int firstOffset = Integer.MAX_VALUE;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (!resource.isReplaced()) {
          int offset = (int) resource.getOffset();
          if (offset < firstOffset) {
            firstOffset = offset;
          }
        }
      }

      // Write Header Data

      // 4 - Directory Offset
      int srcDirOffset = src.readInt();
      fm.writeInt(0); // dummy for now - will set it correctly later on

      // 4 - Directory Length
      // X - App ID String (BEGIN_APP_ID_STRINGPakDat v1.01END_APP_ID_STRING)
      // 1 - null App ID String Terminator
      // X - Unknown
      // 0-255 - null Padding to a multiple of 256 bytes
      fm.writeBytes(src.readBytes(firstOffset - 4));

      // Write Files (compressed files will stay compressed, replaced files will come in as raw data)
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      Exporter_Default exporterDefault = Exporter_Default.getInstance();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        ExporterPlugin realExporter = resource.getExporter();
        resource.setExporter(exporterDefault);

        // X - File Data
        write(resource, fm);

        resource.setExporter(realExporter);

        // 0-511 - null Padding to a multiple of 512 bytes
        int length = (int) fm.getLength();
        int padding = calculatePadding(length, 512);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      src.seek(srcDirOffset);

      long dirOffset = fm.getOffset();

      // 4 - Unknown (-5)
      // 4 - Number of Files
      fm.writeBytes(src.readBytes(8));

      int offset = firstOffset;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getLength();
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset [*256] [+FileOffsetAdditionalValue]
        fm.writeInt(offset >> 8);

        // 4 - Compressed Length
        fm.writeInt(length);

        // 4 - Decompressed Length
        fm.writeInt(decompLength);

        // 3 - Unknown
        // 1 - File Offset Additional Value
        fm.writeInt(0);

        offset += length;
        offset += calculatePadding(length, 512);
      }

      src.skip(numFiles * 16);

      // 4 - Number of Names
      int numNames = src.readInt();
      fm.writeInt(numNames);

      // NAME DETAILS DIRECTORY
      fm.writeBytes(src.readBytes(numNames * nameEntrySize));

      // 4 - Names Directory Length
      int nameDirLength = src.readInt();
      fm.writeInt(nameDirLength);

      // NAMES DIRECTORY
      fm.writeBytes(src.readBytes(nameDirLength));

      // FILENAME CRC DIRECTORY
      fm.writeBytes(src.readBytes(numFiles * 4));

      // 8 - null
      fm.writeLong(0);

      int dirLength = (int) (fm.getLength() - dirOffset);

      // Now after all this, go back and write the header details
      fm.seek(0);

      // 4 - Directory Offset
      fm.writeInt(dirOffset);

      // 4 - Directory Length
      fm.writeInt(dirLength);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
