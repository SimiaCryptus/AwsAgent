package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.skyenet.body.ChatSessionFlexmark
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import com.simiacryptus.util.JsonUtil
import java.awt.Desktop
import java.net.URI

class SoftwareProjectGenerator(
    applicationName: String,
    baseURL: String,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : SkyenetMacroChat(
    applicationName = applicationName,
    baseURL = baseURL,
    temperature = temperature,
    oauthConfig = oauthConfig,
) {
    interface ProjectAPI {

        data class ProjectParameters(
            val title: String = "",
            val description: String = "",
            val programmingLanguage: String = "",
            val requirements: List<String> = listOf(),
        )

        fun parseProject(projectDescription: String): ProjectParameters

        fun expandProject(project: ProjectParameters): FileSpecList

        data class FileSpecList(
            val items: List<FileSpec>
        )

        data class FileSpec(
            val filepath: String = "",
            val requirements: List<String> = listOf(),
        )

        fun implementFile(file: FileSpec): FileImpl
        fun modify(
            projectParameters: ProjectParameters,
            userInput: String
        ): ProjectParameters

        data class FileImpl(
            val filepath: String = "",
            val language: String = "",
            val text: String = "",
        )


    }

    val projectAPI = ChatProxy(
        clazz = ProjectAPI::class.java,
        api = api,
        model = OpenAIClient.Models.GPT4,
        temperature = temperature
    ).create()

    override fun processMessage(
        sessionId: String,
        userMessage: String,
        sessionUI: SessionUI,
        sendUpdate: (String, Boolean) -> Unit
    ) {
        try {
            sendUpdate("""<div>${ChatSessionFlexmark.renderMarkdown(userMessage)}</div>""", true)
            val projectParameters = projectAPI.parseProject(userMessage)
            reviewProject(sendUpdate, projectParameters, sessionUI, sessionId)
            //sendUpdate("<hr/><div><em>${projectParameters.title}</em></div>", true)
            //handleProject(projectParameters, sendUpdate, sessionUI, sessionId)
        } catch (e: Throwable) {
            logger.warn("Error", e)
        }
    }

    fun handleProject(
        projectParameters: ProjectAPI.ProjectParameters,
        sendUpdate: (String, Boolean) -> Unit,
        sessionUI: SessionUI,
        sessionId: String
    ) {
        val fileSpecList = projectAPI.expandProject(projectParameters)

        fileSpecList.items.forEach { fileSpec ->
            sendUpdate(
                """<div>${
                    sessionUI.hrefLink {
                        sendUpdate("<hr/><div><em>${fileSpec.filepath}</em></div>", true)
                        val fileImpl = projectAPI.implementFile(fileSpec)
                        // HtmlEncode fileImpl.text
                        val fileImplText = fileImpl.text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        sendUpdate("<pre>${fileImplText}</pre>", false)
                        val toFile =
                            sessionDataStorage.getSessionDir(sessionId).toPath().resolve(fileSpec.filepath).toFile()
                        toFile.parentFile.mkdirs()
                        toFile.writeText(fileImplText)
                    }
                }${fileSpec.filepath}</a></div>""", false)
        }
    }

    fun reviewProject(
        sendUpdate: (String, Boolean) -> Unit,
        projectParameters: ProjectAPI.ProjectParameters,
        sessionUI: SessionUI,
        sessionId: String
    ) {
        sendUpdate("""
            <pre>${JsonUtil.toJson(projectParameters)}</pre>
            <br/>
            ${sessionUI.textInput { userInput ->
                sendUpdate("", true)
                reviewProject(sendUpdate, projectAPI.modify(projectParameters, userInput), sessionUI, sessionId)
            } }
            <br/>
            ${sessionUI.hrefLink {
                sendUpdate("", true)
                handleProject(projectParameters, sendUpdate, sessionUI, sessionId)
            } }Execute</a>
            """, true)
    }

    companion object {

        const val port = 8081
        const val baseURL = "http://localhost:$port"

        @JvmStatic
        fun main(args: Array<String>) {
            val httpServer = SoftwareProjectGenerator("SoftwareProjectGenerator", baseURL).start(port)
            Desktop.getDesktop().browse(URI(baseURL))
            httpServer.join()
        }
    }
}