package com.jev.probe.core

import android.graphics.Rect

/**
 * One captured chat bubble (or system tip treated as a row).
 *
 * [kind] is a capture-layer tag so the service can tell text / image / withdraw
 * apart without re-parsing Chinese tip strings downstream:
 * - [KIND_TEXT] ordinary bubble
 * - [KIND_IMAGE] image placeholder (`[图片]` or an image bubble with empty text)
 * - [KIND_WITHDRAW] WeChat "…撤回了一条消息" system tip
 */
data class Msg(
    val side: String,
    val text: String,
    val kind: String = KIND_TEXT
) {
    companion object {
        const val KIND_TEXT = "text"
        const val KIND_IMAGE = "image"
        const val KIND_WITHDRAW = "withdraw"
    }
}

/**
 * A bubble the node tree can locate but not read (Feishu draws its message text
 * itself). [rect] is in screen coordinates; [side] is what the tree could infer
 * around the bubble. The service OCRs each rect to get the words.
 */
data class BubbleRect(val rect: Rect, val side: String)

/**
 * A snapshot of the currently-open conversation in whichever chat app is
 * foreground (see ChatAppAdapter).
 *
 * Adapter contract: `extract` returning null means "not in a chat window".
 * Returning a snapshot whose [messages] is empty means "in a chat window, but
 * the tree holds no text" — that is the OCR fallback's cue, and the one case
 * where [bubbleRects] may be populated.
 *
 * [note] is a caveat about how this snapshot was produced, shown verbatim in
 * the analysis panel (OCR captures cannot tell who said what).
 */
data class ChatSnapshot(
    val title: String?,
    val messages: List<Msg>,
    val bubbleRects: List<BubbleRect> = emptyList(),
    val note: String? = null
) {
    val latestFrom: String? get() = messages.lastOrNull()?.side

    /** A stable signature of the last few messages, to detect real changes. */
    fun signature(): String =
        messages.takeLast(6).joinToString("|") { "${it.side}:${it.kind}:${it.text}" }

    /** True when the newest row is a withdraw tip from the other person. */
    val latestIsOtherWithdraw: Boolean
        get() {
            val last = messages.lastOrNull() ?: return false
            return last.kind == Msg.KIND_WITHDRAW && last.side == "other"
        }
}

/** Jev's judgment result for one snapshot, plus the ranked candidate replies. */
data class Analysis(
    val trueIntent: Choice?,
    val dangerLevel: Score?,
    val sheNeeds: Choice?,
    val shouldReplyNow: Double?,
    val bestAction: Choice?,
    val tensionResolved: Double?,
    val literalQuestion: Double?,
    val rankedReplies: List<RankedReply>,
    val latencyMs: Long,
    val error: String? = null
)

data class Choice(val choice: String, val confidence: Double, val probabilities: Map<String, Double>)
data class Score(val score: Double, val confidence: Double, val maxLevel: Int)
data class RankedReply(val text: String, val prob: Double)
