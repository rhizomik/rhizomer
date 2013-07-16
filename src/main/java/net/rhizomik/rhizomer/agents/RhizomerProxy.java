package net.rhizomik.rhizomer.agents;

import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.net.URL;
import java.net.HttpURLConnection;


public class RhizomerProxy extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response){
        BufferedInputStream webToProxyBuf = null;
        BufferedOutputStream proxyToClientBuf = null;
        HttpURLConnection con;

        try{
            int statusCode;
            int oneByte;
            String methodName;
            String headerText;

            String urlString = request.getParameter("url");

            //String urlString = request.getRequestURL().toString();
            //String queryString = request.getQueryString();

            //urlString += queryString==null?"":"?"+queryString;
            URL url = new URL(urlString);

            System.out.println("Fetching >"+url.toString());

            con =(HttpURLConnection) url.openConnection();

            methodName = request.getMethod();
            con.setRequestMethod(methodName);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setFollowRedirects(false);
            con.setUseCaches(true);

            for( Enumeration e = request.getHeaderNames() ; e.hasMoreElements();){
                String headerName = e.nextElement().toString();
                con.setRequestProperty(headerName,	request.getHeader(headerName));
            }

            con.connect();

            if(methodName.equals("POST")){
                BufferedInputStream clientToProxyBuf = new BufferedInputStream(request.getInputStream());
                BufferedOutputStream proxyToWebBuf 	= new BufferedOutputStream(con.getOutputStream());

                while ((oneByte = clientToProxyBuf.read()) != -1)
                    proxyToWebBuf.write(oneByte);

                proxyToWebBuf.flush();
                proxyToWebBuf.close();
                clientToProxyBuf.close();
            }

            statusCode = con.getResponseCode();
            response.setStatus(statusCode);

            for( Iterator i = con.getHeaderFields().entrySet().iterator() ; i.hasNext() ;){
                Map.Entry mapEntry = (Map.Entry)i.next();
                if(mapEntry.getKey()!=null)
                    response.setHeader(mapEntry.getKey().toString(), ((List)mapEntry.getValue()).get(0).toString());
            }

            webToProxyBuf = new BufferedInputStream(con.getInputStream());
            proxyToClientBuf = new BufferedOutputStream(response.getOutputStream());

            while ((oneByte = webToProxyBuf.read()) != -1)
                proxyToClientBuf.write(oneByte);

            proxyToClientBuf.flush();
            proxyToClientBuf.close();

            webToProxyBuf.close();
            con.disconnect();

        }catch(Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally{
        }
    }
}