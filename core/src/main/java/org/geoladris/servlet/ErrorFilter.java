package org.geoladris.servlet;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

public class ErrorFilter implements Filter {

  private static Logger logger = Logger.getLogger(ErrorFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      chain.doFilter(request, response);
    } catch (Throwable e) {

      String errorMsg = "Server error: ";
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      int status;
      if (e instanceof StatusServletException) {
        status = ((StatusServletException) e).getStatus();
      } else {
        status = 500;
      }
      httpResponse.setStatus(status);
      if (status == 500) {
        logger.error("Error handling request", e);
      } else {
        logger.error("Error handling request: " + e.getMessage());
      }
      while (e != null) {
        errorMsg += e.getMessage() + ". ";
        e = e.getCause();
      }
      response.setContentType("application/json");
      response.setCharacterEncoding("utf8");
      if (errorMsg != null) {
        HashMap<String, String> doc = new HashMap<String, String>();
        doc.put("message", errorMsg);
        response.getOutputStream().write(JSONSerializer.toJSON(doc).toString(0).getBytes());
      }
    }
  }

  @Override
  public void destroy() {}

}
