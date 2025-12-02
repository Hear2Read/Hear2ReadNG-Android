// In the asset pack's build.gradle.kts file:
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("eng") // Directory name for the asset pack
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}