package com.simiacryptus.skyenet

import com.amazonaws.regions.Regions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AwsAgentTest {

    @Test
    fun testAwsClients() {
        val awsClients = AwsAgent.AwsClients(Regions.US_EAST_1)
        assertNotNull(awsClients.s3())
        assertNotNull(awsClients.ec2())
        assertNotNull(awsClients.rds())
        assertNotNull(awsClients.cloudwatch())
        assertNotNull(awsClients.route53())
        assertNotNull(awsClients.emr())
        assertNotNull(awsClients.lambda())
    }

    @Test
    fun testHttpUtil() {
        val httpUtil = AwsAgent.HttpUtil()
        assertNotNull(httpUtil.client())
    }

    @Test
    fun testMain() {
        // This test is left intentionally blank as main method launches a server and opens a browser
        // which is not suitable for a unit test.
    }
}