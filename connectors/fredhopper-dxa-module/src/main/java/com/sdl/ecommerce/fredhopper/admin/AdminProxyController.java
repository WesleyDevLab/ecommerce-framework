package com.sdl.ecommerce.fredhopper.admin;


import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

/**
 * Admin Proxy Controller
 *
 * Proxies requests to Fredhopper Business Manager and filter out unnecessary HTML.
 *
 * @author nic
 */
@Controller
@RequestMapping("/fh-edit")
public class AdminProxyController {

    // TODO: Migrate to Http Client 4.5.
    // There is an issue with some AJAX post requests that does not work with 4.5, therefore we use 3.1 for this controller
    //

    private static final Logger LOG = LoggerFactory.getLogger(AdminProxyController.class);

    static final String ADMIN_URL = "/fredhopper/admin";

    private String fredhopperAdminUrl;

    @Value("${fredhopper.adminserver.url}")
    private String fredhopperBaseUrl = "http://localhost:8180";

    @Value("${fredhopper.access.username}")
    private String accessUsername = null;
    @Value("${fredhopper.access.password}")
    private String accessPassword = null;

    @Value("${fredhopper.admin.username}")
    private String username = "admin";
    @Value("${fredhopper.admin.password}")
    private String password = "admin";

    private long sessionTimeout = 1 * 60 * 1000; // TODO: Have configurable
    private long lastAccessTime;

    private HttpClient client;

    @PostConstruct
    public void initialize() throws IOException {

        this.fredhopperAdminUrl = this.fredhopperBaseUrl + ADMIN_URL;
        MultiThreadedHttpConnectionManager connectionManager =
                new MultiThreadedHttpConnectionManager();
        this.client = new HttpClient(connectionManager);

        if ( this.accessUsername != null && !this.accessUsername.isEmpty() ) {
            Credentials credentials = new UsernamePasswordCredentials(this.accessUsername, this.accessPassword);
            client.getState().setCredentials(AuthScope.ANY, credentials);
        }
        this.login();
        this.lastAccessTime = System.currentTimeMillis();
    }

    protected void login() throws IOException {
        GetMethod method = new GetMethod(fredhopperAdminUrl + "/login.fh?username=" + this.username + "&password=" + this.password);
        int statusCode = client.executeMethod(method);
        LOG.debug("Fredhopper admin login status: " + statusCode);
        method.releaseConnection();

    }

