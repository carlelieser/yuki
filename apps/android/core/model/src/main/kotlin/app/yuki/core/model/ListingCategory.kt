package app.yuki.core.model

enum class ListingCategory(val wireValue: String, val label: String) {
    SystemTweaks("system_tweaks", "System"),
    AppManagement("app_management", "Apps"),
    FileManagement("file_management", "Files"),
    Media("media", "Media"),
    Gaming("gaming", "Gaming"),
    Automation("automation", "Automation"),
    Networking("networking", "Network"),
    PrivacySecurity("privacy_security", "Privacy"),
    DeveloperTools("developer_tools", "Developer"),
    DeviceSpecific("device_specific", "Device"),
    Customization("customization", "Customization"),
    Connectivity("connectivity", "Connectivity"),
    Utilities("utilities", "Utilities"),
}

fun readListingCategory(raw: String?): ListingCategory? =
    ListingCategory.entries.firstOrNull { category -> category.wireValue == raw }
