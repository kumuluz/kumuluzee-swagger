package com.kumuluz.ee.swagger.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SwaggerUIFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    public void destroy() {

    }
}