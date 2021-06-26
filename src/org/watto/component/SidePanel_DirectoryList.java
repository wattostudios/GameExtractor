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

package org.watto.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.TemporarySettings;
import org.watto.TypecastSingletonManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.event.WSEnterableInterface;
import org.watto.event.WSSelectableInterface;
import org.watto.ge.helper.ShellFolderFile;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.NoConversionPlugin;
import org.watto.ge.plugin.PluginFinderMatchFileFilter;
import org.watto.ge.plugin.PluginList;
import org.watto.ge.plugin.PluginListBuilder;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.script.ScriptManager;
import org.watto.io.FilenameSplitter;
import org.watto.task.Task;
import org.watto.task.Task_AddFiles;
import org.watto.task.Task_AnalyzeDirectory;
import org.watto.task.Task_CutArchive;
import org.watto.task.Task_ExportFiles;
import org.watto.task.Task_Popup_ShowMessage;
import org.watto.task.Task_ReadArchive;
import org.watto.task.Task_ReadArchiveWithPlugin;
import org.watto.task.Task_ReadScriptArchive;
import org.watto.task.Task_RemoveFiles;
import org.watto.task.Task_ReplaceMatchingFiles;
import org.watto.task.Task_ReplaceSelectedFiles;
import org.watto.task.Task_ScanArchive;
import org.watto.task.Task_WriteArchive;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/

