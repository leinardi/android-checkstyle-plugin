package com.leinardi.android.checkstyle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.FeaturePlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.tasks.StopExecutionException
/*
    Copyright 2017 Roberto Leinardi

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/**
 * A plugin that lets you use the checkstyle plugin for Android projects.
 */
class AndroidCheckstylePlugin implements Plugin<Project> {

    public static final String TASK_GROUP_NAME = "Verification"

    @Override
    void apply(Project project) {
        project.plugins.with {
            apply CheckstylePlugin
        }

        project.afterEvaluate { Project afterProject ->
            def dependsOn = []
            def classpath
            def source


            def variants
            if (hasPlugin(afterProject, AppPlugin) || hasPlugin(afterProject, TestPlugin)) {
                variants = afterProject.android.applicationVariants
            } else if (hasPlugin(afterProject, LibraryPlugin)) {
                variants = afterProject.android.libraryVariants
            } else if (hasPlugin(afterProject, FeaturePlugin)) {
                variants = afterProject.android.featureVariants
            } else {
                throw new StopExecutionException("Must be applied with 'android' or 'android-library' plugin.")
            }

            variants.all { variant ->
                dependsOn << variant.javaCompiler
                if (!source) {
                    source = variant.javaCompiler.source.filter { p ->
                        return p.getPath().contains("${afterProject.projectDir}/src/main/")
                    }
                }
                source += variant.javaCompiler.source.filter { p ->
                    return !p.getPath().contains("${afterProject.projectDir}/src/main/")
                }
                if (!classpath) {
                    classpath = afterProject.fileTree(variant.javaCompiler.destinationDir)
                } else {
                    classpath += afterProject.fileTree(variant.javaCompiler.destinationDir)
                }

                def name = variant.name
                def checkstyleVariant = project.tasks.create "${Checkstyle.simpleName.toLowerCase()}${name.capitalize()}", Checkstyle
                checkstyleVariant.group = AndroidCheckstylePlugin.TASK_GROUP_NAME
                checkstyleVariant.dependsOn variant.javaCompile
                checkstyleVariant.source variant.javaCompile.source
                checkstyleVariant.classpath = project.fileTree(variant.javaCompile.destinationDir)
                checkstyleVariant.exclude('**/BuildConfig.java')
                checkstyleVariant.exclude('**/R.java')
                checkstyleVariant.showViolations true
            }

            def checkstyle = afterProject.tasks.create "${Checkstyle.simpleName.toLowerCase()}", Checkstyle
            checkstyle.group = AndroidCheckstylePlugin.TASK_GROUP_NAME
            checkstyle.dependsOn dependsOn
            checkstyle.source source
            checkstyle.classpath = classpath
            checkstyle.exclude('**/BuildConfig.java')
            checkstyle.exclude('**/R.java')
            checkstyle.exclude('**/BR.java')
            checkstyle.showViolations true
            afterProject.tasks.getByName("check").dependsOn checkstyle
        }
    }

    static def hasPlugin(Project project, Class<? extends Plugin> plugin) {
        return project.plugins.hasPlugin(plugin)
    }

}