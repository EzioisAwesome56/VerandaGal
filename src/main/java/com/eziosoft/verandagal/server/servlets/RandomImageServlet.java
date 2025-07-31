package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.database.objects.Image;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Random;

public class RandomImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get how many images we have in the database
        long numimgs = VerandaServer.maindb.getCountOfRecords(Image.class);
        // check it didnt fail
        if (numimgs == 0){
            resp.setStatus(500);
            AsyncContext cxt = req.startAsync();
            ServletOutputStream out = resp.getOutputStream();
            out.setWriteListener(new BasicTextWriter("There are no images to select from randomly", cxt, out));
            return;
        }
        // otherwise, do some random number shit
        Random random = new Random();
        long destid = random.nextLong(numimgs) + 1L;
        // redirect to that
        resp.setStatus(302);
        resp.setHeader("Location", "/image/?id=" + destid);
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter("you should not be seeing this!", cxt, out));
        return;
    }
}
