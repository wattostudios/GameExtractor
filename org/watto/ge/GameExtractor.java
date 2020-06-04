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

package org.watto.ge;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.JFrame;
/***************************************** TODO LIST *****************************************\

// FOR PACKAGING AND RELEASE...
- Update the version numbers in the Settings file, Language file, NSIS Installer, and Launcher EXE
- Upload the Basic Version and Source Code to a new folder on SourceForge and set it as the default
- Update the Supported Games List on the website
- Update the schema.org code at the bottom of the GameExtractor.html page
- Deploy the Full Version to Google App Engine (refresh the project first!)
- Post an update on Facebook and Twitter
- Inform SoftPedia of the new Release and the Changes
- Update the Plugins Spreadsheet

// TO DO EVERY TIME...
 - New EXPORTERS need to be added in to SidePanel_PluginList.loadExporters()
 - If a plugin uses QuickBMS Compression in CHUNKS/BLOCKS, ensure the compression method is in QuickBMSHelper.buildProcessingScript()

// EXAMPLE PLUGINS...
- Archive Properties (Resource_Property) - Plugin_FPK_FKP
- Forcing TXT Previews for some files - Plugin_VPK --> see method previewHint(Resource)
- Custom Table Columns - Plugin_ZIP_PK
- Replaceable Archive writing (including renaming of files) - Plugin_FPK_FKP
- Using QuickBMS for decompressing files with uncommon compression formats - Plugin_PAK_PACK_4
- Using an Exporter for a file that is compressed in multiple blocks - Plugin_CAGE
- Image Viewer with properties on the ImageResource, including writing with properties - Viewer_1TXD_1TXD
- Writing DXT/DDS images, including padding smallest mipmaps to minimum 4x4 - Viewer_1TXD_1TXD or Viewer_3TXD_3TXD
- Archive that is decompressed first, then analysed - Plugin_BIG_BIGF or Plugin_UE3_576
- Archive where the directory needs to be decompressed first, then analysed - Plugin_PAK_PAK_3
- Archive with nested directories that we need to read - Plugin_FMF_FMF
- Archive where multiple files are stored in a single ZLib block, so you need to decompress the ZLib, then find the file within it - Plugin_RSB_1BSR
- Image Viewer where the file is (optionally) decompressed before being viewed - Viewer_UE3_Texture2D_648 / 539
- Image Viewer where a separate Palette file is extracted from the archive, and then used to create the image - IFF_SPR
- Image Viewer where the image data is a big image, but it's read as blocks of 32x32 - Viewer_RSB_1BSR_PTX (see reorderPixelBlocks() method)
- Image Viewer where the image width and height are stored on the Resource by the Plugin, so need to be retrieved before processing the image in the Viewer
- Scanning unknown files to determine their file type (by adding some custom types to the list of standard ones) - Plugin_PAK_44
- Scanning unknown files to determine their file type (custom, rather than using the automatic scanner) - Plugin_000_9 (see end of read() method)

// STANDARD RESOURCE PROPERTY CODES... (Note the uppercase/lowercase)
- MipmapCount
- ColorCount
- FrameCount (the number of frames in an animation)
- FileID
- Hash
- Filename
- ImageFormat (eg DXT1, DXT3, DXT5, ARGB, 16F16F16F16F_ABGR, 8BitPaletted, etc.)
- Version (A number, such as an Unreal version number (95, 63, 127, ...)
- Width
- Height
- XOffset
- YOffset

// COMMON ARCHIVE PLUGINS...
- Unreal Engine 4 = Plugin_PAK_38
- Unreal Engine 3 with support for decompressing the whole archive first, if needed = Plugin_UE3_868 or Plugin_UE3_576
- Valve Source Engine = Plugin_VPK
- Unity3D Engine (version 15) = Plugin_ASSETS_15
- Unity3D Engine (version 17) = Plugin_ASSETS_17
- NW_PAK = Plugin_PAK_44 or Plugin_PAK_48

// NEW IDEAS...
- Add legal info to GE similarly to the website - eg no refunds, no guarantees, no support for non-windows, etc.
- The KeyListeners on the FileListPanels and DirPanel (for selecting next filename with given letter) should allow input of text strings
  - ie pressing 2 characters should look for files starting with these 2 characters.
  - Resets the search string after 1 second.
- Build a package for the Microsoft Store, and other similar websites
- Popup when loading for the first time, offering wizards and help
  - WSOverlayPopup with the ability to load in a custom Layout XML file
- Integrate with Windows Explorer, like ZIP files.
  - Can then add right-click items to Windows Explorer, eg "Extract all to new folder", which calls the command-line interface of GE
    - https://superuser.com/questions/392212/how-can-i-add-a-program-to-the-context-menu-of-all-files?rq=1
- YouTube videos showing how to do common tasks
- Add filters to the thumbnail view, just like TreeTable
  - Include a Filename Filter above the "Groups" for simple matching like *Texture* for all files with Texture in the name
- Add a SidePanel for "Search in contents of selected files" and "search in all file contents"
  - Instead of reading through from the offset, need to extract using the exporter, then read from the file instead.
    - Task_SearchFileContents
- Add a "reset to defaults" button for the Options, which grabs the defaults from defaultSettings, etc.
- Extracting files from a tree branch should extract all files within the branch
- Select All / Select None buttons for SidePanels like Search and FileListExporter
- Add right-click menu (and buttons) to the DirList
  - Make new folder
  - Rename file/folder
  - Delete file
- Option to auto-shrink the SidePanel when on mouseout, and unshrink when on mouseover

// PLUGIN THINGS...
- Write an FFMPEG converter, to add it to the bottom of Audio Previews, so you can convert WAV to OGG, for example
- BC7 Renderer (for "A Way Out" game, and "Park the Car" etc.) --> C:\_WATTOz\____Development_Stuff\lwjgl-release-3.1.6-custom\EXAMPLE_CODE.java
  --> See if we can implement this --> https://github.com/hglm/detex/blob/master/decompress-bptc.c
  --> Also BC6/7 as per game Blind Mind and First Telegram War
  --> https://github.com/Microsoft/DirectXTex/wiki/Texconv
- ETC1 and ETC2 Decompression from here --> C:\_WATTOz\____Development_Stuff\detex-master\detex-master
- PVRTC Decoder --> https://www.javatips.net/api/NearInfinity-master/src/org/infinity/resource/graphics/PvrDecoder.java
- Password-protected ZIP files --> https://github.com/srikanth-lingala/zip4j
  - feed in a list of known passwords (from an external file), and ask the user to choose which game it is
- Further Viewer Plugins for Unity3D Formats
- Use FFMPEG for more file types - eg for EA-ADPCM Audio in the SCHl archives
- See about embedding FFPLAY in a Viewer window for displaying videos and/or audio?
  - Use JNA as per this link ... http://www.rgagnon.com/javadetails/java-detect-if-a-program-is-running-using-jna.html
  - Maybe use JNA as per http://stackoverflow.com/questions/12148505/embed-c-opengl-window-in-java-window
  - Use this link to get the HWND of a Canvas (JPanel) ... https://stackoverflow.com/questions/4809713/embed-hwnd-window-handle-in-a-jpanel
- Before running the Scanner, maybe try all the Viewer plugins to see if they can read the file?
  - Maybe by creating a New archive, adding the file to it, and then running through the Viewer plugins to find a match
- New scanners for common formats used in newer Engines (eg Unity3D formats) and common XBox/Playstation/DirectX formats (eg XWMA audio)
  - Also maybe find a better way of listing this stuff on the website - a separate table???
- Add padding support to the ImplicitFileReplacing classes (for all XBox games mostly)
- Integrate with ZModeller or Noesis or something, for generating 3D models

// GAME THINGS...
- Examine the Medal Of Honor *.xxx UE3 compressed archives - format of the compression
- Some Iron Defense TEX images don't show properly
- Unreal Engine...
  - Plugins for more Unreal Engine 4 games
  - Test more Unreal Engine 4 games (images) in the ViewerPlugin
  - Plugins for more Unreal Engine 3 games
  - Test more Unreal Engine 3 games (images) in the ViewerPlugin
  - Test/Fix plugins for Unreal Engine 2 games
  - Write a plugin for viewing Unreal Engine 2 Texture2D Images (or whatever the equivalent type is)
  - Add write support for Unreal Engine archives (using PluginGroup_U.writeIndex() method)
  - U_Texture_Generic
    - need to check from a proper Unreal archive, in case we have an exporter that extracts the Palette file along with the Texture file for generating Previews and Thumbnails
    - Also needs to write out the Palette file to match!
  - TEST older Unreal engine archives, including downloading demos of older Unreal games to test/correct
- Other GameByro Engine games
  - See list at the top of https://github.com/niftools/nifxml/blob/develop/nif.xml

// OTHER THINGS...
- Play a sound (such as a beep) when a task has completed.
  - Useful so that we know a task has completed, even if the Popup has been dismissed.
- Add Syntax Checker for QuickBMS (instead of just showing a "can't check this" error message) and color syntax highlighting for QuickBMS
- Websites to update
  - Wikipedia
    - https://en.wikipedia.org/wiki/Comparison_of_file_archivers
  - http://www.nationalarchives.gov.uk/PRONOM/Default.aspx
  - Game Modding Communities
    - Moddb
    - NexusMods
- Write HTML help pages for Tutorials
  - Also add the tutorial pages to the website
  - Add Facebook/Twitter links to the GameExtractor webpage
  - Topics...
    - how to preview images on disk by adding them to a blank archive
    - how to export images to PNG for editing, then mass export them back to a real game format for saving in an archive
- Other file signatures from https://www.filesignatures.net/index.php?page=search&search=EFBBBF&mode=SIG
- www.fileformat.info for file format spec documents

// WSProgram THINGS...
- Make the WSStatusBar a normal panel, so it can contain sub-fields and groups (like the statusbar at the bottom of Textpad)
- Add AWTEvent.HIERARCHY_EVENT_MASK to all WSComponents that should do hovering!
- PreviewPanels should use WSComponents in them (so as to pass events back up the tree)
- Arrow on the FileListTableHeader that shows the column that is sorted, and in which order (asc/des)
- Center the WSFontChooser popup when the button to show it is clicked in the Options
- Work out why WSComboBox requires addItemListener() before it will allow list selections
  (and work out a way to remove the listener so it is handled normally)
  - Same for WSList
  - Same for WSCheckBox (and maybe WSRadioButton?)

// BUGS
- When you change the PluginListType, it should rebuild the pluginlists (do a hard reload?)
  - Maybe each setting can set whether to do a hard reload or not?
- Changing fonts doesn't reload the interface properly - might need to add repaint-like methods to all components?
- Because of the way that Resources[] are stored in tasks (like Task_RenameFiles) to allow for undo, if you undo
  multiple renames then redo multiple renames, the 2-n rename tasks add extra name fragments. Not sure if we can do
  anything to fix this, without finding another way to store the Resource[] changes applied by Tasks rather than
  Resource.clone() and Resource.copyFrom() as we currently use. Doesn't affect single undo/redo tasks, just multiples.
- Need a better way of doing ColorConverter.changeColorCount() when going from 80000 colors to 256, for example.
+- The Prince of Persia: Warrior Within *.bf archive shows the files as being 4 bytes decompressed (but they
   do extract properly, so it is only a display bug?)
+- Check Abomination: The Nemisis Project *.clt (why is numFiles -1? - need to fix in write() when know why!!!)

// ARCHIVE PLUGINS
- ISO CD Image (and other CD images)
- Use Wikipedia to find more generic archive types!
- ARC
- RAR
- ACE
- BZip
- BZip2

// VIEWER PLUGINS
- http://jcodec.org/ (Audio and Video Formats)
- Bink Video Player
- More image viewers from the Image Specs website (textbook)
- ACM audio (GAP)
- APC audio (GAP)
- ASF audio (GAP)
- AUD audio (GAP)
- BLBSFX audio (GAP)
- CLUSFX audio (GAP)
- CMP audio (GAP)
- EACS audio (GAP)
- FST audio (GAP)
- ISS audio (GAP)
- JDLZ audio (Need For Speed Underground)
- MGI audio (GAP)
- MVE audio (GAP)
- Rad video
- RAW audio (GAP)
- SOL audio (GAP)
- VOC audio (GAP)
- WADSFX audio (GAP)
- WMA audio
- WMV video
- XA audio (GAP)
- Microsoft Windows Media Image Format (Picture?)
- ** Photoshop / Irfanview / Gimp images **

// SCANNER PLUGINS
- WMV/WMA
- TGA image
- PS2 formats
- More image scanners from the Image Specs website (textbook)
- MOV/QT Quicktime Video
- MP3 Audio
- MPEG Video
- SCT Scitec Images
- WMF/EMF Windows/Enhanced MetaFile Images
- ** GAP formats **
- ** Photoshop / Irfanview / Gimp images **

// OTHER PLUGINS
- Implement JDLZ Audio Decompression! (NFSU AST_ASHI)

// OTHER
- Improvements to the ProgramUpdater
  - Betting-looking interface (images, etc.)
  - NativeJ EXE files for running the ProgramUpdater?

\*********************************************************************************************/
import org.watto.ChangeMonitor;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.TypecastSingletonManager;
import org.watto.WSProgram;
import org.watto.component.ComponentRepository;
import org.watto.component.DirectoryListPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.component.SidePanel_DirectoryList;
import org.watto.component.WSButton;
import org.watto.component.WSComponent;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSMenuItem;
import org.watto.component.WSPanel;
import org.watto.component.WSPanelPlugin;
import org.watto.component.WSPlugin;
import org.watto.component.WSPluginManager;
import org.watto.component.WSRecentFileMenu;
import org.watto.component.WSRecentFileMenuItem;
import org.watto.component.WSSidePanelHolder;
import org.watto.component.WSSplitPane;
import org.watto.component.WSStatusBar;
import org.watto.component.WSTaskMenuItem;
import org.watto.datatype.Archive;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.WSResizableInterface;
import org.watto.event.WSWindowFocusableInterface;
import org.watto.event.listener.WSWindowFocusableListener;
import org.watto.ge.helper.CommandLineProcessor;
import org.watto.ge.helper.FileTypeDetector;
import org.watto.ge.helper.FullVersionVerifier;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.script.ScriptManager;
import org.watto.io.converter.StringConverter;
import org.watto.plaf.LookAndFeelManager;
import org.watto.task.Task;
import org.watto.task.TaskProgressManager;
import org.watto.task.Task_NewArchive;
import org.watto.task.Task_Popup_PromptToSaveBeforeClose;
import org.watto.task.Task_ReadArchive;
import org.watto.xml.XMLNode;

