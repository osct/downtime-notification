/*
*************************************************************************
Copyright (c) 2011-2016:
Istituto Nazionale di Fisica Nucleare (INFN), Italy
Consorzio COMETA (COMETA), Italy

See http://www.infn.it and and http://www.consorzio-cometa.it for details on
the copyright holders.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author <a href="mailto:giuseppe.larocca@ct.infn.it">Giuseppe La Rocca</a>
***************************************************************************
*/
package it.infn.ct.downtime;

// import liferay libraries

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;

// import DataEngine libraries

// import generic Java libraries
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// import portlet libraries
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import javax.mail.MessagingException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

// Importing Apache libraries
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Downtime extends GenericPortlet {

    private static Log log = LogFactory.getLog(Downtime.class);   

    @Override
    protected void doEdit(RenderRequest request,
            RenderResponse response)
            throws PortletException, IOException
    {

        PortletPreferences portletPreferences =
                (PortletPreferences) request.getPreferences();

        response.setContentType("text/html");                
        
        String downtime_REFRESH = portletPreferences.getValue("downtime_REFRESH", "1440000");        
        String downtime_LOGLEVEL = portletPreferences.getValue("downtime_LOGLEVEL", "INFO");
        String[] downtime_endpoints = portletPreferences.getValues("downtime_endpoints", new String[15]);
        //String downtime_FILESIZE = portletPreferences.getValue("downtime_FILESIZE", "-1");  
        String downtime_IDs = portletPreferences.getValue("downtime_IDs", "-1");
        String SMTP_HOST = portletPreferences.getValue("SMTP_HOST", "N/A");        
        String SENDER_MAIL = portletPreferences.getValue("SENDER_MAIL", "N/A");        
        
        request.setAttribute("downtime_REFRESH", downtime_REFRESH.trim());
        request.setAttribute("downtime_LOGLEVEL", downtime_LOGLEVEL.trim());
        request.setAttribute("downtime_endpoints", downtime_endpoints);
        //request.setAttribute("downtime_FILESIZE", downtime_FILESIZE.trim());
        request.setAttribute("downtime_IDs", downtime_IDs.trim());
        request.setAttribute("SMTP_HOST", SMTP_HOST.trim());
        request.setAttribute("SENDER_MAIL", SENDER_MAIL.trim());
        
        PortletRequestDispatcher dispatcher =
                getPortletContext().getRequestDispatcher("/edit.jsp");

        dispatcher.include(request, response);
    }

    @Override
    protected void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException 
    {
        try {
            PortletPreferences portletPreferences =
                    (PortletPreferences) request.getPreferences();

            response.setContentType("text/html");
            
            PortletRequestDispatcher dispatcher = null;            
            String[] downtime_endpoints = new String [15];
                                
            String downtime_REFRESH = portletPreferences.getValue("downtime_REFRESH", "1440000");            
            String downtime_LOGLEVEL = portletPreferences.getValue("downtime_LOGLEVEL", "INFO");
            downtime_endpoints = portletPreferences.getValues("downtime_endpoints", new String[15]);                   
            String downtime_IDs = portletPreferences.getValue("downtime_IDs", "-1");
            String SMTP_HOST = portletPreferences.getValue("SMTP_HOST", "N/A");            
            String SENDER_MAIL = portletPreferences.getValue("SENDER_MAIL", "N/A");                   
                        
            File df_full = new File("/tmp/full_report.xml");
            File df = null;
            BufferedReader bufReader = null;
            String endpoints = "";
            boolean flag = false;
            
            // Create a file containing all the downtimes.
            FileWriter fileWritter = new FileWriter(df_full, true);            
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            
            String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
            String results_tag_start = "<results>";
            String results_tag_end = "</results>";
            
            bufferWritter.write(header+"\n");
            bufferWritter.write(results_tag_start + "\n");
                        
            for (int i=0; i<downtime_endpoints.length; i++)
            {
                if(downtime_endpoints[i] != null &&
                  !downtime_endpoints[i].isEmpty())
                {
                    SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());

                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
                    String downtime_url = "https://goc.egi.eu/gocdbpi/public/"
                    + "?method=get_downtime_nested_services"                
                    + "&topentity=" + downtime_endpoints[i]
                    + "&page=1&ongoing_only=yes";
                
                    log.info("GOC-DB endpoint: " + downtime_url);
            
                    // Get the list of ongoing downtime
                    URL url = new URL(downtime_url); 
                    try {
                        df = File.createTempFile("result_", ".xml");
                            
                        org.apache.commons.io.FileUtils.copyURLToFile(url, df);                
                        log.info("File: " + df.getAbsolutePath());
            
                        // our XML file for this example
                        File xmlFile = new File(df.getAbsolutePath());                    
                        Reader fileReader = new FileReader(xmlFile);
                        bufReader = new BufferedReader(fileReader);
            
                        StringBuilder sb = new StringBuilder();
                        // Skip the first 2 rows
                        bufReader.readLine();
                        bufReader.readLine();
                    
                        String line = bufReader.readLine();
                        while( line != null)                 
                        {       
                            if ((!line.contains("results")))
                                sb.append(line).append("\n");
                        
                            line = bufReader.readLine();
                        }
                
                        String xml2String = sb.toString();
                        if (xml2String != null && !xml2String.isEmpty()) {
                            log.info("XML to String using BufferedReader: ");                    
                            log.info(xml2String); 
                            
                            bufferWritter.write(xml2String);
                            
                            flag=true;
                        }                                            
                        
                        bufReader.close();
                        df.deleteOnExit();
                        
                    } catch (IOException ex) { log.error(ex); } 
                      finally { df.delete(); }
                }
            }               
            
            bufferWritter.write(results_tag_end+"\n");
            bufferWritter.close();
            
            boolean sending = false;
            if (flag) 
            {
                //request.setAttribute("downtime_IDs", getIDs(df_full));
                
                // Store the new IDs in the portlet preferences
                if (downtime_IDs.equals("-1")) {                    
                    portletPreferences.setValue("downtime_IDs", getIDs(df_full));
                    sending = true; // enable the notification
                } else {                                        
                    String[] array1 = getIDs(df_full).split(",");
                    String[] array2 = downtime_IDs.trim().split(",");
                    
                    log.info("XML = " + Arrays.toString(array1));
                    log.info("Preferences = " + Arrays.toString(array2));

                    String ids = "";
                    String[] list = getIntersection(array1, array2);                                        
                    for (String id : list) ids += id + ",";                                        
                                        
                    // Check if we need to add new IDs and remove old ones
                    for (int i1=0; i1<array1.length; i1++) {
                        if (!ids.contains(array1[i1])) {
                            ids += array1[i1] + ",";
                            sending = true; // enable the notification
                        }
                    }
                                        
                    List<String> listwithduplicate = 
                            new ArrayList<String>(Arrays.asList(ids));
                     
                     Set<String> s = new LinkedHashSet<String>(listwithduplicate);
                     //log.info("ids(after)= " + s);
                    
                    log.info("Preferences to store = " + ids.substring(0, ids.length()-1));
                    portletPreferences.setValue("downtime_IDs", ids.substring(0, ids.length()-1));
                }
                
                // Get list of users in Liferay
                int countUser = UserLocalServiceUtil.getUsersCount();
                List <User> users = UserLocalServiceUtil.getUsers(0, countUser);
                for (User liferay_user: users) 
                {
                    /*log.info("UserID = " + liferay_user.getUserId() 
                        + " UserCompanyID = " + liferay_user.getCompanyId() 
                        + " UserEmail = " + liferay_user.getEmailAddress() 
                        + " UserScreenName = " + liferay_user.getScreenName());*/
                
                    if (sending) {
                        if ( (SMTP_HOST==null) || 
                             (SMTP_HOST.trim().equals("")) ||
                             (SMTP_HOST.trim().equals("N/A")) ||
                             (SENDER_MAIL==null) || 
                             (SENDER_MAIL.trim().equals("")) ||
                             (SENDER_MAIL.trim().equals("N/A")) )
                        log.info ("\nThe Notification Service is not properly configured!!");
                    
                        else {
                            log.info ("\nSending notification to the user [ OK ]");
                            sendHTMLEmail(                                     
                                liferay_user.getEmailAddress(),                                
                                SENDER_MAIL,
                                SMTP_HOST,
                                df_full);          
                        }
                    }
                }
            } else {
                // Recover original setting.
                portletPreferences.setValue("downtime_IDs", "-1");
            }
            
            // Save the portlet preferences
            request.setAttribute("downtime_REFRESH", downtime_REFRESH.trim());
            request.setAttribute("downtime_LOGLEVEL", downtime_LOGLEVEL.trim());
            request.setAttribute("downtime_endpoints", endpoints);                        
            request.setAttribute("SMTP_HOST", SMTP_HOST.trim());
            request.setAttribute("SENDER_MAIL", SENDER_MAIL.trim());            
            request.setAttribute("DOWNTIME_XML", df_full.toString().trim());
                                    
            // Storing the preferences
            portletPreferences.store();
                            
            dispatcher = getPortletContext().getRequestDispatcher("/view.jsp");       
            dispatcher.include(request, response);                        
                    
        } catch (SystemException ex) {
            Logger.getLogger(Downtime.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyManagementException ex) {
            Logger.getLogger(Downtime.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Downtime.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    public static String[] getIntersection(String[] arr1, String[] arr2)
    {                
        Set<String> s1 = new HashSet<String>(Arrays.asList(arr1));
        Set<String> s2 = new HashSet<String>(Arrays.asList(arr2));
        s1.retainAll(s2);

        String[] result = s1.toArray(new String[s1.size()]);

      return result;
    }
  
     // trust all certificates (don't do this is production)
    public static TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }
        }
    };

    // The init method will be called when installing for the first time the portlet
    // This is the right time to setup the default values into the preferences
    @Override
    public void init() throws PortletException { super.init(); }

    @Override
    public void processAction(ActionRequest request,
                              ActionResponse response)
                throws PortletException, IOException 
    {
      
        String action = "";
            
        // Getting the action to be processed from the request
        action = request.getParameter("ActionEvent");

        PortletPreferences portletPreferences =
            (PortletPreferences) request.getPreferences();

        if (action.equals("CONFIG_DOWNTIME_PORTLET")) {
            log.info("\nPROCESS ACTION => " + action);
                                
            String downtime_REFRESH = request.getParameter("downtime_REFRESH");               
            String downtime_LOGLEVEL = request.getParameter("downtime_LOGLEVEL");                
            String[] downtime_endpoints = request.getParameterValues("downtime_endpoints");                         
            String downtime_IDs = request.getParameter("downtime_IDs");
            String SMTP_HOST = request.getParameter("SMTP_HOST");                
            String SENDER_MAIL = request.getParameter("SENDER_MAIL");                
                
            int nmax=0;                
            for (int i = 0; i < downtime_endpoints.length; i++)
                if ( downtime_endpoints[i]!=null && 
                   (!downtime_endpoints[i].trim().equals("N/A")) )
                   nmax++;
                                    
            String[] downtime_endpoints_trimmed = new String[nmax];
            for (int i = 0; i < nmax; i++)                
                downtime_endpoints_trimmed[i]=downtime_endpoints[i].trim();                    
                
            log.info("\n\nPROCESS ACTION => " + action
                     + "\ndowntime_REFRESH: " + downtime_REFRESH
                     + "\ndowntime_LOGLEVEL: " + downtime_LOGLEVEL                     
                     + "\ndowntime_IDs: " + downtime_IDs
                     + "\nSMTP_HOST: " + SMTP_HOST
                     + "\nSENDER_MAIL: " + SENDER_MAIL);                      
                                
             portletPreferences.setValue("downtime_REFRESH", downtime_REFRESH.trim());
             portletPreferences.setValue("downtime_LOGLEVEL", downtime_LOGLEVEL.trim());                
             portletPreferences.setValues("downtime_endpoints", downtime_endpoints_trimmed);
             portletPreferences.setValue("downtime_IDs", downtime_IDs.trim());
             portletPreferences.setValue("SMTP_HOST", SMTP_HOST.trim());
             portletPreferences.setValue("SENDER_MAIL", SENDER_MAIL.trim());                
                               
             portletPreferences.store();
             response.setPortletMode(PortletMode.VIEW);
        } // end PROCESS ACTION [ CONFIG_DOWNTIME_PORTLET ]
            

        if (action.equals("SUBMIT_DOWNTIME_PORTLET")) {
            log.info("\nPROCESS ACTION => " + action);                
        } // end PROCESS ACTION [ SUBMIT_DOWNTIME_PORTLET ]        
    }
    
    private String getIDs (File file)
    {
        String ids_list = "";

        try {
                //Get Document Builder
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();

                //Build Document
                Document document = builder.parse(file);

                //Normalize the XML Structure; It's just too important !!
                document.getDocumentElement().normalize();

                //Here comes the root node
                Element root = document.getDocumentElement();
                NodeList List = document.getElementsByTagName("DOWNTIME");

                for (int i=0; i<List.getLength(); i++)
                {
                    Node node = List.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE)
                    {
                        Element eElement = (Element) node;
                        ids_list += eElement.getAttribute("ID") + ",";
                    }
                }

        } catch (SAXException ex) {
            Logger.getLogger(Downtime.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Downtime.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Downtime.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        // Strip the last separator
        return (ids_list.substring(0, ids_list.length()-1));
    }

                        
    private void sendHTMLEmail (String TO, 
                                String FROM, 
                                String SMTP_HOST,
                                File file)
    {
                        
        String[] downtime_text = new String[4];        
        
        // Assuming you are sending email from localhost
        String HOST = "localhost";
        
        // Get system properties
        Properties properties = System.getProperties();
        properties.setProperty(SMTP_HOST, HOST);
        
        // Get the default Session object.
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(properties);
        
        try {            
            //Get Document Builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            //Build Document
            Document document = builder.parse(file);
            
            //Normalize the XML Structure; It's just too important !!
            document.getDocumentElement().normalize();
            
            //Here comes the root node
            Element root = document.getDocumentElement();
            NodeList List = document.getElementsByTagName("DOWNTIME");
            
            for (int i=0; i<List.getLength(); i++) 
            {
                Node node = List.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) 
                {                    
                    Element eElement = (Element) node; 
                    
                    // Downtime period
                    downtime_text[0] = "SCHEDULED Downtime period <br/>";
                    downtime_text[0] += "Start of downtime \t[UCT]: "
                            + eElement.getElementsByTagName("FORMATED_START_DATE").item(0).getTextContent() 
                            + "<br/>";
                    downtime_text[0] += "End  of downtime \t[UCT]: "
                            + eElement.getElementsByTagName("FORMATED_END_DATE").item(0).getTextContent() 
                            + "<br/><br/>";                    
                    
                    // Entities in downtime
                    downtime_text[1] = "<b><u>Entities in downtime:</u></b><br/>";
                    downtime_text[1] += "Server Host: "
                            + eElement.getElementsByTagName("HOSTED_BY").item(0).getTextContent() 
                            + "<br/><br/>";
                    for (int k=0; k<eElement.getElementsByTagName("SERVICE").getLength(); k++)      
                    {
                        downtime_text[1] += "Nodes: "
                            + eElement.getElementsByTagName("HOSTNAME").item(k).getTextContent() 
                                + "<br/>";
                        downtime_text[1] += "Service Type: "
                            + eElement.getElementsByTagName("SERVICE_TYPE").item(k).getTextContent() 
                                + "<br/>";
                        downtime_text[1] += "Hosted service(s): "
                                + eElement.getElementsByTagName("HOSTNAME").item(k).getTextContent()
                                + "<br/><br/>";
                    }
                    
                    // Description
                    downtime_text[2] = "<b><u>Description:</u></b><br/>";                    
                    downtime_text[2] += 
                            eElement.getElementsByTagName("DESCRIPTION").item(0).getTextContent() 
                            + "<br/>";
                    downtime_text[2] += 
                            "More details are available in this <a href="
                            + eElement.getElementsByTagName("GOCDB_PORTAL_URL").item(0).getTextContent() 
                            + "> link</a>"
                            + "<br/><br/>";
                    
                    // Severity
                    downtime_text[3] = "<b><u>Severity:</u></b> ";
                    downtime_text[3] += 
                            eElement.getElementsByTagName("SEVERITY").item(0).getTextContent() 
                            + "<br/><br/>";
                    
                    // Sending notification                
                    // Create a default MimeMessage object.
                    javax.mail.internet.MimeMessage message = new javax.mail.internet.MimeMessage(session);

                    // Set From: header field of the header.
                    message.setFrom(new javax.mail.internet.InternetAddress(FROM));

                    // Set To: header field of the header.
                    message.addRecipient(javax.mail.Message.RecipientType.TO, 
                            new javax.mail.internet.InternetAddress(TO));
                    //message.addRecipient(Message.RecipientType.CC, new InternetAddress(FROM));

                    // Set Subject: header field
                    message.setSubject(" [EGI DOWNTIME] ANNOUNCEMENT ");

                    Date currentDate = new Date();
                    currentDate.setTime (currentDate.getTime());
                
                    // Send the actual HTML message, as big as you like
                    message.setContent("<br/><H4>" 
                    + "<img src=\"http://scilla.man.poznan.pl:8080/confluence/download/attachments/5505438/egi_logo.png\" width=\"100\">" 
                    + "</H4>" 
                    + "Long Tail of Science (LToS) services in downtime<br/><br/>"
                    + downtime_text[0]
                    + downtime_text[1]
                    + downtime_text[2]
                    + downtime_text[3]
                    + "<b><u>TimeStamp:</u></b><br/>" + currentDate + "<br/><br/>" 
                    + "<b><u>Disclaimer:</u></b><br/>"
                    + "<i>This is an automatic message sent by the LToS Science Gateway based on Liferay technology."
                    + "<br/><br/>",
                    "text/html");

                    // Send notification to the user
                    javax.mail.Transport.send(message);                    
                }
            }                        
        } catch (MessagingException ex) {
            Logger.getLogger(Downtime.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Downtime.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Downtime.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Downtime.class.getName())
                    .log(Level.SEVERE, null, ex);
        }                
    }      
}