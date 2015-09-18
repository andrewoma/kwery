/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.kwery.core

import java.util.concurrent.atomic.AtomicLong

/**
 * Transaction defines the currently executing transaction
 */
public interface Transaction {
    /**
     * If set to true, forces the transaction to roll back
     */
    public var rollbackOnly: Boolean

    /**
     * Adds a call back that is invoked prior to committing.
     * Can be used for adding things like audit logging into the current transaction.
     */
    public fun preCommitHandler(preCommit: (Session) -> Unit)

    /**
     * Adds a call back that is invoked after committing.
     * The Boolean parameter is true if committed, false if rolled back.
     * Can be used for adding things like invalidating caches after commit.
     */
    public fun postCommitHandler(postCommit: (Boolean, Session) -> Unit)

    /**
     * A unique id associated with this transaction.
     * It has no purpose other than to provide a useful identifier for logging
     */
    public val id: Long
}

/**
 * ManualTransaction allows explicit control over whether to commit or roll back transactions
 */
public interface ManualTransaction : Transaction {
    public fun commit()
    public fun rollback()
}

class DefaultTransaction(val session: DefaultSession) : ManualTransaction {
    companion object {
        val transactionId = AtomicLong()
    }

    init {
        check(session.transaction == null) { "A transaction is already started for this session" }
        session.connection.autoCommit = false
        session.transaction = this
    }

    override val id: Long = transactionId.incrementAndGet()

    override fun preCommitHandler(preCommit: (Session) -> Unit) {
        preCommitHandlers.add(preCommit)
    }

    override fun postCommitHandler(postCommit: (Boolean, Session) -> Unit) {
        postCommitHandlers.add(postCommit)
    }

    private val preCommitHandlers = arrayListOf<(Session) -> Unit>()
    private val postCommitHandlers = arrayListOf<(Boolean, Session) -> Unit>()

    override public var rollbackOnly: Boolean = false // Can only set to true, can never unset
        set(value) {
            if (value) $rollbackOnly = value
        }

    override fun commit() {
        check(!rollbackOnly) { "Invalid attempt to call commit after transaction is set to rollback only" }
        preCommitHandlers.forEach { it(session) }
        session.connection.commit()
        postCommitHandlers.forEach { it(true, session) }
        session.transaction = null
    }

    override fun rollback() {
        session.connection.rollback()
        postCommitHandlers.forEach { it(false, session) }
        session.transaction = null
    }

    fun <R> withTransaction(f: (Transaction) -> R): R {
        try {
            val result = f(this)
            if (rollbackOnly) {
                rollback()
            } else {
                commit()
            }
            return result
        } catch (t: Throwable) {
            rollback()
            throw t
        } finally {
            session.connection.autoCommit = true
        }
    }
}