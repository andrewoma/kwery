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

trait SessionCallback {
    fun invoke(session: Session)
}

public trait Transaction {
    public var rollbackOnly: Boolean
    public fun preCommitHandler(name: String, ifAbsent: () -> SessionCallback): SessionCallback
    public fun postCommitHandler(name: String, ifAbsent: () -> SessionCallback): SessionCallback
    public fun postRollbackHandler(name: String, ifAbsent: () -> SessionCallback): SessionCallback
}

public trait ManualTransaction : Transaction {
    public fun commit()
    public fun rollback()
}

class DefaultTransaction(val session: DefaultSession) : ManualTransaction {
    {
        check(session.transaction == null, "A transaction is already started for this session")
        session.connection.setAutoCommit(false)
        session.transaction = this
    }

    override fun postCommitHandler(name: String, ifAbsent: () -> SessionCallback): SessionCallback {
        return handler(name, postCommitHandlers, ifAbsent)
    }

    override fun preCommitHandler(name: String, ifAbsent: () -> SessionCallback): SessionCallback {
        return handler(name, preCommitHandlers, ifAbsent)
    }

    override fun postRollbackHandler(name: String, ifAbsent: () -> SessionCallback): SessionCallback {
        return handler(name, postRollbackHandlers, ifAbsent)
    }

    [suppress("UNCHECKED_CAST")]
    fun handler(name: String, handlers: MutableMap<String, SessionCallback>, ifAbsent: () -> SessionCallback): SessionCallback {
        return handlers.getOrPut(name) { ifAbsent() }
    }

    private val preCommitHandlers = hashMapOf<String, SessionCallback>()
    private val postCommitHandlers = hashMapOf<String, SessionCallback>()
    private val postRollbackHandlers = hashMapOf<String, SessionCallback>()

    override public var rollbackOnly: Boolean = false // Can only set to true, can never unset
        set(value) {
            if (value) $rollbackOnly = value
        }

    override fun commit() {
        check(!rollbackOnly, "Invalid attempt to call commit after transaction is set to rollback only")
        preCommitHandlers.values().forEach { it(session) }
        session.connection.commit()
        session.transaction = null
        postCommitHandlers.values().forEach { it(session) }
    }

    override fun rollback() {
        session.transaction = null
        session.connection.rollback()
        postRollbackHandlers.values().forEach { it(session) }
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
            session.connection.setAutoCommit(true)
        }
    }
}