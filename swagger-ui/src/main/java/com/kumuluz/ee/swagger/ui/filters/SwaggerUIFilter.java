package com.kumuluz.ee.swagger.ui.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * SwaggerUIFilter class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
public class SwaggerUIFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        String path = httpServletRequest.getServletPath();

        if (path.contains("/ui")) {
            Pattern pattern = Pattern.compile("^(.*?)(css|js|url)(.*)$");
            String request_query_string = (httpServletRequest.getQueryString() != null) ? httpServletRequest.getRequestURI() +
                    httpServletRequest.getQueryString() : httpServletRequest.getRequestURI();
            if ((pattern.matcher(request_query_string).find()) || httpServletRequest.getRequestURI().contains("oauth2-redirect.html")) {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            } else {
                String url = filterConfig.getInitParameter("url");
                String oauth2RedirectUrl = filterConfig.getInitParameter("oauth2RedirectUrl");
                String servletPath = filterConfig.getInitParameter("servlet");
                httpServletResponse.sendRedirect(servletPath + "/api-specs/ui/?url=" + url + "&" + "oauth2RedirectUrl=" +
                        oauth2RedirectUrl);
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
    }
}