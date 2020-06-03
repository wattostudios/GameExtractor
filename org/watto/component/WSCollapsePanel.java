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

package org.watto.component;

import java.awt.BorderLayout;
import java.awt.Component;
import org.watto.ErrorLogger;
import org.watto.event.WSEvent;
import org.watto.event.WSEventableInterface;
import org.watto.xml.XMLNode;

/***********************************************************************************************
A Panel GUI <code>Component</code> that can be expanded and collapsed. This class contains 2
<code>Component</code>s, an <code>expandedPanel</code> and a <code>collapsedPanel</code>, of
which 1 will be shown depending on whether this panel is <code>expanded</code> or not. In order
for the <code>expandedPanel</code> or the <code>collapsedPanel</code> to trigger an expand or
collapse event, they simply need to implement <code>WSHelper.fireEvent(event,this)</code>, where
<code>event</code> is a <code>WSEvent</code> of type <code>EXPAND_PANEL</code> or
<code>COLLAPSE_PANEL</code>. These 2 events will be detected by this panel and will trigger the
change to the appropriate panel.
***********************************************************************************************/

public abstract class WSCollapsePanel extends WSPanel implements WSEventableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** Whether the panel is expanded or collapsed **/
  boolean expanded = false;
  /** The component to show when expanded **/
  WSComponent expandedPanel = null;
  /** The component to show when collapsed **/
  WSComponent collapsedPanel = null;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSCollapsePanel() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
  ***********************************************************************************************/
  public WSCollapsePanel(XMLNode node) {
    super(node);
  }

  /***********************************************************************************************
  Fires a <code>WSEvent</code> when the panel is collapsed
  ***********************************************************************************************/
  private void fireCollapsedEvent() {
    if (!initialised) {
      return;
    }
    WSHelper.fireEvent(new WSEvent(this, WSEvent.COLLAPSE_PANEL), this);
  }

  /***********************************************************************************************
  Fires a <code>WSEvent</code> when the panel is expanded
  ***********************************************************************************************/
  private void fireExpandedEvent() {
    if (!initialised) {
      return;
    }
    WSHelper.fireEvent(new WSEvent(this, WSEvent.EXPAND_PANEL), this);
  }

  /***********************************************************************************************
  Gets whether the panel is expanded or compressed
  @return <b>true</b> if the panel is expanded, and showing the <code>expandedPanel</code><br />
          <b>false</b> if the panel is collapsed, and showing the <code>collapsedPanel</code>
  ***********************************************************************************************/
  public boolean isExpanded() {
    return expanded;
  }

  /***********************************************************************************************
  Collapses the panel, and performs any cleanup that is required. By default, it just changes the
  current panel to show the <code>collapsedPanel</code>
  ***********************************************************************************************/
  public void onCollapse() {
    this.expanded = false;
    removeAll();
    add((Component) collapsedPanel);
  }

  /***********************************************************************************************
  Performs an action when a <code>WSEvent</code> event is triggered
  @param source the <code>Object</code> that triggered the event
  @param event the <code>WSEvent</code>
  @param type the events type ID
  @return <b>true</b> if the event was handled by this class<br />
          <b>false</b> if the event wasn't handled by this class, and thus should be passed on to
          the parent class for handling.
  ***********************************************************************************************/
  @Override
  public boolean onEvent(Object source, WSEvent event, int type) {
    if (type == WSEvent.EXPAND_PANEL) {
      onExpand();
    }
    else if (type == WSEvent.COLLAPSE_PANEL) {
      onCollapse();
    }
    else {
      return super.onEvent(source, event, type);
    }
    return true;
  }

  /***********************************************************************************************
  Expands the panel, and performs any cleanup that is required. By default, it just changes the
  current panel to show the <code>expandedPanel</code>
  ***********************************************************************************************/
  public void onExpand() {
    this.expanded = true;
    removeAll();
    add((Component) expandedPanel);
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    setLayout(new BorderLayout());

    try {
      // add the children to the correct positions
      int childCount = node.getChildCount();
      if (childCount != 2) {
        throw new WSComponentException("A WSCollapsePanel requires exactly 2 children, but you have specified " + childCount);
      }
      for (int i = 0; i < childCount; i++) {

        XMLNode child = node.getChild(i);
        String position = child.getAttribute("position");
        if (position == null) {
          throw new WSComponentException("The children of a WSCollapsePanel must be declared as position=\"expanded\" or position=\"collapsed\", but you didn't specify a position attribute for one of the children.");
        }
        else if (position.equals("expanded")) {
          expandedPanel = (WSComponent) WSHelper.toComponent(child);
        }
        else if (position.equals("collapsed")) {
          collapsedPanel = (WSComponent) WSHelper.toComponent(child);
        }
        else {
          throw new WSComponentException("The children of a WSCollapsePanel must be declared as position=\"expanded\" or position=\"collapsed\", but you used " + position);
        }

      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    String state = node.getAttribute("state");
    if (state == null) {
      state = "collapsed";
    }

    if (state.equals("collapsed")) {
      fireCollapsedEvent();
    }
    else if (state.equals("expanded")) {
      fireExpandedEvent();
    }
    else {
      try {
        throw new WSComponentException("The 'state' of a WSCollapsePanel must be declared as either state=\"expanded\" or state=\"collapsed\", but you specified " + state);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }

  }

  /***********************************************************************************************
  Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
  @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = WSHelper.toXML(this);

    if (expanded) {
      node.setAttribute("state", "expanded");
    }
    else {
      node.setAttribute("state", "collapsed");
    }

    XMLNode expandedChild = expandedPanel.toXML();
    expandedChild.setAttribute("position", "expanded");
    node.addChild(expandedChild);

    XMLNode collapsedChild = collapsedPanel.toXML();
    collapsedChild.setAttribute("position", "collapsed");
    node.addChild(collapsedChild);

    return node;
  }
}