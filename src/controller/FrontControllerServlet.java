package controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;

public class FrontControllerServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                String url = request.getRequestURL().toString();

                out.println(url);
                
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {

                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                String url = request.getRequestURL().toString();

                out.println(url);

        }

}