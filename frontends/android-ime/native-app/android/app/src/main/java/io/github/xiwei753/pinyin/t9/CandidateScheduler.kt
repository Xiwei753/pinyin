package io.github.xiwei753.pinyin.t9

import android.os.Handler
import android.os.Looper
import io.github.xiwei753.pinyin.imecore.CandidateRequest
import io.github.xiwei753.pinyin.imecore.CandidateResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

interface CandidateScheduler {
    fun submit(
        request: CandidateRequest,
        compute: () -> CandidateResult,
        onResult: (CandidateResult) -> Unit,
    )

    fun shutdown()
}

class AsyncCandidateScheduler(
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) : CandidateScheduler {

    private val latestRequestId = AtomicLong(0L)

    override fun submit(
        request: CandidateRequest,
        compute: () -> CandidateResult,
        onResult: (CandidateResult) -> Unit,
    ) {
        latestRequestId.set(request.requestId)
        executor.execute {
            val result = compute()
            if (result.requestId != latestRequestId.get()) return@execute
            mainHandler.post {
                if (result.requestId == latestRequestId.get()) {
                    onResult(result)
                }
            }
        }
    }

    override fun shutdown() {
        executor.shutdownNow()
    }
}
