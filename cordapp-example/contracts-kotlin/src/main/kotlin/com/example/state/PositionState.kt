package com.example.state


import com.example.contract.PositionContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne

@BelongsToContract(PositionContract::class)
data class PositionState(
        val position: Position,
        val local: Party,
        val global: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier(null, UUID.nameUUIDFromBytes("${position.beneficialOwnerId}${position.securityId}".toByteArray()))
) : LinearState, QueryableState {
    override val participants: List<AbstractParty> get() = listOf(local, global)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PositionSchemaV1 -> PositionSchemaV1.PersistentPositionState(
                    linearId = this.linearId.toString(),
                    beneficialOwnerId = this.position.beneficialOwnerId,
                    securityId = this.position.securityId,
                    pendingQuantity = this.position.pendingQuantity,
                    identity = this.position.identityState?.let { identityPersist(this.position.identityState) }
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PositionSchemaV1)
}

@CordaSerializable
data class Position(
        val identityState: IdentityState? = null,
        val beneficialOwnerId: String = "",
        val securityId: String = "",
        val pendingQuantity: Int = 0
)

object PositionSchema
object PositionSchemaV1 : MappedSchema(
        schemaFamily = PositionSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPositionState::class.java)) {

    @Entity(name = "position")
    data class PersistentPositionState(
            @Column(name = "linear_id")
            val linearId: String = "",

            @Column(name = "beneficial_owner_id")
            val beneficialOwnerId: String = "",

            @Column(name = "security_id")
            val securityId: String = "",

            @Column(name = "pending_quantity")
            var pendingQuantity: Int = 0,

            @ManyToOne(targetEntity = IdentitySchemaV1.PersistentIdentity::class)
            var identity: IdentitySchemaV1.PersistentIdentity? = null
    ) : PersistentState()
}

fun retrievePositions(transaction: Transaction, serviceHub: ServiceHub): List<StateAndRef<PositionState>> {
    var boIdIndex = PositionSchemaV1.PersistentPositionState::beneficialOwnerId.equal(transaction.beneficialOwnerId)
    var securityIdIndex = PositionSchemaV1.PersistentPositionState::securityId.equal(transaction.securityId)
    val queryCriteria = QueryCriteria.VaultCustomQueryCriteria(boIdIndex)
            .and(QueryCriteria.VaultCustomQueryCriteria(securityIdIndex))

    return serviceHub.vaultService.queryBy<PositionState>(queryCriteria).states
}

fun retrievePosition(transaction: Transaction, serviceHub: ServiceHub): StateAndRef<PositionState>? {
    val states = retrievePositions(transaction, serviceHub)
    if (states.isEmpty()) {
        return null
    } else if (states.size >= 2) {
        throw FlowException("Multiple positions for transaction $transaction")
    }
    return states.first()
}