    protected void checkSession() throws IOException {
        if ( this.lastAccessTime + this.sessionTimeout < System.currentTimeMillis() ) {
            login();
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/**", produces = {MediaType.ALL_VALUE})
    public void proxyAssets(HttpServletRequest request, HttpServletResponse response) throws IOException {

        this.checkSession();

        final String requestPath = request.getRequestURI().replaceFirst("/fh-edit", "");
        final String fredhopperUrl = fredhopperAdminUrl + requestPath + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        final boolean isAjax = request.getHeader("x-requested-with") != null;
        HttpMethodBase method = new GetMethod(fredhopperUrl);

        Enumeration<String> headerNames = request.getHeaderNames();
        while ( headerNames.hasMoreElements() ) {
            String headerName = headerNames.nextElement();
            if ( headerName.startsWith("wicket") || headerName.startsWith("x-") || headerName.startsWith("accept") || headerName.startsWith("user-agent") ) {
                method.setRequestHeader(headerName, request.getHeader(headerName));
            }
        }

        int statusCode = client.executeMethod(method);

        if ( statusCode == HttpStatus.SC_OK ) {

            Header contentEncoding =  method.getResponseHeader("Content-Encoding");
            if ( requestPath.endsWith(".fh") ) {


                String htmlBody;
                if ( contentEncoding != null && contentEncoding.getValue().equals("gzip") ) {
                    GZIPInputStream zipStream = new GZIPInputStream(method.getResponseBodyAsStream());
                    htmlBody = IOUtils.toString(zipStream);
                }
                else {
                    htmlBody = method.getResponseBodyAsString();
                }

                // Process HTML
                //
                htmlBody = this.processHtml(htmlBody);
                response.setContentType("text/html");
                response.getWriter().write(htmlBody);

            }
            else {
                Header contentType = method.getResponseHeader("Content-Type");
                response.setContentType(contentType.getValue());
                if (  contentEncoding != null && contentEncoding.getValue().equals("gzip") ) {
                    if ( isAjax ) {
                        GZIPInputStream zipStream = new GZIPInputStream(method.getResponseBodyAsStream());
                        String htmlBody = IOUtils.toString(zipStream);
                        htmlBody = htmlBody.replaceAll("\\.jsp", ".fhjsp");
                        response.getWriter().write(htmlBody);
                    }
                    else {
                        IOUtils.copy(new GZIPInputStream(method.getResponseBodyAsStream()), response.getOutputStream());
                    }
                }
                else {
                    IOUtils.copy(method.getResponseBodyAsStream(), response.getOutputStream());
                }
            }
            response.flushBuffer();
        }
        else {
            response.sendError(statusCode);
        }
        synchronized ( this ) {
            this.lastAccessTime = System.currentTimeMillis();
        }
        method.releaseConnection();
    }

    // TODO: Merge this method with the GET method
    @RequestMapping(method = RequestMethod.POST, value = "/**", produces = {MediaType.ALL_VALUE})
    public void postHtml(HttpServletRequest request, HttpServletResponse response) throws IOException {

        this.checkSession();

        final String requestPath = request.getRequestURI().replaceFirst("/fh-edit", "");
        final String fredhopperUrl = fredhopperAdminUrl + requestPath + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        PostMethod method = new PostMethod(fredhopperUrl);

        Enumeration<String> headerNames = request.getHeaderNames();
        while ( headerNames.hasMoreElements() ) {
            String headerName = headerNames.nextElement();
            if ( headerName.startsWith("wicket") || headerName.startsWith("x-") || headerName.startsWith("accept") || headerName.startsWith("user-agent") ) {
                method.setRequestHeader(headerName, request.getHeader(headerName));
            }
        }

        Enumeration<String> paramNames = request.getParameterNames();
        while ( paramNames.hasMoreElements() ) {
            String paramName = paramNames.nextElement();
            method.setParameter(paramName, request.getParameter(paramName));
        }
        int statusCode = client.executeMethod(method);
        if ( statusCode == HttpStatus.SC_OK ) {
            Header contentType = method.getResponseHeader("Content-Type");
            response.setContentType(contentType.getValue());
            Header contentEncoding =  method.getResponseHeader("Content-Encoding");
            if ( contentEncoding != null && contentEncoding.getValue().equals("gzip") ) {
                IOUtils.copy(new GZIPInputStream(method.getResponseBodyAsStream()), response.getOutputStream());
            }
            else {
                IOUtils.copy(method.getResponseBodyAsStream(), response.getOutputStream());
            }
            response.flushBuffer();
        }
        else {
            response.sendError(statusCode);
        }
        method.releaseConnection();
    }


    protected String processHtml(final String html) {

        // First do some global find-replace
        //
        String processedHtml = html.replaceAll("\\.\\./\\.\\./heatmap", "/heatmap");
        processedHtml = processedHtml.replaceAll("if \\(InMethod &&", "if (false && InMethod &&");

        // Then manipulate the HTML (remove divs etc)
        //
        Document htmlDoc = Jsoup.parse(processedHtml);
        this.removeElementsWithId(htmlDoc, "header");
        this.removeElementsWithId(htmlDoc, "lhsLeftContainer");
        this.addStyleToElementsWithId(htmlDoc, "content", "top: 0px;");
        this.addStyleToElementsWithId(htmlDoc, "verticalsplitpanelRightPanel", "margin-left: 0px;");

        htmlDoc.body().append("<script src='/system/assets/scripts/fredhopper-edit-popup.js'></script>");

        return htmlDoc.html();
    }

    protected void removeElementsWithId(Document htmlDoc, String id) {
        Elements elements = htmlDoc.body().select("#" + id);
        for ( Element element : elements )  {
            element.remove();
        }
    }

    protected void addStyleToElementsWithId(Document htmlDoc, String id, String style) {
        Elements elements = htmlDoc.body().select("#" + id);
        for ( Element element : elements )  {
            if ( element.hasAttr("style") ) {
                String currentStyle = element.attr("style");
                currentStyle += ";" + style;
                element.attr("style", currentStyle);
            }
            else {
                element.attr("style", style);
            }
        }
    }

}

