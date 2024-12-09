package com.eziosoft.verandagal.server.servlets;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // this servlet returns html, so make set its content type to that
        resp.setContentType("text/html");
        // pretty much just display the main page. its a simple process
        StringBuilder b = new StringBuilder();
        try {
            b.append(VerandaServer.template.getTemplate("header"));
            b.append(VerandaServer.sidebarBuilder.getSideBar());
            String what = VerandaServer.template.getTemplate("stockmain");
            String main = VerandaServer.template.getTemplate("mainpage");
            // if the option to show the stock main is turned on, patch it out with the stock content
            if (VerandaServer.configFile.isShowStockMain()){
                main = main.replace("${STOCK}", what);
            } else {
                main = main.replace("${STOCK}", "");
            }
            if (VerandaServer.configFile.isShowUserMain()){
                String user = FileUtils.readFileToString(new File("usermain.html"), StandardCharsets.UTF_8);
                main = main.replace("${USRCONTENT}", user);
            } else {
                // just blank it out if the user doesnt want any custom content
                main = main.replace("${USRCONTENT}", "");
            }
            b.append(main);
            b.append(VerandaServer.template.getTemplate("footer"));
        } catch (IOException e){
            VerandaServer.LOGGER.error("IO Error while trying to get template", e);
            b.append("Failed");
        }
        // async write to output
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(b.toString(), cxt, out));
    }
}
