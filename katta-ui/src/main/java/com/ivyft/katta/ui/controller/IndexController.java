package com.ivyft.katta.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ivyft.katta.ui.annaotion.Action;
import com.ivyft.katta.ui.annaotion.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 *
 * Created by zhenqin.
 * User: zhenqin
 * Date: 17/12/13
 * Time: 18:02
 * Vendor: yiidata.com
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
@Action
@Singleton
public class IndexController {



    @Inject
    public IndexController() {
    }

    @Path("/")
    public String rootIndex(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) {
        return index(params, request, response);
    }


    @Path("/home")
    public String home(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) {
        return index(params, request, response);
    }


    @Path("/home.html")
    public String homeHtml(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) {
        return index(params, request, response);
    }


    @Path("/index.html")
    public String index(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", "asdfasdfad");
        map.put("name", "asdfasdfad");
        map.put("nodeCount", 10);
        map.put("indexCount", 10);
        map.put("uri", "asdfasd");

        params.put("title", "Katta Overview");
        params.put("cluster", map);

        return "index.ftl";
    }
}
