package com.example.state

import com.example.contract.PositionContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import javax.persistence.Column
import javax.persistence.Entity

@CordaSerializable
data class Transaction(
        var reference: String,
        val beneficialOwnerId: String,
        val securityId: String,
        var quantity: Int = 0)

@CordaSerializable
data class Settlement(
        var reference: String,
        var position: LinearPointer<PositionState>)

object SettlementSchema

object SettlementSchemaV1 : MappedSchema(
        schemaFamily = SettlementSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentSettlementState::class.java)) {

    @Entity(name = "settlement")
    data class PersistentSettlementState(
            @Column(name = "reference", nullable = false)
            var reference: String,

            @Column(name = "local")
            var local: String = "",

            @Column(name = "global")
            var global: String = ""

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("",
                "", "")
    }
}

@BelongsToContract(PositionContract::class)
data class SettlementState(
        val settlement: Settlement,
        val local: Party,
        val global: Party
) : QueryableState {
    override val participants: List<AbstractParty> get() = listOf(local, global)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is SettlementSchemaV1 -> SettlementSchemaV1.PersistentSettlementState(
                    reference = this.settlement.reference,
                    local = this.local.name.toString(),
                    global = this.global.name.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(SettlementSchemaV1)
}