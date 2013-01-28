/* Copyright (C) 2011 Brian P. Hinz
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

package com.turbovnc.rfb;

import com.turbovnc.rdr.*;
import com.turbovnc.vncviewer.*;

public class CSecurityIdent extends CSecurity {

  public CSecurityIdent() { }

  public boolean processMsg(CConnection cc) {
    OutStream os = cc.getOutStream();

    StringBuffer username = new StringBuffer();

    // JW: Launcher passes in username as property of Options object,
    // so there's no need to pop up a dialog asking for a username
    // if it is already recorded within the Options object.
    // Cast cc object as CConn, so we access to opts
    CConn cconn = (CConn) cc;
    if (cconn.opts.username==null || cconn.opts.username.equals(""))
      CConn.upg.getUserPasswd(username, null);
    else
      username.append(cconn.opts.username);

    // Return the response to the server
    os.writeU32(username.length());
    try {
      byte[] utf8str = username.toString().getBytes("UTF8");
      os.writeBytes(utf8str, 0, username.length());
    } catch(java.io.UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    os.flush();
    return true;
  }

  public int getType() { return Security.secTypeIdent; }

  java.net.Socket sock;
  UserPasswdGetter upg;

  static LogWriter vlog = new LogWriter("Ident");
  public String description() { return "Ident"; }

}