/**
 **********************************************************************************************
 * The Game Extractor program. This class contains the main interface, loads major components
 * such as the <code>PluginManager</code>s, and handles toolbar/menubar events.
 **********************************************************************************************
 **/

public class GameExtractor extends WSProgram implements WSClickableInterface,
    WSResizableInterface,
    WSWindowFocusableInterface,
    WSKeyableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
   * A singleton holder for the GameExtractor program, so other classes can directly access the
   * same instance
   **/
  static GameExtractor instance = new GameExtractor();

  /**
   **********************************************************************************************
   * Deletes all the temporary files from the <i>directory</i>.
   * @param directory the directory that contains the temporary files.
   **********************************************************************************************
   **/
  public static void deleteTempFiles(File directory) {
    try {

      if (!directory.exists()) {
        return;
      }

      File[] tempFiles = directory.listFiles();

      if (tempFiles == null) {
        return;
      }

      for (int i = 0; i < tempFiles.length; i++) {
        if (tempFiles[i].isDirectory()) {
          deleteTempFiles(tempFiles[i]);
        }
        tempFiles[i].delete();
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   * Returns the singleton instance of the program. This allows other classes to all address the
   * same instance of the interface, rather than separate instances.
   * @return the singleton <i>instance</i> of GameExtractor
   **********************************************************************************************
   **/
  public static GameExtractor getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static boolean isFullVersion() {
    return false;
  }

  /**
   **********************************************************************************************
   * The main method that starts the program.
   * @param args the arguments passed in from the commandline.
   **********************************************************************************************
   **/
  public static void main(String[] args) {
    boolean commandLineOnly = false;

    // TESTING ONLY
    //args = new String[] { "-help" };
    //args = new String[] { "-list", "-input", "C:\\_WATTOz\\+Backups\\_GE_Testing\\+Backups.zip", "-output", "C:\\_WATTOz\\+Backups\\_GE_Testing\\+Backups.zip.list.csv", "-format", "CSV", "-fields", "All" };
    //args = new String[] { "-extract", "-input", "C:\\_WATTOz\\+Backups\\_GE_Testing\\+Backups.zip", "-output", "C:\\_WATTOz\\+Backups\\_GE_Testing\\extractedFiles" };
    //args = new String[] { "-extract", "-input", "C:\\_WATTOz\\+Backups\\_GE_Testing\\+Backups.zip", "-output", "C:\\_WATTOz\\+Backups\\_GE_Testing\\extractedFiles", "-convert", "PNG" };

    if (args.length > 0) {
      if (args[0].charAt(0) == '-') {
        // Check if it's one of the valid options for Command-Line-Only
        for (int i = 0; i < args.length; i++) {
          String arg = args[i];
          if (arg.equalsIgnoreCase("-help") || arg.equalsIgnoreCase("-list") || arg.equalsIgnoreCase("-extract")) {
            // found one of the valid operations, so we must be running as command-line-only
            commandLineOnly = true;
            break;
          }
        }
      }
    }

    // load the whole program (except for diplaying the GUI)
    GameExtractor ge = GameExtractor.getInstance();

    // The program is now fully loaded (from the line above), so we can choose GUI or Command Line
    if (commandLineOnly) {
      // Command Line
      new CommandLineProcessor().processCommandLine(args);
      System.exit(0); // stop here
    }
    else {
      // Show the GUI
      ge.showInterface();
    }

    ge.showInterface();

    ge.makeNewArchive();
    ge.openSidePanelOnStartup();

    // THIS IS TO FORCE THE THUMBNAIL PANEL TO LOAD AT STARTUP. IT THEN FLICKS OVER TO THE LAST-CHOSEN PANEL.
    // THIS IS SO THAT THE THUMBNAIL PANEL (and all subcomponents) GET VALIDATED, SO WHEN YOU HAVE AN ARCHIVE
    // OPEN AND CHANGE TO THUMBNAIL VIEW, IT SHOWS THE PROGRESS BAR!
    String selectedFileList = Settings.get("FileListView");
    //ge.setFileListPanel("Thumbnails");
    ge.setFileListPanel(selectedFileList);

    if (args.length > 0) {
      File fileToOpen = new File(args[0]);
      if (fileToOpen.exists()) {
        ((SidePanel_DirectoryList) ComponentRepository.get("SidePanel_DirectoryList")).readArchive(fileToOpen);
        //sidePanelHolder.reloadPanel();
      }
    }
  }

  WSSidePanelHolder sidePanelHolder;

  WSFileListPanelHolder fileListPanelHolder;

  /**
   * Listens for the user typing "WATTO", and triggers something special when they do type it
   **/
  String keywordWatto = "";

  /**
   **********************************************************************************************
   * Not to be used - use "GameExtractor.getInstance()" instead of "new GameExtractor()"
   **********************************************************************************************
   **/
  @SuppressWarnings("static-access")
  public GameExtractor() {
    // DONT PUT THIS LINE HERE, CAUSE IT IS DONE AUTOMATICALLY BY super()
    // EVEN THOUGH super() ISNT CALLED, IT IS RUN BECAUSE THIS CONSTRUCTOR EXTENDS WSProgram
    // AND THUS MUST RUN super() BEFORE THIS CLASS CAN BE BUILT.
    // HAVING THIS LINE CAUSES THE PROCESSES TO BE RUN TWICE, ENDING UP WITH 2 OF
    // EACH PLUGIN, AND STUPID THINGS LIKE THAT.
    //buildProgram(this);

    // add the window focus listener, so it wil reload the dirpanel when focus has regained
    addWindowFocusListener(new WSWindowFocusableListener(this));

    //setIconImage(new ImageIcon(getClass().getResource("images/WSFrame/icon.png")).getImage());
    Image[] icons = new Image[5];
    icons[0] = LookAndFeelManager.getImageIcon("images/WSFrame/icon256.png").getImage();
    icons[1] = LookAndFeelManager.getImageIcon("images/WSFrame/icon128.png").getImage();
    icons[2] = LookAndFeelManager.getImageIcon("images/WSFrame/icon64.png").getImage();
    icons[3] = LookAndFeelManager.getImageIcon("images/WSFrame/icon32.png").getImage();
    icons[4] = LookAndFeelManager.getImageIcon("images/WSFrame/icon16.png").getImage();
    setIconImages(Arrays.asList(icons));

    // Construct the default archive
    splash.setMessage("FileTypes");

    new Archive(); // required, to set up the icons, etc.

    FileTypeDetector.loadGenericDescriptions();

    ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(Language.get("Welcome"));

    // Register the WSRecentFileMenu with the RecentFilesManager to capture the events
    WSRecentFileMenu recentFileMenu = ((WSRecentFileMenu) ComponentRepository.get("RecentFileMenu"));
    TypecastSingletonManager.getRecentFilesManager("RecentFilesManager").addMonitor(recentFileMenu);

    /*// THIS IS NOW DONE IN THE COMPONENTS THEMSELVES --> setTaskManager() called from toComponent()
    // Register the Undo/Redo Menus/Buttons with the TaskManager to capture the events
    TaskManager taskManager = TypecastSingletonManager.getTaskManager("TaskManager");
    if (taskManager != null) {
      WSUndoTaskMenu undoMenu = ((WSUndoTaskMenu) ComponentRepository.get("UndoMenu"));
      if (undoMenu != null) {
        taskManager.addMonitor(undoMenu);
      }
      WSRedoTaskMenu redoMenu = ((WSRedoTaskMenu) ComponentRepository.get("RedoMenu"));
      if (redoMenu != null) {
        taskManager.addMonitor(redoMenu);
      }

      WSUndoTaskComboButton undoButton = ((WSUndoTaskComboButton) ComponentRepository.get("UndoTaskComboButton"));
      if (undoButton != null) {
        taskManager.addMonitor(undoButton);
      }
      WSRedoTaskComboButton redoButton = ((WSRedoTaskComboButton) ComponentRepository.get("RedoTaskComboButton"));
      if (redoButton != null) {
        taskManager.addMonitor(redoButton);
      }
    }
    */

    // close the splash screen

    splash.dispose();

    /*
    if (commandLineOnly) {
      return; // stop here - don't want to display the interface
    }

    pack();
    setExtendedState(JFrame.MAXIMIZED_BOTH);

    fileListPanelHolder.setMinimumSize(new Dimension(0, 0));
    sidePanelHolder.setMinimumSize(new Dimension(0, 0));

    WSSplitPane mainSplit = (WSSplitPane) ComponentRepository.get("MainSplit");
    mainSplit.setDividerSize(5);

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    //double splitPos = Settings.getDouble("DividerLocation");
    //if (splitPos < 0 || splitPos > 1){
    //  splitPos = 0.7;
    //  }

    //mainSplit.setDividerLocation(splitPos);
    //mainSplit.setResizeWeight(1);
    setVisible(true);
    //pack();

    //int location = (int)(mainSplit.getWidth() * splitPos);
    //System.out.println(location);
    //mainSplit.resetToPreferredSizes();
    //mainSplit.setDividerLocation(splitPos);

     */

    // writes out the list of ArchivePlugins and ViewerPlugins, for the excel spreadsheet
    //outputPluginExcelList();

  }

  /**
   **********************************************************************************************
   * Builds the interface of the program. Can be overwritten if you want to do additional things
   * when the interface is being constructed, or if you dont want to load the interface from an
   * XML file.
   **********************************************************************************************
   **/
  @Override
  @SuppressWarnings("static-access")
  public void constructInterface() {
    // scripts are now loaded by SidePanel_DirectoryList and SidePanel_ScriptBuilder when
    // they are needed. (optional)
    if (Settings.getBoolean("LoadScriptsAtStartup")) {
      splash.setMessage("Scripts");
      ScriptManager.loadScripts();
    }

    super.constructInterface();
    sidePanelHolder = (WSSidePanelHolder) ComponentRepository.get("SidePanelHolder");
    sidePanelHolder.loadPanel(Settings.get("CurrentSidePanel"));

    fileListPanelHolder = (WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder");
    //fileListPanelHolder.loadPanel(Settings.get("FileListView"));

    String selectedPanel = Settings.get("FileListView");
    fileListPanelHolder.loadPanel("Thumbnails");
    Settings.set("FileListView", selectedPanel);

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void makeNewArchive() {
    makeNewArchive(false);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void makeNewArchive(boolean runInNewThread) {
    Task_NewArchive task = new Task_NewArchive();
    task.setDirection(Task.DIRECTION_REDO);
    if (runInNewThread) {
      new Thread(task).start();
    }
    else {
      task.redo();
    }
    //new Thread(task).start();
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSButtonableListener when a button click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onClick(JComponent c, java.awt.event.MouseEvent e) {
    if (!(c instanceof WSComponent)) {
      return false;
    }

    String code = ((WSComponent) c).getCode();

    if (c instanceof WSRecentFileMenuItem) {
      // opening a recent file - the 'code' is the filename to open
      File recentFile = new File(code);
      if (recentFile.exists()) {
        Task_ReadArchive task = new Task_ReadArchive(recentFile);
        task.setDirection(Task.DIRECTION_REDO);
        new Thread(task).start();
      }
    }
    else if (c instanceof WSTaskMenuItem) {
      // an undo or redo task
      Task task = ((WSTaskMenuItem) c).getTask();
      int taskParentType = ((WSTaskMenuItem) c).getParentType();

      if (taskParentType == WSTaskMenuItem.PARENT_REDO) {
        // redo a task
        TypecastSingletonManager.getTaskManager("TaskManager").redo(task);
      }
      else if (taskParentType == WSTaskMenuItem.PARENT_UNDO) {
        // undo a task
        TypecastSingletonManager.getTaskManager("TaskManager").undo(task);
      }
    }
    else if (c instanceof WSMenuItem || c instanceof WSButton) {
      if (code.equals("NewArchive")) {
        makeNewArchive(true); // run in a new thread so that the "do you want to save" popup will appear
      }
      else if (code.equals("ReadArchive_Normal")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ReadPanel", false);
      }
      else if (code.equals("ReadArchive_OpenWith")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ReadPanel", false);
      }
      else if (code.equals("ReadArchive_Script")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ScriptPanel", false);
      }
      else if (code.equals("ReadArchive_Scanner")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ReadPanel", false);
      }
      else if (code.equals("WriteArchive")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("WritePanel", true);
      }
      else if (code.equals("ConvertArchive")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("WritePanel", true);
      }
      else if (code.equals("CutArchive")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("CutPanel", false);
      }
      else if (code.equals("CloseProgram")) {
        onClose();
      }
      else if (code.equals("AddResources")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ModifyPanel", true);
      }
      else if (code.equals("RemoveResources")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ModifyPanel", true);
      }
      else if (code.equals("RenameResources")) {
        setSidePanel("RenameFile");
      }
      else if (code.equals("ReplaceResources")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ModifyPanel", true);
      }
      else if (code.equals("Search")) {
        setSidePanel("Search");
      }
      else if (code.equals("SelectResources_All")) {
        fileListPanelHolder.selectAll();
      }
      else if (code.equals("SelectResources_None")) {
        fileListPanelHolder.selectNone();
      }
      else if (code.equals("SelectResources_Inverse")) {
        fileListPanelHolder.selectInverse();
      }
      else if (code.equals("FileListView_Table")) {
        setFileListPanel("Table");
      }
      else if (code.equals("FileListView_Tree")) {
        setFileListPanel("Tree");
      }
      else if (code.equals("FileListView_Thumbnails")) {
        setFileListPanel("Thumbnails");
      }
      //else if (code.equals("FileListView_FolderTable")){
      //  setFileListPanel("FolderTable");
      //  }
      else if (code.equals("FileListView_TreeTable")) {
        setFileListPanel("TreeTable");
      }
      else if (code.equals("ExtractSelectedResources")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ExportPanel", false);
      }
      else if (code.equals("ExtractAllResources")) {
        setSidePanel("DirectoryList");
        ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("ExportPanel", false);
      }
      else if (code.equals("PreviewResource")) {
        setSidePanel("Preview");
      }
      else if (code.equals("ImageInvestigator")) {
        setSidePanel("ImageInvestigator");
      }
      else if (code.equals("HexEditor")) {
        Settings.set("AutoChangedToHexPreview", "false");
        setSidePanel("HexEditor");
      }
      else if (code.equals("Options")) {
        setSidePanel("Options");
      }
      else if (code.equals("ScriptBuilder")) {
        setSidePanel("ScriptBuilder");
      }
      else if (code.equals("PluginList")) {
        setSidePanel("PluginList");
      }
      else if (code.equals("Information")) {
        setSidePanel("Information");
      }
      else if (code.equals("FileListExporter")) {
        setSidePanel("FileListExporter");
      }
      else if (code.equals("Help")) {
        setSidePanel("Help");
      }
      else if (code.equals("About")) {
        setSidePanel("About");
      }
      else {
        return false;
      }
      return true;
    }

    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSClosableListener when a component is closed
   **********************************************************************************************
   **/
  @Override
  public boolean onClose() {

    return onClose(true);

  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSClosableListener when a component is closed
   **********************************************************************************************
   **/
  public boolean onClose(boolean promptToSave) {

    // ask to save the modified archive
    if (promptToSave) {
      // Run as a task, so the overlay popup is displayed
      Task_Popup_PromptToSaveBeforeClose task = new Task_Popup_PromptToSaveBeforeClose();
      task.setDirection(Task.DIRECTION_REDO);
      new Thread(task).start();

      // stop the remaining processing - don't want to actually exit.
      return false;
    }

    ChangeMonitor.reset();

    // so that the PreviewPanel loads on next startup
    Settings.set("AutoChangedToHexPreview", "false");

    deleteTempFiles(new File("temp"));

    // do onClose() on FileListPanel and SidePanel
    sidePanelHolder.onCloseRequest();
    fileListPanelHolder.onCloseRequest();

    // Remember the location of the main split divider
    WSSplitPane mainSplit = (WSSplitPane) ComponentRepository.get("MainSplit");
    double splitLocationOld = Settings.getDouble("DividerLocation");
    double splitLocationNew = (double) (mainSplit.getDividerLocation()) / (double) (mainSplit.getWidth());
    double diff = splitLocationOld - splitLocationNew;
    if (diff > 0.01 || diff < -0.01) {
      // only set if the change is large.
      // this gets around the problem with the split slowly moving left over each load
      Settings.set("DividerLocation", splitLocationNew);
    }

    // Save settings files
    //XMLWriter.write(new File(Settings.getString("ToolbarFile")),toolbar.constructXMLNode());
    //XMLWriter.write(new File(Settings.getString("MenuBarFile")),menubar.constructXMLNode());
    Settings.saveSettings();

    // Saves the interface to XML, in case there were changes made by the program, such as
    // the adding/removal of buttons, or repositioning of elements
    saveInterface();

    ErrorLogger.closeLog();

    System.exit(0);

    return true;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSHoverableListener when a component is hovered over
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHover(JComponent c, java.awt.event.MouseEvent e) {
    //statusbar.setText(((JComponent)c).getToolTipText());
    ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(c.getToolTipText());
    return true;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSHoverableListener when a component is no longer hovered
   * over (ie loses its hover)
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHoverOut(JComponent c, java.awt.event.MouseEvent e) {
    //statusbar.revertText();
    ((WSStatusBar) ComponentRepository.get("StatusBar")).revertText();
    return true;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSKeyableListener when a key is pressed
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onKeyPress(JComponent c, java.awt.event.KeyEvent e) {
    char keyCode = e.getKeyChar();
    if (keyCode == 'w') {
      keywordWatto = "W";
    }
    else if (keyCode == 'a' && keywordWatto.equals("W")) {
      keywordWatto = "WA";
    }
    else if (keyCode == 't' && keywordWatto.equals("WA")) {
      keywordWatto = "WAT";
    }
    else if (keyCode == 't' && keywordWatto.equals("WAT")) {
      keywordWatto = "WATT";
    }
    else if (keyCode == 'o' && keywordWatto.equals("WATT")) {
      keywordWatto = "";
      triggerKeywordWatto();
    }
    else {
      keywordWatto = "";
    }

    return true;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSResizableListener when a component is resized
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onResize(JComponent c, java.awt.event.ComponentEvent e) {
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();
      if (code == null) {
        return false;
      }
      if (code.equals("MainSplit")) {
        // reposition the splitpane divider when the splitpane changes sizes
        double splitPos = Settings.getDouble("DividerLocation");
        if (splitPos < 0 || splitPos > 1) {
          splitPos = 0.7;
        }

        //System.out.println("Before: " + splitPos);
        ((WSSplitPane) c).setDividerLocation(splitPos);
        //System.out.println("After: " + ((double)((WSSplitPane)c).getDividerLocation() / ((WSSplitPane)c).getWidth()));
      }
    }
    return true;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSWindowFocusableListener when a component gains focus
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onWindowFocus(java.awt.event.WindowEvent e) {
    if (sidePanelHolder.getCurrentPanelCode().equals("SidePanel_DirectoryList")) {
      // reload the directory list
      ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).reloadDirectoryList();
    }

    return true;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onWindowFocusOut(WindowEvent event) {
    return false;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void openSidePanelOnStartup() {
    WSPanel panel = sidePanelHolder.getCurrentPanel();
    if (panel instanceof WSPanelPlugin) {
      ((WSPanelPlugin) panel).onOpenRequest();
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void outputPluginExcelList() {
    // Archive Plugins
    WSPlugin[] plugins = WSPluginManager.getGroup("Archive").getPlugins();
    for (int i = 0; i < plugins.length; i++) {
      ArchivePlugin plugin = (ArchivePlugin) plugins[i];

      if (!plugin.isEnabled()) {
        continue; // only want working plugins
      }

      String[] games = plugin.getGames();
      String[] platforms = plugin.getPlatforms();
      String[] extensions = plugin.getExtensions();

      for (int j = 0; j < games.length; j++) {
        String text = plugin.getClass() + "\t" + plugin.getCode() + "\t" + games[j] + "\t";

        for (int p = 0; p < extensions.length; p++) {
          if (p > 0) {
            text += " ";
          }
          text += extensions[p];
        }
        text += "\t";

        for (int p = 0; p < platforms.length; p++) {
          if (p > 0) {
            text += " ";
          }
          text += platforms[p];
        }
        text += "\t";

        if (plugin.canWrite() || plugin.canImplicitReplace() || plugin.canReplace()) {
          text += "Yes\t";
        }
        else {
          text += "No\t";
        }

        System.out.println(text);
      }
    }

    // Viewer Plugins
    plugins = WSPluginManager.getGroup("Viewer").getPlugins();
    for (int i = 0; i < plugins.length; i++) {
      ViewerPlugin plugin = (ViewerPlugin) plugins[i];

      if (!plugin.isEnabled()) {
        continue; // only want working plugins
      }

      String[] games = plugin.getGames();
      String[] platforms = plugin.getPlatforms();
      String[] extensions = plugin.getExtensions();

      for (int j = 0; j < games.length; j++) {
        String text = plugin.getClass() + "\t" + plugin.getCode() + "\t" + games[j] + "\t";

        for (int p = 0; p < extensions.length; p++) {
          if (p > 0) {
            text += " ";
          }
          text += extensions[p];
        }
        text += "\t";

        for (int p = 0; p < platforms.length; p++) {
          if (p > 0) {
            text += " ";
          }
          text += platforms[p];
        }
        text += "\t";

        if (plugin.canWrite(new PreviewPanel_Image())) {
          text += "Yes\t";
        }
        else {
          text += "No\t";
        }

        System.out.println(text);
      }
    }

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public boolean promptToSave() {
    if (ChangeMonitor.check()) {
      if (isFullVersion()) {
        if (ChangeMonitor.popup()) {
          // save changes
          setSidePanel("DirectoryList");
          ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).changeControls("WritePanel", true);
          return true;
        }
      }
    }
    return false;
  }

  /**
   **********************************************************************************************
   * Does a hard reload (rebuilds the entire interface after language/font/interface change)
   **********************************************************************************************
   **/
  public void rebuild() {

    WSPlugin[] plugins = WSPluginManager.getGroup("DirectoryList").getPlugins();
    for (int i = 0; i < plugins.length; i++) {
      ((DirectoryListPanel) plugins[i]).constructInterface(new File(Settings.get("CurrentDirectory")));
    }

    plugins = WSPluginManager.getGroup("Options").getPlugins();
    for (int i = 0; i < plugins.length; i++) {
      ((WSPanelPlugin) plugins[i]).toComponent(new XMLNode());
    }

    constructInterface();
    sidePanelHolder.rebuild();
    fileListPanelHolder.rebuild();
  }

  /**
   **********************************************************************************************
   * Does a soft reload, after options changes
   **********************************************************************************************
   **/
  public void reload() {
    //menubar.reload();
    //toolbar.reload();

    fileListPanelHolder.reload();

    //FileListPanelManager.reloadCurrentPanel();

    //SidePanelManager.reloadPanels();
    sidePanelHolder.reload();

    //repositionToolbar();
    //repositionSidePanel();

    ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(Language.get("Welcome"));

    //SwingUtilities.updateComponentTreeUI(this);
    validate();
    repaint();

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setFileListPanel(String name) {
    Settings.set("FileListView", name);
    fileListPanelHolder.loadPanel(name);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setSidePanel(String name) {
    Settings.set("AutoChangedToHexPreview", "false");
    ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).loadPanel("SidePanel_" + name);
  }

  /**
   **********************************************************************************************
   Displays the interface after everything else is built
   **********************************************************************************************
   **/
  public void showInterface() {
    new TaskProgressManager(); // Set the ProgressBar to use WSOverlayProgressDialog, not WSProgressDialog
    TaskProgressManager.stopTask(); // makes sure the OverlayPanel isn't showing (in case GE writes out interface.xml with setVisible=true)

    pack();
    setExtendedState(JFrame.MAXIMIZED_BOTH);

    fileListPanelHolder.setMinimumSize(new Dimension(0, 0));
    sidePanelHolder.setMinimumSize(new Dimension(0, 0));

    WSSplitPane mainSplit = (WSSplitPane) ComponentRepository.get("MainSplit");
    mainSplit.setDividerSize(5);

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    //double splitPos = Settings.getDouble("DividerLocation");
    //if (splitPos < 0 || splitPos > 1){
    //  splitPos = 0.7;
    //  }

    //mainSplit.setDividerLocation(splitPos);
    //mainSplit.setResizeWeight(1);
    setVisible(true);
    //pack();

    //int location = (int)(mainSplit.getWidth() * splitPos);
    //System.out.println(location);
    //mainSplit.resetToPreferredSizes();
    //mainSplit.setDividerLocation(splitPos);
  }

  /**
   **********************************************************************************************
   * Does something special when the user enters the word "watto"
   **********************************************************************************************
   **/
  public void triggerKeywordWatto() {

    /*
     * // Cycle through all the colors of the rainbow, then back to the original interface color
     * WSSidePanelHolder holder =
     * ((WSSidePanelHolder)ComponentRepository.get("SidePanelHolder")); WSPanel panel =
     * holder.getCurrentPanel(); if (panel == null ||
     * panel.getCode().equals("SidePanel_FileListExporter")){ Task_KeywordWatto task = new
     * Task_KeywordWatto(this); task.setDirection(UndoTask.DIRECTION_REDO); new
     * Thread(task).start(); }
     */

    // reverse the order of the program title
    setTitle(StringConverter.reverse(getTitle()));

  }

}
