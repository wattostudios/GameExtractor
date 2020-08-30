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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_LZX_2;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XNB_XNB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XNB_XNB() {

    super("XNB_XNB", "Microsoft XNB Archive [XNB_XNB]");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Axiom Verge",
        "Carrion Reanimating",
        "Clover: A Curious Tale",
        "Core Fighter",
        "Fez",
        "Flotilla",
        "Hacknet",
        "Reus",
        "The Bridge");
    setExtensions("xnb"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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
  public FileManipulator decompressArchive(FileManipulator fm, int compLength, int decompLength) {
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

      long currentOffset = fm.getOffset();

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(decompLength); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_LZX_2 exporter = Exporter_LZX_2.getInstance();
      exporter.open(fm, compLength, decompLength);

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(currentOffset);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
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
      if (fm.readString(3).equals("XNB")) {
        rating += 50;
      }

      fm.skip(3);

      long arcSize = fm.getLength();

      // Archive Length
      if (fm.readInt() == arcSize) {
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
  @SuppressWarnings("unused")
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

      // 3 - Header (XNB)
      // 1 - Target Platform
      // 1 - Version
      fm.skip(5);

      // 1 - Flags
      int flags = ByteConverter.unsign(fm.readByte());
      if ((flags & 128) == 128) {
        // 4 - Compressed Length (including these header fields) (= Archive Length)
        int compLength = fm.readInt() - 14; // -14 for these header fields
        FieldValidator.checkLength(compLength, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // X - Compressed Data (LZX Compression)
        FileManipulator decompFM = decompressArchive(fm, compLength, decompLength);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the start of the file

          path = fm.getFile(); // So the resources are stored against the decompressed file
          arcSize = path.length();
        }
      }
      else {
        // 4 - Archive Length
        fm.skip(4);
      }

      // Now we've decompressed the archive (if needed) and we can start processing it...

      int typeReaderCount = get7BitEncodedInt(fm);

      // The first type reader is used for reading the primary asset
      String primaryTypeReaderName = getCSharpString(fm);
      int primaryTypeReaderVersion = fm.readInt();

      // Type reader names MIGHT contain assembly information
      int assemblyInformationIndex = primaryTypeReaderName.indexOf(',');
      if (assemblyInformationIndex != -1) {
        primaryTypeReaderName = primaryTypeReaderName.substring(0, assemblyInformationIndex);
      }

      String primaryType = primaryTypeReaderName.replace("Microsoft.Xna.Framework.Content.", "").replace("Reader", "");

      // Skip the remaining type readers, as all types are known
      for (int k = 1; k < typeReaderCount; ++k) {
        getCSharpString(fm);
        fm.readInt();
      }

      if (get7BitEncodedInt(fm) != 0) {
        throw new RuntimeException("[XNB_XNB] Shared resources are not supported");
      }

      if (get7BitEncodedInt(fm) != 1) {
        throw new RuntimeException("[XNB_XNB] Primary asset is invalid");
      }

      int numFiles = 1; // a single file

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        if (primaryType.equals("Texture2D")) {
          // Texture2D Image

          // 4 - Image Format (0=RGBA)
          int imageFormat = fm.readInt();

          // 4 - Image Width
          int imageWidth = fm.readInt();
          FieldValidator.checkWidth(imageWidth);

          // 4 - Image Height
          int imageHeight = fm.readInt();
          FieldValidator.checkHeight(imageHeight);

          // 4 - Mipmap Count
          fm.skip(4);

          // 4 - Image Data Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - Image Data
          long offset = fm.getOffset();

          String filename = Resource.generateFilename(i) + "." + primaryType;

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);

          resource.addProperty("Width", imageWidth);
          resource.addProperty("Height", imageHeight);
          if (imageFormat == 0) {
            resource.addProperty("ImageFormat", "RGBA");
          }
          else if (imageFormat == 1) {
            resource.addProperty("ImageFormat", "BGRA");
          }
          else if (imageFormat == 28) {
            resource.addProperty("ImageFormat", "DXT1");
          }
          else {
            resource.addProperty("ImageFormat", imageFormat);
          }

          resource.forceNotAdded(true);
          resources[i] = resource;

          TaskProgressManager.setValue(offset);
        }
        else if (primaryType.equals("SoundEffect")) {
          // SoundEffect Audio

          // 4 - Audio Format (18=WAV_RIFF)
          int audioFormat = fm.readInt();

          // 2 - Audio Codec (1 = WAV_RIFF)
          int codec = fm.readShort();

          // 2 - Channels
          int channels = ShortConverter.unsign(fm.readShort());

          // 4 - Samples per Second
          int frequency = fm.readInt();

          // 4 - Average Bytes per Second
          int averageBytesPerSecond = fm.readInt();

          // 2 - Block Align
          int blockAlign = ShortConverter.unsign(fm.readShort());

          // 2 - Bits
          int bitrate = ShortConverter.unsign(fm.readShort());

          // 2 - Unknown
          fm.skip(2);

          // 4 - Audio Data Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - Audio Data
          long offset = fm.getOffset();

          String filename = Resource.generateFilename(i) + "." + primaryType;

          //path,name,offset,length,decompLength,exporter
          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
          resource.setAudioProperties(frequency, (short) bitrate, (short) channels);
          resource.forceNotAdded(true);
          resources[i] = resource;

          TaskProgressManager.setValue(offset);
        }
        else if (primaryType.equals("Song")) {
          // Song Audio (pointer to an external file)

          // 1 - External Filename Length
          int filenameLength = get7BitEncodedInt(fm);

          // X - External Filename
          String externalFilename = fm.readString(filenameLength);
          File externalFile = new File(FilenameSplitter.getDirectory(path.getAbsolutePath()) + File.separatorChar + externalFilename);

          // 4 - Unknown
          // 1 - null
          fm.skip(5);

          if (externalFile.exists()) {

            String filename = externalFilename;

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(externalFile, filename, 0, externalFile.length());
            resource.forceNotAdded(true);
            resources[i] = resource;

            TaskProgressManager.setValue(fm.getOffset());
          }
        }
        else {
          long offset = fm.getOffset();
          long length = arcSize - offset;
          String filename = Resource.generateFilename(i) + "." + primaryType;

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.forceNotAdded(true);
          resources[i] = resource;

          TaskProgressManager.setValue(offset);
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
  
   **********************************************************************************************
   **/
  protected static int get7BitEncodedInt(FileManipulator fm) {
    int result = 0;
    int bitsRead = 0;
    int value;

    do {
      value = fm.readByte();
      result |= (value & 0x7f) << bitsRead;
      bitsRead += 7;
    }
    while ((value & 0x80) != 0);

    return result;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  protected static char getCSharpChar(FileManipulator fm) {
    char result = (char) fm.readByte();
    if ((result & 0x80) != 0) {
      int bytes = 1;
      while ((result & (0x80 >> bytes)) != 0)
        bytes++;
      result &= (1 << (8 - bytes)) - 1;
      while (--bytes > 0) {
        result <<= 6;
        result |= fm.readByte() & 0x3F;
      }
    }
    return result;
  }

  /*
  protected static <T> void getList(ByteBuffer buffer, List<T> list, Class<T> clazz) {
    if (get7BitEncodedInt(buffer) == 0) {
      throw new RuntimeException("List is null");
    }
  
    int len = buffer.getInt();
    for (int i = 0; i < len; ++i) {
      if (clazz == Rectangle.class) {
        list.add(clazz.cast(getRectangle(buffer)));
      } else if (clazz == Vector3.class) {
        list.add(clazz.cast(getVector3(buffer)));
      } else {
        throw new RuntimeException("Unsupported array type");
      }
    }
  }
  */

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  protected static String getCSharpString(FileManipulator fm) {
    int len = get7BitEncodedInt(fm);
    return fm.readString(len);
  }

  /*
  protected static Rectangle getRectangle(ByteBuffer buffer) {
    return new Rectangle(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt());
  }
  
  protected static Vector3 getVector3(ByteBuffer buffer) {
    return new Vector3(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
  }
  */

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
