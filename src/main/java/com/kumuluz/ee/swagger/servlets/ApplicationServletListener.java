package com.kumuluz.ee.swagger.servlets;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Logger;

/**
 * Created by zvoneg on 27/09/2017.
 */
public class ApplicationServletListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(ApplicationServletListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext context = servletContextEvent.getServletContext();

        LOG.info(context.getInitParameter("javax.ws.rs.Application"));
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
