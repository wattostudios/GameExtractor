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

package org.watto.task;

import java.io.File;
import java.util.Arrays;
import org.watto.ChangeMonitor;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.PreviewPanel;
import org.watto.component.WSPlugin;
import org.watto.component.WSPluginManager;
import org.watto.component.WSPopup;
import org.watto.component.WSStatusBar;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.GameExtractor;
import org.watto.ge.helper.AnalysisPluginGroup;
import org.watto.ge.helper.AnalysisViewerGroup;
import org.watto.ge.helper.FileTypeDetector;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB5_ProcessWithinArchive;
import org.watto.io.DirectoryBuilder;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ExporterByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_AnalyzeDirectory extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  File path;

  String reportFilename = "";

  boolean useViewerPlugins = false; // old, to remove

  int outputFormat = 0;

  public static int FORMAT_HTML = 0;

  public static int FORMAT_CSV = 1;

  public static int FORMAT_TXT = 2; // tabbed

  public static int FORMAT_XML = 3;

  public static int FORMAT_JSON = 4;

  public static int FORMAT_XLS = 5;

  boolean checkViewerPlugins = false;

  boolean extractAllFiles = false;

  boolean scanInArchives = false;

  boolean processSubDirectories = false;

  File extractDirectory = null;

  ViewerPlugin[] converterPlugins = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setConverterPlugins(ViewerPlugin[] converterPlugins) {
    this.converterPlugins = converterPlugins;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_AnalyzeDirectory(File path, String reportFilename) {
    this.path = path;
    this.reportFilename = reportFilename;

    // Work out the output format
    String extension = FilenameSplitter.getExtension(reportFilename);
    if (extension.equalsIgnoreCase("csv")) {
      outputFormat = FORMAT_CSV;
    }
    else if (extension.equalsIgnoreCase("txt")) {
      outputFormat = FORMAT_TXT;
    }
    else if (extension.equalsIgnoreCase("xls")) {
      outputFormat = FORMAT_XLS;
    }
    else if (extension.equalsIgnoreCase("html")) {
      outputFormat = FORMAT_HTML;
    }
    else if (extension.equalsIgnoreCase("json")) {
      outputFormat = FORMAT_JSON;
    }
    else if (extension.equalsIgnoreCase("xml")) {
      outputFormat = FORMAT_XML;
    }
    else {
      outputFormat = FORMAT_HTML;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeReportHeader(FileManipulator fm) {
    writeHTML(fm, "<html>\n<head>\n<title>Game Extractor - Analysis of Directory</title>\n</head>\n<body bgcolor='white'>\n<center>\n");
    writeCSV(fm, "Archive_Plugin_Code,Archive_Plugin_Name,Archive_Filename,Viewer_Plugin_Code,Viewer_Plugin_Name,Resource_Name,Resource_Directory,Resource_Filename,Resource_Extension,Resource_Offset,Resource_Compressed_Length,Resource_Decompressed_Length,Resource_Compression_Type,Resource_Description,Resource_Exported_Path\n");
    writeTXT(fm, "Archive_Plugin_Code\tArchive_Plugin_Name\tArchive_Filename\tViewer_Plugin_Code\tViewer_Plugin_Name\tResource_Name\tResource_Directory\tResource_Filename\tResource_Extension\tResource_Offset\tResource_Compressed_Length\tResource_Decompressed_Length\tResource_Compression_Type\tResource_Description\tResource_Exported_Path\n");
    writeXLS(fm, "Archive_Plugin_Code\tArchive_Plugin_Name\tArchive_Filename\tViewer_Plugin_Code\tViewer_Plugin_Name\tResource_Name\tResource_Directory\tResource_Filename\tResource_Extension\tResource_Offset\tResource_Compressed_Length\tResource_Decompressed_Length\tResource_Compression_Type\tResource_Description\tResource_Exported_Path\n");
    writeJSON(fm, "{ \"json\" : {\n\"archives\" : [");
    writeXML(fm, "<xml><archives>\n");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeReportFooter(FileManipulator fm) {
    writeHTML(fm, "</center></body></html>");
    writeCSV(fm, "\n");
    writeTXT(fm, "\n");
    writeXLS(fm, "\n");
    writeJSON(fm, "]\n}\n}");
    writeXML(fm, "</archives></xml>\n");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeArchiveHeader(FileManipulator fm, File archiveFile, ArchivePlugin archivePlugin, Resource[] resources) {
    String archiveFilePath = "";
    if (archiveFile != null) {
      archiveFilePath = archiveFile.getPath();
    }

    String archivePluginName = archivePlugin.getName();
    String archivePluginCode = archivePlugin.getCode();

    writeHTML(fm, "The archive " + archiveFilePath + " could be opened with the " + archivePluginName + " [<b>" + archivePluginCode + "</b>] archive plugin, and contains these files...<br /><br />");

    if (foundMultipleArchives) {
      writeJSON(fm, ",");
    }
    foundMultipleArchives = true;

    writeJSON(fm, "{\n\"archiveFilename\" : \"" + archiveFilePath.replace("\\", "\\\\") + "\",\n\"archivePluginCode\" : \"" + archivePluginCode + "\",\n\"archivePluginName\" : \"" + archivePluginName + "\",\n\"viewers\" : [\n");
    writeXML(fm, "<archive>\n<archiveFilename>" + archiveFilePath + "</archiveFilename>\n<archivePluginCode>" + archivePluginCode + "</archivePluginCode>\n<archivePluginName>" + archivePluginName + "</archivePluginName>\n<viewers>\n");

    foundMultipleViewers = false; // each archive has a new set of viewers in it
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeArchiveFooter(FileManipulator fm, File archiveFile, ArchivePlugin archivePlugin, Resource[] resources) {
    writeHTML(fm, "<br />\n");
    writeJSON(fm, "]\n}\n");
    writeXML(fm, "</viewers></archive>\n");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeViewerHeader(FileManipulator fm, File archiveFile, ArchivePlugin archivePlugin, ViewerPlugin viewerPlugin, Resource[] resources) {
    String archiveFilePath = "";
    if (archiveFile != null) {
      archiveFilePath = archiveFile.getPath();
    }

    /*
    String archivePluginName = "";
    String archivePluginCode = "";
    if (archivePlugin != null) {
      archivePluginName = archivePlugin.getName();
      archivePluginCode = archivePlugin.getCode();
    }
    */

    String viewerPluginName = "Unknown";
    String viewerPluginCode = "";
    if (viewerPlugin != null) {
      viewerPluginName = viewerPlugin.getName();
      viewerPluginCode = viewerPlugin.getCode();
    }

    if (foundMultipleViewers) {
      writeJSON(fm, ",");
    }
    foundMultipleViewers = true;

    if (archivePlugin == null) {
      // viewer for files in the filesystem

      writeHTML(fm, "The following files in directory " + archiveFilePath + " could be opened with the " + viewerPluginName + " [<b>" + viewerPluginCode + "</b>] viewer plugin...<br /><br />");
      writeJSON(fm, "{\n\"directoryName\" : \"" + archiveFilePath.replace("\\", "\\\\") + "\",\n\"viewerPluginCode\" : \"" + viewerPluginCode + "\",\n\"viewerPluginName\" : \"" + viewerPluginName + "\",\n\"resources\" : [\n");
      writeXML(fm, "<viewer>\n<directoryName>" + archiveFilePath + "</directoryName>\n<viewerPluginCode>" + viewerPluginCode + "</viewerPluginCode>\n<viewerPluginName>" + viewerPluginName + "</viewerPluginName>\n<resources>\n");
    }
    else {
      // viewer for files in an archive

      if (viewerPlugin == null) {
        writeHTML(fm, "The following files are of unknown type, with no viewer plugin available...<br /><br />");
      }
      else {
        writeHTML(fm, "The plugin " + viewerPluginName + " [<b>" + viewerPluginCode + "</b>] was able to open the following files...<br /><br />");
      }
      writeJSON(fm, "{\n\"viewerPluginCode\" : \"" + viewerPluginCode + "\",\n\"viewerPluginName\" : \"" + viewerPluginName + "\",\n\"resources\" : [\n");
      writeXML(fm, "<viewer>\n<viewerPluginCode>" + viewerPluginCode + "</viewerPluginCode>\n<viewerPluginName>" + viewerPluginName + "</viewerPluginName>\n<resources>\n");
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeViewerFooter(FileManipulator fm, File archiveFile, ArchivePlugin archivePlugin, ViewerPlugin viewerPlugin, Resource[] resources) {
    if (archivePlugin == null) {
      // viewer for files in the filesystem

      writeHTML(fm, "<br />\n");
      writeJSON(fm, "]\n}\n");
      writeXML(fm, "</resources>\n</viewer>\n");
    }
    else {
      // viewer for files in an archive

      writeHTML(fm, "<br />\n");
      writeJSON(fm, "]\n}\n");
      writeXML(fm, "</resources>\n</viewer>\n");
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeResources(FileManipulator fm, File archiveFile, ArchivePlugin archivePlugin, ViewerPlugin viewerPlugin, Resource[] resources) {
    if (resources == null || resources.length <= 0) {
      return;
    }

    String archiveFilePath = "";
    if (archiveFile != null) {
      archiveFilePath = archiveFile.getPath();
    }

    String archivePluginName = "";
    String archivePluginCode = "";
    if (archivePlugin != null) {
      archivePluginName = archivePlugin.getName();
      archivePluginCode = archivePlugin.getCode();
    }

    String viewerPluginName = "Unknown";
    String viewerPluginCode = "";
    if (viewerPlugin != null) {
      viewerPluginName = viewerPlugin.getName();
      viewerPluginCode = viewerPlugin.getCode();
    }

    int numFiles = resources.length;
    for (int i = 0; i < numFiles; i++) {
      Resource resource = resources[i];

      String resourceName = resource.getName();
      String resourceDirectory = resource.getDirectory();
      String resourceFilename = resource.getFilename();
      String resourceExtension = resource.getExtension();
      long resourceOffset = resource.getOffset();
      long resourceCompressedLength = resource.getLength();
      long resourceDecompressedLength = resource.getDecompressedLength();
      String resourceCompressionType = resource.getExporter().getCode();
      String resourceDescription = FileTypeDetector.getFileType(resourceExtension).getDescription();

      String resourceExportedPath = "";
      File exportedPath = resource.getExportedPath();
      if (exportedPath != null) {
        resourceExportedPath = exportedPath.getAbsolutePath();
      }

      // Resource_Name
      writeHTML(fm, resourceName + "<br />");

      // Resource_Name,Resource_Directory,Resource_Filename,Resource_Extension,Resource_Offset,Resource_Compressed_Length,Resource_Decompressed_Length,Resource_Compression_Type,Resource_Description,Resource_Exported_Path
      writeJSON(fm, "{\"resourceName\" : \"" + resourceName.replace("\\", "\\\\") + "\",\n" +
          "\"resourceDirectory\" : \"" + resourceDirectory.replace("\\", "\\\\") + "\",\n" +
          "\"resourceFilename\" : \"" + resourceFilename + "\",\n" +
          "\"resourceExtension\" : \"" + resourceExtension + "\",\n" +
          "\"resourceOffset\" : \"" + resourceOffset + "\",\n" +
          "\"resourceCompressedLength\" : \"" + resourceCompressedLength + "\",\n" +
          "\"resourceDecompressedLength\" : \"" + resourceDecompressedLength + "\",\n" +
          "\"resourceCompressionType\" : \"" + resourceCompressionType + "\",\n" +
          "\"resourceDescription\" : \"" + resourceDescription + "\",\n" +
          "\"resourceExportedPath\" : \"" + resourceExportedPath.replace("\\", "\\\\") + "\"\n" +
          "}");
      if (i != numFiles - 1) {
        writeJSON(fm, ",");
      }

      // Resource_Name,Resource_Directory,Resource_Filename,Resource_Extension,Resource_Offset,Resource_Compressed_Length,Resource_Decompressed_Length,Resource_Compression_Type,Resource_Description,Resource_Exported_Path
      writeXML(fm, "<resource>\n" +
          "<resourceName>" + resourceName + "</resourceName>\n" +
          "<resourceDirectory>" + resourceDirectory + "</resourceDirectory>\n" +
          "<resourceFilename>" + resourceFilename + "</resourceFilename>\n" +
          "<resourceExtension>" + resourceExtension + "</resourceExtension>\n" +
          "<resourceOffset>" + resourceOffset + "</resourceOffset>\n" +
          "<resourceCompressedLength>" + resourceCompressedLength + "</resourceCompressedLength>\n" +
          "<resourceDecompressedLength>" + resourceDecompressedLength + "</resourceDecompressedLength>\n" +
          "<resourceCompressionType>" + resourceCompressionType + "</resourceCompressionType>\n" +
          "<resourceDescription>" + resourceDescription + "</resourceDescription>\n" +
          "<resourceExportedPath>" + resourceExportedPath + "</resourceExportedPath>\n" +
          "</resource>\n");

      //           Archive_Plugin_Code,      Archive_Plugin_Name,      Archive_Filename,       Viewer_Plugin_Code,      Viewer_Plugin_Name,      Resource_Name,       Resource_Directory,       Resource_Filename,       Resource_Extension,       Resource_Offset,       Resource_Compressed_Length,      Resource_Decompressed_Length,      Resource_Compression_Type,      Resource_Description,       Resource_Exported_Path
      writeCSV(fm, archivePluginCode + "," + archivePluginName + "," + archiveFilePath + "," + viewerPluginCode + "," + viewerPluginName + "," + resourceName + "," + resourceDirectory + "," + resourceFilename + "," + resourceExtension + "," + resourceOffset + "," + resourceCompressedLength + "," + resourceDecompressedLength + "," + resourceCompressionType + "," + resourceDescription + "," + resourceExportedPath + "\n");

      //           Archive_Plugin_Code\t      Archive_Plugin_Name\t      Archive_Filename\t       Viewer_Plugin_Code\t      Viewer_Plugin_Name\t      Resource_Name\t       Resource_Directory\t       Resource_Filename\t       Resource_Extension\t       Resource_Offset\t       Resource_Compressed_Length\t      Resource_Decompressed_Length\t      Resource_Compression_Type\t      Resource_Description\t       Resource_Exported_Path
      writeTXT(fm, archivePluginCode + "\t" + archivePluginName + "\t" + archiveFilePath + "\t" + viewerPluginCode + "\t" + viewerPluginName + "\t" + resourceName + "\t" + resourceDirectory + "\t" + resourceFilename + "\t" + resourceExtension + "\t" + resourceOffset + "\t" + resourceCompressedLength + "\t" + resourceDecompressedLength + "\t" + resourceCompressionType + "\t" + resourceDescription + "\t" + resourceExportedPath + "\n");
      writeXLS(fm, archivePluginCode + "\t" + archivePluginName + "\t" + archiveFilePath + "\t" + viewerPluginCode + "\t" + viewerPluginName + "\t" + resourceName + "\t" + resourceDirectory + "\t" + resourceFilename + "\t" + resourceExtension + "\t" + resourceOffset + "\t" + resourceCompressedLength + "\t" + resourceDecompressedLength + "\t" + resourceCompressionType + "\t" + resourceDescription + "\t" + resourceExportedPath + "\n");
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeHTML(FileManipulator fm, String text) {
    if (outputFormat == FORMAT_HTML) {
      fm.writeString(text);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeXML(FileManipulator fm, String text) {
    if (outputFormat == FORMAT_XML) {
      fm.writeString(text);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeJSON(FileManipulator fm, String text) {
    if (outputFormat == FORMAT_JSON) {
      fm.writeString(text);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeCSV(FileManipulator fm, String text) {
    if (outputFormat == FORMAT_CSV) {
      fm.writeString(text);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeTXT(FileManipulator fm, String text) {
    if (outputFormat == FORMAT_TXT) {
      fm.writeString(text);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeXLS(FileManipulator fm, String text) {
    if (outputFormat == FORMAT_XLS) {
      fm.writeString(text);
    }
  }

  boolean foundMultipleArchives = false;

  boolean foundMultipleViewers = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }

    if (path == null || !path.exists()) {
      WSPopup.showError("AnalyzeDirectory_FilenameMissing", true);
      TaskProgressManager.stopTask();
      return;
    }

    if (!path.isDirectory()) {
      WSPopup.showError("AnalyzeDirectory_FileNotADirectory", true);
      TaskProgressManager.stopTask();
      return;
    }

    // ask to save the modified archive
    if (GameExtractor.getInstance().promptToSave()) {
      return;
    }
    ChangeMonitor.reset();

    // Global Settings
    checkViewerPlugins = Settings.getBoolean("SidePanel_DirectoryList_AnalyzeDirectory_CheckViewerPlugins");
    extractAllFiles = Settings.getBoolean("SidePanel_DirectoryList_AnalyzeDirectory_ExtractAllFiles");
    scanInArchives = Settings.getBoolean("SidePanel_DirectoryList_AnalyzeDirectory_ScanFilesInArchives");
    processSubDirectories = Settings.getBoolean("SidePanel_DirectoryList_AnalyzeDirectory_ScanSubDirectories");
    extractDirectory = new File(new File(Settings.get("AnalysisExtractDirectory")).getAbsolutePath());

    foundMultipleArchives = false; // for writing a comma between archives in JSON
    foundMultipleViewers = false; // for writing a comma between archives in JSON

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_AnalyzingDirectory"));
    TaskProgressManager.startTask();

    // Start building the report
    File directory = new File(new File(Settings.get("AnalysisExtractDirectory") + File.separator + reportFilename).getAbsolutePath());

    DirectoryBuilder.buildDirectory(directory, false);
    FileManipulator fm = new FileManipulator(directory, true);

    writeReportHeader(fm);

    // analyse the directory (and sub-directories)
    boolean playAudio = Settings.getBoolean("PlayAudioOnLoad");
    Settings.set("PlayAudioOnLoad", false);

    processDirectory(fm, path);

    Settings.set("PlayAudioOnLoad", playAudio);

    writeReportFooter(fm);

    // Close the report

    fm.close();

    TaskProgressManager.stopTask();

    WSPopup.showMessage("AnalyzeDirectory_DirectoryAnalyzed", true);
  }

  /**
   **********************************************************************************************
   
   **********************************************************************************************
   **/
  public void processDirectory(FileManipulator fm, File directory) {

    // Prepare the viewer groups, in case we look for viewers as well as archives
    int maxViewers = WSPluginManager.getGroup("Viewer").getPluginCount();
    AnalysisViewerGroup[] groups = new AnalysisViewerGroup[maxViewers];

    // A special group to record all the other files that don't have a Viewer for them
    AnalysisViewerGroup unknownGroup = new AnalysisViewerGroup(null);

    // Get all the files in this directory
    File[] sourceFiles = directory.listFiles();
    int numSourceFiles = sourceFiles.length;

    // process the files, then process the sub-directories 
    File[] directories = new File[numSourceFiles];
    int numDirectories = 0;

    File[] files = new File[numSourceFiles];
    int numFiles = 0;

    for (int i = 0; i < numSourceFiles; i++) {
      // find all the directories first
      File file = sourceFiles[i];
      if (file.isDirectory()) {
        directories[numDirectories] = file;
        numDirectories++;
      }
      else {
        files[numFiles] = file;
        numFiles++;
      }
    }

    WSStatusBar statusBar = (WSStatusBar) ComponentRepository.get("StatusBar");

    // Now process the files only
    TaskProgressManager.setMaximum(numFiles);
    for (int f = 0; f < numFiles; f++) {
      TaskProgressManager.setValue(f);

      boolean foundPlugin = false;
      Archive.makeNewArchive();

      File archiveFile = files[f];
      Settings.set("CurrentArchive", archiveFile.getAbsolutePath());

      // Set statusbar message
      if (statusBar != null) {
        statusBar.setText("Analyzing file " + (f + 1) + " of " + numFiles + ": " + archiveFile.getName());
      }

      // See whether an Archive or a Viewer plugin can open the file
      RatedPlugin[] plugins = PluginFinder.findPlugins(archiveFile, ArchivePlugin.class);

      if (plugins != null && plugins.length > 0) {
        Arrays.sort(plugins);

        for (int i = 0; i < plugins.length; i++) {
          //System.out.println(plugins[i].getRating());

          // Open the archive
          ArchivePlugin archivePlugin = (ArchivePlugin) plugins[i].getPlugin();
          Resource[] resources = archivePlugin.read(archiveFile);

          if (resources == null || resources.length <= 0) {
            continue;
          }

          // Successfully opened the archive
          foundPlugin = true;
          i = plugins.length;

          Archive.setResources(resources);
          Archive.setReadPlugin(archivePlugin);
          Archive.setBasePath(archiveFile);

          // Export all the files to disk (setting), including image conversions if chosen
          if (extractAllFiles) {
            // extract each archive to a separate folder in the extract directory
            File specificExtractDirectory = new File(extractDirectory.getAbsolutePath() + File.separatorChar + archiveFile.getName());
            Task_ExportFiles task = new Task_ExportFiles(specificExtractDirectory, resources);

            if (converterPlugins != null) {
              task.setConverterPlugins(converterPlugins);
            }

            task.setShowPopups(false);
            TaskProgressManager.setTaskRunning(false); // otherwise the Export doesn't run
            task.redo();
          }

          // Write the archive Header details
          writeArchiveHeader(fm, archiveFile, archivePlugin, resources);

          // Process each file in the archive, look for a viewer plugin for them
          analyzeArchiveContents(fm, archiveFile, archivePlugin, resources);

          // Write the archive Footer details
          writeArchiveFooter(fm, archiveFile, archivePlugin, resources);

        }

      }

      if (!foundPlugin) {
        if (checkViewerPlugins) {
          // No archive plugins found, or none opened the file successfully.
          // So, try to open with the viewers

          plugins = PluginFinder.findPlugins(archiveFile, ViewerPlugin.class);

          if (plugins != null && plugins.length > 0) {
            Arrays.sort(plugins);

            // try to open the preview using each plugin and previewFile(File,Plugin)
            for (int p = 0; p < plugins.length; p++) {
              ViewerPlugin viewerPlugin = (ViewerPlugin) plugins[p].getPlugin();

              PreviewPanel panel = viewerPlugin.read(archiveFile);
              if (panel == null) {
                continue;
              }

              // Successfully opened the file using a viewer
              foundPlugin = true;

              // Add it to the plugin list
              addToGroup(new Resource(archiveFile), viewerPlugin, groups, unknownGroup);

              break;
            }
          }

        }
      }
    }

    // Now that we've written out the archives, we can write out the Viewers that were found
    if (checkViewerPlugins) {
      boolean foundTheEnd = false;
      for (int g = 0; g < maxViewers; g++) {
        if (foundTheEnd) {
          break;
        }

        AnalysisViewerGroup group = groups[g];

        if (group == null) {
          foundTheEnd = true;
          // process the Unknown Group at the very end
          group = unknownGroup;
        }

        ViewerPlugin viewerPlugin = group.getPlugin();
        Resource[] resources = group.getResources();

        // Write the viewer Header details
        writeViewerHeader(fm, directory, null, viewerPlugin, resources);

        // Write out the resources for this viewer
        writeResources(fm, directory, null, viewerPlugin, resources);

        // Write the viewer Footer details
        writeViewerFooter(fm, directory, null, viewerPlugin, resources);
      }
    }

    // Now process the sub-directories 
    if (processSubDirectories) {
      for (int i = 0; i < numDirectories; i++) {
        File subDirectory = directories[i];
        processDirectory(fm, subDirectory);
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void OLDredo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }

    useViewerPlugins = Settings.getBoolean("SidePanel_DirectoryList_AnalyzeDirectory_CheckViewerPlugins");

    if (path == null || !path.exists()) {
      WSPopup.showError("AnalyzeDirectory_FilenameMissing", true);
      TaskProgressManager.stopTask();
      return;
    }

    if (!path.isDirectory()) {
      WSPopup.showError("AnalyzeDirectory_FileNotADirectory", true);
      TaskProgressManager.stopTask();
      return;
    }

    // ask to save the modified archive
    if (GameExtractor.getInstance().promptToSave()) {
      return;
    }
    ChangeMonitor.reset();

    // Progress dialog

    TaskProgressManager.show(1, 0, Language.get("Progress_AnalyzingDirectory"));
    TaskProgressManager.startTask();

    int numPlugins = WSPluginManager.getGroup("Archive").getPluginCount();
    numPlugins += WSPluginManager.getGroup("Viewer").getPluginCount();

    AnalysisPluginGroup[] pluginGroups = new AnalysisPluginGroup[numPlugins];
    Arrays.fill(pluginGroups, null);

    // analyse the directory (and sub-directories)
    OLDprocessDirectory(path, pluginGroups);

    // now we have all the plugins that have been found, with the files attached to them

    // Start building the report
    File directory = new File(new File(Settings.get("FileListExporterDirectory") + File.separator + reportFilename).getAbsolutePath());

    DirectoryBuilder.buildDirectory(directory, false);
    FileManipulator fm = new FileManipulator(directory, true);

    writeHTML(fm, "<html>\n<head>\n<title>Game Extractor - [Analysis of Directory " + directory.getAbsolutePath() + "]</title>\n</head>\n<body bgcolor='white'>\n<center>\n");
    writeCSV(fm, "Plugin_Code,Plugin_Name,Filename\n");
    writeTXT(fm, "Plugin_Code\tPlugin_Name\tFilename\n");
    writeXLS(fm, "Plugin_Code\tPlugin_Name\tFilename\n");
    writeJSON(fm, "{ \"json\" : { \"files\" : [\n");
    writeXML(fm, "<xml>\n<files>\n");

    // List all the plugins that were found, and the files that are compatible with it
    for (int g = 0; g < numPlugins; g++) {
      AnalysisPluginGroup group = pluginGroups[g];
      if (group == null) {
        break; // end of array
      }
      else if (g != 0) {
        writeJSON(fm, ",");
      }

      WSPlugin plugin = group.getPlugin();
      String pluginName = plugin.getName();
      String pluginCode = plugin.getCode();

      writeHTML(fm, "The plugin " + pluginName + " [<b>" + pluginCode + "</b>] was able to open the following files...<br /><br />");
      writeJSON(fm, "{\n\"pluginCode\" : \"" + pluginCode + "\",\n\"pluginName\" : \"" + pluginName + "\",\n\"files\" : [\n");
      writeXML(fm, "<plugin>\n<pluginCode>" + pluginCode + "</pluginCode>\n<pluginName>" + pluginName + "</pluginName>\n<files>\n");

      File[] files = group.getFiles();
      int numFiles = files.length;

      for (int i = 0; i < numFiles; i++) {
        String filename = files[i].getAbsolutePath();

        writeHTML(fm, filename + "<br />\n");
        writeCSV(fm, pluginCode + "," + pluginName + "," + filename + "\n");
        writeTXT(fm, pluginCode + "\t" + pluginName + "\t" + filename + "\n");
        writeXLS(fm, pluginCode + "\t" + pluginName + "\t" + filename + "\n");
        writeJSON(fm, "{\n\"file\" : \"" + filename.replace("\\", "\\\\") + "\"\n}\n");
        if (i != numFiles - 1) {
          writeJSON(fm, ",");
        }
        writeXML(fm, "<file>" + filename + "</file>\n");

      }

      writeHTML(fm, "<br />\n");
      writeJSON(fm, "]\n}\n");
      writeXML(fm, "</files></plugin>\n");

    }

    writeHTML(fm, "<br />\n");
    writeCSV(fm, "\n");
    writeTXT(fm, "\n");
    writeXLS(fm, "\n");
    writeJSON(fm, "]\n");
    writeXML(fm, "</files>\n");

    boolean scanInArchives = Settings.getBoolean("SidePanel_DirectoryList_AnalyzeDirectory_ScanFilesInArchives");
    if (scanInArchives) {
      // Now for each archive...
      // 1. Open the file into an Archive
      // 2. Analyse each Resource to see what Viewers can process them
      // 3. Add this information into the report

      writeHTML(fm, "<br /><br />");
      writeCSV(fm, "Archive_Plugin_Code,Archive_Plugin_Name,Archive_Filename,Inner_Plugin_Code,Inner_Plugin_Name,Inner_Filename\n");
      writeTXT(fm, "Archive_Plugin_Code\tArchive_Plugin_Name\tArchive_Filename\tInner_Plugin_Code\tInner_Plugin_Name\tInner_Filename\n");
      writeXLS(fm, "Archive_Plugin_Code\tArchive_Plugin_Name\tArchive_Filename\tInner_Plugin_Code\tInner_Plugin_Name\tInner_Filename\n");
      writeJSON(fm, ",\"archives\" : [\n");
      writeXML(fm, "<archives>\n");

      boolean foundAny = false;
      for (int g = 0; g < numPlugins; g++) {
        AnalysisPluginGroup group = pluginGroups[g];
        if (group == null) {
          break; // end of array
        }

        WSPlugin plugin = group.getPlugin();
        if (!(plugin instanceof ArchivePlugin)) {
          continue; // only want to process Archive Plugins in this area
        }

        else if (foundAny) {
          writeJSON(fm, ",");
        }

        foundAny = true;

        String pluginName = plugin.getName();
        String pluginCode = plugin.getCode();

        File[] files = group.getFiles();
        int numFiles = files.length;

        for (int i = 0; i < numFiles; i++) {
          File file = files[i];

          String filePath = file.getAbsolutePath();

          writeHTML(fm, "Analysis of file <b>" + filePath + "</b> using plugin " + pluginName + " [<b>" + pluginCode + "</b>]...<br /><br />");
          writeJSON(fm, "{\n\"pluginCode\" : \"" + pluginCode + "\",\n\"pluginName\" : \"" + pluginName + "\",\n\"file\" : \"" + filePath.replace("\\", "\\\\") + "\",\n\"innerFiles\" : [\n");
          writeXML(fm, "<archive>\n<plugin>\n<pluginCode>" + pluginCode + "</pluginCode>\n<pluginName>" + pluginName + "</pluginName><file>" + filePath + "</file>\n<innerFiles>\n");

          // Open an archive
          AnalysisPluginGroup[] resources = new AnalysisPluginGroup[numPlugins];
          Arrays.fill(resources, null);

          // Work out the viewers for each file
          boolean playAudio = Settings.getBoolean("PlayAudioOnLoad");
          Settings.set("PlayAudioOnLoad", false);
          OLDanalyzeArchiveContents(file, (ArchivePlugin) plugin, resources);
          Settings.set("PlayAudioOnLoad", playAudio);

          // Print out the files for each viewer
          for (int g2 = 0; g2 < numPlugins; g2++) {
            AnalysisPluginGroup resourceGroup = resources[g2];
            if (resourceGroup == null) {
              break; // end of array
            }
            else if (g2 != 0) {
              writeJSON(fm, ",");
            }

            File[] resourceFiles = resourceGroup.getFiles();
            if (resourceFiles == null) {
              break; // this happens if all the files in an archive were actually identified
            }
            int numResources = resourceFiles.length;

            WSPlugin viewerPlugin = resourceGroup.getPlugin();

            if (viewerPlugin == null) {
              // special case - all the "unknown" files

              writeHTML(fm, "The files in this archive with unknown type are...<br /><br />");
              writeJSON(fm, "{\"viewerPlugin\" : {\n\"pluginCode\" : \"Unknown\",\n\"pluginName\" : \"Unknown\",\n\"files\" : [\n");
              writeXML(fm, "<viewerPlugin>\n<pluginCode>Unknown</pluginCode>\n<pluginName>Unknown</pluginName>\n<files>\n");

              for (int i2 = 0; i2 < numResources; i2++) {
                String innerFilename = resourceFiles[i2].getPath();

                writeHTML(fm, innerFilename + "<br />");
                writeCSV(fm, pluginCode + "," + pluginName + "," + filePath + "," + "Unknown" + "," + "Unknown" + "," + innerFilename + "\n");
                writeTXT(fm, pluginCode + "\t" + pluginName + "\t" + filePath + "\t" + "Unknown" + "\t" + "Unknown" + "\t" + innerFilename + "\n");
                writeXLS(fm, pluginCode + "\t" + pluginName + "\t" + filePath + "\t" + "Unknown" + "\t" + "Unknown" + "\t" + innerFilename + "\n");
                writeJSON(fm, "{\"file\" : \"" + innerFilename.replace("\\", "\\\\") + "\"\n}");
                if (i2 != numResources - 1) {
                  writeJSON(fm, ",");
                }
                writeXML(fm, "<file>" + innerFilename + "</file>\n");

              }

            }
            else {
              // has a viewer plugin

              String viewerPluginName = viewerPlugin.getName();
              String viewerPluginCode = viewerPlugin.getCode();

              writeHTML(fm, "The plugin " + viewerPluginName + " [<b>" + viewerPluginCode + "</b>] was able to open the following files...<br /><br />");
              writeJSON(fm, "{\"viewerPlugin\" : {\n\"pluginCode\" : \"" + viewerPluginCode + "\",\n\"pluginName\" : \"" + viewerPluginName + "\",\n\"files\" : [\n");
              writeXML(fm, "<viewerPlugin>\n<pluginCode>" + viewerPluginCode + "</pluginCode>\n<pluginName>" + viewerPluginName + "</pluginName>\n<files>\n");

              for (int i2 = 0; i2 < numResources; i2++) {
                String innerFilename = resourceFiles[i2].getPath();

                writeHTML(fm, innerFilename + "<br />");
                writeCSV(fm, pluginCode + "," + pluginName + "," + filePath + "," + viewerPluginCode + "," + viewerPluginName + "," + innerFilename + "\n");
                writeTXT(fm, pluginCode + "\t" + pluginName + "\t" + filePath + "\t" + viewerPluginCode + "\t" + viewerPluginName + "\t" + innerFilename + "\n");
                writeXLS(fm, pluginCode + "\t" + pluginName + "\t" + filePath + "\t" + viewerPluginCode + "\t" + viewerPluginName + "\t" + innerFilename + "\n");
                writeJSON(fm, "{\"file\" : \"" + innerFilename.replace("\\", "\\\\") + "\"\n}");
                if (i2 != numResources - 1) {
                  writeJSON(fm, ",");
                }
                writeXML(fm, "<file>" + innerFilename + "</file>\n");

              }

            }

            writeHTML(fm, "<br />\n");
            writeJSON(fm, "]\n}\n}");
            writeXML(fm, "</files>\n</viewerPlugin>\n");

          }

          writeHTML(fm, "<br />\n");
          writeJSON(fm, "]\n}\n");
          writeXML(fm, "</innerFiles>\n</plugin>\n");

          writeHTML(fm, "<br />\n");
          writeJSON(fm, "");
          writeXML(fm, "</archive>\n");

          if (i != numFiles - 1) {
            writeJSON(fm, ",");
          }

        }

      }

      writeHTML(fm, "<br />\n");
      writeJSON(fm, "]}\n");
      writeXML(fm, "</archives>\n");

    }

    // Close the report
    writeHTML(fm, "</center></body></html>");
    writeCSV(fm, "\n");
    writeTXT(fm, "\n");
    writeXLS(fm, "\n");
    writeJSON(fm, "}");
    writeXML(fm, "</xml>\n");

    fm.close();

    TaskProgressManager.stopTask();

    WSPopup.showMessage("AnalyzeDirectory_DirectoryAnalyzed", true);

  }

  /**
   **********************************************************************************************
   
   **********************************************************************************************
   **/
  public void addToGroup(Resource resource, ViewerPlugin viewerPlugin, AnalysisViewerGroup[] groups, AnalysisViewerGroup unknownGroup) {

    if (viewerPlugin == null) {
      unknownGroup.addResource(resource);
      return;
    }

    int numGroups = groups.length;

    for (int g = 0; g < numGroups; g++) {
      AnalysisViewerGroup group = groups[g];
      if (group == null) {
        // haven't found a group, so create a new one
        group = new AnalysisViewerGroup(viewerPlugin);
        group.addResource(resource);

        groups[g] = group;
        g = numGroups; // skip over the remaining groups - already found one
      }
      else {
        // if this group matches, add it
        if (group.getPlugin() == viewerPlugin) {
          group.addResource(resource);

          g = numGroups; // skip over the remaining groups - already found one
        }
      }
    }
  }

  /**
   **********************************************************************************************
   
   **********************************************************************************************
   **/
  public void analyzeArchiveContents(FileManipulator fm, File archiveFile, ArchivePlugin archivePlugin, Resource[] resources) {
    //System.out.println("Analyzing archive file " + archiveFile.getAbsolutePath() + " using plugin " + plugin);

    int maxViewers = WSPluginManager.getGroup("Viewer").getPluginCount();
    AnalysisViewerGroup[] groups = new AnalysisViewerGroup[maxViewers];

    // A special group to record all the other files that don't have a Viewer for them
    AnalysisViewerGroup unknownGroup = new AnalysisViewerGroup(null);

    // Now analyze each file
    int numFiles = resources.length;

    TaskProgressManager.show(1, 0, Language.get("Progress_AnalyzingDirectory"));
    TaskProgressManager.setMaximum(numFiles);

    WSStatusBar statusBar = (WSStatusBar) ComponentRepository.get("StatusBar");
    String archiveName = archiveFile.getName();

    for (int i = 0; i < numFiles; i++) {
      TaskProgressManager.setValue(i);

      Resource resource = resources[i];
      //System.out.println(resource.getName());

      SingletonManager.set("CurrentResource", resource); // so it can be detected by ViewerPlugins for Thumbnail Generation

      // Set statusbar message
      if (statusBar != null) {
        statusBar.setText("Analyzing resource " + (i + 1) + " of " + numFiles + " in archive " + archiveName);
      }

      boolean foundPlugin = false;

      //
      // THIS CODE HERE IS COPIED FROM Task_LoadThumbnailLater
      //
      FileManipulator exportFM = null;
      // See if the Resource has been exported already - if it has, read from that file instead of the original archive.
      File exportedPath = resource.getExportedPath();
      if (exportedPath != null && exportedPath.exists()) {
        // already exported - read from disk
        exportFM = new FileManipulator(exportedPath, false);
      }
      else {
        // Need to read the file from the archive
        if (resource.getExporter() instanceof Exporter_Custom_FSB5_ProcessWithinArchive) {
          continue; // SPECIAL CASE: this exporter is a bit intensive, and it doesn't generate thumbnails, so skip it early.
        }

        ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);
        exportFM = new FileManipulator(byteBuffer);

        // Need to set a fake file, so that the ViewerPlugins can get the extension when running getMatchRating()
        exportFM.setFakeFile(new File(resource.getName()));
      }

      // now find a previewer for the file

      RatedPlugin[] viewerPlugins = PluginFinder.findPlugins(exportFM, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (viewerPlugins != null && viewerPlugins.length >= 0) {
        // found a viewer

        Arrays.sort(viewerPlugins);

        // re-open the file - it was closed at the end of findPlugins();
        if (exportedPath != null) {
          // already exported - read from disk
          exportFM = new FileManipulator(exportedPath, false);
        }
        else {
          // Need to read the file from the archive
          ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);
          exportFM.open(byteBuffer);
        }

        // try to open the preview using each plugin and previewFile(File,Plugin)
        for (int p = 0; p < viewerPlugins.length; p++) {
          ViewerPlugin viewerPlugin = (ViewerPlugin) viewerPlugins[p].getPlugin();

          exportFM.seek(0); // go back to the start of the file

          //
          // Try as an Image first...
          //      
          ImageResource imageResource = viewerPlugin.readThumbnail(exportFM);

          if (imageResource != null) {
            foundPlugin = true;

            // Add it to the plugin list
            addToGroup(resource, viewerPlugin, groups, unknownGroup);

            exportFM.close();
            break;
          }

          //
          // Now try other viewers (audio, for example)
          //     
          exportFM.seek(0); // go back to the start of the file

          //
          // THIS CODE HERE IS COPIED FROM SidePanel_Preview.previewFile();
          //

          // This relies on the file being exported first
          exportedPath = resource.getExportedPath();
          if (exportedPath == null) {
            // extract each archive to a separate folder in the extract directory
            File specificExtractDirectory = new File(extractDirectory.getAbsolutePath() + File.separatorChar + archiveFile.getName());
            Task_ExportFiles task = new Task_ExportFiles(specificExtractDirectory, resources);

            if (converterPlugins != null) {
              task.setConverterPlugins(converterPlugins);
            }

            task.setShowPopups(false);
            TaskProgressManager.setTaskRunning(false); // otherwise the Export doesn't run
            task.redo();
          }
          exportedPath = resource.getExportedPath();

          PreviewPanel panel = viewerPlugin.read(exportedPath);

          if (panel != null) {
            panel.onCloseRequest(); // to stop Audio playing, etc.

            foundPlugin = true;

            // Add it to the plugin list
            addToGroup(resource, viewerPlugin, groups, unknownGroup);

            exportFM.close();
            break;
          }

        }
      }

      if (!foundPlugin) {
        // try to use a viewer hint from the ArchivePlugin
        ViewerPlugin viewerPlugin = archivePlugin.previewHint(resource);
        if (viewerPlugin != null) {
          PreviewPanel panel = viewerPlugin.read(exportFM);

          if (panel != null) {
            foundPlugin = true;

            // Add it to the plugin list
            addToGroup(resource, viewerPlugin, groups, unknownGroup);

            exportFM.close();
            //break;
          }
        }
      }

      if (!foundPlugin) {
        // Still no ViewerPlugin found, so add to the "unknown" list
        unknownGroup.addResource(resource);
      }

      exportFM.close();

    }

    // Now go through and write out the Resources and their Viewers
    boolean foundTheEnd = false;
    for (int g = 0; g < maxViewers; g++) {
      if (foundTheEnd) {
        break;
      }

      AnalysisViewerGroup group = groups[g];

      if (group == null) {
        foundTheEnd = true;
        // process the Unknown Group at the very end
        group = unknownGroup;
      }

      ViewerPlugin viewerPlugin = group.getPlugin();
      resources = group.getResources();

      // Write the viewer Header details
      writeViewerHeader(fm, archiveFile, archivePlugin, viewerPlugin, resources);

      // Write out the resources for this viewer
      writeResources(fm, archiveFile, archivePlugin, viewerPlugin, resources);

      // Write the viewer Footer details
      writeViewerFooter(fm, archiveFile, archivePlugin, viewerPlugin, resources);
    }

  }

  /**
   **********************************************************************************************
   
   **********************************************************************************************
   **/
  public void OLDanalyzeArchiveContents(File archiveFile, ArchivePlugin plugin, AnalysisPluginGroup[] resourceGroups) {

    System.out.println("Analyzing archive file " + archiveFile.getAbsolutePath() + " using plugin " + plugin);

    File tempDirectory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());

    int numGroups = resourceGroups.length;

    // A special group to record all the other files that don't have a Viewer for them
    AnalysisPluginGroup unknownGroup = new AnalysisPluginGroup(null);

    Archive.makeNewArchive();

    // Open the archive
    Resource[] resources = plugin.read(archiveFile);

    if (resources == null || resources.length <= 0) {
      return;
    }

    Archive.setResources(resources);
    Archive.setReadPlugin(plugin);
    Archive.setBasePath(archiveFile);

    // Now analyze each file
    int numFiles = resources.length;

    TaskProgressManager.show(1, 0, Language.get("Progress_AnalyzingDirectory"));

    TaskProgressManager.setMaximum(numFiles);
    for (int i = 0; i < numFiles; i++) {
      TaskProgressManager.setValue(i);

      Resource resource = resources[i];
      System.out.println(resource.getName());

      SingletonManager.set("CurrentResource", resource); // so it can be detected by ViewerPlugins for Thumbnail Generation

      //
      // THIS CODE HERE IS COPIED FROM Task_LoadThumbnailLater
      //
      FileManipulator fm = null;
      // See if the Resource has been exported already - if it has, read from that file instead of the original archive.
      File exportedPath = resource.getExportedPath();
      if (exportedPath != null && exportedPath.exists()) {
        // already exported - read from disk
        fm = new FileManipulator(exportedPath, false);
        //System.out.println("Loading Thumbnail for " + resource.getName() + " (already exported)");
      }
      else {
        // Need to read the file from the archive
        //System.out.println("Loading Thumbnail for " + resource.getName() + " (NEEDS EXPORTING)");

        if (resource.getExporter() instanceof Exporter_Custom_FSB5_ProcessWithinArchive) {
          continue; // SPECIAL CASE: this exporter is a bit intensive, and it doesn't generate thumbnails, so skip it early.
        }

        ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);

        fm = new FileManipulator(byteBuffer);
        // Need to set a fake file, so that the ViewerPlugins can get the extension when running getMatchRating()
        fm.setFakeFile(new File(resource.getName()));
      }

      // now find a previewer for the file
      // preview the first selected file

      RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (plugins == null || plugins.length == 0) {
        // no viewer plugins found that will accept this file
        // leave the BlankResource here
        //continue;
      }

      Arrays.sort(plugins);

      // re-open the file - it was closed at the end of findPlugins();
      if (exportedPath != null) {
        // already exported - read from disk
        fm = new FileManipulator(exportedPath, false);
      }
      else {
        // Need to read the file from the archive
        ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);
        fm.open(byteBuffer);
      }

      boolean foundPlugin = false;

      // try to open the preview using each plugin and previewFile(File,Plugin)
      for (int p = 0; p < plugins.length; p++) {

        fm.seek(0); // go back to the start of the file

        //
        // Try as an Image first...
        //      
        ImageResource imageResource = ((ViewerPlugin) plugins[p].getPlugin()).readThumbnail(fm);

        if (imageResource != null) {
          // If the image is animated, remove the animations to clean up those memory areas.
          // We don't really want to consider animated thumbnail images, do we!?
          imageResource.setNextFrame(null);

          // if we don't want to retain the original image data after thumbnail generate, trigger a thumbnail generation now so
          // that we can clean up the memory instantly rather than after the whole archive is loaded.
          //if (Settings.getBoolean("RemoveImageAfterThumbnailGeneration")) {
          imageResource.shrinkToThumbnail();
          //}

          // a plugin opened the file successfully, so if it's an Image, generate and set an ImageResource for it.
          resource.setImageResource(imageResource);

          foundPlugin = true;

          // Add it to the plugin list
          File resourceFile = new File(resource.getName());
          WSPlugin resourcePlugin = plugins[p].getPlugin();

          for (int g = 0; g < numGroups; g++) {
            AnalysisPluginGroup group = resourceGroups[g];
            if (group == null) {
              // haven't found a group, so create a new one
              group = new AnalysisPluginGroup(resourcePlugin);
              group.addFile(resourceFile);

              resourceGroups[g] = group;
              g = numGroups; // skip over the remaining groups - already found one
            }
            else {
              // if this group matches, add it
              if (group.getPlugin() == resourcePlugin) {
                group.addFile(resourceFile);

                g = numGroups; // skip over the remaining groups - already found one
              }
            }

          }

          fm.close();

          break;
        }

        //
        // Now try other viewers (audio, for example)
        //     
        fm.seek(0); // go back to the start of the file

        //
        // THIS CODE HERE IS COPIED FROM SidePanel_Preview.previewFile();
        //

        // This relies on the file being exported first
        File tempExportedPath = resource.getExportedPath();
        if (tempExportedPath == null) {
          Task_ExportFiles task = new Task_ExportFiles(tempDirectory, resource);
          task.setShowPopups(false);
          task.setShowProgressPopups(false); // this barely appears, and slows down the preview repainting significantly, so don't worry about it.
          TaskProgressManager.setTaskRunning(false); // otherwise the Export doesn't run
          task.redo();
        }
        tempExportedPath = resource.getExportedPath();

        PreviewPanel panel = ((ViewerPlugin) plugins[p].getPlugin()).read(tempExportedPath);

        if (panel != null) {
          panel.onCloseRequest(); // to stop Audio playing, etc.

          foundPlugin = true;

          // Add it to the plugin list
          File resourceFile = new File(resource.getName());
          WSPlugin resourcePlugin = plugins[p].getPlugin();

          for (int g = 0; g < numGroups; g++) {
            AnalysisPluginGroup group = resourceGroups[g];
            if (group == null) {
              // haven't found a group, so create a new one
              group = new AnalysisPluginGroup(resourcePlugin);
              group.addFile(resourceFile);

              resourceGroups[g] = group;
              g = numGroups; // skip over the remaining groups - already found one
            }
            else {
              // if this group matches, add it
              if (group.getPlugin() == resourcePlugin) {
                group.addFile(resourceFile);

                g = numGroups; // skip over the remaining groups - already found one
              }
            }

          }

          fm.close();

          break;
        }

      }

      if (!foundPlugin) {
        ViewerPlugin hintViewerPlugin = plugin.previewHint(resource);
        if (hintViewerPlugin != null) {
          PreviewPanel panel = hintViewerPlugin.read(fm);

          if (panel != null) {
            foundPlugin = true;

            // Add it to the plugin list
            File resourceFile = new File(resource.getName());
            WSPlugin resourcePlugin = hintViewerPlugin;

            for (int g = 0; g < numGroups; g++) {
              AnalysisPluginGroup group = resourceGroups[g];
              if (group == null) {
                // haven't found a group, so create a new one
                group = new AnalysisPluginGroup(resourcePlugin);
                group.addFile(resourceFile);

                resourceGroups[g] = group;
                g = numGroups; // skip over the remaining groups - already found one
              }
              else {
                // if this group matches, add it
                if (group.getPlugin() == resourcePlugin) {
                  group.addFile(resourceFile);

                  g = numGroups; // skip over the remaining groups - already found one
                }
              }

            }

            fm.close();

            break;
          }
        }
      }

      if (!foundPlugin) {
        // no viewer plugin, so add to the "unknown" list
        unknownGroup.addFile(new File(resource.getName()));
      }

      fm.close();

    }

    // Find the last real group in the array, and add the unknownGroup to the end of it
    for (int g = 0; g < numGroups; g++) {
      AnalysisPluginGroup group = resourceGroups[g];
      if (group == null) {
        resourceGroups[g] = unknownGroup;
        break;
      }
    }
  }

  /**
   **********************************************************************************************
   
   **********************************************************************************************
   **/
  public void OLDprocessDirectory(File directory, AnalysisPluginGroup[] pluginGroups) {

    int numGroups = pluginGroups.length;

    // Get all the files in this directory
    File[] files = directory.listFiles();

    // process the files, then process the sub-directories 
    int numFiles = files.length;

    File[] directories = new File[numFiles];
    int numDirectories = 0;

    File[] processedFiles = new File[numFiles];
    int realNumFiles = 0;

    for (int i = 0; i < numFiles; i++) {
      // find all the directories first
      File file = files[i];
      if (file.isDirectory()) {
        directories[numDirectories] = file;
        numDirectories++;
      }
      else {
        processedFiles[realNumFiles] = file;
        realNumFiles++;
      }
    }

    files = processedFiles;
    numFiles = realNumFiles;

    // Now process the files only
    TaskProgressManager.setMaximum(numFiles);
    for (int f = 0; f < numFiles; f++) {
      TaskProgressManager.setValue(f);

      File file = files[f];

      WSPlugin foundPlugin = null;

      // See whether an Archive or a Viewer plugin can open the file
      RatedPlugin[] plugins = PluginFinder.findPlugins(file, ArchivePlugin.class);

      if (plugins != null && plugins.length > 0) {
        java.util.Arrays.sort(plugins);

        // try to open the archive using these plugins, until we find a successful one
        boolean archiveOpened = false;

        //String oldCurrentArchive = Settings.getString("CurrentArchive");
        Settings.set("CurrentArchive", file.getAbsolutePath());

        for (int i = 0; i < plugins.length; i++) {
          //System.out.println(plugins[i].getRating());

          // true, so it knows it is started within a current task
          Task_ReadArchiveWithPlugin task = new Task_ReadArchiveWithPlugin(file, plugins[i].getPlugin(), true);
          task.redo();
          archiveOpened = task.getResult();

          if (archiveOpened) {
            foundPlugin = plugins[i].getPlugin();
            i = plugins.length;
          }
          else {
          }

        }

      }

      if (foundPlugin == null || plugins == null || plugins.length <= 0) {
        if (useViewerPlugins) {
          // No archive plugins found, or none opened the file successfully.
          // So, try to open with the viewers
          plugins = PluginFinder.findPlugins(file, ViewerPlugin.class);

          if (plugins != null && plugins.length > 0) {
            java.util.Arrays.sort(plugins);

            // try to open the file using these plugins, until we find a successful one
            boolean previewOpened = false;

            if (plugins != null && plugins.length > 0) {

              Arrays.sort(plugins);

              // try to open the preview using each plugin and previewFile(File,Plugin)
              for (int i = 0; i < plugins.length; i++) {
                PreviewPanel panel = ((ViewerPlugin) plugins[i].getPlugin()).read(file);
                previewOpened = panel != null;

                if (previewOpened) {
                  foundPlugin = plugins[i].getPlugin();
                  i = plugins.length;
                }

              }
            }
          }
        }
      }

      // Now if we've found a plugin, we want to add this file to the group
      if (foundPlugin != null) {
        for (int g = 0; g < numGroups; g++) {
          AnalysisPluginGroup group = pluginGroups[g];
          if (group == null) {
            // haven't found a group, so create a new one
            group = new AnalysisPluginGroup(foundPlugin);
            group.addFile(file);

            pluginGroups[g] = group;
            g = numGroups; // skip over the remaining groups - already found one
          }
          else {
            // if this group matches, add it
            if (group.getPlugin() == foundPlugin) {
              group.addFile(file);

              g = numGroups; // skip over the remaining groups - already found one
            }
          }

        }
      }
    }

    // Now process the sub-directories only
    boolean processSubDirectories = Settings.getBoolean("SidePanel_DirectoryList_AnalyzeDirectory_ScanSubDirectories");

    if (processSubDirectories) {
      for (int i = 0; i < numDirectories; i++) {
        File file = directories[i];
        OLDprocessDirectory(file, pluginGroups);
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("rawtypes")
  public String toString() {
    Class cl = getClass();
    String name = cl.getName();
    Package pack = cl.getPackage();

    if (pack != null) {
      name = name.substring(pack.getName().length() + 1);
    }

    return Language.get(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void undo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }
  }

}
