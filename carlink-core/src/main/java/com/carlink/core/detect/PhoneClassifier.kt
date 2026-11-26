package com.carlink.core.detect

import com.carlink.core.core.PhoneKind
import java.util.Locale
import java.util.UUID

/**
 * Decides whether a USB or Bluetooth device is an iPhone (CarPlay) or an Android phone
 * (Android Auto). Pure functions over plain data so every rule is unit tested.
 */
object PhoneClassifier {
    const val APPLE_VENDOR_ID = 0x05AC
    const val GOOGLE_VENDOR_ID = 0x18D1

    /** Product IDs a phone uses after switching to Android Open Accessory mode (AOA 1.0/2.0). */
    private val AOA_PRODUCT_IDS = 0x2D00..0x2D05

    /** iAP2 over Bluetooth: the RFCOMM service DiPlay connects to on a paired iPhone. */
    val IAP2_BLUETOOTH_UUID: UUID = UUID.fromString("00000000-deca-fade-deca-deafdecacafe")

    /** Android Auto wireless setup service. */
    val ANDROID_AUTO_BLUETOOTH_UUID: UUID = UUID.fromString("4de17a00-52cb-11e6-bdf4-0800200c9a66")

    /** Bluetooth major device class for phones (BluetoothClass.Device.Major.PHONE). */
    const val BLUETOOTH_MAJOR_PHONE = 0x0200

    /** BluetoothClass.Device.Major.UNCATEGORIZED: some phones report no class at all. */
    const val BLUETOOTH_MAJOR_UNCATEGORIZED = 0x1F00

    /**
     * OBEX Object Push. Every Android phone publishes it ("Bluetooth share"); iOS has never
     * supported it, so it is positive evidence of Android, unlike a missing iAP2 record, which an
     * iPhone can also show (GitHub #89, #90).
     */
    val OBEX_OBJECT_PUSH_UUID: UUID = UUID.fromString("00001105-0000-1000-8000-00805f9b34fb")

    /**
     * Words in the default Bluetooth names of Android phones (Xiaomi 14T Pro, Galaxy S24, Pixel 8,
     * Redmi Note 13…). Whole words only, so a brand inside another word does not count.
     */
    private val ANDROID_NAME_WORDS = setOf(
        "android", "xiaomi", "redmi", "poco", "galaxy", "samsung", "pixel", "oneplus", "huawei", "honor",
        "oppo", "vivo", "iqoo", "realme", "motorola", "moto", "nokia", "xperia", "infinix", "tecno",
        "zte", "nubia", "lenovo", "zenfone", "meizu", "fairphone",
    )

    /** Model numbers used as default names: Samsung SM-/GT-, OPPO CPH, realme RMX. */
    private val ANDROID_MODEL_PREFIX = Regex("""^(sm-[a-z]\d|gt-[a-z]\d|cph\d{4}|rmx\d{4})""")

    /** Whether [name] is an Android phone's default name. Never for a name that says iPhone or iPad. */
    fun looksLikeAndroidName(name: String?): Boolean {
        val lower = name.orEmpty().trim().lowercase(Locale.ROOT)
        if (lower.isEmpty() || "iphone" in lower || "ipad" in lower) return false
        if (ANDROID_MODEL_PREFIX.containsMatchIn(lower)) return true
        return lower.split(Regex("""[^\p{L}\p{N}]+""")).any { it in ANDROID_NAME_WORDS }
    }

    data class UsbInterfaceInfo(
        val interfaceClass: Int,
        val subclass: Int,
        val protocol: Int,
        val hasBulkEndpoint: Boolean = true,
    )

    data class UsbDeviceInfo(
        val vendorId: Int,
        val productId: Int,
        val interfaces: List<UsbInterfaceInfo>,
        /** The USB product string ("iPhone", "iPad"), when Android could read it. */
        val productName: String? = null,
    )

    data class BluetoothDeviceInfo(
        val address: String,
        val name: String?,
        /** Service UUIDs from the last SDP query; empty when the stack has none cached. */
        val uuids: Set<UUID>,
        /** BluetoothClass major device class, or null when unknown. */
        val majorClass: Int?,
    )

    /**
     * Any Apple device goes to CarPlay, as in DiPlay. Otherwise the device must look like an
     * Android phone: already in accessory mode, or exposing one of the interfaces DiAuto's
     * UsbDeviceCompat accepts (AOA, MTP, ADB, RNDIS, IAD, PTP). Flash drives, the CH341 MFi
     * bridge and other peripherals stay UNKNOWN and are ignored.
     */
    /** An iPad or iPod: by its product string, else by the product IDs current ones use (0x12AB iPad, 0x12AA iPod). */
    fun isAppleNonPhone(device: UsbDeviceInfo): Boolean = device.vendorId == APPLE_VENDOR_ID && (
        device.productName?.let { it.contains("iPad", ignoreCase = true) || it.contains("iPod", ignoreCase = true) }
            ?: (device.productId == 0x12AB || device.productId == 0x12AA))

    fun classifyUsb(device: UsbDeviceInfo): PhoneKind = when {
        // CarPlay runs only on an iPhone; an iPad refuses the CarPlay configuration and the
        // session retried forever (Galaxy Tab, 2026-10-27).
        isAppleNonPhone(device) -> PhoneKind.UNKNOWN
        device.vendorId == APPLE_VENDOR_ID -> PhoneKind.IPHONE
        isAccessoryMode(device) -> PhoneKind.ANDROID
        device.interfaces.any(::isAndroidInterface) -> PhoneKind.ANDROID
        // A bare vendor-specific bulk interface is what some phones show in charging-only mode,
        // but USB Ethernet adapters (Realtek r8152 in a USB-C dock) look the same, so it only
        // counts for phone makers (found on the user's tablet, 2026-10-09).
        device.vendorId in ANDROID_PHONE_VENDOR_IDS && device.interfaces.any(::isVendorBulk) -> PhoneKind.ANDROID
        else -> PhoneKind.UNKNOWN
    }

