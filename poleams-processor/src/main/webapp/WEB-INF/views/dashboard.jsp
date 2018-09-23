<%-- 
    Document   : dashboard
    Created on : Aug 14, 2015, 2:12:36 PM
    Author     : Philip A. Chapman
--%>

<%@page import="com.windams.domain.ResourceStatus"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ page contentType="text/html" pageEncoding="UTF-8" session="false"  isELIgnored="false"%>

<%@ page import="com.windams.processor.bean.*" %>
<%@ page import="com.windams.processor.controller.MAVConstants" %>
<%@ page import="com.windams.processor.controller.DateAndTimeFormatter" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>

<%
    String version = (String)request.getAttribute(MAVConstants.MV_VERSION);
%>

<!DOCTYPE html>
<html>
    <%
        Locale locale = request.getLocale();
        List<JobInfo> jobInfoList = (List<JobInfo>)request.getAttribute(MAVConstants.MODEL_JOB_INFO);
     %>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>WindAMS :: Processor</title>
        <style>
            td {
                padding: 15px;
            }
        </style>
        <script type="text/JavaScript">
            // Refreshes the page.  30000 is 30 seconds
            function timeRefresh(timeoutPeriod) 
            {
                    setTimeout("location.reload(true);",timeoutPeriod);
            }
        </script>
    </head>
    <body onload="JavaScript:timeRefresh(30000);">
        <h1>WindAMS Processor Status</h1>
        <table>
            <thead>
                <tr><th>Job</th><th>Last Run Start</th><th>Last Run Finish</th><th>Last Processed Count</th></tr>
            </thead>
            <tbody>
                <%
                    for (JobInfo jinfo : jobInfoList) {
                 %>
                <tr>
                    <td><%= jinfo.getJobName() %></td>
                    <td><%= jinfo.getLastStart() == null ? "" : DateAndTimeFormatter.formatDateTime(jinfo.getLastStart(), DateAndTimeFormatter.Style.Medium, DateAndTimeFormatter.Style.Medium, Locale.US) %></td>
                    <td><%= jinfo.getLastFinish() == null ? "" : DateAndTimeFormatter.formatDateTime(jinfo.getLastFinish(), DateAndTimeFormatter.Style.Medium, DateAndTimeFormatter.Style.Medium, Locale.US) %></td>
                    <td><%= jinfo.getLastProcessedCount() == null ? "" : jinfo.getLastProcessedCount() %></td>
                </tr>
                <%
                    }
                 %>
            </tbody>
        </table>
        <div>Version: <%= version %></div>
    </body>
</html>
