
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.task.TaskProgressManager;
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SIMS2PACK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SIMS2PACK() {

    super("SIMS2PACK", "The Sims 2 Package");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Sims 2");
    setExtensions("sims2pack");
    setPlatforms("PC");

    setFileTypes("package", "The Sims 2 Package Archive");

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
      if (fm.readString(18).equals("Sims2 Packager 1.0")) {
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

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 18 - Header
      fm.skip(18);

      // 4 - Data Offset
      int dataStart = fm.readInt();
      FieldValidator.checkOffset(dataStart, arcSize);

      // X - The structure

      /*
       * <?xml version="1.0" encoding="UTF-8"?> <Sims2Package type='Object'>
       * <GameVersion>2141707388.153.1</GameVersion> <!-- START REPEATABLE --> <PackagedFile>
       * <Name><![CDATA[eggShrub.package]]></Name> <Crc>552674faeebf57f3a8f0550f690ac902</Crc>
       * <Length>499276</Length> <Type>Object</Type> <Offset>0</Offset>
       * <Description><![CDATA[]]></Description> <!-- END REPEATABLE --> </PackagedFile>
       * </Sims2Package>
       */

      String name = "";
      long offset = -1;
      long length = -1;

      int realNumFiles = 0;

      // Setup storage arrays
      Resource[] resources = new Resource[Archive.getMaxFiles()];

      // Loop through directory
      boolean endXML = false;
      //int readSize = 0;
      while (!endXML) {
        if (!name.equals("") && offset > -1 && length > -1) {
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, name, offset, length);

          realNumFiles++;

          name = "";
          offset = -1;
          length = -1;
        }

        String line = fm.readLine();
        if (line.indexOf("</Sims2Package>") >= 0 || fm.getOffset() >= dataStart) {
          endXML = true;
        }
        else if (line.indexOf("<Name>") >= 0) {
          int startName = line.indexOf(">") + 1;

          // checks for the <CData bit
          if (line.charAt(startName) == '<') {
            startName += 9;
          }

          int endName = line.lastIndexOf("<") - 3;
          name = line.substring(startName, endName);
          FieldValidator.checkFilename(name);
        }
        else if (line.indexOf("<Length>") >= 0) {
          int startLength = line.indexOf(">") + 1;
          int endLength = line.lastIndexOf("<");
          length = Integer.parseInt(line.substring(startLength, endLength));
          FieldValidator.checkLength(length, arcSize);
        }
        else if (line.indexOf("<Offset>") >= 0) {
          int startOffset = line.indexOf(">") + 1;
          int endOffset = line.lastIndexOf("<");
          offset = dataStart + Integer.parseInt(line.substring(startOffset, endOffset));
          FieldValidator.checkOffset(offset, arcSize);
        }
      }

      fm.close();

      return resizeResources(resources, realNumFiles);

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
  @Override
  public void write(Resource[] resources, File path) {
    try {

      // WE NEED TO FIND OUT HOW THE CHECKSUM IS CALCULATED
      // THE CHECKSUM IS 16-BYTE (128-BIT) CRC CHECKSUM
      // eg. 95abb5d160cfd12c6ae38da7da5b0d6a (in hex)

      FileManipulator fm = new FileManipulator(path, true);
      fm.setLength(26); //big enough to skip over the data Offset, and write it later

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // 18 - Header
      fm.writeString("Sims2 Packager 1.0");

      // 4 - Data Offset
      fm.skip(4);

      // X - The structure

      fm.writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + (char) 10);
      fm.writeString("<Sims2Package type=\"assets\">" + (char) 10);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = 0;
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long length = resources[i].getDecompressedLength();

        fm.writeString("  <PackagedFile>" + (char) 10);

        // Name
        fm.writeString("    <Name>" + name + "</Name>" + (char) 10);
        // Length
        fm.writeString("    <Length>" + length + "</Length>" + (char) 10);
        // Type
        fm.writeString("    <Type>part</Type>" + (char) 10);
        // Offset
        fm.writeString("    <Offset>" + currentPos + "</Offset>" + (char) 10);
        // Description
        fm.writeString("    <Description></Description>" + (char) 10);

        fm.writeString("  </PackagedFile>" + (char) 10);
        currentPos += length;
      }

      fm.writeString("</Sims2Package>");

      // write dirOffset in header
      long dirOffset = (int) fm.getOffset();
      fm.seek(18);
      fm.writeInt((int) dirOffset);
      fm.seek(dirOffset);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
