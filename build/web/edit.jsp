<%
/**************************************************************************
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
****************************************************************************/
%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="javax.portlet.*"%>
<%@page import="java.util.Arrays"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<portlet:defineObjects/>

<jsp:useBean id="downtime_REFRESH" class="java.lang.String" scope="request"/>
<jsp:useBean id="downtime_LOGLEVEL" class="java.lang.String" scope="request"/>
<jsp:useBean id="downtime_endpoints" class="java.lang.String[]" scope="request"/>
<jsp:useBean id="downtime_IDs" class="java.lang.String" scope="request"/>
<jsp:useBean id="SMTP_HOST" class="java.lang.String" scope="request"/>
<jsp:useBean id="SENDER_MAIL" class="java.lang.String" scope="request"/>

<script type="text/javascript">
    
    $(document).ready(function() {
        
        var endpoints_inputs = 1;        
        // ADDING a new enpoint for the LATO (MAX. 15)
        $('#adding_endpoints').click(function() {        
            ++endpoints_inputs;        
            if (endpoints_inputs>1 && endpoints_inputs<16) {
            var c = $('.cloned_downtime_endpoints:first').clone(true);            
            c.children(':text').attr('name','downtime_endpoints' );
            c.children(':text').attr('id','downtime_endpoints' );
            $('.cloned_downtime_endpoints:last').after(c);
        }        
        });
        
        // REMOVING enpoin
        $('.btnDel_endpoints').click(function() {
        if (endpoints_inputs > 1)
            if (confirm('Do you really want to delete the item ?')) {
            --endpoints_inputs;
            $(this).closest('.cloned_downtime_endpoints').remove();
            $('.btnDel_endpoints').attr('disabled',($('.cloned_downtime_endpoints').length  < 2));
        }
        });
        
        $('.btnDel_endpoints2').click(function() {            
            if (confirm('Do you really want to delete the item ?')) {
            --endpoints_inputs;
            $(this).closest('.cloned_cached_latoWMS').remove();
            $('.btnDel_endpoints2').attr('disabled',($('.cloned_cached_latoWMS').length  < 2));
        }
        });
        
        // Validate input form
        $('#DowntimeEditForm').validate({
            rules: {
                downtime_REFRESH: {
                    required: true              
                },                
                downtime_endpoints: {
                    required: true
                },                
                downtime_LOGLEVEL: {
                    required: true              
                },
                downtime_ACCOUNT_FILE: {
                    required: true              
                }
            },
            
            invalidHandler: function(form, validator) {
                var errors = validator.numberOfInvalids();
                if (errors) {
                    $("#error_message").empty();
                    var message = errors == 1
                    ? ' You missed 1 field. It has been highlighted'
                    : ' You missed ' + errors + ' fields. They have been highlighted';                    
                    $('#error_message').append("<img width='30' src='<%=renderRequest.getContextPath()%>/images/Warning.png' border='0'>"+message);
                    $("#error_message").show();
                } else $("#error_message").hide();                
            },
            
            submitHandler: function(form) {
                   form.submit();
            }
        });
        
        $("#DowntimeEditForm").bind('submit', function () {            
       });                
    });    
            
</script>

<br/>
<p style="width:690px; font-family: Tahoma,Verdana,sans-serif,Arial; font-size: 14px;">
    Please, select the preference(s) of this portlet</p>  

<!DOCTYPE html>
<form id="DowntimeEditForm"
      name="DowntimeEditForm"
      action="<portlet:actionURL><portlet:param name="ActionEvent" value="CONFIG_DOWNTIME_PORTLET"/></portlet:actionURL>" 
      method="POST">

<fieldset>
<legend>Portlet Settings</legend>
<div style="margin-left:15px; font-family: Tahoma,Verdana,sans-serif,Arial; font-size: 14px;" id="error_message"></div>
<br/>
<table id="results" border="0" width="620" style="width:690px; font-family: Tahoma,Verdana,sans-serif,Arial; font-size: 14px;">

<tr></tr>

<!-- LAST -->
<tr></tr>
<tr>    
    <td width="150">
    <img width="30" 
         align="absmiddle"
         src="<%= renderRequest.getContextPath()%>/images/question.png"  
         border="0" title="Time to refresh (in days)" />
   
        <label for="downtime_REFRESH">TTR<em>*</em></label> 
    </td>
    <td>
        <input type="text" 
               id="downtime_REFRESH"
               name="downtime_REFRESH"
               class="textfield ui-widget ui-widget-content ui-state-focus required"
               size="10px;" 
               value=" <%= downtime_REFRESH %>" />    
    </td>    
</tr>

