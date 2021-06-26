////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
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

package org.watto.plaf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import org.watto.component.WSGradientColorPanel;


/***********************************************************************************************
Used to paint the GUI for <code>WSGradientColorPanel</code>s
***********************************************************************************************/
public class ButterflyGradientColorPanelUI extends BasicPanelUI {

  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    WSGradientColorPanel panel = (WSGradientColorPanel)component;
    Color color = panel.getColor();
    Color selectedColor = panel.getSelectedColor();

    int selRed = selectedColor.getRed();
    int selGreen = selectedColor.getGreen();
    int selBlue = selectedColor.getBlue();

    boolean foundDot = false;
    int selPosX = 255;
    int selPosY = 0;

    Dimension dotPos = panel.getDotPos();
    if (dotPos != null) {
      selPosX = (int)dotPos.getWidth();
      selPosY = (int)dotPos.getHeight();
      foundDot = true;
    }

    double red = color.getRed();
    double green = color.getGreen();
    double blue = color.getBlue();

    int lastPixel = 256 * 256;
    int[] colors = new int[lastPixel];

    lastPixel--;

    for (int d = 0;d < 256;d++) {
      double redBlack = 255 - ((double)d) / 255 * red;
      double greenBlack = 255 - ((double)d) / 255 * green;
      double blueBlack = 255 - ((double)d) / 255 * blue;

      for (int l = 0;l < 256;l++) {
        int redBlackWhite = (int)(255 - ((255 - ((double)l)) / 255 * redBlack));
        int greenBlackWhite = (int)(255 - ((255 - ((double)l)) / 255 * greenBlack));
        int blueBlackWhite = (int)(255 - ((255 - ((double)l)) / 255 * blueBlack));

        if (!foundDot && redBlackWhite == selRed && greenBlackWhite == selGreen && blueBlackWhite == selBlue) {
          selPosX = 255 - l;
          selPosY = 255 - d;
          foundDot = true;
        }

        int colorNum = lastPixel - (d * 256 + l);
        colors[colorNum] = ((255 << 24) | (redBlackWhite << 16) | (greenBlackWhite << 8) | blueBlackWhite);
      }
    }

    Image image = component.createImage(new MemoryImageSource(256,256,ColorModel.getRGBdefault(),colors,0,256));
    graphics.drawImage(image,0,0,null);

    // circle the selected color
    selPosX -= 5;
    selPosY -= 5;

    ButterflyPainter.enableAntialias((Graphics2D)graphics);

    graphics.setColor(Color.BLACK);
    graphics.drawOval(selPosX + 1,selPosY + 1,8,8);
    graphics.drawOval(selPosX - 1,selPosY - 1,12,12);

    graphics.setColor(Color.WHITE);
    graphics.drawOval(selPosX,selPosY,10,10);
  }


  /***********************************************************************************************
  Creates a <code>ButterflyGradientColorPanelUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyGradientColorPanelUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyGradientColorPanelUI();
  }
}