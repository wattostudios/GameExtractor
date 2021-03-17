////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       wattostudios                                         //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2020  wattostudios                            //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the wattostudios website at http://www.watto.org or email watto@watto.org               //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumnModel;

/***********************************************************************************************
Adds the individual <code>WSTextField</code>s for a <code>WSTableHeaderFilter</code>
***********************************************************************************************/

public class WSTableHeaderFilterRenderer extends JPanel implements TableColumnModelListener {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  JTable table = new JTable(0, 0);

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public WSTableHeaderFilterRenderer(JTable table) {
    rebuild(table);
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public void rebuild(JTable table) {
    this.table = table;
    table.setPreferredScrollableViewportSize(table.getPreferredSize());
    table.getColumnModel().addColumnModelListener(this);
    removeAll();
    setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    for (int i = 0; i < table.getColumnCount(); i++) {
      JTextField tf = new JTextField();
      tf.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.BLACK));
      tf.setBackground(Color.yellow);
      add(tf);
    }
    columnMarginChanged(new ChangeEvent(table.getColumnModel()));
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  @Override
  public void columnMarginChanged(ChangeEvent e) {
    TableColumnModel tcm = table.getColumnModel();
    int columns = tcm.getColumnCount();

    for (int i = 0; i < columns; i++) {
      JTextField textField = (JTextField) getComponent(i);
      Dimension d = textField.getPreferredSize();
      d.width = tcm.getColumn(i).getWidth();
      textField.setPreferredSize(d);
    }
    revalidate();

    /*SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
            revalidate();
        }
    });*/
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  @Override
  public void columnMoved(TableColumnModelEvent e) {
    Component moved = getComponent(e.getFromIndex());
    remove(e.getFromIndex());
    add(moved, e.getToIndex());
    validate();
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  @Override
  public void columnAdded(TableColumnModelEvent e) {
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  @Override
  public void columnRemoved(TableColumnModelEvent e) {
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  @Override
  public void columnSelectionChanged(ListSelectionEvent e) {
  }
}