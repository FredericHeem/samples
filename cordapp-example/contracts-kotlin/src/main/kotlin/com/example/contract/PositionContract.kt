package com.example.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


class PositionContract : Contract {
    companion object {
        @JvmStatic
        val ID = "com.example.contract.PositionContract"
    }

    override fun verify(tx: LedgerTransaction) {
        //val command = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
        }
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}
