@file:Suppress("unused")

package com.simiacryptus.skyenet

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.rds.AmazonRDSClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.body.SessionServerUtil.asJava
import com.simiacryptus.skyenet.body.SkyenetCodingSessionServer
import com.simiacryptus.skyenet.heart.ScalaLocalInterpreter
import com.simiacryptus.skyenet.heart.`ScalaLocalInterpreter$`
import com.simiacryptus.util.describe.AbbrevBlacklistYamlDescriber

import java.awt.Desktop
import java.net.URI

object AwsAgent {

    // Function to Say Hello World
    class AwsClients(val defaultRegion: Regions) {
        fun s3() = AmazonS3ClientBuilder.standard().withRegion(defaultRegion).build()
        fun ec2() = AmazonEC2ClientBuilder.standard().withRegion(defaultRegion).build()
        fun rds() = AmazonRDSClientBuilder.standard().withRegion(defaultRegion).build()
        fun cloudwatch() = AmazonCloudWatchClientBuilder.standard().withRegion(defaultRegion).build()
        fun route53() = com.amazonaws.services.route53.AmazonRoute53ClientBuilder
            .standard().withRegion(defaultRegion).build()
        fun emr() = com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder
            .standard().withRegion(defaultRegion).build()
        fun lambda() = com.amazonaws.services.lambda.AWSLambdaClientBuilder
            .standard().withRegion(defaultRegion).build()
    }

    class HttpUtil {
        fun client() = org.apache.http.impl.client.HttpClients.createDefault()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val port = 8081
        val baseURL = "http://localhost:$port"
        val server = object : SkyenetCodingSessionServer(
                applicationName = "AwsAgent",
                //oauthConfig = File(File(System.getProperty("user.home")), "client_secret_google_oauth.json").absolutePath,
                typeDescriber = AbbrevBlacklistYamlDescriber(
                        "com.amazonaws",
                        "org.apache"
                ),
                baseURL = baseURL,
                model = OpenAIClient.Models.GPT4,
                apiKey = OpenAIClient.keyTxt
        ) {
            override fun hands() = mapOf(
                    "aws" to AwsClients(Regions.US_EAST_1) as Object,
                    "client" to HttpUtil() as Object,
            ).asJava

            override fun toString(e: Throwable): String {
                return e.message ?: e.toString()
            }

            //            override fun heart(hands: java.util.Map<String, Object>): Heart = GroovyInterpreter(hands)
            override fun heart(hands: java.util.Map<String, Object>): Heart = ScalaLocalInterpreter::class.java.getConstructor(java.util.Map::class.java).newInstance(hands)
        }.start(port)
        dosomethingCool(34843)
        Desktop.getDesktop().browse(URI(baseURL))
        server.join()
    }

    private fun dosomethingCool(i: Int) {
        TODO("Not yet implemented")
    }
}