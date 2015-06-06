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

package com.github.andrewoma.kwery.mapper.listener

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.SessionCallback
import com.github.andrewoma.kwery.core.Transaction
import com.github.andrewoma.kwery.mapper.Table

public interface Listener {
    fun onEvent(session: Session, events: List<Event>)
}

public open class Event(val table: Table<*, *>, val id: Any)
public data class InsertEvent(table: Table<*, *>, id: Any, val value: Any) : Event(table, id)
public data class DeleteEvent(table: Table<*, *>, id: Any, val value: Any?) : Event(table, id)
public data class UpdateEvent(table: Table<*, *>, id: Any, val new: Any?, val old: Any?) : Event(table, id)

public class PostCommitListener(handlerFactory: () -> DeferredEventHandler) : DeferredListener(handlerFactory) {
    override fun handler(transaction: Transaction) =
            transaction.postCommitHandler(handlerFactory.javaClass.getName()) { handlerFactory() } as DeferredEventHandler
}

public class PreCommitListener(handlerFactory: () -> DeferredEventHandler) : DeferredListener(handlerFactory) {
    override fun handler(transaction: Transaction) =
            transaction.preCommitHandler(handlerFactory.javaClass.getName()) { handlerFactory() } as DeferredEventHandler
}

abstract class DeferredListener(val handlerFactory: () -> DeferredEventHandler) : Listener {
    override fun onEvent(session: Session, events: List<Event>) {
        val transaction = session.currentTransaction
        if (transaction == null) return

        for (event in events) handler(transaction).addEvent(event)
    }

    abstract fun handler(transaction: Transaction): DeferredEventHandler
}

public abstract class DeferredEventHandler : SessionCallback {
    protected val events: MutableList<Event> = arrayListOf()

    public open fun supports(event: Event): Boolean = true

    public fun addEvent(event: Event) {
        if (supports(event)) events.add(event)
    }
}