package org.home.zaval.zavalbackend.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class StaticController {
    @RequestMapping("/todo/**")
    fun home(model: Model?): String {
        println("In StaticController!!!")
        return "/200.html"
    }
}