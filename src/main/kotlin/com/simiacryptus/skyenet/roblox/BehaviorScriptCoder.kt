package com.simiacryptus.skyenet.roblox

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.body.ChatSessionFlexmark
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import java.awt.Desktop
import java.net.URI

class BehaviorScriptCoder(
    applicationName: String,
    baseURL: String,
    oauthConfig: String? = null,
    temperature: Double = 0.1
) : SkyenetMacroChat(
    applicationName = applicationName,
    baseURL = baseURL,
    temperature = temperature,
    oauthConfig = oauthConfig
) {

    override fun processMessage(
        sessionId: String,
        userMessage: String,
        sessionUI: SessionUI,
        sendUpdate: (String, Boolean) -> Unit
    ) {
        sendUpdate("""<div>$userMessage</div>""", true)

        val model = OpenAIClient.Models.GPT4
        val response = api.chat(
            OpenAIClient.ChatRequest(
                messages = arrayOf(
                    OpenAIClient.ChatMessage(role = OpenAIClient.ChatMessage.Role.system, content = """
                        You will convert the natural language description of an behavior for a Roblox game script into a Lua definition
                    """.trimIndent()),
                    OpenAIClient.ChatMessage(role = OpenAIClient.ChatMessage.Role.user, content = "Kill the player on touch"),
                    OpenAIClient.ChatMessage(role = OpenAIClient.ChatMessage.Role.assistant, content = """
                        ```lua
                        function handlePart(part)
                        	part.Touched:Connect(function(hit)
                        		local humanoid = hit.Parent:FindFirstChild('Humanoid')
                        		if humanoid then
                        			humanoid.Health = 0
                        		end
                        	end)
                        end

                        function handleNode(model)
                        	for i, part in pairs(model:GetChildren()) do
                        		if part:IsA('Part') then
                        			handlePart(part)
                        		else
                        			handleNode(part)
                        		end
                        	end
                        end

                        handleNode(script.Parent)
                        ```
                    """.trimIndent()),
                    OpenAIClient.ChatMessage(role = OpenAIClient.ChatMessage.Role.user, content = userMessage)
                ),
                temperature = temperature,
                model = model.modelName,
            ), model
        )

        sendUpdate("""<div>${ChatSessionFlexmark.renderMarkdown(response.choices.get(0).message?.content ?: "")}</div>""", true)
    }

    companion object {

        const val port = 8771
        const val baseURL = "http://localhost:$port"

        @JvmStatic
        fun main(args: Array<String>) {
            val httpServer = BehaviorScriptCoder("RobloxLuaCoder", baseURL).start(port)
            Desktop.getDesktop().browse(URI(baseURL))
            httpServer.join()
        }
    }

}