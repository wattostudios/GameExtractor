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
import org.watto.Language;
import org.watto.component.ComponentRepository;
import org.watto.component.SidePanel_Help;
import org.watto.component.WSPluginManager;
import org.watto.component.WSPopup;
import org.watto.component.WSSidePanelHolder;
import org.watto.ge.helper.QuickBMSHelper;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_Popup_QuickBMSDownloader extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  String messageCode = "";

  boolean messageHidable = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_Popup_QuickBMSDownloader(String messageCode) {
    this.messageCode = messageCode;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_Popup_QuickBMSDownloader(String messageCode, boolean messageHidable) {
    this.messageCode = messageCode;
    this.messageHidable = messageHidable;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    String buttonClicked = WSPopup.showConfirm(messageCode, messageHidable);

    if (buttonClicked.equals(WSPopup.BUTTON_YES)) {
      // try to download QuickBMS
      boolean downloadSuccessful = QuickBMSHelper.downloadFromWebsite();
      if (!downloadSuccessful) {
        // For any issue when trying to download the file...
        // Show an error message to the user, and open the help information to the right place, so they can download it manually
        ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).loadPanel("SidePanel_Help");
        ((SidePanel_Help) WSPluginManager.getGroup("SidePanel").getPlugin("SidePanel_Help")).loadFile(new File("help/ExternalSoftware.html"));

        Task_Popup_ShowError popupTask = new Task_Popup_ShowError("QuickBMSDownloadFailure", false);
        popupTask.setDirection(Task.DIRECTION_REDO);
        popupTask.redo();
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
