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

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalRadioButtonUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSRadioButton</code>s
***********************************************************************************************/
public class ButterflyRadioButtonUI extends MetalRadioButtonUI {

  /** static instance of the GUI painter **/
  private static final ButterflyRadioButtonUI radioButtonUI = new ButterflyRadioButtonUI();


  /***********************************************************************************************
  Sets up the <code>ButterflyCheckBoxIcon</code> as the icon for this <code>checkbox</code>
  @param checkbox the <code>WSCheckBox</code>
  ***********************************************************************************************/
  public void installDefaults(AbstractButton radioButton){
    super.installDefaults(radioButton);
    icon = new ButterflyRadioButtonIcon();
  }


  /***********************************************************************************************
  Gets the static <code>radioButtonUI</code> instance
  @param component the <code>Component</code> to get the painter for
  @return the painter <code>ComponentUI</code>
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return radioButtonUI;
  }
}