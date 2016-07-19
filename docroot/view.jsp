<%
/**************************************************************************
Copyright (c) 2011-2016:
Istituto Nazionale di Fisica Nucleare (INFN), Italy
    
See http://www.infn.it and details on the copyright holders.
    
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
**************************************************************************/
%>
<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="javax.portlet.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="javax.xml.parsers.DocumentBuilderFactory,javax.xml.parsers.DocumentBuilder,org.w3c.dom.*" %>
<%@ page import="com.liferay.portal.kernel.portlet.LiferayWindowState"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<portlet:defineObjects/>

<style type="text/css">
    
    @media screen {
        #view tbody td span { display: none;}
    }
    @media print {
        #view tbody td img { display: none;}
        #view tbody td span { display: block;}
    }
    
    td.details-control {
    background: url('https://www.datatables.net/examples/resources/details_open.png') no-repeat center center;
    cursor: pointer;
    }
    
    tr.shown td.details-control {
    background: url('https://www.datatables.net/examples/resources/details_close.png') no-repeat center center;
    }
    
    .hidden { display: none; }
    
    .fieldset-auto-width { 
        display: inline-block; height: auto;         
    }
    
    .td {font-size: 10pt !important; }
</style>


<jsp:useBean id="downtime_REFRESH" class="java.lang.String" scope="request"/>
<jsp:useBean id="downtime_endpoints" class="java.lang.String" scope="request"/>
<jsp:useBean id="downtime_IDs" class="java.lang.String" scope="request"/>
<jsp:useBean id="downtime_SOFTWARE" class="java.lang.String" scope="request"/>
<jsp:useBean id="TRACKING_DB_HOSTNAME" class="java.lang.String" scope="request"/>
<jsp:useBean id="TRACKING_DB_USERNAME" class="java.lang.String" scope="request"/>
<jsp:useBean id="TRACKING_DB_PASSWORD" class="java.lang.String" scope="request"/>
<jsp:useBean id="SMTP_HOST" class="java.lang.String" scope="request"/>
<jsp:useBean id="SENDER_MAIL" class="java.lang.String" scope="request"/>
<jsp:useBean id="DOWNTIME_XML" class="java.lang.String" scope="request"/>
<jsp:useBean id="LOCAL_ACCOUNT" class="java.lang.String" scope="request"/>

<script type="text/javascript">
    
    //setTimeout("location.reload(true);",100000);           
    setTimeout(function(){
        $.post('<portlet:renderURL windowState="<%=LiferayWindowState.EXCLUSIVE.toString()%>"></portlet:renderURL>', 
        function(data){
            $("#portlet").html(data);
        })
    }, <%= downtime_REFRESH %>);
    
    function format(text)
    {
        var sOut = '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:50px;">';
        sOut += "Affected Services:";
        sOut += '<tr><td>'+text+'</td></tr>';
        sOut += '</table>';                
        return sOut;
    }
        
    $(document).ready(function() 
    {
        //console.log(jQuery.fn.jquery);
        
        <%
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();            
            Document document = builder.parse(DOWNTIME_XML);
            
            //Normalize the XML Structure; It's just too important !!
            document.getDocumentElement().normalize();
            
            //Here comes the root node
            Element root = document.getDocumentElement();

            // Get all the donwtimes                       
            NodeList DOWNTIME = document.getElementsByTagName("DOWNTIME");            
            NodeList SERVICE = document.getElementsByTagName("SERVICE");            
                        
            NodeList DESCRIPTION = document.getElementsByTagName("DESCRIPTION");
            NodeList HOSTED_BY = document.getElementsByTagName("HOSTED_BY");
            NodeList FORMATED_START_DATE = document.getElementsByTagName("FORMATED_START_DATE");
            NodeList FORMATED_END_DATE = document.getElementsByTagName("FORMATED_END_DATE");
            NodeList GOCDB_URL = document.getElementsByTagName("GOCDB_PORTAL_URL");            
            NodeList SEVERITY = document.getElementsByTagName("SEVERITY");             
        %>               
               
        var dataTableSettings = {
          "aaSorting": [[ 0, "asc" ]],
          "sDom": '<"H"<"clear"T>f<"clear"l><"right"p>t<"F"ip>',
          "bRetrieve": true,
          "bJQueryUI": true,
          "iDisplayLength": 5,                    
          "bautoWidth":false,
          "columnDefs": [
                { "width": "5%", "targets": 0 },
                { "width": "10%", "targets": 1 },
                { "width": "10%", "targets": 2 },
                { "width": "20%", "targets": 3 },
                { "width": "10%", "targets": 4 },
                { "width": "10%", "targets": 5 }
           ],
          
          "oTableTools": {
	    "sSwfPath": "<%=renderRequest.getContextPath()%>/js/datatables/media/swf/copy_cvs_xls_pdf.swf",
            "aButtons": ["copy", "print", {
            "sExtends":    "collection",
            "sButtonText": "Save",
            "aButtons":    [ "csv", "pdf" ]
            }
            ]
          }            
        };                
        
       var table = $("#chart").DataTable(dataTableSettings);              
       $('#chart').on('click', 'td.details-control', function ()       
       {
            var nTr = $(this).parents('tr')[0];
	    $(this).parent('tr').toggleClass('shown');
            if (table.fnIsOpen(nTr))
            { 
                table.fnClose(nTr);
            } else {                
                var text = $("[name='services']",$(this).parent()).html();                
                table.fnOpen(nTr, format(text), 'details');
            }
       });              
    });        
</script>

<br/>
<div style="font-family: Tahoma,Verdana,sans-serif,Arial; font-size: 14px;" 
     id="portlet">Last update on: <%= new Date() %>    
<table id="chart" class="display" cellspacing="0">
<thead>
<tr>    
    <th></th>
    <th>ID / Severity</th>
    <th>Host</th>
    <th>Description</th>
    <th>Start</th>          
    <th>End</th>
</tr>
</thead>

<tfoot>
<tr>    
    <th></th>
    <th>ID / Severity</th>
    <th>Host</th>    
    <th>Description</th>
    <th>Start</th>          
    <th>End</th>
</tr>
</tfoot>    
<tbody>
    
<%
int i;
for(i=0; i<=DOWNTIME.getLength()-1; i++)
{
    Node node = DOWNTIME.item(i);
    Element eElement = (Element) node;
%>
<tr>
   
<td class="details-control"></td>
<td>
 <a href="<%= eElement.getElementsByTagName("GOCDB_PORTAL_URL").item(0).getTextContent() %>">
   [ <%= eElement.getAttribute("ID") %> ]</a><br/>
    <%= eElement.getElementsByTagName("SEVERITY").item(0).getTextContent() %>
   <div name="services" class="hidden"> 
	<ul>
        <%
            int k;
            for(k=0; k<=eElement.getElementsByTagName("SERVICE").getLength()-1; k++)
            {	    
        %>
        <li>
        <%=eElement.getElementsByTagName("SERVICE_TYPE").item(k).getTextContent()%>,
        <%=eElement.getElementsByTagName("HOSTNAME").item(k).getTextContent()%>
        </li>
        <% } %>
        </ul>
   </div>
</td>

<td><%= eElement.getElementsByTagName("HOSTED_BY").item(0).getTextContent() %></td>

<td><%= eElement.getElementsByTagName("DESCRIPTION").item(0).getTextContent() %></td>
<td><%= eElement.getElementsByTagName("FORMATED_START_DATE").item(0).getTextContent() %></td>
<td><%= eElement.getElementsByTagName("FORMATED_END_DATE").item(0).getTextContent() %></td>
</tr>

<% } %>

</tbody>
</table>
</div>
