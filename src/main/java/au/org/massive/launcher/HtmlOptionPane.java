package au.org.massive.launcher;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * htmlContent should not include opening and closing html tags. They will be added automatically.
 */
public class HtmlOptionPane
{
    public static void showMessageDialog(Component parentComponent, String htmlContent, String title, int messageType, Icon icon)
    {
        // for copying style
        JLabel label = new JLabel();
        Font font = label.getFont();

        // create some css from the label's font
        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
        style.append("font-size:" + font.getSize() + "pt;");

        // html content
        JEditorPane editorPane = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" +
                //+ "some text, and <a href=\"http://google.com/\">a link</a>" 
                htmlContent +
                "</body></html>");

        // handle link events
        editorPane.addHyperlinkListener(new HyperlinkListener()
        {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                    //ProcessHandler.launchUrl(e.getURL().toString()); // roll your own link launcher or use Desktop if J6+
                    // java.awt.Desktop requires Java 1.6 or later.
                    try
                    {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    }
                    catch(java.net.URISyntaxException e1)
                    {
                        System.err.println(e1.getMessage());
                    }
                    catch(java.io.IOException e2)
                    {
                        System.err.println(e2.getMessage());
                    }
            }
        });
        editorPane.setEditable(false);
        editorPane.setBackground(label.getBackground());

        // show
        JOptionPane.showMessageDialog(parentComponent, editorPane, title, messageType, icon);
    }
}
