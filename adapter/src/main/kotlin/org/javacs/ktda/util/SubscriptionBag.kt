package org.javacs.ktda.util

class SubscriptionBag: Subscription {
    private val subscriptions = mutableListOf<Subscription>()

    fun add(subscription: Subscription) {
        subscriptions.add(subscription)
    }

    override fun unsubscribe() {
        var iterator = subscriptions.iterator()
        while (iterator.hasNext()) {
            iterator.next().unsubscribe()
            iterator.remove()
        }
    }
}