<tr>
    <td width="150">
    <img width="30" 
         align="absmiddle"
         src="<%= renderRequest.getContextPath()%>/images/question.png"  
         border="0" title="GOCDB IDs list" />

        <label for="downtime_IDs">IDs</label>
    </td>
    <td>
        <input type="text" 
               id="downtime_IDs"
               name="downtime_IDs"
               class="textfield ui-widget ui-widget-content ui-state-focus"
               size="50px;" 
               value=" <%= downtime_IDs %>" />
    </td>
</tr>


<tr>    
    <td width="150">
    <img width="30" 
         align="absmiddle"
         src="<%= renderRequest.getContextPath()%>/images/question.png"  
         border="0" title="The Log Level of the portlet (E.g.: VERBOSE, INFO)" />
   
        <label for="downtime_LOGLEVEL">Log Level<em>*</em></label> 
    </td>
    <td>
        <input type="text" 
               id="downtime_LOGLEVEL"
               name="downtime_LOGLEVEL"
               class="textfield ui-widget ui-widget-content ui-state-focus required"
               size="50px;" 
               value=" <%= downtime_LOGLEVEL %>" />    
    </td>    
</tr>

<tr>    
    <td width="150">
    <img width="30" 
         align="absmiddle"         
         src="<%= renderRequest.getContextPath()%>/images/question.png"  
         border="0" title="The GOC-DB endpoints to query" />
   
        <label for="downtime_endpoints">Endpoint<em>*</em></label>
    </td>
    <td>          
        <c:forEach var="wms" items="<%= downtime_endpoints %>">
            <c:if test="${(!empty wms && wms!='N/A')}">
            <div style="margin-bottom:4px;" class="cloned_cached_latoWMS">
            <input type="text"                
                   name="downtime_endpoints"
                   class="textfield ui-widget ui-widget-content ui-state-focus required"
                   size="50px;"               
                   value=" <c:out value="${wms}"/>" />
            <img type="button" class="btnDel_endpoints2" width="18"
                 src="<%= renderRequest.getContextPath()%>/images/remove.png" 
                 border="0" title="Remove the endopoint" />
            </div>
            </c:if>
        </c:forEach>        
        
        <div style="margin-bottom:4px;" class="cloned_downtime_endpoints">
        <input type="text"                
               name="downtime_endpoints"
               class="textfield ui-widget ui-widget-content ui-state-focus required"
               size="50px;"               
               value=" N/A"/>
        <img type="button" id="adding_endpoints" width="18"
             src="<%= renderRequest.getContextPath()%>/images/plus_orange.png" 
             border="0" title="Add a new endopoint" />
        <img type="button" class="btnDel_endpoints" width="18"
             src="<%= renderRequest.getContextPath()%>/images/remove.png" 
             border="0" title="Remove the endopoint" />
        </div>
    </td>    
</tr>

<tr>    
    <td width="150">
    <img width="30" 
         align="absmiddle"
         src="<%= renderRequest.getContextPath()%>/images/question.png"  
         border="0" title="The SMTP Server for sending notification" />
   
        <label for="SMTP_HOST">SMTP</label>
    </td>
    <td>
        <input type="text" 
               id="SMTP_HOST"
               name="SMTP_HOST"
               class="textfield ui-widget ui-widget-content ui-state-focus"
               size="50px;" 
               value=" <%= SMTP_HOST %>" />    
    </td>    
</tr>

<tr>    
    <td width="150">
    <img width="30" 
         align="absmiddle"
         src="<%= renderRequest.getContextPath()%>/images/question.png"  
         border="0" title="The email address for sending notification" />
   
        <label for="Sender">Sender</label>
    </td>
    <td>
        <input type="text" 
               id="SENDER_MAIL"
               name="SENDER_MAIL"
               class="textfield ui-widget ui-widget-content ui-state-focus"
               size="50px;" 
               value=" <%= SENDER_MAIL %>" />
    </td>    
</tr>

<!-- Buttons -->
<tr>            
    <tr><td>&nbsp;</td></tr>
    <td align="left">    
    <input type="image" src="<%= renderRequest.getContextPath()%>/images/save.png"
           width="50"
           name="Submit" title="Save the portlet settings" />        
    </td>
</tr>  

</table>
<br/>
<div id="pageNavPosition" style="width:690px; font-family: Tahoma,Verdana,sans-serif,Arial; font-size: 14px;">   
</div>
</fieldset>
           
<!--script type="text/javascript">
    var pager = new Pager('results', 14); 
    pager.init(); 
    pager.showPageNav('pager', 'pageNavPosition'); 
    pager.showPage(1);
</script-->
</form>