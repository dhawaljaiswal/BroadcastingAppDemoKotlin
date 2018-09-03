package com.ratufa.livebroadcastingdemo.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.ratufa.livebroadcastingdemo.R
import com.ratufa.livebroadcastingdemo.models.Gift

/**
 * Created by Nand Kishor Patidar on 21,August,2018
 * Email nandkishor.patidar@ratufa.com.
 *
 */
class GiftAdapter(private val context: Context, private val giftList: List<Gift>) : RecyclerView.Adapter<GiftAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.gift_item_row, parent, false)
        // set the view's size, margins, paddings and layout parameters

        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gift = giftList[position]
        //set data on gift row
        holder.giftName.text = gift.giftType
        holder.giftImage.setImageResource(gift.giftImage)

        holder.itemView.setOnClickListener { Toast.makeText(context, "Click on " + gift.giftType, Toast.LENGTH_LONG).show() }
    }

    override fun getItemCount(): Int {
        return giftList.size
    }

    class ViewHolder(internal var MyView: View) : RecyclerView.ViewHolder(MyView) {
        internal var giftName: TextView
        internal var giftImage: ImageView
        init {
            giftName = MyView.findViewById(R.id.giftName)
            giftImage = MyView.findViewById(R.id.giftImage)
        }
    }
}