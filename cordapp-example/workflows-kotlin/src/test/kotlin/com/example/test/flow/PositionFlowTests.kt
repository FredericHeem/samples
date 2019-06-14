package com.example.test.flow

import com.example.flow.PositionCreateFlow
import com.example.state.IOUState
import com.example.state.Position
import com.example.state.PositionState
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PositionFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.example.contract"),
                TestCordapp.findCordapp("com.example.flow")
        )))
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
    fun createPosition(): CordaFuture<SignedTransaction> {
        val position = Position("bo","APPL", 0)
        val flow = PositionCreateFlow.Initiator(position, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        return future
    }
    @Test
    fun `create twice`() {
        createPosition().getOrThrow()
        assertEquals(1,  a.services.vaultService.queryBy<PositionState>().states.size)
        createPosition().getOrThrow()
        val positionNew = a.services.vaultService.queryBy<PositionState>().states

        positionNew.map{ println(it.state.data.position)}
        assertEquals(2, positionNew.size)

    }


}