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
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;


/***********************************************************************************************
Used to paint the GUI for <code>LayeredOverlayColor</code>s
***********************************************************************************************/
public class ButterflyShadowPanelUI extends BasicPanelUI {

  /** static instance of the GUI painter **/
  private static final ButterflyShadowPanelUI panelUI = new ButterflyShadowPanelUI();


  /***********************************************************************************************
  Generates the shadow for a <code>BufferedImage</code>. The shadow is specified in <code>size</code>, 
  <code>opacity</code> and <code>color</code>.
  <br /><br />
  The generated shadow <code>BufferedImage</code> has <i>width = imageWidth + 2 * size</i> and
  <i>height = imageHeight + 2 * size</i>.
  <br /><br />
  This code comes directly from the org.jdesktop.swingx.graphics.ShadowRenderer class, written by
  Romain Guy, Sebastien Petrucci, and rbair
  @param image the <code>BufferedImage</code> to generate the shadow for
  @param size the size of the shadow in pixels
  @param opacity the opacity of the shadow
  @param color the color of the shadow
  @return a <code>BufferedImage</code> containing the shadow of <code>image</code>
  ***********************************************************************************************/
  public BufferedImage createShadow(BufferedImage image,int size,float opacity,Color color){
    int shadowSize = size * 2;

    int srcWidth = image.getWidth();
    int srcHeight = image.getHeight();

    int dstWidth = srcWidth + shadowSize;
    int dstHeight = srcHeight + shadowSize;

    int left = size;
    int right = shadowSize - left;

    int yStop = dstHeight - right;

    int shadowRgb = color.getRGB() & 0x00FFFFFF;
    int[] aHistory = new int[shadowSize];
    int historyIdx;

    int aSum;

    BufferedImage dst = new BufferedImage(dstWidth,dstHeight,BufferedImage.TYPE_INT_ARGB);

    int[] dstBuffer = new int[dstWidth * dstHeight];
    int[] srcBuffer = new int[srcWidth * srcHeight];

    Raster raster = image.getRaster();
    srcBuffer = (int[])raster.getDataElements(0,0,srcWidth,srcHeight,srcBuffer);

    int lastPixelOffset = right * dstWidth;
    float hSumDivider = 1.0f / shadowSize;
    float vSumDivider = opacity / shadowSize;

    int[] hSumLookup = new int[256 * shadowSize];
    for (int i = 0;i < hSumLookup.length;i++) {
      hSumLookup[i] = (int)(i * hSumDivider);
    }

    int[] vSumLookup = new int[256 * shadowSize];
    for (int i = 0;i < vSumLookup.length;i++) {
      vSumLookup[i] = (int)(i * vSumDivider);
    }

    int srcOffset;

    // horizontal pass : extract the alpha mask from the source picture and
    // blur it into the destination picture
    for (int srcY = 0,dstOffset = left * dstWidth;srcY < srcHeight;srcY++) {

      // first pixels are empty
      for (historyIdx = 0;historyIdx < shadowSize;) {
        aHistory[historyIdx++] = 0;
      }

      aSum = 0;
      historyIdx = 0;
      srcOffset = srcY * srcWidth;

      // compute the blur average with pixels from the source image
      for (int srcX = 0;srcX < srcWidth;srcX++) {

        int a = hSumLookup[aSum];
        dstBuffer[dstOffset++] = a << 24; // store the alpha value only
        // the shadow color will be added in the next pass

        aSum -= aHistory[historyIdx]; // substract the oldest pixel from the sum

        // extract the new pixel ...
        a = srcBuffer[srcOffset + srcX] >>> 24;
        aHistory[historyIdx] = a; // ... and store its value into history
        aSum += a; // ... and add its value to the sum

        if (++historyIdx >= shadowSize) {
          historyIdx -= shadowSize;
        }
      }

      // blur the end of the row - no new pixels to grab
      for (int i = 0;i < shadowSize;i++) {

        int a = hSumLookup[aSum];
        dstBuffer[dstOffset++] = a << 24;

        // substract the oldest pixel from the sum ... and nothing new to add !
        aSum -= aHistory[historyIdx];

        if (++historyIdx >= shadowSize) {
          historyIdx -= shadowSize;
        }
      }
    }

    // vertical pass
    for (int x = 0,bufferOffset = 0;x < dstWidth;x++,bufferOffset = x) {

      aSum = 0;

      // first pixels are empty
      for (historyIdx = 0;historyIdx < left;) {
        aHistory[historyIdx++] = 0;
      }

      // and then they come from the dstBuffer
      for (int y = 0;y < right;y++,bufferOffset += dstWidth) {
        int a = dstBuffer[bufferOffset] >>> 24; // extract alpha
        aHistory[historyIdx++] = a; // store into history
        aSum += a; // and add to sum
      }

      bufferOffset = x;
      historyIdx = 0;

      // compute the blur average with pixels from the previous pass
      for (int y = 0;y < yStop;y++,bufferOffset += dstWidth) {

        int a = vSumLookup[aSum];
        dstBuffer[bufferOffset] = a << 24 | shadowRgb; // store alpha value + shadow color

        aSum -= aHistory[historyIdx]; // substract the oldest pixel from the sum

        a = dstBuffer[bufferOffset + lastPixelOffset] >>> 24; // extract the new pixel ...
        aHistory[historyIdx] = a; // ... and store its value into history
        aSum += a; // ... and add its value to the sum

        if (++historyIdx >= shadowSize) {
          historyIdx -= shadowSize;
        }
      }

      // blur the end of the column - no pixels to grab anymore
      for (int y = yStop;y < dstHeight;y++,bufferOffset += dstWidth) {

        int a = vSumLookup[aSum];
        dstBuffer[bufferOffset] = a << 24 | shadowRgb;

        aSum -= aHistory[historyIdx]; // substract the oldest pixel from the sum

        if (++historyIdx >= shadowSize) {
          historyIdx -= shadowSize;
        }
      }
    }

    WritableRaster writeRaster = dst.getRaster();
    writeRaster.setDataElements(0,0,dstWidth,dstHeight,dstBuffer);

    return dst;
  }


  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);
    //component.setOpaque(false);
    //component.setBackground(Color.WHITE);
  }


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    int x = 0;
    int y = 0;
    int w = component.getWidth() - 8;
    int h = component.getHeight() - 8;

    int round = LookAndFeelManager.getPropertyInt("ROUNDNESS");

    BufferedImage buffer = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics2D = buffer.createGraphics();
    ButterflyPainter.enableAntialias(graphics2D);

    // BACKGROUND
    Shape clip = graphics2D.getClip();
    graphics2D.clip(new RoundRectangle2D.Float(x,y,w,h,round,round));
    ButterflyPainter.paintSolidBackground(graphics2D,x,y,w,h,new Color(0,0,0,80));
    graphics2D.setClip(clip);

    BufferedImage shadow = createShadow(buffer,5,1.0f,Color.BLACK);

    graphics.drawImage(shadow,x,y,null);
  }


  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component){
    super.uninstallUI(component);
    //component.setOpaque(true);
  }


  /***********************************************************************************************
  Gets the static <code>panelUI</code> instance
  @param component the <code>Component</code> to get the painter for
  @return the painter <code>ComponentUI</code>
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return panelUI;
  }

}