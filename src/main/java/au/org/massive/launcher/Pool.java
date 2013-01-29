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
import org.json.JSONException;


public class Pool {
    public static void main() {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("queryMessage", "username=jupitertest1");
            jsonObj.put("query", "Send to user management");

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://cvl.massive.org.au/usermanagement/query.php");

            httpPost.setEntity(new StringEntity(jsonObj.toString(), "multipart/form-data", "UTF-8")); // application/x-www-form-urlencoded", "UTF-8"));

            HttpResponse response = httpclient.execute(httpPost);

            InputStream is = response.getEntity().getContent();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            StringBuilder str = new StringBuilder();

            String line = null;
            System.out.println("here");

            while ((line = bufferedReader.readLine()) != null) {
                str.append(line + "\n");
            }
            System.out.print("str: ");
            System.out.println(str.toString());


            // entity = response.getEntity();
            // System.out.println(EntityUtils.getContentMimeType(entity));
            // System.out.println(EntityUtils.getContentCharSet(entity));

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
    }
}
      

