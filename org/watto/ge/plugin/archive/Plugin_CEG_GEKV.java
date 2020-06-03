
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CEG_GEKV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CEG_GEKV() {

    super("CEG_GEKV", "CEG_GEKV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("ceg");
    setGames("The Punisher");
    setPlatforms("PC");

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
      if (fm.readString(4).equals("GEKV")) {
        rating += 50;
      }
      else {
        fm.seek(0);
        if (fm.readString(1).equals("x")) {
          rating += 25;
          return rating;
        }
        else {
          return rating;
        }
      }

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (GEKF)
      byte checkByte = fm.readByte();
      while (checkByte == 0 && fm.getOffset() < fm.getLength()) {
        checkByte = fm.readByte();
      }

      if (new String(new byte[] { checkByte }).equals("x")) {

        long offset = fm.getOffset() - 1;
        //System.out.println(offset);

        // close AFTER we use FM in the line above
        fm.close();

        //extract the cab file first
        FileManipulator extDir = new FileManipulator(new File("temp" + File.separator + "ceg_decompressed.ceg"), true);
        String dirName = extDir.getFilePath();
        Resource directory = new Resource(path, dirName, offset, (int) path.length() - offset, (int) path.length() * 20);

        Exporter_ZLib.getInstance().extract(directory, extDir);

        extDir.close();

        path = new File(dirName);

        fm = new FileManipulator(path, false);
        fm.skip(1);
      }

      long arcSize = fm.getLength();

      // 4 - Version (1)
      fm.skip(7);

      // 4 - Directory Length (first file offset = dirLength + 32)
      int firstDataOffset = fm.readInt() + 32;
      FieldValidator.checkOffset(firstDataOffset, arcSize);

      // 4 - File Data Length
      fm.skip(4);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - null
      // 4 - numFiles
      // 4 - Unknown (128)
      fm.skip(12);

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(12);

        // 24 - Filename (null)
        String filename = fm.readNullString(24);
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}