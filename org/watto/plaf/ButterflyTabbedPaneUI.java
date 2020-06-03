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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSSplitPane</code>s
***********************************************************************************************/
public class ButterflyTabbedPaneUI extends BasicTabbedPaneUI {

  /***********************************************************************************************
  Creates a <code>ButterflyTabbedPaneUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyTabbedPaneUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyTabbedPaneUI();
  }

  /***********************************************************************************************

   ***********************************************************************************************/
  public int calculateTabWidth(int tabPlacement,int tabIndex,FontMetrics metrics){
    return tabPane.getWidth() / tabPane.getComponentCount();
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public void paintTabBorder(Graphics graphics,int tabPlacement,int tabIndex,int x,int y,int w,int h,boolean isSelected){
    if (isSelected) {
      ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,x,y,w,h + 10);
    }
    else {
      ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,x,y-1,w,h + 10,LookAndFeelManager.getLightColor(),LookAndFeelManager.getMidColor());
    }
  }

  /***********************************************************************************************

   ***********************************************************************************************/
//  public LayoutManager createLayoutManager(){
//    // TODO - overwrite this so that the selected tabs are not bigger than the other tabs, etc.
//    return new TabbedPaneLayout();
//  }

  /***********************************************************************************************

   ***********************************************************************************************/
  public int getTabLabelShiftX(int tabPlacement,int tabIndex,boolean isSelected){
    return 0;
  }

  /***********************************************************************************************

   ***********************************************************************************************/
  public int getTabLabelShiftY(int tabPlacement,int tabIndex,boolean isSelected){
    return 0;
  }


  /***********************************************************************************************

  ***********************************************************************************************/
  public void paintTabBackground(Graphics graphics,int tabPlacement,int tabIndex,int x,int y,int w,int h,boolean isSelected){
    //ButterflyPainter.paintSolidBackground((Graphics2D)graphics,x,y,w,h,LookAndFeelManager.getBackgroundColor());
  }

}