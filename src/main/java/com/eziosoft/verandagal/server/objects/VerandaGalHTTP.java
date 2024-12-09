package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.server.VerandaServer;
import jakarta.servlet.http.HttpServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class VerandaGalHTTP {

    private final Server server;
    private final ServletContextHandler servlets;
    private final ServerConnector connector;
    private final QueuedThreadPool pool;
    private final DefaultSessionIdManager sessionManager;
    private final SessionHandler sessionHandler;

    public VerandaGalHTTP(int port, int max_threads) throws Exception {
        // run thru the process of creating a server
        this.pool = new QueuedThreadPool(max_threads);
        this.server = new Server(this.pool);
        this.connector = new ServerConnector(this.server);
        this.connector.setPort(port);
        this.server.addConnector(this.connector);
        this.servlets = new ServletContextHandler();
        // more session setup stuff
        this.sessionHandler = new SessionHandler();
        this.sessionHandler.setSameSite(HttpCookie.SameSite.STRICT);
        this.servlets.setSessionHandler(this.sessionHandler);
        // todo: deal with CSRF attacks later
        this.server.setHandler(this.servlets);
        // setup the session manager
        this.sessionManager = new DefaultSessionIdManager(this.server);
        this.sessionManager.setWorkerName("VerandaGal");
        this.server.addBean(this.sessionManager);
    }

    public void start() throws Exception{
        VerandaServer.LOGGER.debug("Now starting HTTP Server...");
        this.server.start();
    }

    public void addAsyncServlet(Class<? extends HttpServlet> servlet, String endpoint){
        ServletHolder hold = this.servlets.addServlet(servlet, endpoint);
        // turn on async mode
        hold.setAsyncSupported(true);
    }
    public void addBlockingServlet(Class<? extends HttpServlet> servlet, String endpoint){
        // just add it, we dont need to change anything
        this.servlets.addServlet(servlet, endpoint);
    }
}
