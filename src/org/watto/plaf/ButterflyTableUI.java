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

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


/***********************************************************************************************
Used to paint the GUI for <code>WSTable</code>s
***********************************************************************************************/
public class ButterflyTableUI extends BasicTableUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);

    JTable table = (JTable)component;

    ButterflyTableCellRenderer rend = new ButterflyTableCellRenderer();
    table.setDefaultRenderer(Object.class,rend);
    table.setDefaultRenderer(Number.class,rend);
    table.setDefaultRenderer(Icon.class,rend);

    //table.setIntercellSpacing(new Dimension(0,0));

    TableColumnModel model = table.getColumnModel();
    for (int i = 0;i < table.getColumnCount();i++) {
      //model.getColumn(i).sizeWidthToFit();
      TableColumn column = model.getColumn(i);
      column.setCellRenderer(rend);
    }
  }


  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component){
    super.uninstallUI(component);

    JTable table = (JTable)component;

    DefaultTableCellRenderer rend = new DefaultTableCellRenderer();
    table.setDefaultRenderer(Object.class,rend);

    TableColumnModel model = table.getColumnModel();
    for (int i = 0;i < table.getColumnCount();i++) {
      //model.getColumn(i).sizeWidthToFit();
      TableColumn column = model.getColumn(i);
      column.setCellRenderer(rend);
    }
  }


  /***********************************************************************************************
  Creates a <code>ButterflyTableUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyTableUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyTableUI();
  }
}