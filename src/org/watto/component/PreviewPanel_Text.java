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
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.WSSelectableInterface;
import org.watto.event.listener.WSKeyableListener;
import org.watto.event.listener.WSSelectableListener;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.task.Task;
import org.watto.task.Task_WriteEditedTextFile;
import org.watto.xml.XMLReader;

public class PreviewPanel_Text extends PreviewPanel implements WSSelectableInterface, WSKeyableInterface, WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  JTextArea preview = null;

  static Font defaultFont = null;

  static Font monoFont = null;

  boolean textChanged = false;

  WSButton saveButton = null;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public PreviewPanel_Text(String text) {
    super();

    // 3.16 Added "codes" to every XML-built object, so that they're cleaned up when the object is destroyed (otherwise it was being retained in the ComponentRepository)

    textChanged = false; // not edited

    WSOptionCheckBox wordwrapCheckbox = new WSOptionCheckBox(XMLReader.read("<WSOptionCheckBox opaque=\"false\" code=\"PreviewPanel_Text_WordWrap\" setting=\"PreviewPanel_Text_WordWrap\" />"));
    WSOptionCheckBox monospacedFontCheckbox = new WSOptionCheckBox(XMLReader.read("<WSOptionCheckBox opaque=\"false\" code=\"PreviewPanel_Text_MonospacedFont\" setting=\"PreviewPanel_Text_MonospacedFont\" />"));

    //add a listener to the checkbox, so we can capture and process select/deselect
    WSSelectableListener selectableListener = new WSSelectableListener(this);
    wordwrapCheckbox.addItemListener(selectableListener);
    monospacedFontCheckbox.addItemListener(selectableListener);

    WSPanel topPanel = new WSPanel(XMLReader.read("<WSPanel code=\"PreviewPanel_Text_TopPanelWrapper\" showBorder=\"true\" layout=\"GridLayout\" rows=\"1\" columns=\"2\" />"));
    topPanel.add(wordwrapCheckbox);
    topPanel.add(monospacedFontCheckbox);

    add(topPanel, BorderLayout.NORTH);

    preview = new JTextArea(text);
    preview.addKeyListener(new WSKeyableListener(this));

    defaultFont = preview.getFont();
    monoFont = new Font("monospaced", Font.PLAIN, 12);

    if (Settings.getBoolean("PreviewPanel_Text_MonospacedFont")) {
      preview.setFont(monoFont);
    }
    else {
      preview.setFont(defaultFont);
    }

    try {
      ArchivePlugin archivePlugin = Archive.getReadPlugin();
      if (archivePlugin != null) {
        if (archivePlugin.canWrite() || archivePlugin.canReplace() || archivePlugin.canImplicitReplace()) {


        }
        else {
          preview.setEditable(false);
        }
      }
    }
    catch (Throwable t) {
      preview.setEditable(false);
    }
    preview.setLineWrap(Settings.getBoolean("PreviewPanel_Text_WordWrap"));
    preview.setWrapStyleWord(true);

    add(new JScrollPane(preview), BorderLayout.CENTER);

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public String getText() {
    return preview.getText();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
    // Flush the variables clear for garbage collection

    if (textChanged) {
      saveChanges();
    }

    preview = null;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onDeselect(JComponent c, Object e) {
    if (c instanceof WSCheckBox) { // WSCheckBox, not WSOptionCheckBox, because we've registered the listener on the checkbox
      WSCheckBox checkbox = (WSCheckBox) c;
      String code = checkbox.getCode();
      if (code.equals("PreviewPanel_Text_WordWrap")) {
        // disable wordwrap
        preview.setLineWrap(false);
      }
      else if (code.equals("PreviewPanel_Text_MonospacedFont")) {
        // Set to a normal font
        preview.setFont(defaultFont);
      }

      return true; // changing the Setting is handled by a separate listener on the WSObjectCheckbox class, so we can return true here OK
    }
    return false;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onSelect(JComponent c, Object e) {
    if (c instanceof WSCheckBox) { // WSCheckBox, not WSOptionCheckBox, because we've registered the listener on the checkbox
      WSCheckBox checkbox = (WSCheckBox) c;
      String code = checkbox.getCode();
      if (code.equals("PreviewPanel_Text_WordWrap")) {
        // disable wordwrap
        preview.setLineWrap(true);
      }
      else if (code.equals("PreviewPanel_Text_MonospacedFont")) {
        // Set to a normal font
        preview.setFont(monoFont);
      }

      return true; // changing the Setting is handled by a separate listener on the WSObjectCheckbox class, so we can return true here OK
    }
    return false;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onKeyPress(JComponent source, KeyEvent event) {
    if (source == preview) {
      if (!textChanged) {
        if (saveButton != null) {
          saveButton.setEnabled(true);
        }
      }
      textChanged = true;
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent source, MouseEvent event) {
    if (source instanceof WSComponent) {
      WSComponent c = (WSComponent) source;
      String code = c.getCode();
      if (code.equals("PreviewPanel_Text_SaveChanges")) {
        if (textChanged) {
          saveChanges();
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
  public boolean isTextChanged() {
    return textChanged;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setTextChanged(boolean textChanged) {
    this.textChanged = textChanged;
    saveButton.setEnabled(textChanged);
  }

  /**
  **********************************************************************************************
  Overwriting to support standard method
  **********************************************************************************************
  **/
  public void setObjectChanged(boolean changed) {
    setTextChanged(changed);
  }

  /**
  **********************************************************************************************
  1. Export the file, if it isn't already
  2. Save the changes to the exported file
  3. Set the Archive as being edited
  **********************************************************************************************
  **/
  public void saveChanges() {
    /*
    try {
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null) {
        return;
      }
      Resource resource = (Resource) resourceObject;

      File exportedPath = resource.getExportedPath();
      if (exportedPath == null || !exportedPath.exists()) {
        // Export it
        File directory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());

        Task_ExportFiles task = new Task_ExportFiles(directory, resource);
        task.setShowPopups(false);
        task.setShowProgressPopups(false); // this barely appears, and slows down the preview repainting significantly, so don't worry about it.
        task.redo();
      }

      exportedPath = resource.getExportedPath();
      if (exportedPath == null || !exportedPath.exists()) {
        return; // couldn't extract the file for some reason
      }

      // Rename the original extracted file to _GE_ORIGINAL
      File originalPath = new File(exportedPath.getAbsolutePath() + "_ge_original");
      if (originalPath.exists()) {
        // ignore it
      }
      else {
        exportedPath.renameTo(originalPath);
      }

      // Now save the changes to the proper Extracted filename
      if (exportedPath.exists() && exportedPath.isFile()) {
        // try to delete it first
        exportedPath.delete();
      }
      if (exportedPath.exists() && exportedPath.isFile()) {
        return; // Failed to delete for some reason
      }

      FileManipulator fm = new FileManipulator(exportedPath, true);
      fm.writeString(getText());
      fm.close();

      if (!exportedPath.exists()) {
        return; // failed to save for some reason
      }

      boolean reloadRequired = !resource.isReplaced();

      // Set the Resource as being changed
      resource.setReplaced(true);

      // Set the Archive as being changed
      ChangeMonitor.change();

      // the changes have been saved to the temp file
      textChanged = false;
      saveButton.setEnabled(false);

      if (reloadRequired) {
        FileListPanel fileListPanel = ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel());
        if (fileListPanel instanceof FileListPanel_TreeTable) {
          // Only want to reload the table, as that's the only thing that's changed.
          // This stops the tree from being reloaded, removing the filter.
          ((FileListPanel_TreeTable) fileListPanel).reloadTable();
        }
        else {
          fileListPanel.reload();
        }
      }

      WSPopup.showMessage("PreviewPanel_Text_ChangesSaved", true); // TODO Add to Settings and Language

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    */

    Task_WriteEditedTextFile task = new Task_WriteEditedTextFile(this);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

}