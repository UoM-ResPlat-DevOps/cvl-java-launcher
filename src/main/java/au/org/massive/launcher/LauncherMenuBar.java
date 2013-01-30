/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2011 Brian P. Hinz
 * Copyright (C) 2012 D. R. Commander.  All Rights Reserved.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

package au.org.massive.launcher;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.TextAction;
import javax.swing.text.JTextComponent;

import com.turbovnc.rfb.*;

public class LauncherMenuBar extends JMenuBar
{
    private LauncherMenuBar launcherMenuBar = this;

    private JFrame launcherMainFrame;
    private JFrame launcherLogWindow;

    private JMenu fileMenu = new JMenu("File");
    private JMenuItem exitMenuItem = new JMenuItem("Exit");

    private JMenu editMenu = new JMenu("Edit");
    private JMenuItem cutMenuItem = new JMenuItem("Cut");
    private JMenuItem copyMenuItem = new JMenuItem("Copy");
    private JMenuItem pasteMenuItem = new JMenuItem("Paste");
    private JMenuItem selectAllMenuItem = new JMenuItem("Select All");
    private JMenuItem selectNoneMenuItem = new JMenuItem("Select None");

    private JMenu windowMenu = new JMenu("Window");
    private JCheckBoxMenuItem launcherMainFrameMenuItem = new JCheckBoxMenuItem("MASSIVE/CVL Launcher");
    private JCheckBoxMenuItem launcherLogWindowMenuItem = new JCheckBoxMenuItem("MASSIVE/CVL Launcher Log Window");

    private JMenu helpMenu = new JMenu("Help");
    private JMenuItem aboutMenuItem = new JMenuItem("About MASSIVE/CVL Launcher");

    private boolean logWindowHasBeenAddedToWindowsMenu = false;

