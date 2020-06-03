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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import org.watto.component.WSGradientColorSlider;


/***********************************************************************************************
Used to paint the GUI for <code>WSGradientColorSlider</code>s
***********************************************************************************************/
public class ButterflyGradientColorSliderUI extends BasicPanelUI {

  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    WSGradientColorSlider panel = (WSGradientColorSlider)component;

    int[] colors = new int[256 * 20];

    double red = 255;
    double green = 0;
    double blue = 0;

    double increment42 = ((double)255 / 42);
    double increment43 = ((double)255 / 43);

    int colorNum = 0;

    // Red --> Purple
    for (int d = 0;d < 42;d++) {
      for (int l = 0;l < 20;l++) {
        colors[colorNum] = ((255 << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue);
        colorNum++;
      }
      blue += increment42;
    }

    // Purple --> Blue
    red = 255;
    green = 0;
    blue = 255;

    for (int d = 0;d < 43;d++) {
      for (int l = 0;l < 20;l++) {
        colors[colorNum] = ((255 << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue);
        colorNum++;
      }
      red -= increment43;
    }

    // Blue --> Cyan
    red = 0;
    green = 0;
    blue = 255;
    for (int d = 0;d < 42;d++) {
      for (int l = 0;l < 20;l++) {
        colors[colorNum] = ((255 << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue);
        colorNum++;
      }
      green += increment42;
    }

    // Cyan --> Green
    red = 0;
    green = 255;
    blue = 255;
    for (int d = 0;d < 43;d++) {
      for (int l = 0;l < 20;l++) {
        colors[colorNum] = ((255 << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue);
        colorNum++;
      }
      blue -= increment43;
    }

    // Green --> Yellow
    red = 0;
    green = 255;
    blue = 0;
    for (int d = 0;d < 42;d++) {
      for (int l = 0;l < 20;l++) {
        colors[colorNum] = ((255 << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue);
        colorNum++;
      }
      red += increment42;
    }

    // Yellow --> Red
    red = 255;
    green = 255;
    blue = 0;
    for (int d = 0;d < 43;d++) {
      for (int l = 0;l < 20;l++) {
        colors[colorNum] = ((255 << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue);
        colorNum++;
      }
      green -= increment43;
    }

    // red again, for the last rowstartPos = 0;
    red = 255;
    green = 0;
    blue = 0;
    int lastColor = ((255 << 24) | ((int)red << 16) | ((int)green << 8) | (int)blue);
    for (int l = 0;l < 20;l++) {
      colors[colorNum] = lastColor;
      colorNum++;
    }

    Image image = component.createImage(new MemoryImageSource(20,256,ColorModel.getRGBdefault(),colors,0,20));
    graphics.drawImage(image,0,0,null);

    if (panel.getShowDot()) {
      int dotPos = panel.getDotPos() - 2;

      ButterflyPainter.enableAntialias((Graphics2D)graphics);

      graphics.setColor(Color.BLACK);
      graphics.fillOval(21,dotPos,4,4);
    }
  }


  /***********************************************************************************************
  Creates a <code>ButterflyGradientColorSliderUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyGradientColorSliderUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyGradientColorSliderUI();
  }
}