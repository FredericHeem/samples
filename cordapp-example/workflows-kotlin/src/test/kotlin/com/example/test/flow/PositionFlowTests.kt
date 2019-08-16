package com.example.test.flow

import com.example.flow.IdentityCreateFlow
import com.example.flow.PositionCreateFlow
import com.example.flow.PositionUpdateFlow
import com.example.state.*
import net.corda.core.concurrent.CordaFuture
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PositionFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    val beneficialOwnerId = "bo123"
    val securityId = "GB123456789"

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.example.contract"),
                TestCordapp.findCordapp("com.example.flow")
        ), networkParameters = testNetworkParameters().copy(minimumPlatformVersion = 4)))

        a = network.createPartyNode()
        b = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(PositionCreateFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    fun createIdentity(identity: Identity) {
        val identity = Identity(beneficialOwnerId = beneficialOwnerId, name = "alice")
        val flow = IdentityCreateFlow.Initiator(identity, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()
    }

    fun createPosition(position: Position): CordaFuture<SignedTransaction> {
        val flow = PositionCreateFlow.Initiator(position, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        return future
    }

    @Test
    fun `create`() {
        val identity = Identity(beneficialOwnerId = beneficialOwnerId, name = "bob")
        createIdentity(identity)
        val identityState = a.services.vaultService.queryBy<IdentityState>().states.first().state.data

        val position = Position(
                beneficialOwnerId = identity.beneficialOwnerId,
                securityId = securityId,
                pendingQuantity = 0,
                identityState = identityState)

        createPosition(position).getOrThrow()
        assertEquals(1, a.services.vaultService.queryBy<PositionState>().states.size)
    }

    @Test
    fun `create twice`() {
        val position = Position(beneficialOwnerId = beneficialOwnerId, securityId = securityId, pendingQuantity = 0)
        createPosition(position).getOrThrow()
        assertEquals(1, a.services.vaultService.queryBy<PositionState>().states.size)
        createPosition(position).getOrThrow()
        val positionNew = a.services.vaultService.queryBy<PositionState>().states

        positionNew.map { println(it.state.data.position) }
        assertEquals(1, positionNew.size)
    }

    @Test
    fun `update position`() {
        val position = Position(beneficialOwnerId = beneficialOwnerId, securityId = securityId, pendingQuantity = 0)
        createPosition(position).getOrThrow()
        var transaction = Transaction(reference = "ABC", beneficialOwnerId = beneficialOwnerId, securityId = securityId)
        val flow = PositionUpdateFlow.Initiator(transaction, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

    }

}