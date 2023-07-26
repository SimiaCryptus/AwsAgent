package com.simiacryptus.skyenet

object EncryptFile {

    @JvmStatic
    fun main(args: Array<String>) {
        AwsAgent.encrypt("E:\\backup\\winhome\\openai.key", "C:\\Users\\andre\\code\\AwsAgent\\src\\main\\resources\\openai.key.kms")
    }
}