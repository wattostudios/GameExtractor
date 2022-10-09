
package org.watto.ge.plugin.archive;

import java.io.File;
import java.util.List;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_RAR_RAR;
import org.watto.ge.plugin.resource.Resource_RAR_RAR;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;
import org.watto.task.Task_ExportFiles;
import com.github.junrar.RarArchive;
import com.github.junrar.rarfile.FileHeader;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RAR_RAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RAR_RAR() {

    super("RAR_RAR", "RAR Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Generic Rar Archive",
        "Alexander",
        "Hidden And Dangerous");
    setExtensions("rar");
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
      if (fm.readString(4).equals("Rar!")) {
        rating += 50;
      }
      else {
        return 0;
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

      /*
      
      ExporterPlugin exporter = Exporter_RAR_RAR.getInstance();
      
      RARFile rarArchive = new RARFile(path);
      RARArchivedFile[] files = rarArchive.getArchivedFiles();
      
      int numFiles = files.length;
      
      Resource[] resources = new Resource[numFiles];
      
      TaskProgressManager.setMaximum(numFiles);
      
      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        RARArchivedFile rarEntry = files[i];
        if (!rarEntry.isDirectory()) {
      
          String filename = rarEntry.getName();
          long length = rarEntry.getPackedSize();
          long decompLength = rarEntry.getUnpackedSize();
      
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, 0, length, decompLength, exporter);
      
          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
      }
      
      resources = resizeResources(resources, realNumFiles);
      
      return resources;
      */

      RarArchive arch = new RarArchive(path);

      ExporterPlugin exporter = new Exporter_RAR_RAR(arch);

      List<FileHeader> fileHeaders = arch.getFileHeaders();

      int numFiles = fileHeaders.size();
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      for (int i = 0; i < numFiles; i++) {
        FileHeader fileHeader = fileHeaders.get(i);
        if (!fileHeader.isDirectory()) {
          String filename = fileHeader.getFileNameString();

          long length = fileHeader.getFullPackSize();
          long decompLength = fileHeader.getFullUnpackSize();
          long offset = fileHeader.getPositionInFile();

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource_RAR_RAR(path, filename, offset, length, decompLength, exporter, fileHeader);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      // Now, if the Archive is a SOLID archive, we need to extract all the files to TEMP because they need to be read in order
      if (realNumFiles > 0) {
        if (arch.getMainHeader().isSolid()) {
          // Yep, extract them all 1 at a time to TEMP

          TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
          TaskProgressManager.setMaximum(realNumFiles); // progress bar

          File tempDirectory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());

          TaskProgressManager.setTaskRunning(false); // want to force the extract

          Task_ExportFiles task = new Task_ExportFiles(tempDirectory, resources);
          task.setShowPopups(false);
          //task.setShowProgressPopups(false); // this barely appears, and slows down the preview repainting significantly, so don't worry about it.
          task.redo();

          TaskProgressManager.setTaskRunning(true); // reset back to "task running", which is that the archive is being read

          // Now that the files have been exported, go through and change the exporter to the Default
          ExporterPlugin defaultExporter = Exporter_Default.getInstance();
          for (int i = 0; i < realNumFiles; i++) {
            resources[i].setExporter(defaultExporter);
          }

          // We've exported all the files to TEMP - if we don't set this, they will be deleted after the archive is opened
          SingletonManager.add("BulkExport_KeepTempFiles", true);
        }
      }

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
