package com.example.state


import com.example.contract.IdentityContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.util.*
import javax.persistence.*

fun identityId(beneficialOwnerId: String) = UniqueIdentifier(null, UUID.nameUUIDFromBytes(beneficialOwnerId.toByteArray()))

fun identityPersist(identityState: IdentityState): IdentitySchemaV1.PersistentIdentity {
    val identity = identityState.identity
    return IdentitySchemaV1.PersistentIdentity(
            linearId = identityState.linearId.id,
            beneficialOwnerId = identity.beneficialOwnerId,
            name = identity.name
    )
}

@BelongsToContract(IdentityContract::class)
data class IdentityState(
        val identity: Identity,
        val global: Party,
        val local: Party
) : LinearState, QueryableState {
    override val linearId: UniqueIdentifier
        get() = identityId(identity.beneficialOwnerId)

    override val participants: List<AbstractParty> get() = listOf(local, global)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is IdentitySchemaV1 -> identityPersist(this)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(IdentitySchemaV1)
}

@CordaSerializable
data class Identity(
        var beneficialOwnerId: String = "",
        val name: String? = null
)

object IdentitySchema
object IdentitySchemaV1 : MappedSchema(
        schemaFamily = IdentitySchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentIdentity::class.java)) {
    @Entity
    @Table(name = "identity")
    class PersistentIdentity(
            @Column(name = "linear_id", nullable = false)
            var linearId: UUID,

            @Column(name = "beneficial_owner_identifier", length = 32, nullable = false)
            var beneficialOwnerId: String,

            @Column(name = "name", length = 140, nullable = true)
            var name: String?,

            @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
            @JoinColumns(JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id"), JoinColumn(name = "output_index", referencedColumnName = "output_index"))
            @OrderColumn
            var positions: MutableSet<PositionSchemaV1.PersistentPositionState> = mutableSetOf()

    ) : PersistentState()
}