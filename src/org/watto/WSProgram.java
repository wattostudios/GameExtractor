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

package org.watto;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;
import javax.swing.JFrame;
import org.watto.component.WSComponent;
import org.watto.component.WSHelper;
import org.watto.component.WSPluginManager;
import org.watto.component.WSPopup;
import org.watto.component.WSSplashDialog;
import org.watto.event.WSClosableInterface;
import org.watto.event.WSHoverableInterface;
import org.watto.event.listener.WSClosableWindowListener;
import org.watto.plaf.ButterflyLookAndFeel;
import org.watto.plaf.LookAndFeelManager;
import org.watto.task.TaskManager;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;
import org.watto.xml.XMLWriter;

/***********************************************************************************************
 * <b><i>WSProgram 4.0</i></b><br />
 * <i>See the Readme file for information on WSProgram</i><br />
 * <br />
 * Designed to be extended by your own main program class, it sets up and initialises components
 * such as the <code>ErrorLogger</code>, <code>Settings</code>, and <code>Language</code> when
 * <code>super()</code> is called in your main class constructor.
 ***********************************************************************************************/

public abstract class WSProgram extends JFrame implements WSHoverableInterface, WSClosableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The splash screen to show while your program is loading **/
  public WSSplashDialog splash;

  /***********************************************************************************************
   * Constructs the program
   ***********************************************************************************************/
  public WSProgram() {
    buildProgram(this);
  }

  /***********************************************************************************************
   * Generates the interface from the XML file, and sets up the basic program utilities, such as:
   * <ul>
   * <li><code>ErrorLogger</code></li>
   * <li><code>Settings</code></li>
   * <li><code>Language</code></li>
   * <li><code>LookAndFeelManager</code></li>
   * <li><code>WSPopup</code></li>
   * <li><code>TaskManager</code></li>
   * <li><code>WSPluginManager</code></li>
   * <li><code>ComponentRepository</code></li>
   * </ul>
   * @param program <code>this</code> program
   ***********************************************************************************************/
  public void buildProgram(Object program) {
    new ErrorLogger();
    new Settings();
    new Language();

    WSHelper.setResourcePath(program);

    LookAndFeelManager.installLookAndFeel(new ButterflyLookAndFeel());
    LookAndFeelManager.setLookAndFeel(Settings.get("InterfaceLookAndFeel"));

    SingletonManager.add("TaskManager", new TaskManager());
    SingletonManager.add("RecentFilesManager", new RecentFilesManager());

    // We have all the information for the Splash Screen,
    // so now we can load and display it
    splash = WSSplashDialog.getInstance();
    //if (Settings.getBoolean("ShowSplashScreen")) {
    splash.setVisible(true);
    //}

    LookAndFeelManager.calculateTextHeight(splash.getGraphics());

    // load the plugins
    WSSplashDialog.setMessage("Plugins");
    try {
      WSPluginManager.loadPlugins();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    // construct the interface
    WSSplashDialog.setMessage("ConstructInterface");
    constructInterface();

    // This needs to be here, so that it correctly grabs the OverlayPopupDialog if we're using it (rather than the normal PopupDialog)
    new WSPopup();

    // Close the splash screen. If you use the splash screen in your main
    // program constructor, you should set this setting to false so that
    // you can continue using the same splash screen that was created here.
    if (Settings.getBoolean("WSProgramClosesSplashScreen")) {
      splash.dispose();
    }
  }

  /***********************************************************************************************
   * Builds the interface of the program using the Interface XML file from the
   * <code>Settings</code>
   ***********************************************************************************************/
  public void constructInterface() {

    File interfaceFile = new File(Settings.getString("InterfaceFile"));
    if (!interfaceFile.exists()) {
      // try to load the default interface file instead of the real interface file
      interfaceFile = new File(Settings.getString("DefaultInterfaceFile"));
    }

    constructInterface(interfaceFile);

  }

  /***********************************************************************************************
   * Builds the interface of the program from an XML-format <code>File</code>.
   * @param interfaceFile the interface XML <code>File</code>
   ***********************************************************************************************/
  public void constructInterface(File interfaceFile) {
    // TODO - make it read the look and feel from the settings
    LookAndFeelManager.setLookAndFeel("Butterfly");

    Container frame = getContentPane();
    frame.removeAll();
    frame.setLayout(new BorderLayout(0, 0));
    frame.add(WSHelper.toComponent(XMLReader.read(interfaceFile)), BorderLayout.CENTER);

    addWindowListener(new WSClosableWindowListener(this));
  }

  /***********************************************************************************************
   * Saves the interface of the program to the Interface XML file in the <code>Settings</code>
   ***********************************************************************************************/
  public void saveInterface() {
    File interfaceFile = new File(Settings.getString("InterfaceFile"));
    saveInterface(interfaceFile);
  }

  /***********************************************************************************************
   * Saves the interface of the program to an XML-format <code>File</code>.
   * @param interfaceFile the interface XML <code>File</code>
   ***********************************************************************************************/
  public void saveInterface(File interfaceFile) {
    WSComponent rootComponent = (WSComponent) getContentPane().getComponent(0);
    XMLNode node = rootComponent.toXML();

    // Write to a temporary file first
    File tempPath = new File(interfaceFile.getAbsolutePath() + ".tmp");
    if (tempPath.exists()) {
      tempPath.delete();
    }

    boolean success = XMLWriter.writeWithValidation(tempPath, node);
    if (!success) {
      return; // something went wrong when writing the settings, so don't replace the real file with the corrupt one
    }

    // if all is OK, remove the Proper file and then rename the temp one to it.
    // This helps to avoid the occasional issue where the settings file becomes corrupt during write, due to stream being closed.
    if (interfaceFile.exists()) {
      interfaceFile.delete();
    }
    tempPath.renameTo(interfaceFile);

  }
}