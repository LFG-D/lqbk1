package cn.edu.guet.mvc;

import com.google.gson.GsonBuilder;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {
    Map<String, ControllerMapping> controllerMapping;

    @Override
    public void init(ServletConfig config) throws ServletException {
        controllerMapping = (Map<String, ControllerMapping>) config.getServletContext().getAttribute("cn.guet.web.controller");
        System.out.println("////" + controllerMapping);

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            ControllerMapping mapping = null;
            String uri = request.getRequestURI();
            uri = uri.substring(uri.indexOf("/", 1) + 1);
            System.out.println("真实的客户请求：" + uri);
            if (controllerMapping.containsKey(uri)) {
                mapping = controllerMapping.get(uri);
            }
            Class controllerMappingClass = mapping.getControllerClass();
            Method method = mapping.getHandleMethod();
            Class[] parameterType = method.getParameterTypes();//参数
        /*
        反射获取参数的名称
         */
            List<String> paramterList = new ArrayList<String>();
            Parameter[] params = method.getParameters();
            for (Parameter parameter : params) {
                System.out.println("参数名字：" + parameter.getName());
                paramterList.add(parameter.getName());
            }
            Object[] parameterValues = new Object[parameterType.length];
            for (int i = 0; i < parameterType.length; i++) {
                if (parameterType[i].isPrimitive()) {
                    if (parameterType[i].getTypeName().equals("int")) {
                        parameterValues[i] = Integer.parseInt(request.getParameter(paramterList.get(i)));
                    } else if (parameterType[i].getTypeName().equals("float")) {
                        parameterValues[i] = Float.parseFloat(request.getParameter(paramterList.get(i)));
                    }
                } else if (ClassUtils.isAssignable(parameterType[i], String.class)) {
                    parameterValues[i] = request.getParameter(paramterList.get(i));
                } else {
                    //Bean
                    Object pojo = parameterType[i].newInstance();
                    //得到请求里所有的参数：Map<参数名, value>
                    //获取表单里的数据
                    Map<String, String[]> parameterMap = request.getParameterMap();
                    //beanutils会自动将map里的key与bean的属性名进行反射赋值
                    BeanUtils.populate(pojo, parameterMap);
                    parameterValues[i] = pojo;
                }
            }
            System.out.println("长度" + parameterType.length);
            Object obj = controllerMappingClass.newInstance();
            Object returnValue = method.invoke(obj, parameterValues);
            if (returnValue != null && returnValue instanceof String) { //方法返回的是一个字符串类
                String path = returnValue.toString();
                if (((String) returnValue).startsWith("forward:")) {
                    request.getRequestDispatcher(StringUtils.substringAfter(path, "forward:")).forward(request, response);
                }else if(((String) returnValue).startsWith("redirect:")){
                    response.sendRedirect(StringUtils.substringAfter(path, "redirect:"));
                }
            }
            else if (returnValue != null && !(returnValue instanceof String)) {
                response.setContentType("application/json; charset=UTF-8");
                //返回的是一个bean，即客户端发送的是ajax请求，并将该bean转换成json
                String json = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd HH:mm:ss")
                        .setPrettyPrinting()
                        .create()
                        .toJson(returnValue);
                PrintWriter out = response.getWriter();
                out.write(json);
                out.flush();
                out.close();
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }
}
