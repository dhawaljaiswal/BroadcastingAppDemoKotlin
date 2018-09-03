package com.ratufa.livebroadcastingdemo

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import java.text.DateFormat
import java.util.*

/**
 * Created by Nand Kishor Patidar on 21,August,2018
 * Email nandkishor.patidar@ratufa.com.
 *
 */
class ExampleChatController(context: Context, private val mChatListView: ListView, layoutRes: Int, textRes: Int, timeRes: Int) {

    val isShown: Boolean
        get() = mChatListView.isShown

    private val mChatAdapter: ChatLineArrayAdapter

    init {
        mChatAdapter = ChatLineArrayAdapter(context, layoutRes, textRes, timeRes)
        mChatListView.adapter = mChatAdapter
    }

    fun add(msg: String) {
        mChatAdapter.add(ChatLine(DateFormat.getTimeInstance().format(Date()), msg))
    }

    fun show() {
        mChatListView.visibility = View.VISIBLE
    }

    fun hide() {
        mChatListView.visibility = View.GONE
    }

    fun hasMessages(): Boolean {
        return !mChatAdapter.isEmpty
    }

    private class ChatLine internal constructor(val mTime: String, val mText: String) {
        override fun toString(): String {
            return mText
        }
    }

    private class ChatLineArrayAdapter(context: Context, resource: Int, textViewResourceId: Int, private val mTimeViewResourceId: Int) : ArrayAdapter<ChatLine>(context, resource, textViewResourceId) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val chatLineView = super.getView(position, convertView, parent)
            if (mTimeViewResourceId != 0)
                (chatLineView.findViewById<View>(mTimeViewResourceId) as TextView).text = getItem(position)!!.mTime
            return chatLineView
        }
    }
}