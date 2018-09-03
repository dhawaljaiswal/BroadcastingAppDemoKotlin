package com.ratufa.livebroadcastingdemo.models

/**
 * Created by Nand Kishor Patidar on 21,August,2018
 * Email nandkishor.patidar@ratufa.com.
 *
 */
class Gift(giftType: String, giftImage: Int, count: Int) {
    var giftType = ""
    var giftImage = 0
    var count = 0

    init {
        this.giftType = giftType
        this.giftImage = giftImage
        this.count = count
    }
}