public class SidePanel_DirectoryList extends WSPanelPlugin implements WSSelectableInterface,
    WSEnterableInterface {

  String[] plugins = null;

  String[] extensions = null;

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The current controls **/
  WSPanel currentControl;

  // Panels and buttons for the control subgroups at the bottom of the sidepanel
  /** The controls for reading archives **/
  WSPanel readControls;

  /** The controls for modifying archives **/
  WSPanel modifyControls;

  /** The controls for extracting files from archives **/
  WSPanel exportControls;

  /** The controls for writing archives **/
  WSPanel writeControls;

  /** The controls for cutting archives **/
  WSPanel cutControls;

  /** The controls for analyzing directories **/
  WSPanel analyzeControls;

  /** The controls for reading script archives **/
  WSPanel scriptControls;

  /** The controls to show when tying to write/modify archives that are uneditable **/
  WSPanel invalidControls;

  /** Holder for the controls **/
  WSPanel controlHolder;

  /** Holder for the directory list **/
  WSDirectoryListHolder dirHolder;

  /** The first row of the control buttons **/
  WSPanel buttonPanelRow1 = null;

  /** The second row of the control buttons **/
  WSPanel buttonPanelRow2 = null;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_DirectoryList() {
    super(new XMLNode());
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   * @param caller the object that contains this component, created this component, or more
   *        formally, the object that receives events from this component.
   **********************************************************************************************
   **/
  public SidePanel_DirectoryList(XMLNode node) {
    super(node);
  }

  ///////////////
  //
  // Configurable
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addFiles() {
    File[] selectedFiles = dirHolder.getAllSelectedFiles();
    addFiles(selectedFiles);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addFiles(File selectedFile) {
    addFiles(new File[] { selectedFile });
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addFiles(File[] selectedFiles) {
    ArchivePlugin plugin = Archive.getReadPlugin();
    if (plugin != null) {
      if (!plugin.canWrite()) {
        WSPopup.showMessageInNewThread("ModifyArchive_NotWritable", true);
        return;
      }
    }

    Task_AddFiles task = new Task_AddFiles(selectedFiles);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
    TypecastSingletonManager.getTaskManager("TaskManager").add(task);
  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
   **********************************************************************************************
   * Changes the control bar to a different panel
   * @param controlName the name of the control panel to load
   * @param fullVersionOnly true if the panel to change to is only a full version feature
   **********************************************************************************************
   **/
  public void changeControls(String controlName, boolean fullVersionOnly) {

    boolean fullVersion = true;
    if (fullVersionOnly) {
      if (!checkFullVersion(false)) {
        //return;
        fullVersion = false;
      }
    }

    WSPanel newControl = currentControl;

    // reset the filter on the directoryList
    setFileFilter(null);

    boolean invalid = false;
    if (controlName.equals("ReadPanel")) {
      newControl = readControls;
      loadReadPlugins();
      setPluginFilter("SidePanel_DirectoryList_ReadPluginList", "CurrentSelectedReadPlugin");
    }
    else if (controlName.equals("ModifyPanel")) {
      if (!Archive.getReadPlugin().canWrite() && !Archive.getReadPlugin().canReplace()) {
        invalid = true;
      }
      else {
        newControl = modifyControls;
      }
    }
    else if (controlName.equals("ExportPanel")) {
      newControl = exportControls;
      setExportFilename(dirHolder.getSelectedFile());
      loadExportConverters();
    }
    else if (controlName.equals("WritePanel")) {
      newControl = writeControls;
      if (fullVersion) {
        loadWritePlugins();
        setPluginFilter("SidePanel_DirectoryList_WritePluginList", "CurrentSelectedWritePlugin", false);
        if (!Archive.getReadPlugin().canWrite() && !Archive.getReadPlugin().canReplace()) {
          invalid = true;
        }
        else {
          setWriteFilename(dirHolder.getSelectedFile());
        }
      }
    }
    else if (controlName.equals("CutPanel")) {
      newControl = cutControls;
      loadCutLengths();
      setCutFilename(dirHolder.getSelectedFile());
    }
    else if (controlName.equals("AnalyzePanel")) {
      newControl = analyzeControls;
      loadAnalyzeReportFormats();
      setAnalyzeFilename(dirHolder.getSelectedFile());
    }
    else if (controlName.equals("ScriptPanel")) {
      newControl = scriptControls;
      loadScriptPlugins();
    }

    controlHolder.removeAll();

    if (invalid) {
      // the panel is invalid. eg WritePanel is disabled for non-writable archives
      controlHolder.add(invalidControls, BorderLayout.NORTH);
    }
    else {
      currentControl = newControl;
      controlHolder.add(currentControl, BorderLayout.NORTH);
    }

    controlHolder.revalidate();
    controlHolder.repaint();

    Settings.set("SidePanel_DirectoryList_CurrentControl", controlName);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadAnalyzeReportFormats() {

    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectory_ExporterPlugins");

    if (pluginList == null) {
      return;
    }
    if (pluginList.getItemCount() > 0) {
      // already loaded previously
      return;
    }

    plugins = new String[] { "AnalyzeDirectory_Report_CSV", "AnalyzeDirectory_Report_Excel", "AnalyzeDirectory_Report_HTML", "AnalyzeDirectory_Report_JSON", "AnalyzeDirectory_Report_Tabbed", "AnalyzeDirectory_Report_XML" };
    extensions = new String[] { "csv", "xls", "html", "json", "txt", "xml" };

    int numPlugins = plugins.length;
    for (int i = 0; i < numPlugins; i++) {
      plugins[i] = Language.get(plugins[i]);
    }

    pluginList.setModel(new DefaultComboBoxModel(plugins));

    try {
      pluginList.setSelectedIndex(Settings.getInt("SidePanel_DirectoryList_AnalyzeDirectory_ExporterPlugins_CurrentSelectionIndex"));
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean checkFullVersion() {
    return checkFullVersion(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean checkFullVersion(boolean showPopup) {
    // basic version
    if (showPopup) {
      WSPopup.showErrorInNewThread("FullVersionOnly", true);
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void checkInvalidControls() {
    WSPanel newControl = currentControl;
    if (currentControl == null) {
      return;
    }

    String controlName = Settings.getString("SidePanel_DirectoryList_CurrentControl");
    if (controlName.equals("ModifyPanel") || controlName.equals("WritePanel")) {

      if (currentControl != invalidControls && (!Archive.getReadPlugin().canWrite() && !Archive.getReadPlugin().canReplace())) {
        // on one of these panels already, but these controls aren't allowed for this archive.
        // change it to the invalid controls
        newControl = invalidControls;
      }
      else if (currentControl == invalidControls) {
        // change it to the normal panel
        if (controlName.equals("ModifyPanel")) {
          newControl = modifyControls;
        }
        else if (controlName.equals("WritePanel")) {
          newControl = writeControls;
        }
      }
    }

    controlHolder.removeAll();
    controlHolder.add(newControl, BorderLayout.NORTH);
    controlHolder.revalidate();
    controlHolder.repaint();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void convertArchive() {
    WSComboBox convertPlugins = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_WritePluginList");
    Object pluginObj = convertPlugins.getSelectedItem();
    if (pluginObj == null || !(pluginObj instanceof PluginList)) {
      return;
    }
    ArchivePlugin plugin = ((PluginList) pluginObj).getPlugin();

    String dirName = dirHolder.getCurrentDirectory().getAbsolutePath();
    String filename = ((WSTextField) ComponentRepository.get("SidePanel_DirectoryList_WriteFilenameField")).getText();

    if (filename == null || filename.equals("")) {
      WSPopup.showErrorInNewThread("ConvertArchive_FilenameMissing", true);
      return;
    }

    // append the default extension, if no extension exists
    if (FilenameSplitter.getExtension(filename).equals("")) {
      filename += "." + plugin.getExtension(0);
    }

    if (filename.indexOf(dirName) >= 0) {
    }
    else {
      filename = dirName + File.separator + filename;
    }

    File outputPath = new File(filename);

    convertArchive(outputPath, plugin);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void convertArchive(File outputPath, ArchivePlugin plugin) {
    if (plugin != Archive.getReadPlugin()) {
      plugin.setDefaultProperties(true);
    }
    writeArchive(outputPath, plugin);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void cutArchive() {
    WSTextField cutInputFilenameField = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_CutInputFilenameField");
    WSTextField cutOutputFilenameField = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_CutOutputFilenameField");

    String inputFile = cutInputFilenameField.getText();
    String outputFile = cutOutputFilenameField.getText();

    if (inputFile.length() == 0) {
      WSPopup.showErrorInNewThread("CutArchive_InvalidInputFile", true);
      return;
    }

    if (outputFile.length() == 0) {
      outputFile = inputFile + ".zip";
    }

    String dirName = dirHolder.getCurrentDirectory().getAbsolutePath();

    if (inputFile.indexOf(dirName) < 0) {
      inputFile = dirName + File.separator + inputFile;
    }

    if (outputFile.indexOf(dirName) < 0) {
      outputFile = dirName + File.separator + outputFile;
    }

    File inputPath = new File(inputFile);
    File outputPath = new File(outputFile);

    if (!inputPath.exists() || inputPath.isDirectory()) {
      WSPopup.showErrorInNewThread("CutArchive_InvalidInputFile", true);
      return;
    }

    cutOutputFilenameField.setText(outputFile);

    int length = 0;
    try {
      String lengthString = (String) ((WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_CutLengthPresets")).getSelectedItem();
      if (lengthString.equals("500KB")) {
        length = 512000;
      }
      else if (lengthString.equals("1MB")) {
        length = 1048576;
      }
      else if (lengthString.equals("2MB")) {
        length = 2097152;
      }
      else if (lengthString.equals("5MB")) {
        length = 5242880;
      }
      else {
        length = Integer.parseInt(lengthString);
      }
    }
    catch (Throwable t) {
      WSPopup.showErrorInNewThread("CutArchive_InvalidCutLength", true);
      return;
    }

    cutArchive(inputPath, outputPath, length);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void cutArchive(File inputPath, File outputPath, int length) {
    Task_CutArchive task = new Task_CutArchive(inputPath, outputPath, length);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyzeDirectory() {

    String dirName = dirHolder.getCurrentDirectory().getAbsolutePath();
    File directoryPath = new File(dirName);

    if (directoryPath == null || !directoryPath.exists()) {
      WSPopup.showError("AnalyzeDirectory_FilenameMissing", true);
      return;
    }

    if (!directoryPath.isDirectory()) {
      WSPopup.showError("AnalyzeDirectory_FileNotADirectory", true);
      return;
    }

    WSTextField reportFilenameField = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectoryOutputFilenameField");
    String reportFile = reportFilenameField.getText();

    if (reportFile.length() == 0) {
      WSPopup.showErrorInNewThread("AnalyzeDirectory_InvalidReportFile", true);
      return;
    }

    analyzeDirectory(directoryPath, reportFile);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyzeDirectory(File inputPath, String reportFile) {
    Task_AnalyzeDirectory task = new Task_AnalyzeDirectory(inputPath, reportFile);
    task.setDirection(Task.DIRECTION_REDO);

    // If the user chose any Converter plugins, set them here
    task.setConverterPlugins(getChosenAnalysisConverters());

    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void exportAllFiles() {
    /*
    File directory = new File(((WSTextField) ComponentRepository.get("SidePanel_DirectoryList_ExportFilenameField")).getText());
    if (directory == null || !directory.exists()) {
      directory = dirHolder.getCurrentDirectory();
    }
    */

    File directory;
    try {
      WSTextField exportField = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_ExportFilenameField");
      String exportFilename = exportField.getText();
      if (exportFilename == null || exportFilename.length() <= 0) {
        // we haven't loaded the export panel, so show it, and then try exporting to the location again
        changeControls("ExportPanel", false);
        exportFilename = exportField.getText();
      }

      directory = new File(exportFilename);

      if (!directory.exists()) {
        directory = ShellFolderFile.getFileForPath(directory); // check if this is a special folder 
      }
    }
    catch (Throwable t) {
      // use the GE directory, if all else fails
      directory = new File(new File("").getAbsolutePath());
    }
    exportFiles(Archive.getResources(), directory);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void exportFiles(Resource[] files, File directory) {

    if (SingletonManager.has("ShowMessageBeforeExport")) {
      // show a message to the user (eg saying that they need QuickBMS or something)
      Task_Popup_ShowMessage popupTask = new Task_Popup_ShowMessage((String) SingletonManager.get("ShowMessageBeforeExport"));
      popupTask.setDirection(Task.DIRECTION_REDO);
      new Thread(popupTask).start();
      SingletonManager.remove("ShowMessageBeforeExport");
      return; // don't continue exporting - they need to click "Extract" again
    }

    Task_ExportFiles task = new Task_ExportFiles(directory, files);
    task.setDirection(Task.DIRECTION_REDO);

    // If the user chose any Converter plugins, set them here
    task.setConverterPlugins(getChosenExportConverters());

    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void exportSelectedFiles() {
    File directory;
    try {
      WSTextField exportField = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_ExportFilenameField");
      String exportFilename = exportField.getText();
      if (exportFilename == null || exportFilename.length() <= 0) {
        // we haven't loaded the export panel, so show it, and then try exporting to the location again
        changeControls("ExportPanel", false);
        exportFilename = exportField.getText();
      }

      directory = new File(exportFilename);

      if (!directory.exists()) {
        directory = ShellFolderFile.getFileForPath(directory); // check if this is a special folder 
      }
      // FIXED 3.10, was removing custom directories added to the field - if it doesn't exist, we actually want to create it
      //if (directory == null || !directory.exists()) { 
      //  directory = dirHolder.getCurrentDirectory();
      //}
    }
    catch (Throwable t) {
      // use the GE directory, if all else fails
      directory = new File(new File("").getAbsolutePath());
    }

    exportFiles(((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getSelected(), directory);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ViewerPlugin[] getChosenExportConverters() {
    WSComboBox imagePluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_ExportPanel_ImageConverters");
    ViewerPlugin[] imageConverters = getChosenConverters(imagePluginList);

    WSComboBox modelPluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_ExportPanel_ModelConverters");
    ViewerPlugin[] modelConverters = getChosenConverters(modelPluginList);

    int numConverters = imageConverters.length + modelConverters.length;
    if (numConverters == 2) {
      return new ViewerPlugin[] { imageConverters[0], modelConverters[0] };
    }
    else if (numConverters == 1) {
      if (imageConverters.length == 1) {
        return imageConverters;
      }
      else if (modelConverters.length == 1) {
        return modelConverters;
      }
    }

    return new ViewerPlugin[0];
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ViewerPlugin[] getChosenAnalysisConverters() {
    //WSComboBox imagePluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectory_ConverterPlugins");
    //return getChosenConverters(imagePluginList);

    WSComboBox imagePluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectory_ConverterPlugins");
    ViewerPlugin[] imageConverters = getChosenConverters(imagePluginList);

    WSComboBox modelPluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectory_ModelConverterPlugins");
    ViewerPlugin[] modelConverters = getChosenConverters(modelPluginList);

    int numConverters = imageConverters.length + modelConverters.length;
    if (numConverters == 2) {
      return new ViewerPlugin[] { imageConverters[0], modelConverters[0] };
    }
    else if (numConverters == 1) {
      if (imageConverters.length == 1) {
        return imageConverters;
      }
      else if (modelConverters.length == 1) {
        return modelConverters;
      }
    }

    return new ViewerPlugin[0];
  }

  /**
  **********************************************************************************************
  Only Image Conversion supported at this stage
  **********************************************************************************************
  **/
  public ViewerPlugin[] getChosenConverters(WSComboBox imagePluginList) {

    ViewerPlugin[] converters = new ViewerPlugin[1];
    int numConverters = 0;

    // IMAGE conversion type
    if (imagePluginList != null) {
      ViewerPlugin imagePlugin = (ViewerPlugin) imagePluginList.getSelectedItem();
      if (imagePlugin != null) {
        if (!(imagePlugin instanceof NoConversionPlugin)) { // ignore the "No Conversion" plugin (same as "Full Version Only" plugin)
          converters[numConverters] = imagePlugin;
          numConverters++;
        }
      }
    }

    if (numConverters == 0) {
      return new ViewerPlugin[0];
    }
    else if (numConverters == converters.length) {
      return converters;
    }
    else {
      // resize the array and then return it
      ViewerPlugin[] oldConverters = converters;
      converters = new ViewerPlugin[numConverters];
      System.arraycopy(oldConverters, 0, converters, 0, numConverters);
      return converters;
    }
  }

  /**
   **********************************************************************************************
   * Gets the plugin description
   **********************************************************************************************
   **/
  @Override
  public String getDescription() {
    String description = toString() + "\n\n" + Language.get("Description_SidePanel");

    if (!isEnabled()) {
      description += "\n\n" + Language.get("Description_PluginDisabled");
    }
    else {
      description += "\n\n" + Language.get("Description_PluginEnabled");
    }

    return description;
  }

  /**
   **********************************************************************************************
   * Gets the plugin name
   **********************************************************************************************
   **/
  @Override
  public String getText() {
    return super.getText();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadControlPanels() {
    readControls = (WSPanel) WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_DirectoryList_ReadPanel.xml")));
    scriptControls = (WSPanel) WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_DirectoryList_ScriptPanel.xml")));
    exportControls = (WSPanel) WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_DirectoryList_ExtractPanel.xml")));

    if (checkFullVersion(false)) {
      // full version panels
      modifyControls = (WSPanel) WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_DirectoryList_ModifyPanel.xml")));
      writeControls = (WSPanel) WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_DirectoryList_WritePanel.xml")));
    }
    else {
      // basic panels
      modifyControls = (WSPanel) WSHelper.toComponent(XMLReader.read("<WSPanel code=\"SidePanel_DirectoryList_ModifyPanel_Main\" showBorder=\"true\" layout=\"BorderLayout\"><WSLabel code=\"SidePanel_DirectoryList_ModifyPanel_Basic\" wrap=\"true\" vertical-alignment=\"true\" height=\"80\" position=\"CENTER\" /></WSPanel>"));
      writeControls = (WSPanel) WSHelper.toComponent(XMLReader.read("<WSPanel code=\"SidePanel_DirectoryList_WritePanel_Main\" showBorder=\"true\" layout=\"BorderLayout\"><WSLabel code=\"SidePanel_DirectoryList_WritePanel_Basic\" wrap=\"true\" vertical-alignment=\"true\" height=\"80\" position=\"CENTER\" /></WSPanel>"));
    }

    cutControls = (WSPanel) WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_DirectoryList_CutPanel.xml")));

    analyzeControls = (WSPanel) WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_DirectoryList_AnalyzePanel.xml")));

    invalidControls = (WSPanel) WSHelper.toComponent(XMLReader.read("<WSPanel code=\"SidePanel_DirectoryList_ReadPanel_Main\" repository=\"false\" showBorder=\"true\" layout=\"BorderLayout\"><WSLabel code=\"SidePanel_DirectoryList_InvalidControls\" wrap=\"true\" vertical-alignment=\"true\" height=\"80\" position=\"CENTER\" /></WSPanel>"));

    loadExportConverters();
    loadAnalysisConverters();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadCutLengths() {

    WSComboBox cutLengthsList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_CutLengthPresets");
    cutLengthsList.setModel(new DefaultComboBoxModel(new String[] { "500KB", "1MB", "2MB", "5MB" }));

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadDirList() {
    //dirHolder.loadPanel(Settings.get("DirectoryListView"));
    //dirHolder.setMultipleSelection(false);

    //dirHolder.revalidate();
    //dirHolder.repaint();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadExportConverters() {
    WSComboBox imagePluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_ExportPanel_ImageConverters");
    PreviewPanel imagePreviewPanel = new PreviewPanel_Image(); // a dummy preview panel
    loadConverters(imagePluginList, imagePreviewPanel);

    WSComboBox modelPluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_ExportPanel_ModelConverters");
    PreviewPanel modelPreviewPanel = new PreviewPanel_3DModel(); // a dummy preview panel
    loadConverters(modelPluginList, modelPreviewPanel);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadAnalysisConverters() {
    WSComboBox imagePluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectory_ConverterPlugins");
    PreviewPanel imagePreviewPanel = new PreviewPanel_Image(); // a dummy preview panel
    loadConverters(imagePluginList, imagePreviewPanel);

    WSComboBox modelPluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectory_ModelConverterPlugins");
    PreviewPanel modelPreviewPanel = new PreviewPanel_3DModel(); // a dummy preview panel
    loadConverters(modelPluginList, modelPreviewPanel);
  }

  /**
  **********************************************************************************************
  Loads all the available converters of a given type into a combobox.
  @param pluginList the list to load the plugins into
  @param convertType the type of conversion (eg Image, Model, etc)
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadConverters(WSComboBox pluginList, PreviewPanel convertType) {
    if (pluginList == null) {
      return;
    }
    if (pluginList.getItemCount() > 0) {
      // already loaded previously
      return;
    }

    if (!checkFullVersion(false)) {
      // Not the full version - there's no converters available.
      // Load a dummy plugin that says "only in full version"
      NoConversionPlugin plugin = new NoConversionPlugin();
      plugin.setCode("ConversionInFullVersionOnly");

      ViewerPlugin[] plugins = new ViewerPlugin[] { plugin };
      pluginList.setModel(new DefaultComboBoxModel(plugins));
      return;
    }

    ViewerPlugin[] plugins = PluginListBuilder.getWriteViewers(convertType);
    if (plugins == null || plugins.length <= 0) {
      return;
    }

    // If we only want to show standard converters, remove the non-standard ones here
    if (Settings.getBoolean("OnlyShowStandardConverters")) {
      int writePos = 0;

      int numPlugins = plugins.length;
      for (int i = 0; i < numPlugins; i++) {
        if (plugins[i].isStandardFileFormat()) {
          plugins[writePos] = plugins[i];
          writePos++;
        }
      }

      ViewerPlugin[] oldPlugins = plugins;
      plugins = new ViewerPlugin[writePos];
      System.arraycopy(oldPlugins, 0, plugins, 0, writePos);
    }

    // Sort the plugins by name
    Arrays.sort(plugins);

    // add a "No Conversion" option to the plugins
    int numViewerPlugins = plugins.length;
    ViewerPlugin[] oldPlugins = plugins;
    plugins = new ViewerPlugin[numViewerPlugins + 1];
    System.arraycopy(oldPlugins, 0, plugins, 1, numViewerPlugins);
    plugins[0] = new NoConversionPlugin();// add the No Conversion plugin to the top of the list

    pluginList.setModel(new DefaultComboBoxModel(plugins));

    return;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadReadPlugins() {
    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_ReadPluginList");
    pluginList.setModel(new DefaultComboBoxModel(PluginListBuilder.getPluginList()));
    pluginList.addItem(new PluginList(Language.get("AllFiles"), new AllFilesPlugin()));

    int selectedItem = TemporarySettings.getInt("CurrentSelectedReadPlugin");
    if (selectedItem != -1) {
      pluginList.setSelectedIndex(selectedItem);
    }
    else {
      pluginList.setSelectedIndex(pluginList.getItemCount() - 1);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadScriptPlugins() {

    WSComboBox scriptPluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_ScriptPluginList");

    PluginList[] plugins = null;
    try {
      plugins = PluginListBuilder.getPluginList(WSPluginManager.getGroup("Script").getPlugins());
    }
    catch (Throwable t) {
      // plugins not loaded
    }
    if (plugins == null || plugins.length == 0) {
      // so we only load the scripts when they are needed
      ScriptManager.loadScripts();
      plugins = PluginListBuilder.getPluginList(WSPluginManager.getGroup("Script").getPlugins());
    }
    scriptPluginList.setModel(new DefaultComboBoxModel(plugins));

    String currentScript = Settings.get("SelectedScript");

    for (int i = 0; i < plugins.length; i++) {
      if (plugins[i].getPlugin().getName().equals(currentScript)) {
        scriptPluginList.setSelectedIndex(i);
        break;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadWritePlugins() {
    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_WritePluginList");
    PluginList[] plugins = PluginListBuilder.getWritePluginList();
    pluginList.setModel(new DefaultComboBoxModel(plugins));

    int selectedItem = TemporarySettings.getInt("CurrentSelectedWritePlugin");
    if (selectedItem > 0) {
      pluginList.setSelectedIndex(selectedItem);
    }
    else {
      ArchivePlugin plugin = Archive.getReadPlugin();

      if (!(plugin instanceof AllFilesPlugin)) {
        int numFiles = plugins.length;
        //String code = plugin.getCode();

        for (int i = 0; i < numFiles; i++) {
          if (plugins[i].getPlugin() == plugin) {
            pluginList.setSelectedIndex(i);
            break;
          }
        }

      }
    }
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSClickableListener when a click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    if (c instanceof WSButton) {
      String code = ((WSButton) c).getCode();

      if (code == null) {
        return false;
      }

      // Control Panels
      else if (code.equals("SidePanel_DirectoryList_ReadPanelButton")) {
        changeControls("ReadPanel", false);
      }
      else if (code.equals("SidePanel_DirectoryList_ModifyPanelButton")) {
        changeControls("ModifyPanel", true);
      }
      else if (code.equals("SidePanel_DirectoryList_ExportPanelButton")) {
        changeControls("ExportPanel", true);
      }
      else if (code.equals("SidePanel_DirectoryList_WritePanelButton")) {
        changeControls("WritePanel", true);
      }
      else if (code.equals("SidePanel_DirectoryList_CutPanelButton")) {
        changeControls("CutPanel", false);
      }
      else if (code.equals("SidePanel_DirectoryList_ScriptPanelButton")) {
        changeControls("ScriptPanel", false);
      }
      else if (code.equals("SidePanel_DirectoryList_AnalyzePanelButton")) {
        changeControls("AnalyzePanel", false);
      }

      // Buttons on the Control Panels
      else if (code.equals("SidePanel_DirectoryList_ReadArchiveButton")) {
        readArchive();
      }
      else if (code.equals("SidePanel_DirectoryList_ReadScriptArchiveButton")) {
        readScriptArchive();
      }
      else if (code.equals("SidePanel_DirectoryList_AddFileButton")) {
        if (checkFullVersion()) {
          addFiles();
        }
      }
      else if (code.equals("SidePanel_DirectoryList_RemoveFileButton")) {
        if (checkFullVersion()) {
          removeFiles();
        }
      }
      else if (code.equals("SidePanel_DirectoryList_ReplaceFileSelectedButton")) {
        if (checkFullVersion()) {
          replaceSelectedFiles();
        }
      }
      else if (code.equals("SidePanel_DirectoryList_ReplaceFileMatchingButton")) {
        if (checkFullVersion()) {
          replaceMatchingFiles();
        }
      }
      else if (code.equals("SidePanel_DirectoryList_WriteArchiveButton")) {
        if (checkFullVersion()) {
          writeArchive();
        }
      }
      else if (code.equals("SidePanel_DirectoryList_ConvertArchiveButton")) {
        if (checkFullVersion()) {
          convertArchive();
        }
      }
      else if (code.equals("SidePanel_DirectoryList_ExportAllButton")) {
        exportAllFiles();
      }
      else if (code.equals("SidePanel_DirectoryList_ExportSelectedButton")) {
        exportSelectedFiles();
      }
      else if (code.equals("SidePanel_DirectoryList_ScanArchiveButton")) {
        if (checkFullVersion()) {
          scanArchive();
        }
      }
      else if (code.equals("SidePanel_DirectoryList_CutArchiveButton")) {
        cutArchive();
      }
      else if (code.equals("SidePanel_DirectoryList_AnalyzeDirectoryButton")) {
        analyzeDirectory();
      }

      else {
        return false;
      }

      // returns true even if not the full version,
      // because the click was still handled by this class.
      return true;

    }

    else if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();

      if (code == null) {
        return false;
      }

      if (code.equals("DirectoryList")) {
        // directory list
        if (currentControl == writeControls) {
          setWriteFilename(dirHolder.getSelectedFile());
        }
        else if (currentControl == exportControls) {
          setExportFilename(dirHolder.getCurrentDirectory());
        }
        else if (currentControl == cutControls) {
          setCutFilename(dirHolder.getSelectedFile());
        }
        else if (currentControl == analyzeControls) {
          setAnalyzeFilename(dirHolder.getSelectedFile());
        }
        else {
          return false;
        }
        return true;
      }

    }

    return false;
  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be closed. This method
   * does nothing by default, but can be overwritten to do anything else needed before the panel
   * is closed, such as garbage collecting and closing pointers to temporary objects.
   **********************************************************************************************
   **/
  @Override
  public void onCloseRequest() {
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSSelectableListener when an item is deselected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDeselect(JComponent c, Object e) {
    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSDoubleClickableListener when a double click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDoubleClick(JComponent c, MouseEvent e) {
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();

      if (code == null) {
        return false;
      }

      if (code.equals("DirectoryList")) {
        // perform double click on the directory list

        File selected = dirHolder.getSelectedFile();

        // read
        if (currentControl == readControls) {
          if (!selected.isDirectory()) {
            readArchive(selected);
          }
          return true;
        }
        // script
        else if (currentControl == scriptControls) {
          if (!selected.isDirectory()) {
            readScriptArchive(selected);
          }
          return true;
        }

        // double click reads file by default (if setting is enabled)
        // it is here so that it is checked AFTER the script, thus allowing
        // double-clicks to be handled by the script panel if it is open
        else if (Settings.getBoolean("OpenArchiveOnDoubleClick")) {
          if (!selected.isDirectory()) {
            readArchive(selected);
          }
          return true;
        }

        // modify (add)
        else if (currentControl == modifyControls) {
          if (checkFullVersion(false)) {
            addFiles();
          }
          return true;
        }

      }

    }
    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSEnterableListener when a key is pressed
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onEnter(JComponent c, java.awt.event.KeyEvent e) {
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();
      if (code == null) {
        return false;
      }

      if (code.equals("SidePanel_DirectoryList_ExportFilenameField")) {
        exportAllFiles();
      }
      else if (code.equals("SidePanel_DirectoryList_WriteFilenameField")) {
        if (checkFullVersion()) {
          writeArchive();
        }
      }
      else if (code.equals("SidePanel_DirectoryList_CutInputFilenameField")) {
        ((WSTextField) ComponentRepository.get("SidePanel_DirectoryList_CutOutputFilenameField")).requestFocus();
      }
      else if (code.equals("SidePanel_DirectoryList_CutOutputFilenameField")) {
        cutArchive();
      }
      else if (code.equals("SidePanel_DirectoryList_AnalyzeDirectoryOutputFilenameField")) {
        analyzeDirectory();
      }
      return true;
    }
    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSHoverableListener when the mouse moves over an object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHover(JComponent c, MouseEvent e) {
    return super.onHover(c, e);
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSHoverableListener when the mouse moves out of an object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHoverOut(JComponent c, MouseEvent e) {
    return super.onHoverOut(c, e);
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSKeyableListener when a key press occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onKeyPress(JComponent c, KeyEvent e) {
    return false;
  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be opened. By default,
   * it just calls checkLoaded(), but can be overwritten to do anything else needed before the
   * panel is displayed, such as resetting or refreshing values.
   **********************************************************************************************
   **/
  @Override
  public void onOpenRequest() {
    String controlName = Settings.getString("SidePanel_DirectoryList_CurrentControl");
    boolean fullVersionOnly = true;
    if (controlName.equals("ReadPanel") || controlName.equals("ScriptPanel") || controlName.equals("CutPanel") || controlName.equals("AnalyzePanel") || controlName.equals("ExportPanel")) {
      fullVersionOnly = false;
    }
    changeControls(controlName, fullVersionOnly);

    // Show/Hide the Advanced controls
    if (buttonPanelRow2 == null) {
      buttonPanelRow1 = ((WSPanel) ComponentRepository.get("SidePanel_DirectoryList_ButtonsPanelRow1"));
      buttonPanelRow2 = ((WSPanel) ComponentRepository.get("SidePanel_DirectoryList_ButtonsPanelRow2"));
    }

    WSPanel buttonPanel = ((WSPanel) ComponentRepository.get("SidePanel_DirectoryList_ButtonsPanel"));

    if (Settings.getBoolean("ShowAdvancedControlsInDirectoryList")) {
      buttonPanel.removeAll();
      buttonPanel.setLayout(new GridLayout(2, 1));
      buttonPanel.add(buttonPanelRow1);
      buttonPanel.add(buttonPanelRow2);
    }
    else {
      buttonPanel.removeAll();
      buttonPanel.setLayout(new GridLayout(1, 1));
      buttonPanel.add(buttonPanelRow1);
    }

    dirHolder.checkFiles();
    dirHolder.scrollToSelected();
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSSelectableListener when an item is selected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onSelect(JComponent c, Object e) {
    if (c instanceof WSComboBox) {
      String code = ((WSComboBox) c).getCode();

      if (code.equals("SidePanel_DirectoryList_ReadPluginList")) {
        setPluginFilter(code, "CurrentSelectedReadPlugin");
      }
      else if (code.equals("SidePanel_DirectoryList_WritePluginList")) {
        setPluginFilter(code, "CurrentSelectedWritePlugin", false);
        setWriteFilename();
        setFileFilter(null);
      }

      if (code.equals("SidePanel_DirectoryList_ScriptPluginList")) {
        String scriptName = ((PluginList) ((WSComboBox) c).getSelectedItem()).getPlugin().getName();
        Settings.set("SelectedScript", scriptName);
        return true;
      }

      if (code.equals("SidePanel_DirectoryList_AnalyzeDirectory_ExporterPlugins")) {
        setAnalyzeFilename(dirHolder.getSelectedFile());

        try {
          WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectory_ExporterPlugins");
          Settings.set("SidePanel_DirectoryList_AnalyzeDirectory_ExporterPlugins_CurrentSelectionIndex", pluginList.getSelectedIndex());
        }
        catch (Throwable t) {
        }
        return true;
      }

    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readArchive() {
    File selectedFile = dirHolder.getSelectedFile();
    readArchive(selectedFile);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readArchive(File selectedFile) {
    WSComboBox readPlugins = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_ReadPluginList");
    PluginList selectedItem = (PluginList) readPlugins.getSelectedItem();

    WSPlugin plugin = null;
    if (selectedItem != null) {
      plugin = selectedItem.getPlugin();
    }
    else {
      plugin = new AllFilesPlugin();
    }

    if (plugin instanceof AllFilesPlugin) {
      // auto-detect a plugin
      Task_ReadArchive task = new Task_ReadArchive(selectedFile);
      task.setDirection(Task.DIRECTION_REDO);
      new Thread(task).start();
    }
    else {
      // open with the chosen plugin
      Task_ReadArchiveWithPlugin task = new Task_ReadArchiveWithPlugin(selectedFile, plugin);
      task.setDirection(Task.DIRECTION_REDO);
      new Thread(task).start();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readScriptArchive() {
    readScriptArchive(dirHolder.getSelectedFile());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readScriptArchive(File selectedFile) {
    WSComboBox scriptPlugins = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_ScriptPluginList");
    Object pluginObj = scriptPlugins.getSelectedItem();
    if (pluginObj == null) {
      return;
    }
    ArchivePlugin plugin = ((PluginList) pluginObj).getPlugin();

    readScriptArchive(selectedFile, plugin);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readScriptArchive(File selectedFile, ArchivePlugin scriptPlugin) {
    Task_ReadScriptArchive task = new Task_ReadScriptArchive(selectedFile, scriptPlugin);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /**
   **********************************************************************************************
   * Registers the events that this component generates
   **********************************************************************************************
   **/
  @Override
  public void registerEvents() {
    super.registerEvents();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reloadDirectoryList() {
    dirHolder.reload();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void removeFiles() {
    Resource[] selectedFiles = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getAllSelectedFiles();
    removeFiles(selectedFiles);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void removeFiles(Resource[] selectedFiles) {
    ArchivePlugin plugin = Archive.getReadPlugin();
    if (plugin != null) {
      if (!plugin.canWrite()) {
        WSPopup.showMessageInNewThread("ModifyArchive_NotWritable", true);
        return;
      }
    }

    Task_RemoveFiles task = new Task_RemoveFiles(selectedFiles);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
    TypecastSingletonManager.getTaskManager("TaskManager").add(task);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void replaceMatchingFiles() {
    //Resource[] selectedFiles = ((WSFileListPanelHolder)ComponentRepository.get("FileListPanelHolder")).getAllSelectedFiles();
    Resource[] selectedFiles = Archive.getResources();
    File[] newFiles = dirHolder.getAllSelectedFiles();
    if (newFiles == null || newFiles.length <= 0) {
      newFiles = new File[] { dirHolder.getCurrentDirectory() };
    }

    if (newFiles.length == 1 && newFiles[0].isDirectory()) {
      replaceMatchingFiles(selectedFiles, newFiles[0]);
    }
    else {
      replaceMatchingFiles(selectedFiles, new File(newFiles[0].getParent()));
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void replaceMatchingFiles(Resource[] selectedFiles, File baseDir) {
    ArchivePlugin plugin = Archive.getReadPlugin();
    if (plugin != null) {
      if (!plugin.canWrite() && !plugin.canReplace()) {
        WSPopup.showMessageInNewThread("ModifyArchive_NotReplacable", true);
        return;
      }
    }

    Task_ReplaceMatchingFiles task = new Task_ReplaceMatchingFiles(selectedFiles, baseDir);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
    TypecastSingletonManager.getTaskManager("TaskManager").add(task);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void replaceSelectedFiles() {
    Resource[] selectedFiles = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getAllSelectedFiles();
    File newFile = dirHolder.getSelectedFile();
    replaceSelectedFiles(selectedFiles, newFile);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void replaceSelectedFiles(Resource[] selectedFiles, File newFile) {
    ArchivePlugin plugin = Archive.getReadPlugin();
    if (plugin != null) {
      if (!plugin.canWrite() && !plugin.canReplace()) {
        WSPopup.showMessageInNewThread("ModifyArchive_NotReplacable", true);
        return;
      }
    }

    Task_ReplaceSelectedFiles task = new Task_ReplaceSelectedFiles(selectedFiles, newFile);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
    TypecastSingletonManager.getTaskManager("TaskManager").add(task);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    dirHolder.requestFocus();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void scanArchive() {
    File selectedFile = dirHolder.getSelectedFile();
    scanArchive(selectedFile);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void scanArchive(File selectedFile) {
    Task_ScanArchive task = new Task_ScanArchive(selectedFile);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setCutFilename(File file) {
    if (file.isDirectory()) {
      return;
    }

    WSTextField inputField = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_CutInputFilenameField");
    WSTextField outputField = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_CutOutputFilenameField");

    String filename = file.getName();
    inputField.setText(filename);

    int dotPos = filename.lastIndexOf(".");
    if (dotPos > 0) {
      filename = filename.substring(0, dotPos);
    }
    filename += ".zip";

    outputField.setText(filename);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setAnalyzeFilename(File file) {
    if (!file.isDirectory()) {
      return;
    }

    String extension = "html";
    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectory_ExporterPlugins");
    if (pluginList != null) {
      int selectedExtension = pluginList.getSelectedIndex();
      if (selectedExtension >= 0 && extensions != null && selectedExtension < extensions.length) {
        extension = extensions[selectedExtension];
      }
    }

    WSTextField inputField = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_AnalyzeDirectoryOutputFilenameField");

    String filename = file.getName() + "." + extension;
    inputField.setText(filename);
  }

  /**
   **********************************************************************************************
   * Sets the description of the plugin
   * @param description the description
   **********************************************************************************************
   **/
  @Override
  public void setDescription(String description) {
    super.setDescription(description);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setExportFilename() {
    WSTextField field = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_ExportFilenameField");
    String filename = field.getText();

    if (filename == null || filename.length() <= 0) {
      setExportFilename(dirHolder.getSelectedFile());
    }
    else {
      setExportFilename(filename);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setExportFilename(File file) {
    if (file == null) {
      return;
    }
    setExportFilename(file.getAbsolutePath());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setExportFilename(String filename) {
    WSTextField field = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_ExportFilenameField");

    // The export field should only show directory names, not filenames...
    File directory = new File(filename);
    if (directory.exists() && directory.isFile()) {
      filename = directory.getParentFile().getAbsolutePath();
    }

    if (field.getText().equals(filename)) {
      return; // already the same
    }

    field.setText(filename);
    field.setCaretPosition(filename.length());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setFileFilter(FileFilter filter) {
    dirHolder.setMatchFilter(filter);
    dirHolder.reload();
    dirHolder.scrollToSelected();
  }

  /**
   **********************************************************************************************
   * sets the filter to the filter obtained from the WSComboBox with the given code name
   **********************************************************************************************
   **/
  public void setPluginFilter(String comboBoxCode, String settingCode) {
    setPluginFilter(comboBoxCode, settingCode, true);
  }

  /**
   **********************************************************************************************
   * sets the filter to the filter obtained from the WSComboBox with the given code name
   **********************************************************************************************
   **/
  public void setPluginFilter(String comboBoxCode, String settingCode, boolean setFilter) {

    WSComboBox combo = (WSComboBox) ComponentRepository.get(comboBoxCode);

    if (setFilter) {
      PluginList list = (PluginList) combo.getSelectedItem();
      if (list == null || !combo.isEnabled()) {
        setFileFilter(null);
      }
      else {
        setFileFilter(new PluginFinderMatchFileFilter(list.getPlugin()));
      }
    }

    TemporarySettings.set(settingCode, combo.getSelectedIndex());

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setWriteFilename() {
    WSTextField field = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_WriteFilenameField");
    String filename = field.getText();

    if (filename == null || filename.length() <= 0) {
      setWriteFilename(dirHolder.getSelectedFile());
    }
    else {
      setWriteFilename(filename);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setWriteFilename(File file) {
    if (file == null) {
      return;
    }
    setWriteFilename(file.getAbsolutePath());
  }

  ///////////////
  //
  // Default Implementations
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setWriteFilename(String filename) {
    WSTextField field = (WSTextField) ComponentRepository.get("SidePanel_DirectoryList_WriteFilenameField");

    int dotPos = filename.lastIndexOf(".");
    int slashPos = filename.lastIndexOf("\\");
    if (slashPos < 0) {
      slashPos = filename.lastIndexOf("/");
    }

    if (dotPos > 0 && dotPos > slashPos) {
      filename = filename.substring(0, dotPos);
    }
    else {
      filename = "newArchive";
    }

    String extension = "unk";

    ArchivePlugin plugin = Archive.getReadPlugin();
    if (plugin != null) {
      extension = plugin.getExtension(0);
      if (extension == null || extension.length() == 0 || extension.equals("*")) {
        extension = "unk";
      }
    }

    filename += "." + extension;

    if (field.getText().equals(filename)) {
      return; // already the same
    }

    field.setText(filename);
    field.setCaretPosition(filename.length());
  }

  /**
   **********************************************************************************************
   * Build this object from the <i>node</i>
   * @param node the XML node that indicates how to build this object
   **********************************************************************************************
   **/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    setLayout(new BorderLayout());

    // Build an XMLNode tree containing all the elements on the screen
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_DirectoryList.xml"));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    add(component, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) component).getCode());
    ComponentRepository.add(this);

    loadControlPanels();

    controlHolder = (WSPanel) ComponentRepository.get("SidePanel_DirectoryList_ControlsHolder");
    dirHolder = (WSDirectoryListHolder) ComponentRepository.get("SidePanel_DirectoryList_DirectoryListHolder");

    //changeControls("ReadPanel",false);

    //loadDirList();
  }

  /**
   **********************************************************************************************
   * Builds an XMLNode that describes this object
   * @return an XML node with the details of this object
   **********************************************************************************************
   **/
  @Override
  public XMLNode toXML() {
    return super.toXML();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeArchive() {
    ArchivePlugin plugin = Archive.getReadPlugin();

    if (plugin instanceof AllFilesPlugin) {
      convertArchive();
      return;
    }

    String dirName = dirHolder.getCurrentDirectory().getAbsolutePath();
    String filename = ((WSTextField) ComponentRepository.get("SidePanel_DirectoryList_WriteFilenameField")).getText();

    if (filename == null || filename.equals("")) {
      WSPopup.showErrorInNewThread("WriteArchive_FilenameMissing", true);
      return;
    }

    // append the default extension, if no extension exists
    if (FilenameSplitter.getExtension(filename).equals("")) {
      filename += "." + plugin.getExtension(0);
    }

    if (filename.indexOf(dirName) >= 0) {
    }
    else {
      filename = dirName + File.separator + filename;
    }

    File outputPath = new File(filename);

    writeArchive(outputPath, plugin);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void writeArchive(File outputPath, ArchivePlugin plugin) {
    Task_WriteArchive task = new Task_WriteArchive(outputPath, plugin);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

}