    private fun isVendorBulk(i: UsbInterfaceInfo) =
        i.interfaceClass == 0xFF && i.subclass == 0xFF && i.protocol == 0x00 && i.hasBulkEndpoint

    /** Phone makers whose devices may show only a vendor-specific interface (keep in step with xml/carlink_usb_device_filter). */
    private val ANDROID_PHONE_VENDOR_IDS = setOf(
        0x04E8, // Samsung
        0x18D1, // Google
        0x2717, // Xiaomi / Redmi / POCO
        0x2A70, // OnePlus
        0x22D9, // OPPO / realme
        0x2D95, // vivo
        0x22B8, // Motorola
        0x0FCE, // Sony
        0x12D1, // Huawei / Honor
    )

    fun isAccessoryMode(device: UsbDeviceInfo): Boolean =
        device.vendorId == GOOGLE_VENDOR_ID && device.productId in AOA_PRODUCT_IDS

    private fun isAndroidInterface(i: UsbInterfaceInfo): Boolean = when {
        i.interfaceClass == 0xFF && i.subclass == 0x42 && i.protocol == 0x01 -> true // ADB
        i.interfaceClass == 0xE0 && i.subclass == 0x01 && i.protocol == 0x03 -> true // RNDIS
        i.interfaceClass == 0xEF && i.subclass == 0x04 && i.protocol == 0x01 -> true // IAD
        i.interfaceClass == 0x06 && i.subclass == 0x01 && i.protocol == 0x01 -> true // PTP
        else -> false
    }

    /**
     * Strongest evidence first:
     * 1. a phone the driver or a previous session already identified ([remembered]);
     * 2. the iAP2 service UUID, which only Apple devices publish;
     * 3. an "iPhone"/"iPad" device name (the default name, and DiPlay's own picker rule);
     * 4. the Android Auto service UUID, which wireless Android Auto itself needs;
     * 5. (CarLink) OBEX Object Push, which every Android phone publishes and no iPhone does;
     * 6. (CarLink) an Android phone's default name (Xiaomi, Galaxy, Pixel, SM-…);
     * 7. (CarLink, as v1.6.5) a phone's records with more than media streaming and no iAP2.
     * Anything else stays UNKNOWN and Home asks the driver once: hands-free, contacts and messages are
     * on iPhones too, and iOS 27 iPhones with a custom name sent no iAP2 record (GitHub #89, #90).
     */
    fun classifyBluetooth(device: BluetoothDeviceInfo, remembered: PhoneKind?): PhoneKind {
        // iAP2 is proof of an Apple device, so it overrides an older weak guess of ANDROID (2026-11-08:
        // an iPhone guessed ANDROID from an SDP record without iAP2 was sent to Android Auto for good).
        if (IAP2_BLUETOOTH_UUID in device.uuids) return PhoneKind.IPHONE
        if (remembered != null && remembered != PhoneKind.UNKNOWN) return remembered
        val name = device.name.orEmpty().lowercase(Locale.ROOT)
        if ("iphone" in name || "ipad" in name || "آيفون" in name || "ايفون" in name) return PhoneKind.IPHONE
        if (ANDROID_AUTO_BLUETOOTH_UUID in device.uuids) return PhoneKind.ANDROID
        // CarLink: positive Android evidence that is there on the first pairing too, only for a
        // device that says it is a phone (a Galaxy Watch or a laptop publishes Object Push as well).
        val phoneClass = device.majorClass == null || device.majorClass == BLUETOOTH_MAJOR_PHONE ||
            device.majorClass == BLUETOOTH_MAJOR_UNCATEGORIZED
        if (phoneClass && OBEX_OBJECT_PUSH_UUID in device.uuids) return PhoneKind.ANDROID
        if (phoneClass && looksLikeAndroidName(device.name)) return PhoneKind.ANDROID
        // CarLink: restored from v1.6.5 (removed upstream in 1.0.18, 0b89bd22): a phone whose records
        // hold more than media streaming and no iAP2 is taken as Android. Upstream dropped it because
        // an iOS 27 iPhone with a custom name sent no iAP2 record (#89, #90); here the driver's own
        // phones are Android, iAP2 and an iPhone name still decide first, and the kind can be changed
        // in the phone's details. Media streaming alone still tells nothing (2026-11-08).
        if (device.majorClass == null || device.majorClass == BLUETOOTH_MAJOR_PHONE) {
            if (device.uuids.any { it !in MEDIA_ONLY_UUIDS }) return PhoneKind.ANDROID
        }
        return PhoneKind.UNKNOWN
    }

    /** A2DP source/sink/advanced audio and AVRCP (0x110A-0x110F): every phone has them. */
    private val MEDIA_ONLY_UUIDS: Set<UUID> = (0x110A..0x110F)
        .map { UUID.fromString("0000%04x-0000-1000-8000-00805f9b34fb".format(it)) }.toSet()

    /**
     * Whether a kind is backed by proof worth remembering (an iPhone's iAP2 or name; Android Auto's
     * record, Object Push or an Android default name), as opposed to the v1.6.5 "records without
     * iAP2" reading, which is re-made from fresh records each time instead of being stored.
     */
    fun isProof(device: BluetoothDeviceInfo, kind: PhoneKind): Boolean = when (kind) {
        PhoneKind.IPHONE -> true
        PhoneKind.ANDROID -> ANDROID_AUTO_BLUETOOTH_UUID in device.uuids || OBEX_OBJECT_PUSH_UUID in device.uuids ||
            looksLikeAndroidName(device.name)
        else -> false
    }
}
