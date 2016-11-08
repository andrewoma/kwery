package com.github.andrewoma.kwery.mappertest.issues

import com.github.andrewoma.kwery.mapper.*
import org.junit.Test
import java.util.*

class Issue11_12 {

    @Test(expected = IllegalStateException::class)
    fun `should reject adding more columns after table construction`() {
        val usersTable = UsersTable()
        usersTable.allColumns // Forces instance construction

        val emailColumn = Column(
                User::email, null,
                optional(stringConverter),
                "email", false, false, true, false
        )

        usersTable.addColumn(emailColumn)
    }

    @Test fun `should return allColumns after table construction`() {
        val usersTable = UsersTable()
        assert(usersTable.allColumns.isNotEmpty())
    }
}

private data class User(val id: String, val name: String, val age: Int, val email: String? = null)

private class UsersTable : Table<User, UUID>("users") {

    val id by col(User::id, id = true)
    val userName by col(User::name)
    val age by col(User::age)

    override fun create(value: Value<User>) = User(value of id, value of userName, value of age)

    override fun idColumns(id: UUID) = setOf(this.id to id)
}
