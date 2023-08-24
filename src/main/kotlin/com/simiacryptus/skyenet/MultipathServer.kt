package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.AwsAgent.AwsSkyenetCodingSessionServer
import com.simiacryptus.skyenet.body.AuthenticatedWebsite
import com.simiacryptus.skyenet.body.WebSocketServer
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

        val server = start(
            port,
            authentication.configure(
                newWebAppContext(
                    "/",
                    Resource.newResource(javaClass.classLoader.getResource("welcome"))
                ), false
            ),
            authentication.configure(
                newWebAppContext(
                    "/awsagent", AwsSkyenetCodingSessionServer(
                        baseURL = if (isServer) "https://$domainName/awsagent/" else "http://$localName:$port/awsagent/",
                        oauthConfig = null
                    )
                )),
            newWebAppContext(
                "/storygen", StoryGenerator(
                    applicationName = "StoryGenerator",
                    baseURL = if (isServer) "https://$domainName/storygen/" else "http://$localName:$port/storygen/"
                )
            ),
            newWebAppContext(
                "/cookbook", CookbookGenerator(
                    applicationName = "CookbookGenerator",
                    baseURL = if (isServer) "https://$domainName/cookbook/" else "http://$localName:$port/cookbook/"
                )
            ),
            newWebAppContext(
                "/science", SkyenetScienceBook(
                    applicationName = "ScienceBookGenerator",
                    baseURL = if (isServer) "https://$domainName/science/" else "http://$localName:$port/science/"
                )
            ),
            newWebAppContext(
                "/software", SoftwareProjectGenerator(
                    applicationName = "SoftwareProjectGenerator",
                    baseURL = if (isServer) "https://$domainName/software/" else "http://$localName:$port/software/"
                )
            ),
            newWebAppContext(
                "/roblox", RobloxLuaCoder(
                    applicationName = "RobloxLuaCoder",
                    baseURL = if (isServer) "https://$domainName/roblox/" else "http://$localName:$port/roblox/"
                )
            )
        )
        if (!isServer) try {
            Desktop.getDesktop().browse(URI("http://$localName:$port/"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        server.join()
    }

    fun start(
        port: Int,
        vararg webAppContexts: WebAppContext
    ): Server {
        val contexts = ContextHandlerCollection()
        contexts.handlers = webAppContexts
        val server = Server(port)
        server.handler = contexts
        server.start()
        return server
    }

    fun newWebAppContext(path: String, server: WebSocketServer): WebAppContext {
        val webAppContext = newWebAppContext(path, server.baseResource)
        server.configure(webAppContext)
        return webAppContext
    }

    fun newWebAppContext(path: String, baseResource: Resource?): WebAppContext {
        val awsagentContext = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(awsagentContext, null)
        awsagentContext.baseResource = baseResource
        awsagentContext.contextPath = path
        awsagentContext.welcomeFiles = arrayOf("index.html")
        return awsagentContext
    }

}