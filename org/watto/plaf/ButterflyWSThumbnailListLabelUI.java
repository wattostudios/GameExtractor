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

package org.watto.plaf;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import org.watto.component.WSThumbnailListLabel;
import org.watto.datatype.ImageResource;

/**
**********************************************************************************************
  UI for JPanels
**********************************************************************************************
**/

public class ButterflyWSThumbnailListLabelUI extends BasicPanelUI {

  private final static ButterflyWSThumbnailListLabelUI buttonUI = new ButterflyWSThumbnailListLabelUI();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static ComponentUI createUI(JComponent c) {
    return buttonUI;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void installUI(JComponent c) {
    super.installUI(c);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void paint(Graphics g, JComponent c) {
    int x = 0;
    int y = 0;
    int w = c.getWidth();
    int h = c.getHeight();

    ImageResource imageResource = ((WSThumbnailListLabel) c).getImageResource();

    ButterflyPainter.paintSolidBackground((Graphics2D) g, x, y, w, h, LookAndFeelManager.getBackgroundColor());
    if (((WSThumbnailListLabel) c).isSelected()) {
      ButterflyPainter.paintCurvedGradient((Graphics2D) g, x, y, w - 1, h - 1, LookAndFeelManager.getLightColor(), LookAndFeelManager.getMidColor());
    }
    else {
      //AquanauticPainter.paintCurvedGradient((Graphics2D)g,j*buttonWidth,i*buttonWidth,buttonWidth-2,buttonWidth-2);
      ButterflyPainter.paintSquareSolid((Graphics2D) g, x, y, w - 1, h - 1, LookAndFeelManager.getBackgroundColor());
    }

    // Don't do this any more, thumbnails stay at size 100
    //imageResource.setThumbnailSize(w - 13);

    Image thumbnail = imageResource.getThumbnail();
    //int thumbnailSize = imageResource.getThumbnailSize();

    int thumbWidth = thumbnail.getWidth(null);
    int thumbHeight = thumbnail.getHeight(null);

    int topPos = (w - thumbHeight) / 2;
    int leftPos = (w - thumbWidth) / 2;

    g.drawImage(thumbnail, leftPos, topPos, null);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);
  }

}
