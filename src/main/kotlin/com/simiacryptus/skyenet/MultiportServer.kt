package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import java.awt.Desktop
import java.net.URI

object MultiportServer {

    @JvmStatic
    fun main(args: Array<String>) {
        OpenAIClient.keyTxt = AwsAgent.decrypt("openai.key.kms")
        val isServer = args.contains("--server")
        val localName = "localhost"
        val httpServer = listOf(
            AwsAgent.start(if (isServer) "https://awsagent.simiacrypt.us" else "http://$localName:8081", 8081),
            StoryGenerator(
                applicationName = "StoryGenerator",
                baseURL = if (isServer) "https://stories.simiacrypt.us" else "http://$localName:8082",
                //oauthConfig = oauthConfig.absolutePath
            ).start(8082),
            CookbookGenerator(
                applicationName = "CookbookGenerator",
                baseURL = if (isServer) "https://cookbooks.simiacrypt.us" else "http://$localName:8083",
                //oauthConfig = oauthConfig.absolutePath
            ).start(8083),
            SkyenetScienceBook(
                applicationName = "ScienceBook",
                baseURL = if (isServer) "https://science.simiacrypt.us" else "http://$localName:8084",
                //oauthConfig = oauthConfig.absolutePath
            ).start(8084)
        )
        if(!isServer) try {
            Desktop.getDesktop().browse(URI("http://$localName:8081"))
            Desktop.getDesktop().browse(URI("http://$localName:8082"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        httpServer.forEach { it.join() }
    }

}