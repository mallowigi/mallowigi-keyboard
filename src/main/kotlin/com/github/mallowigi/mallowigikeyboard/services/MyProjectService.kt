package com.github.mallowigi.mallowigikeyboard.services

import com.intellij.openapi.project.Project
import com.github.mallowigi.mallowigikeyboard.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
