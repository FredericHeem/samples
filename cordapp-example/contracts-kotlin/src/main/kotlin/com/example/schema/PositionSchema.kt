package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.io.Serializable
import javax.persistence.*

/**
 * The family of schemas for PositionState.
 */
object PositionSchema

data class PositionKey  (
        @Column(name = "beneficial_owner_id")
        val beneficialOwnerId: String = "",

        @Column(name = "security_id")
        val securityId: String = ""
) : Serializable

/**
 * An PositionState schema.
 */
object PositionSchemaV1 : MappedSchema(
        schemaFamily = PositionSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPositionState::class.java)) {

    @Entity(name = "position")
    @IdClass(PositionKey::class)
    @Access(AccessType.FIELD)
    data class PersistentPositionState(

            @Id
            @Column(name = "beneficial_owner_id")
            var beneficialOwnerId: String = "",

            @Id
            @Column(name = "security_id")
            var securityId: String = "",

            @Column(name = "pending_quantity")
            var pendingQuantity: Int = 0,

            @Column(name = "local")
            var local: String = "",

            @Column(name = "global")
            var global: String = ""

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", 0,"","")
    }
}

