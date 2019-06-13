package com.example.state


import com.example.contract.PositionContract
import com.example.schema.PositionKey
import com.example.schema.PositionSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 */
@BelongsToContract(PositionContract::class)
data class PositionState(
        val position: Position,
        val local: Party,
        val global: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState

{
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(local, global)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PositionSchemaV1 -> PositionSchemaV1.PersistentPositionState(
                    compositeKey=PositionKey(this.position.beneficialOwnerId, this.position.securityId),
                    pendingQuantity = this.position.pendingQuantity,
                    local=this.local.name.toString(),
                    global=this.global.name.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PositionSchemaV1)

}

@CordaSerializable
data class Position(
        val beneficialOwnerId: String,
        val securityId: String,
        val pendingQuantity: Int
        )
