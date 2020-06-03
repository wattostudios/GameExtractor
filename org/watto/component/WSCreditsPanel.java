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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.timer.RepaintTimer;
import org.watto.xml.XHTMLReader;
import org.watto.xml.XHTMLRenderer;
import org.watto.xml.XMLNode;

/**
 **********************************************************************************************
 * A ExtendedTemplate
 **********************************************************************************************
 **/
public class WSCreditsPanel extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  XMLNode tree = null;

  java.util.Timer timer;

  int paintTop = 0;
  int imageHeight = 0;

  int oldWidth = 0;
  int oldHeight = 0;

  BufferedImage bi = null;

  /** the number of times to repaint the window per second **/
  int fps = 40;
  /** the number of pixels to jump up every repaint **/
  int jumpPixels = 1;
  /** whether to paint the fading at the top and bottom (slower) **/
  boolean paintFade = true;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  WSCreditsPanel() {
    super();
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSCreditsPanel(XMLNode node) {
    // NEED TO DO THIS HERE, OTHERWISE THE SETTING VARIABLE DOESN'T GET SAVED!!! (not sure why)
    //super(node);
    super();
    toComponent(node);
    registerEvents();

    loadFile(new File(Settings.get("AboutFile")));
  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void clearBuffer() {
    bi = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadFile(File file) {
    tree = XHTMLReader.read(file);
  }

  /**
   **********************************************************************************************
   * Paints the rendered credits image onto the screen
   **********************************************************************************************
   **/
  public void paintCredits(Graphics g) {

    //System.out.println("PAINTING at " + paintTop);
    //paintTop-= jumpPixels;

    int width = getWidth();
    int height = getHeight();

    if (0 - paintTop > imageHeight) {
      // restart the credits again
      paintTop = 0;
    }

    if (bi == null) {
      if (width <= 0 || height <= 0) {
        return;
      }
    }
    else if (width != oldWidth || height != oldHeight) {
      bi = null;
    }

    if (bi == null) {
      // stops the timer first, builds the image, then starts the timer again.
      // This is try and avoid the occasional problem where the credits panel
      // only shows white.
      stopScrollTimer();
      renderImage();
      startScrollTimer();
    }

    try {
      g.drawImage(bi, 0, paintTop, this);
      //g.drawImage(bi,0,-500,this);
    }
    catch (Throwable t) {
      // occurs when restarting the credits from the beginning???
    }

  }

  /**
   **********************************************************************************************
   * Renders the credits image
   **********************************************************************************************
   **/
  public void renderImage() {
    renderImage(tree);
  }

  /**
   **********************************************************************************************
   * Renders the credits image
   **********************************************************************************************
   **/
  public void renderImage(XMLNode tree) {
    try {

      int width = getWidth();
      int height = getHeight();

      oldWidth = width;
      oldHeight = height;

      //paintTop = height;
      paintTop = 0;

      bi = (BufferedImage) createImage(width, height * 6);
      Graphics g = bi.createGraphics();

      g.setColor(Color.BLACK);
      g.fillRect(0, 0, width, height * 6);

      // Anti-aliasing
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      // -30 here for a 15px border, and +30 a few lines down so the image is the right size
      Dimension totalSize = new XHTMLRenderer().paint(tree, g, 15, height, width - 30, height * 6);

      /*
       * // THIS USE TO CAUSE ERRORS WHEN THE HTML TREE WASN'T LOADED CORRECTLY
       *
       * // Making the credit image the right height BufferedImage bi2 =
       * (BufferedImage)createImage((int)totalSize.getWidth()+30,(int)totalSize.getHeight() +
       * height*2); Graphics gNew = bi2.createGraphics();
       *
       * // throws a null pointer error (for bi?) if (bi != null){ gNew.drawImage(bi,0,0,null); }
       *
       * bi = bi2;
       */

      //imageHeight = height*6;

      // NOTE the +height so it scrolls right through the image before restarting
      imageHeight = (int) totalSize.getHeight() + height + 30;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   * Tells the credits to be rendered a few pixels higher each call
   **********************************************************************************************
   **/
  @Override
  public void repaint() {
    if (!isShowing()) {
      super.repaint();
      return;
    }

    paintTop -= jumpPixels;

    Graphics g = getGraphics();
    paintCredits(g);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reset() {
    paintTop = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void startScrollTimer() {
    stopScrollTimer();

    //bi = null;

    timer = new java.util.Timer();

    int repaintTime = (int) (((double) 1 / fps) * 1000);
    timer.scheduleAtFixedRate(new RepaintTimer(this), 0, repaintTime);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void stopScrollTimer() {
    if (timer != null) {
      timer.cancel();
    }
  }

}