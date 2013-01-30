package au.org.massive.launcher;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.util.Iterator;

public class Pool {
    public String[] getVMList (String username) {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("username", "jupitertest1"); // FIXME use 'username'

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://cvl.massive.org.au/cvlvm");
            httpPost.setEntity(new StringEntity(jsonObj.toString(), "application/x-www-form-urlencoded", "UTF-8"));
            HttpResponse response = httpclient.execute(httpPost);

            InputStream is = response.getEntity().getContent();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            StringBuilder str = new StringBuilder();

            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                str.append(line + "\n");
            }

            JSONTokener tokener = new JSONTokener(str.toString());
            JSONObject root = new JSONObject(tokener);

            JSONArray VM_IPs = (JSONArray) root.get("VM_IPs");
            String IPs[] = new String[VM_IPs.length()];
            for (int i = 0; i < VM_IPs.length(); i++) {
                IPs[i] = (String) VM_IPs.get(i);
            }

            return IPs;
        } catch (UnsupportedEncodingException e) {
            // FIXME
            System.out.println("UnsupportedEncodingException");
        } catch (JSONException e) {
            // FIXME
            System.out.println("JSONException");
        } catch (IOException e) {
            // FIXME
            System.out.println("IOException");
        }

        return (new String[0]);
    }
}
      

