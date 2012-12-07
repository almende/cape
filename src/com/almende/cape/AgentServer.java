package com.almende.cape;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ErrorHandler;
import org.mortbay.jetty.webapp.WebAppContext;

public class AgentServer {
    public static void main(String[] args) throws Exception
    {
    	System.out.println("Starting!");
        Server server = new Server(8080);
        
        WebAppContext wah = new WebAppContext();
        ErrorHandler eh = wah.getErrorHandler();
        eh.setShowStacks(true);
        wah.setErrorHandler(eh);

        wah.setParentLoaderPriority(true);
        
        wah.setWar("WebContent/");

        server.setHandler(wah);
        
        server.start();
        server.join();
    }
}
