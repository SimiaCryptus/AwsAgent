package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.AwsAgent.AwsSkyenetCodingSessionServer
import com.simiacryptus.skyenet.body.AuthenticatedWebsite
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import java.awt.Desktop
import java.net.URI

object MultipathServer {

    @JvmStatic
    fun main(args: Array<String>) {
        OpenAIClient.keyTxt = AwsAgent.decrypt("openai.key.kms")
        val isServer = args.contains("--server")
        val localName = "localhost"
        val domainName = "apps.simiacrypt.us"
        val port = 8081

        val authentication = AuthenticatedWebsite(
            redirectUri = if (isServer) "https://$domainName/oauth2callback" else "http://$localName:$port/oauth2callback",
            applicationName = "Demo",
            key = { AwsAgent.decrypt("client_secret_google_oauth.json.kms").byteInputStream() }
        )
        val webAppContext = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(webAppContext, null)
        webAppContext.baseResource = Resource.newResource(javaClass.classLoader.getResource("welcome"))
        webAppContext.contextPath = "/"
        webAppContext.welcomeFiles = arrayOf("index.html")
        authentication.configure(webAppContext, false)

        val awsagent = AwsSkyenetCodingSessionServer(
            baseURL = if (isServer) "https://$domainName/awsagent/" else "http://$localName:$port/awsagent/",
            oauthConfig = null
        )
        val awsagentContext = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(awsagentContext, null)
        awsagentContext.baseResource = awsagent.baseResource
        awsagentContext.contextPath = "/awsagent"
        awsagentContext.welcomeFiles = arrayOf("index.html")
        authentication.configure(awsagentContext)
        awsagent.configure(awsagentContext, prefix = "")

        val storyGen = StoryGenerator(
            applicationName = "StoryGenerator",
            baseURL = if (isServer) "https://$domainName/storygen/" else "http://$localName:$port/storygen/"
        )
        val storyGenContext = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(storyGenContext, null)
        storyGenContext.baseResource = storyGen.baseResource
        storyGenContext.contextPath = "/storygen"
        storyGenContext.welcomeFiles = arrayOf("index.html")
        //authentication.configure(storyGenContext)
        storyGen.configure(storyGenContext, prefix = "")

        val cookbookGenerator = CookbookGenerator(
            applicationName = "CookbookGenerator",
            baseURL = if (isServer) "https://$domainName/cookbook/" else "http://$localName:$port/cookbook/"
        )
        val cookbookGeneratorContext = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(cookbookGeneratorContext, null)
        cookbookGeneratorContext.baseResource = cookbookGenerator.baseResource
        cookbookGeneratorContext.contextPath = "/cookbook"
        cookbookGeneratorContext.welcomeFiles = arrayOf("index.html")
        //authentication.configure(cookbookGeneratorContext)
        cookbookGenerator.configure(cookbookGeneratorContext, prefix = "")

        val scienceBookGenerator = SkyenetScienceBook(
            applicationName = "ScienceBookGenerator",
            baseURL = if (isServer) "https://$domainName/science/" else "http://$localName:$port/science/"
        )
        val scienceBookGeneratorContext = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(scienceBookGeneratorContext, null)
        scienceBookGeneratorContext.baseResource = scienceBookGenerator.baseResource
        scienceBookGeneratorContext.contextPath = "/science"
        scienceBookGeneratorContext.welcomeFiles = arrayOf("index.html")
        //authentication.configure(scienceBookGeneratorContext)
        scienceBookGenerator.configure(scienceBookGeneratorContext, prefix = "")

        val contexts = ContextHandlerCollection()
        contexts.handlers = arrayOf(
            webAppContext,
            awsagentContext,
            storyGenContext,
            cookbookGeneratorContext,
            scienceBookGeneratorContext
        )

        val server = Server(port)
        server.handler = contexts
        server.start()
        if (!isServer) try {
            Desktop.getDesktop().browse(URI("http://$localName:$port/"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        server.join()
    }

}