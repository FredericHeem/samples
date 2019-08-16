package com.example.contract


import com.example.state.IdentityState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class IdentityContract : Contract {
    companion object {
        @JvmStatic
        val ID = "com.example.contract.IdentityContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.Create-> verifyCreate(tx, setOfSigners)
            is Commands.Update -> verifyUpdate(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")

        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs should be consumed when creating a identity." using (tx.inputs.isEmpty())
        "Only one output state should be created." using (tx.outputs.size == 1)
        val out = tx.outputsOfType<IdentityState>().single()
        "All of the participants must be signers." using (signers.containsAll(out.participants.map { it.owningKey }))
    }

    private fun verifyUpdate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "Should have one input of type identity." using (tx.inputs.size == 1)
        "Only one output state should be created." using (tx.outputs.size == 1)
        //val out = tx.outputsOfType<IdentityState>().single()
        //"The local and the global custodian cannot be the same entity." using (out.local != out.global)
        //"All of the participants must be signers." using (signers.containsAll(out.participants.map { it.owningKey }))
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
    }
}