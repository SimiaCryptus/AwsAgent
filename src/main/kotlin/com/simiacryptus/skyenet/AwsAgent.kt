@file:Suppress("unused")

package com.simiacryptus.skyenet

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.kms.model.DecryptRequest
import com.amazonaws.services.kms.model.EncryptRequest
import com.amazonaws.services.rds.AmazonRDSClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.body.SessionServerUtil.asJava
import com.simiacryptus.skyenet.body.SkyenetCodingSessionServer
import com.simiacryptus.skyenet.heart.ScalaLocalInterpreter
import com.simiacryptus.util.describe.AbbrevWhitelistYamlDescriber
import org.eclipse.jetty.server.Server
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.Map

object AwsAgent {

    // Function to Say Hello World
    class AwsClients(val defaultRegion: Regions) {
        fun s3() = AmazonS3ClientBuilder.standard().withRegion(defaultRegion).build()
        fun ec2() = AmazonEC2ClientBuilder.standard().withRegion(defaultRegion).build()
        fun rds() = AmazonRDSClientBuilder.standard().withRegion(defaultRegion).build()
        fun cloudwatch() = AmazonCloudWatchClientBuilder.standard().withRegion(defaultRegion).build()
        fun route53() =
            com.amazonaws.services.route53.AmazonRoute53ClientBuilder.standard().withRegion(defaultRegion).build()

        fun emr() = com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder.standard()
            .withRegion(defaultRegion).build()

        fun lambda() = com.amazonaws.services.lambda.AWSLambdaClientBuilder.standard().withRegion(defaultRegion).build()
    }

    class HttpUtil {
        fun client() = org.apache.http.impl.client.HttpClients.createDefault()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        OpenAIClient.keyTxt = decrypt("openai.key.kms")
        val domain = "localhost"
        val httpServer = listOf(
            AwsAgent.start("http://$domain:8081", 8081),
            StoryGenerator(
                applicationName = "StoryGenerator",
                baseURL = "http://$domain:8082",
                oauthConfig = oauthConfig.absolutePath
            ).start(8082)
        )
        try {
            Desktop.getDesktop().browse(URI("http://$domain:8081"))
            Desktop.getDesktop().browse(URI("http://$domain:8082"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        httpServer.forEach { it.join() }
    }

    fun encrypt(inputFilePath: String, outputFilePath: String) {
        val filePath = Paths.get(inputFilePath)
        val fileBytes = Files.readAllBytes(filePath)
        val kmsClient = AWSKMSClientBuilder.standard().build()
        val encryptRequest =
            EncryptRequest().withKeyId("arn:aws:kms:us-east-1:470240306861:key/a1340b89-64e6-480c-a44c-e7bc0c70dcb1")
                .withPlaintext(ByteBuffer.wrap(fileBytes))
        val result = kmsClient.encrypt(encryptRequest)
        val cipherTextBlob = result.ciphertextBlob
        val encryptedData = Base64.getEncoder().encodeToString(cipherTextBlob.array())
        val outputPath = Paths.get(outputFilePath)
        Files.write(outputPath, encryptedData.toByteArray())
    }

    fun decrypt(resourceFile: String): String {
        val encryptedData = javaClass.classLoader.getResourceAsStream(resourceFile)?.readAllBytes()
        if (null == encryptedData) {
            throw RuntimeException("Unable to load resource: $resourceFile")
        }
        val decodedData = Base64.getDecoder().decode(encryptedData)
        val kmsClient = AWSKMSClientBuilder.defaultClient()
        val decryptRequest = DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(decodedData))
        val decryptResult = kmsClient.decrypt(decryptRequest)
        val decryptedData = decryptResult.plaintext.array()
        return String(decryptedData, StandardCharsets.UTF_8)
    }

    val oauthConfig by lazy {
        writeToTempFile(
            decrypt("client_secret_google_oauth.json.kms"),
            "client_secret_google_oauth.json"
        )
    }

    fun start(baseURL: String, port: Int): Server {
        // Load /client_secret_google_oauth.json.kms from classpath, decrypt it, and print it out
        return object : SkyenetCodingSessionServer(
            applicationName = "AwsAgent",
            oauthConfig = oauthConfig.absolutePath,
            typeDescriber = AbbrevWhitelistYamlDescriber("com.simiacryptus", "com.github.simiacryptus"),
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
            override fun heart(hands: Map<String, Object>): Heart =
                ScalaLocalInterpreter::class.java.getConstructor(Map::class.java).newInstance(hands)
        }.start(port)
    }

    fun writeToTempFile(text: String, filename: String): File {
        val tempFile = File.createTempFile(filename, ".tmp")
        tempFile.deleteOnExit()
        tempFile.writeText(text)
        return tempFile
    }

}