    public void addLogWindowToWindowsMenu(JFrame launcherLogWindow)
    {
        if (logWindowHasBeenAddedToWindowsMenu==false)
        {
            logWindowHasBeenAddedToWindowsMenu = true;

            this.launcherLogWindow = launcherLogWindow;
            launcherLogWindowMenuItem.setText(launcherLogWindow.getTitle());
            windowMenu.add(launcherLogWindowMenuItem);
            launcherLogWindowMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    launcherMenuBar.launcherLogWindow.setVisible(true);
                    launcherMenuBar.launcherLogWindow.setExtendedState(JFrame.NORMAL);
                    launcherMenuBar.launcherLogWindow.toFront();
                    launcherMenuBar.launcherLogWindow.requestFocus();
                }
            });
        }
    }

    public void selectLauncherMainFrameInWindowMenu()
    {
        launcherMainFrameMenuItem.setState(true);
        launcherLogWindowMenuItem.setState(false);
    }

    public void selectLauncherLogWindowInWindowMenu()
    {
        launcherLogWindowMenuItem.setState(true);
        launcherMainFrameMenuItem.setState(false);
    }

    public LauncherMenuBar(JFrame launcherMainFrame)
    {
        initializeLauncherMenuBar(launcherMainFrame, null);
        launcherMainFrameMenuItem.setState(true);
        launcherLogWindowMenuItem.setState(false);
    }

    // This constructor is used for the log window's menu bar.
    public LauncherMenuBar(JFrame launcherMainFrame, JFrame launcherLogWindow)
    {
        initializeLauncherMenuBar(launcherMainFrame, launcherLogWindow);
        launcherLogWindowMenuItem.setState(true);
        launcherMainFrameMenuItem.setState(false);
    }

    private void initializeLauncherMenuBar(JFrame launcherMainFrame, JFrame launcherLogWindow)
    {
        this.launcherMainFrame = launcherMainFrame;
        this.launcherLogWindow = launcherLogWindow;

        //int acceleratorMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        // File menu
        
        if (!System.getProperty("os.name").startsWith("Mac"))
        {
            fileMenu.add(exitMenuItem);
            add(fileMenu);
        }

        // Edit menu
        
        // The edit menu was only implemented for Mac OS X in the wxPython 
        // Launcher, because on Mac OS X, in wxPython you don't get the 
        // right-click menu in a text area (e.g. log window), so you need 
        // an Edit menu to be able to copy or select all.
        if (System.getProperty("os.name").startsWith("Mac"))
        {
            editMenu.add(cutMenuItem);
            editMenu.add(copyMenuItem);
            editMenu.add(pasteMenuItem);
            editMenu.add(selectAllMenuItem);
            editMenu.add(selectNoneMenuItem);
            add(editMenu);
        }

        launcherMainFrameMenuItem.setText(launcherMainFrame.getTitle());
        windowMenu.add(launcherMainFrameMenuItem);
        if (launcherLogWindow!=null)
        {
            logWindowHasBeenAddedToWindowsMenu = true;
            launcherLogWindowMenuItem.setText(launcherLogWindow.getTitle());
            windowMenu.add(launcherLogWindowMenuItem);
        }
        add(windowMenu);

        helpMenu.add(aboutMenuItem);
        add(helpMenu);

        // File menu event handlers

        exitMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                // FIXME: Should do a clean up here.
                System.exit(0);
            }
        });

        // Edit menu event handlers

        cutMenuItem.setAccelerator(KeyStroke.getKeyStroke("meta pressed X"));
        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke("meta pressed C"));
        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke("meta pressed V"));
        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke("meta pressed A"));
        selectNoneMenuItem.setAccelerator(KeyStroke.getKeyStroke("shift meta pressed A"));

        if (System.getProperty("os.name").startsWith("Mac"))
        {
            cutMenuItem.addActionListener(new DefaultEditorKit.CutAction());
            copyMenuItem.addActionListener(new DefaultEditorKit.CopyAction());
            pasteMenuItem.addActionListener(new DefaultEditorKit.PasteAction());

            class SelectAllAction extends TextAction
            {
                public SelectAllAction()
                {
                    super("Select All");
                }

                public void actionPerformed(ActionEvent e)
                {
                    JTextComponent component = getFocusedComponent();
                    component.selectAll();
                }
            }
            selectAllMenuItem.addActionListener(new SelectAllAction());

            class SelectNoneAction extends TextAction
            {
                public SelectNoneAction()
                {
                    super("Select None");
                }

                public void actionPerformed(ActionEvent e)
                {
                    JTextComponent component = getFocusedComponent();
                    component.setCaretPosition(0);
                }
            }
            selectNoneMenuItem.addActionListener(new SelectNoneAction());
        }
       
        // Window menu event handlers
        
        launcherMainFrameMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                launcherMenuBar.launcherMainFrame.setVisible(true);
                launcherMenuBar.launcherMainFrame.setExtendedState(JFrame.NORMAL);
                launcherMenuBar.launcherMainFrame.toFront();
                launcherMenuBar.launcherMainFrame.requestFocus();
                //launcherMainFrameMenuItem.setState(true);
                //launcherLogWindowMenuItem.setState(false);
            }
        });

        if (launcherLogWindow!=null)
        {
            launcherLogWindowMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    launcherMenuBar.launcherLogWindow.setVisible(true);
                    launcherMenuBar.launcherLogWindow.setExtendedState(JFrame.NORMAL);
                    launcherMenuBar.launcherLogWindow.toFront();
                    launcherMenuBar.launcherLogWindow.requestFocus();
                    //launcherMainFrameMenuItem.setState(false);
                    //launcherLogWindowMenuItem.setState(true);
                }
            });
        }

        // Help menu event handlers

        aboutMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                Class launcherMainFrameClass = launcherMenuBar.launcherMainFrame.getClass();
                Icon massiveIcon = new ImageIcon(launcherMainFrameClass.getResource("MASSIVElogoTransparent64x64.png"));
                JOptionPane.showMessageDialog(launcherMenuBar.launcherMainFrame,
                  "MASSIVE/CVL Launcher (Java) v" + LauncherVersionNumber.javaLauncherVersionNumber,
                  "About MASSIVE/CVL Launcher", JOptionPane.INFORMATION_MESSAGE, massiveIcon);
            }
        });
    }
}
