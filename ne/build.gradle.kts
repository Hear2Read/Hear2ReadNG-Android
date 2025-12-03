// In the asset pack's build.gradle.kts file:
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("ne") // Directory name for the asset pack
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}