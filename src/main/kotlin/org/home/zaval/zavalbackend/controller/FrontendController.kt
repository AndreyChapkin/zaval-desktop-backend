package org.home.zaval.zavalbackend.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class FrontendController {
    @RequestMapping("/todo/**")
    fun home(model: Model?): String {
        return "/index.html"
    }

    @RequestMapping("/article/**")
    fun homeToo(model: Model?): String {
        return "/index.html"
    }
}