package au.org.massive.launcher;

import java.io.FileReader;
import java.util.ArrayList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

import au.org.massive.launcher.LauncherVersionNumber;

public class VersionNumberCheck
{
    private String javaLauncherVersionNumberFromWebPage = "";
    private VersionNumberCheck versionNumberCheck = this;

    // Main function just for unit testing.
    public final static void main(String[] args) 
    {
        VersionNumberCheck versionNumberCheckTest = new VersionNumberCheck();
        versionNumberCheckTest.getVersionNumberFromWebPage();
        System.out.println("Java Launcher version number from webpage = " + versionNumberCheckTest.javaLauncherVersionNumberFromWebPage);
        System.out.println("Java Launcher version number from LauncherVersionNumber class = " + LauncherVersionNumber.javaLauncherVersionNumber);
    }

    public String getVersionNumberFromWebPage()
    {
        ParserDelegator parserDelegator = new ParserDelegator();
        ParserCallback parserCallback = new ParserCallback()
        {
            public void handleText(final char[] data, final int pos) {}

            public void handleStartTag(Tag tag, MutableAttributeSet attribute, int pos)
            {
                if (tag == Tag.A)
                {
                    String anchorID = (String) attribute.getAttribute(Attribute.ID);
                    if (anchorID!=null && anchorID.equals("JavaLauncherVersionNumber"))
                        versionNumberCheck.javaLauncherVersionNumberFromWebPage = (String) attribute.getAttribute(Attribute.TITLE);
                }
            }

            public void handleEndTag(Tag t, final int pos) {}

            public void handleSimpleTag(Tag t, MutableAttributeSet a, final int pos) {}

            public void handleComment(final char[] data, final int pos) {}

            public void handleError(final java.lang.String errMsg, final int pos) {}
        };
        try
        {
            URL url = new URL("https://www.massive.org.au/userguide/cluster-instructions/massive-launcher");
            InputStream inputStream = url.openStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            boolean ignoreCharSet = true;
            parserDelegator.parse(inputStreamReader, parserCallback, ignoreCharSet);
        }
        catch (IOException e)
        {
            System.err.println(e);
        }    

        return versionNumberCheck.javaLauncherVersionNumberFromWebPage;
    }
}
