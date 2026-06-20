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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CAS() {

    super("CAS", "CAS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dragon Age: Inquisition",
        "Battlefield 3");
    setExtensions("cas"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      byte[] headerBytes = fm.readBytes(4);
      if (ByteConverter.unsign(headerBytes[0]) == 250 && ByteConverter.unsign(headerBytes[1]) == 206 && ByteConverter.unsign(headerBytes[2]) == 15 && ByteConverter.unsign(headerBytes[3]) == 240) {
        rating += 50;
      }

      String filePath = fm.getFile().getAbsolutePath();
      int underscorePos = filePath.lastIndexOf('_');
      if (underscorePos > 0) {
        filePath = filePath.substring(0, underscorePos) + ".cat";
        if (new File(filePath).exists()) {
          rating += 25;
        }
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // GET THE DIRECTORY FILE
      File sourcePath = null;

      String filePath = path.getAbsolutePath();
      String basePath = "";
      int underscorePos = filePath.lastIndexOf('_');
      if (underscorePos > 0) {
        basePath = filePath.substring(0, underscorePos);
        filePath = basePath + ".cat";
        basePath += "_";
        sourcePath = new File(filePath);
      }

      if (sourcePath == null || !sourcePath.exists()) {
        return null;
      }

      long arcSize = 0;

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 16 - Header (NyanNyanNyanNyan)
      fm.skip(16);

      int numFiles = (int) ((fm.getLength() - 16) / 32);
      FieldValidator.checkNumFiles(numFiles / 10); // /10 because this archive covers LOTS of archives, so there can be many thousands of files!

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int previousCasNumber = -1;
      for (int i = 0; i < numFiles; i++) {
        // 20 - Hash?
        fm.skip(20);

        // 4 - File Offset (Relative to the start of the referenced ##.cas file. Points to the offset after the "null" in the CAS file)
        int offset = fm.readInt();

        // 4 - File Length
        int length = fm.readInt();

        // 4 - CAS File Number (1/2/3...)
        int casNumber = fm.readInt();
        FieldValidator.checkPositive(casNumber);

        if (casNumber != previousCasNumber) {
          String zeroPadding = "";
          if (casNumber < 10) {
            zeroPadding = "0";
          }

          // Find the kit file - ensure it exists
          path = new File(basePath + zeroPadding + casNumber + ".cas");
          if (!path.exists()) {
            ErrorLogger.log("[CAS]: Missing CAS file number " + casNumber);

            // break early, only return the files we were able to process
            numFiles = i;
            resources = resizeResources(resources, numFiles);

            TaskProgressManager.setMaximum(numFiles);

            break;

            //return null;
          }

          // get the length of the cas file, for field validation
          arcSize = path.length();

          previousCasNumber = casNumber; // so we can re-use this for the next file
        }

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      // Now open each CAS, read through the files and work out if they're compressed or not.
      /*
      ExporterPlugin exporterLZ4 = Exporter_LZ4.getInstance();
      ExporterPlugin exporterOodle = Exporter_Oodle.getInstance();
      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();
      ExporterPlugin exporterZstd = Exporter_ZStd.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      */

      String previousCasPath = null;
      for (int i = 0; i < numFiles; i++) {
        //System.out.println("file " + i + " of " + numFiles);

        Resource resource = resources[i];

        File casFile = resource.getSource();
        if (casFile.getAbsolutePath() != previousCasPath) {
          // changing the CAS file, open the next one.
          fm.close();
          fm = new FileManipulator(casFile, false, 8); // small quick reads
        }

        fm.seek(resource.getOffset());

        /*
        
        // this was from a BMS script, not really sure if it's applicable, as I can't see any other compressions in Battlefield 3, only ZLib 
         
        // 4 - Uncompression Header? (206,209,178,15)
        byte[] headerBytes = fm.readBytes(4);
        if (ByteConverter.unsign(headerBytes[0]) == 206 && ByteConverter.unsign(headerBytes[1]) == 209 && ByteConverter.unsign(headerBytes[2]) == 178 && ByteConverter.unsign(headerBytes[3]) == 15) {
          // uncompressed file
          System.out.println("  uncompressed. Start at " + resource.getOffset());
        
          // X - File Data (no compression)
          resource.setOffset(fm.getOffset());
        
          long length = resource.getDecompressedLength() - 4;
          resource.setLength(length);
          resource.setDecompressedLength(length);
        }
        else {
          // compressed in blocks
          fm.relativeSeek(resource.getOffset());
        
          int maxBlocks = (int) (resource.getLength() / 1000); // estimate
          if (maxBlocks < 50) {
            maxBlocks = (maxBlocks + 1) * 10;
          }
        
          System.out.println("  compressed - estimating " + maxBlocks + " blocks. Start at " + resource.getOffset() + ". Length = " + resource.getLength());
        
          long[] blockOffsets = new long[maxBlocks];
          long[] blockLengths = new long[maxBlocks];
          long[] blockDecompLengths = new long[maxBlocks];
          ExporterPlugin[] blockExporters = new ExporterPlugin[maxBlocks];
        
          long totalDecompLength = 0;
        
          long totalCompLength = resource.getLength();
        
          long endOffset = resource.getOffset() + totalCompLength;
          int numBlocks = 0;
        
          boolean earlyBreak = false;
          while (fm.getOffset() < endOffset) {
        
            // 1 - Need Dictionary
            // 3 - Decompressed Length
            byte[] decompLengthBytes = fm.readBytes(4);
            int needDictionary = decompLengthBytes[0];
            decompLengthBytes[0] = 0;
            int blockDecompLength = IntConverter.convertBig(decompLengthBytes);
        
            // 2 - Flags
            int flags = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));
        
            // 2 - Compressed Length
            int blockLength = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));
            blockLength = ((flags & 15) << 16) | blockLength;
        
            if (blockLength > totalCompLength || blockLength < 0) {
              // just treat as an uncompressed file
              earlyBreak = true;
              break;
            }
        
            flags >>= 8; // remove the 0x70 part
        
            if (flags != 0) {
        
              if (flags == 0xf) {
                if (needDictionary != 0) {
                  System.out.println("          need dictionary");
                }
                //if NEED_DICTIONARY == 0
                System.out.println("      zstd");
                blockExporters[numBlocks] = exporterZstd;
                //else
                //    comtype zstd DICTIONARY DICTIONARY_SIZE
                //endif
              }
              else if (flags == 0x15) {
                blockExporters[numBlocks] = exporterOodle;
                System.out.println("      oodle");
              }
              else if (flags == 0x9) {
                if (needDictionary != 0) {
                  System.out.println("          need dictionary");
                }
        
                //if NEED_DICTIONARY == 0
                System.out.println("      lz4");
                blockExporters[numBlocks] = exporterLZ4;
                //else
                //    comtype lz4 DICTIONARY DICTIONARY_SIZE
                //endif
              }
              else {
                if (needDictionary != 0) {
                  System.out.println("          need dictionary");
                }
                //if NEED_DICTIONARY == 0
                blockExporters[numBlocks] = exporterZLib;
                //else
                //    comtype zlib DICTIONARY DICTIONARY_SIZE
                //endif
              }
              //clog MEMORY_FILE2 CHUNK_OFF CHUNK_ZSIZE CHUNK_SIZE MEMORY_FILE3
            }
            else {
              //log MEMORY_FILE2 CHUNK_OFF CHUNK_ZSIZE MEMORY_FILE3
              //blockExporters[numBlocks] = exporterDefault;
              blockExporters[numBlocks] = exporterZLib;
            }
        
            System.out.println("    " + blockLength + " at " + (fm.getOffset() - 8) + " decompressing to " + blockDecompLength);
        
            // X - File Data (Compressed)
            blockOffsets[numBlocks] = fm.getOffset();
            blockLengths[numBlocks] = blockLength;
            blockDecompLengths[numBlocks] = blockDecompLength;
            numBlocks++;
        
            totalDecompLength += blockDecompLength;
        
            fm.skip(blockLength);
          }
        
          if (earlyBreak) {
            continue;
          }
        
          if (numBlocks < maxBlocks) {
            // resize the arrays
        
            long[] oldBlockOffsets = blockOffsets;
            long[] oldBlockLengths = blockLengths;
            long[] oldBlockDecompLengths = blockDecompLengths;
            ExporterPlugin[] oldBlockExporters = blockExporters;
        
            blockOffsets = new long[numBlocks];
            blockLengths = new long[numBlocks];
            blockDecompLengths = new long[numBlocks];
            blockExporters = new ExporterPlugin[numBlocks];
        
            System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, numBlocks);
            System.arraycopy(oldBlockLengths, 0, blockLengths, 0, numBlocks);
            System.arraycopy(oldBlockDecompLengths, 0, blockDecompLengths, 0, numBlocks);
            System.arraycopy(oldBlockExporters, 0, blockExporters, 0, numBlocks);
          }
        
          BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);
        
          resource.setDecompressedLength(totalDecompLength);
          resource.setExporter(blockExporter);
        }
        */

        // Compression check
        // 4 - Decompressed Block Length (BIG)
        int testBlockDecompLength = IntConverter.changeFormat(fm.readInt());

        // 4 - Compressed Block Length (BIG)
        int testBlockLength = IntConverter.changeFormat(fm.readInt());

        // X - File Data (ZLib Compression)
        int zlibHeaderByte = fm.readByte();

        // back to the start of the file
        fm.relativeSeek(resource.getOffset());

        int totalCompLength = (int) resource.getLength();

        if (zlibHeaderByte == 120 && (testBlockLength < totalCompLength && testBlockLength >= 0) && (testBlockDecompLength >= 0)) {
          // compressed

          int maxBlocks = (int) (resource.getLength() / 1000); // estimate
          if (maxBlocks < 50) {
            maxBlocks = 50;
          }

          //System.out.println("  compressed - estimating " + maxBlocks + " blocks. Start at " + resource.getOffset() + ". Length = " + resource.getLength());

          long[] blockOffsets = new long[maxBlocks];
          long[] blockLengths = new long[maxBlocks];
          long[] blockDecompLengths = new long[maxBlocks];

          long totalDecompLength = 0;

          long endOffset = resource.getOffset() + resource.getLength();
          int numBlocks = 0;
          while (fm.getOffset() < endOffset) {

            // 4 - Decompressed Block Length (BIG)
            int blockDecompLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(blockDecompLength);

            // 4 - Compressed Block Length (BIG)
            int blockLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(blockLength, arcSize);

            //System.out.println("    " + blockLength + " at " + (fm.getOffset() - 8));

            // X - File Data (ZLib Compression)
            blockOffsets[numBlocks] = fm.getOffset();
            blockLengths[numBlocks] = blockLength;
            blockDecompLengths[numBlocks] = blockDecompLength;
            numBlocks++;

            totalDecompLength += blockDecompLength;

            fm.skip(blockLength);
          }

          if (numBlocks < maxBlocks) {
            // resize the arrays

            long[] oldBlockOffsets = blockOffsets;
            long[] oldBlockLengths = blockLengths;
            long[] oldBlockDecompLengths = blockDecompLengths;

            blockOffsets = new long[numBlocks];
            blockLengths = new long[numBlocks];
            blockDecompLengths = new long[numBlocks];

            System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, numBlocks);
            System.arraycopy(oldBlockLengths, 0, blockLengths, 0, numBlocks);
            System.arraycopy(oldBlockDecompLengths, 0, blockDecompLengths, 0, numBlocks);
          }

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);

          resource.setDecompressedLength(totalDecompLength);
          resource.setExporter(blockExporter);
        }
        else {
          // uncompressed file

          // don't need to do anything here
        }

        TaskProgressManager.setValue(i);

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
