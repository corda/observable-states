package net.corda.demos.crowdFunding.flows

import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.finance.flows.CashIssueFlow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.slf4j.Logger
import java.time.Instant
import java.util.*

abstract class CrowdFundingTest {
    protected lateinit var network: MockNetwork
    protected lateinit var A: StartedMockNode
    protected lateinit var B: StartedMockNode
    protected lateinit var C: StartedMockNode
    protected lateinit var D: StartedMockNode
    protected lateinit var E: StartedMockNode

    @Before
    fun setupNetwork() {
        network = MockNetwork(listOf("net.corda.demos.crowdFunding", "net.corda.finance"), threadPerNode = true)
        A = network.createPartyNode()
        B = network.createPartyNode()
        C = network.createPartyNode()
        D = network.createPartyNode()
        E = network.createPartyNode()
        listOf(A, B, C, D, E).forEach { node -> registerFlowsAndServices(node) }
    }

    @After
    fun tearDownNetwork() {
        network.stopNodes()
    }

    companion object {
        val logger: Logger = loggerFor<CrowdFundingTest>()
    }

    private fun calculateDeadlineInSeconds(interval: Long) = Instant.now().plusSeconds(interval)
    protected val fiveSecondsFromNow get() = calculateDeadlineInSeconds(5L)

    protected fun registerFlowsAndServices(node: StartedMockNode) {
        node.registerInitiatedFlow(RecordTransactionAsObserver::class.java)
        node.registerInitiatedFlow(MakePledge.Responder::class.java)
        node.registerInitiatedFlow(EndCampaign.Responder::class.java)
    }

    fun StartedMockNode.legalIdentity(): Party {
        return this.info.legalIdentities.first()
    }

    protected fun selfIssueCash(party: StartedMockNode,
                                amount: Amount<Currency>): SignedTransaction {
        val notary = party.services.networkMapCache.notaryIdentities.firstOrNull()
                ?: throw IllegalStateException("Could not find a notary.")
        val issueRef = OpaqueBytes.of(0)
        val issueRequest = CashIssueFlow.IssueRequest(amount, issueRef, notary)
        val flow = CashIssueFlow(issueRequest)
        return party.startFlow(flow).getOrThrow().stx
    }

}
