package com.oney.WebRTCModule.voip

import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object FulfillRequestManager {
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timeoutJobs = mutableMapOf<String, Job>()

    fun createRequest(timeoutMs: Long, onTimeout: (String) -> Unit): String {
        val requestId = UUID.randomUUID().toString()
        val timeoutJob = scope.launch(start = CoroutineStart.LAZY) {
            delay(timeoutMs)
            val didRemove = synchronized(lock) {
                timeoutJobs.remove(requestId) != null
            }
            if (didRemove) {
                onTimeout(requestId)
            }
        }

        synchronized(lock) {
            timeoutJobs[requestId] = timeoutJob
        }
        timeoutJob.start()
        return requestId
    }

    fun fulfill(requestId: String): Boolean = remove(requestId)

    fun cancel(requestId: String): Boolean = remove(requestId)

    fun cancelAll() {
        val jobs = synchronized(lock) {
            timeoutJobs.values.toList().also { timeoutJobs.clear() }
        }
        jobs.forEach(Job::cancel)
    }

    private fun remove(requestId: String): Boolean {
        val job = synchronized(lock) {
            timeoutJobs.remove(requestId)
        } ?: return false
        job.cancel()
        return true
    }
}
